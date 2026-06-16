import http from 'http';
import path from 'path';

// ─── Types (mirrors Java GraphResponse) ──────────────────────────────────────

export interface JavaNode {
    id: string;     // Class name (e.g. "GraphService", from Java Node.id)
}

export interface JavaEdge {
    from: string;   // Source class name
    to: string;     // Target class name
}

export interface JavaGraphResponse {
    nodes: JavaNode[];
    edges: JavaEdge[];
    clonedPath: string;   // Local disk path where the repo was cloned
}

// ─── Config ───────────────────────────────────────────────────────────────────

const JAVA_BACKEND_URL = process.env.JAVA_BACKEND_URL ?? `http://${process.env.JAVA_BACKEND_HOST ?? 'localhost'}:${process.env.JAVA_BACKEND_PORT ?? 8080}`;
const JAVA_BACKEND_TIMEOUT_MS = 120_000; // 2 minutes — cloning can be slow

// ─── Health Check ─────────────────────────────────────────────────────────────

export async function isJavaBackendAlive(): Promise<boolean> {
    try {
        const controller = new AbortController();
        const id = setTimeout(() => controller.abort(), 3000);
        const res = await fetch(`${JAVA_BACKEND_URL}/repo/health`, { signal: controller.signal });
        clearTimeout(id);
        return res.ok;
    } catch {
        return false;
    }
}

// ─── Core Client ──────────────────────────────────────────────────────────────

/**
 * Calls the Java Spring Boot backend to:
 * 1. Clone the GitHub repo to a local temp folder.
 * 2. Parse all .java files with JavaParser AST.
 * 3. Return class-level dependency graph (nodes + edges).
 *
 * The returned `clonedPath` can then be passed directly into our
 * Ollama/Gemini summarisation pipeline as if it were a local repo.
 */
export async function cloneAndExtractJavaGraph(repoUrlOrPath: string, localMode = false): Promise<JavaGraphResponse> {
    const alive = await isJavaBackendAlive();
    if (!alive) {
        throw new Error(
            `Java backend is not reachable at ${JAVA_BACKEND_URL}. ` +
            `Ensure the service is running or check your JAVA_BACKEND_URL environment variable.`
        );
    }

    const encoded = encodeURIComponent(repoUrlOrPath);
    const apiPath = localMode
        ? `/repo/local?path=${encoded}`   // Already-cloned local directory
        : `/repo/graph?url=${encoded}`;   // GitHub URL — Spring Boot clones it

    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), JAVA_BACKEND_TIMEOUT_MS);

    try {
        const res = await fetch(`${JAVA_BACKEND_URL}${apiPath}`, {
            method: 'POST',
            signal: controller.signal
        });
        clearTimeout(id);

        if (!res.ok) {
            const data = await res.text();
            throw new Error(`Java backend returned HTTP ${res.status}: ${data}`);
        }

        return await res.json() as JavaGraphResponse;
    } catch (err: any) {
        if (err.name === 'AbortError') {
            throw new Error(`Java backend timed out after ${JAVA_BACKEND_TIMEOUT_MS / 1000}s`);
        }
        throw new Error(`Java backend connection error: ${err.message}`);
    }
}

// ─── Adapter: Java graph → our GraphNode format ────────────────────────────────

import { GraphNode } from './parser';

/**
 * Adapts the raw JavaParser output into the same `GraphNode[]` format
 * that the rest of the ML pipeline (Ollama, vector store, graph store)
 * already understands — making Java repos a full first-class citizen.
 */
import fs from 'fs';

function fastFindJavaFiles(dir: string, fileMap: Map<string, string>) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
        if (entry.isDirectory()) {
            if (!['.git', 'target', 'build', 'node_modules', 'test'].includes(entry.name)) {
                fastFindJavaFiles(path.join(dir, entry.name), fileMap);
            }
        } else if (entry.name.endsWith('.java')) {
            const className = entry.name.replace('.java', '');
            fileMap.set(className, path.join(dir, entry.name).replace(/\\/g, '/'));
        }
    }
}

export function adaptJavaGraphToGraphNodes(
    javaResponse: JavaGraphResponse,
    repoBasePath: string
): GraphNode[] {
    // 1. Scan the repo on disk to find where each Java class ACTUALLY lives.
    const realFileMap = new Map<string, string>();
    try { fastFindJavaFiles(repoBasePath, realFileMap); } catch {}

    // Build a node map: className → absolute path
    const nodeMap = new Map<string, string>();
    for (const node of javaResponse.nodes) {
        // Look up the physical file path, fallback to synthetic if not found
        const actualPath = realFileMap.get(node.id) || path.join(repoBasePath, `${node.id}.java`).replace(/\\/g, '/');
        nodeMap.set(node.id, actualPath);
    }

    // Build edge map: id → Set of import ids
    const importMap = new Map<string, Set<string>>();
    for (const edge of javaResponse.edges) {
        if (!importMap.has(edge.from)) importMap.set(edge.from, new Set());
        importMap.get(edge.from)!.add(edge.to);
    }

    // Produce final GraphNode[]
    const graphNodes: GraphNode[] = [];
    for (const node of javaResponse.nodes) {
        const id = nodeMap.get(node.id)!;
        const rawImports = Array.from(importMap.get(node.id) ?? []);
        const imports = rawImports
            .map(n => nodeMap.get(n))
            .filter(Boolean) as string[];

        graphNodes.push({ id, imports, rawImports });
    }

    return graphNodes;
}
