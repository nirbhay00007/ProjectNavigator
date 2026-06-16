/**
 * javaStore.ts
 *
 * HTTP client for the Java backend's /persist/** REST API.
 * All storage operations (graph, vectors, cache) go through here
 * instead of writing JSON files to .dev-clash/ on disk.
 *
 * Uses native fetch (Node 18+) — no extra npm dependency required.
 *
 * The Java backend (Spring Boot + PostgreSQL) is the single source of
 * truth for all persisted data. This module is the Node.js side of that contract.
 */

import { NodeMetadata, Edge } from './graphStore';
import { RichVectorDoc } from './persistentStore';
import { FileSummary } from '../ai/geminiIntelligence';

const JAVA_BASE = process.env.JAVA_BACKEND_URL ?? 'http://localhost:8080';


// ─── Helpers ──────────────────────────────────────────────────────────────────

async function post(path: string, body: unknown, token?: string): Promise<unknown> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = token;
    }
    const res = await fetch(`${JAVA_BASE}${path}`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
    });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(`[JavaStore] POST ${path} failed (${res.status}): ${text}`);
    }
    return res.json();
}

async function get(path: string, token?: string): Promise<unknown> {
    const headers: Record<string, string> = {};
    if (token) {
        headers['Authorization'] = token;
    }
    const res = await fetch(`${JAVA_BASE}${path}`, { headers });
    if (!res.ok) {
        if (res.status === 404) return null;
        const text = await res.text();
        throw new Error(`[JavaStore] GET ${path} failed (${res.status}): ${text}`);
    }
    return res.json();
}

async function del(path: string, token?: string): Promise<void> {
    const headers: Record<string, string> = {};
    if (token) {
        headers['Authorization'] = token;
    }
    const res = await fetch(`${JAVA_BASE}${path}`, { method: 'DELETE', headers });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(`[JavaStore] DELETE ${path} failed (${res.status}): ${text}`);
    }
}

// ─── Repository ───────────────────────────────────────────────────────────────

export interface RepoRecord {
    id: string;
    repoLabel: string;
    repoPath?: string;
    repoUrl?: string;
    language?: string;
    fileCount: number;
    durationMs: number;
}

export async function upsertRepository(repo: RepoRecord, token?: string): Promise<void> {
    await post('/persist/repository', repo, token);
}

export async function deleteRepository(repoId: string, token?: string): Promise<void> {
    await del(`/persist/repository/${repoId}`, token);
}

export async function listRepositories(token?: string): Promise<RepoRecord[]> {
    const rows = (await get('/persist/repositories', token)) as any[] | null;
    if (!rows) return [];
    return rows.map(row => ({
        id:         row.id,
        repoLabel:  row.repoLabel ?? '',
        repoPath:   row.repoPath ?? '',
        repoUrl:    row.repoUrl,
        language:   row.language,
        fileCount:  row.fileCount ?? 0,
        durationMs: row.durationMs ?? 0,
    }));
}


// ─── File Nodes ───────────────────────────────────────────────────────────────

export async function saveFileNodes(repoId: string, nodes: NodeMetadata[], token?: string): Promise<void> {
    if (nodes.length === 0) return;
    await post('/persist/file-nodes', { repoId, nodes }, token);
}

export async function loadFileNodes(repoId: string, token?: string): Promise<NodeMetadata[]> {
    const rows = (await get(`/persist/file-nodes/${repoId}`, token)) as any[] | null;
    if (!rows) return [];
    return rows.map(row => ({
        id:                row.id,
        repoId:            row.repoId,
        repoLabel:         row.repoLabel ?? '',
        repoPath:          row.repoPath ?? '',
        repoUrl:           row.repoUrl,
        summary:           row.summary ?? '',
        responsibility:    row.responsibility ?? '',
        isEntryPoint:      row.entryPoint ?? row.isEntryPoint ?? false,
        keyExports:        parseJson(row.keyExports, []),
        internalCalls:     parseJson(row.internalCalls, []),
        complexity:        row.complexity ?? 'low',
        layer:             row.layer ?? 'unknown',
        riskCategory:      row.riskCategory ?? 'low',
        codeQuality:       row.codeQuality ?? 'acceptable',
        patterns:          parseJson(row.patterns, []),
        externalDeps:      parseJson(row.externalDeps, []),
        commitChurn:       row.commitChurn ?? 0,
        inboundEdgeCount:  row.fanIn ?? 0,
        outboundEdgeCount: row.fanOut ?? 0,
        isOrphan:          row.orphan ?? row.isOrphan ?? false,
    }));
}

// ─── Dependency Edges ─────────────────────────────────────────────────────────

export async function saveEdges(repoId: string, edges: Edge[], token?: string): Promise<void> {
    if (edges.length === 0) return;
    await post('/persist/edges', { repoId, edges }, token);
}

export async function loadEdges(repoId: string, token?: string): Promise<Edge[]> {
    const rows = (await get(`/persist/edges/${repoId}`, token)) as any[] | null;
    if (!rows) return [];
    return rows.map(row => ({ source: row.source, target: row.target }));
}

// ─── Vector Embeddings ────────────────────────────────────────────────────────

export async function saveVectors(repoId: string, docs: RichVectorDoc[], token?: string): Promise<void> {
    if (docs.length === 0) return;
    await post('/persist/vectors', { repoId, docs }, token);
}

export async function loadVectors(repoId: string, token?: string): Promise<RichVectorDoc[]> {
    const rows = (await get(`/persist/vectors/${repoId}`, token)) as any[] | null;
    if (!rows) return [];
    return rows.map(row => ({
        filePath:       row.fileNodeId,
        fileBasename:   row.fileBasename ?? '',
        compositeText:  row.compositeText ?? '',
        summary:        row.summary ?? '',
        keyExports:     parseJson(row.keyExports, []),
        patterns:       parseJson(row.patterns, []),
        externalDeps:   [],
        complexity:     row.complexity ?? 'low',
        isEntryPoint:   row.entryPoint ?? row.isEntryPoint ?? false,
        responsibility: row.responsibility ?? '',
        internalCalls:  parseJson(row.internalCalls, []),
        vector:         parseJson(row.vectorJson, []),
    }));
}

export async function upsertSingleVector(repoId: string, doc: RichVectorDoc, token?: string): Promise<void> {
    await post('/persist/vectors/upsert-one', { repoId, doc }, token);
}

// ─── File Summary Cache ───────────────────────────────────────────────────────

export async function getCachedSummary(filePath: string, contentHash: string, token?: string): Promise<FileSummary | null> {
    const cacheKey = encodeURIComponent(`${filePath}::${contentHash}`);
    const result = (await get(`/persist/cache/${cacheKey}`, token)) as { summaryJson: string } | null;
    if (!result) return null;
    try {
        return JSON.parse(result.summaryJson) as FileSummary;
    } catch {
        return null;
    }
}

export async function setCachedSummary(filePath: string, contentHash: string, summary: FileSummary, token?: string): Promise<void> {
    await post('/persist/cache', {
        filePath,
        contentHash,
        summaryJson: JSON.stringify(summary),
    }, token);
}

// ─── Utility ──────────────────────────────────────────────────────────────────

function parseJson<T>(value: unknown, fallback: T): T {
    if (value === null || value === undefined) return fallback;
    if (typeof value !== 'string') return value as unknown as T;
    try { return JSON.parse(value) as T; } catch { return fallback; }
}
