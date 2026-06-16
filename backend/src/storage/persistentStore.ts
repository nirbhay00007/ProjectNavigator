/**
 * persistentStore.ts  (v2 — PostgreSQL-backed)
 *
 * This module previously wrote .dev-clash/ JSON files to disk.
 * It now delegates ALL persistence to the Java backend via javaStore.ts.
 *
 * The public API is kept identical so the rest of the codebase
 * (pipeline.ts, index.ts) requires zero changes.
 */

import crypto from 'crypto';
import { FileSummary } from '../ai/geminiIntelligence';
import { NodeMetadata, Edge } from './graphStore';
import * as JavaStore from './javaStore';

// ─── Re-export types consumed by other modules ────────────────────────────────

export interface RepoMeta {
    repoPath: string;
    repoHash: string;
    analyzedAt: string;
    fileCount: number;
    durationMs: number;
    language?: string;
}

export interface PersistedGraph {
    meta: RepoMeta;
    nodes: NodeMetadata[];
    edges: Edge[];
}

export interface RichVectorDoc {
    filePath: string;
    fileBasename: string;
    compositeText: string;
    summary: string;
    keyExports: string[];
    patterns: string[];
    externalDeps: string[];
    complexity: string;
    isEntryPoint: boolean;
    responsibility: string;
    internalCalls: string[];
    vector: number[];
}

export interface PersistedVectors {
    meta: Pick<RepoMeta, 'repoPath' | 'analyzedAt'>;
    docs: RichVectorDoc[];
}

export interface SummaryCache {
    [cacheKey: string]: FileSummary;
}

// ─── PersistentStore (PostgreSQL-backed) ──────────────────────────────────────

export class PersistentStore {
    private readonly repoPath: string;
    private readonly repoHash: string;
    /** repoId is derived from the repoHash — keeps parity with old behaviour */
    private readonly repoId: string;
    private readonly token?: string;

    constructor(repoPath: string, token?: string) {
        this.repoPath = repoPath;
        this.repoHash = crypto.createHash('sha256').update(repoPath).digest('hex').slice(0, 12);
        this.repoId   = this.repoHash;
        this.token    = token;
        console.log(`[PersistentStore] Initialised — repoId: ${this.repoId} (PostgreSQL-backed)`);
    }

    get dirPath(): string { return `postgres://${this.repoId}`; } // No-op, kept for compat

    // ── Cache (SHA-256 hash → FileSummary) ────────────────────────────────────

    async getCachedAsync(filePath: string, contentHash: string): Promise<FileSummary | null> {
        try {
            return await JavaStore.getCachedSummary(filePath, contentHash, this.token);
        } catch (err: any) {
            console.warn(`[PersistentStore] Cache GET failed: ${err?.message}`);
            return null;
        }
    }

    /**
     * Synchronous shim — kept for compatibility with pipeline.ts.
     * In the new PostgreSQL-backed flow, cache lookups are async.
     * Callers should migrate to getCachedAsync() for production.
     */
    getCached(_filePath: string, _contentHash: string): FileSummary | null {
        // Synchronous lookup no longer possible with a remote DB.
        // pipeline.ts has been updated to use getCachedAsync().
        return null;
    }

    async setCachedAsync(filePath: string, contentHash: string, summary: FileSummary): Promise<void> {
        try {
            await JavaStore.setCachedSummary(filePath, contentHash, summary, this.token);
        } catch (err: any) {
            console.warn(`[PersistentStore] Cache SET failed: ${err?.message}`);
        }
    }

    setCached(filePath: string, contentHash: string, summary: FileSummary): void {
        // Fire-and-forget async write — keeps synchronous callers working
        this.setCachedAsync(filePath, contentHash, summary).catch(() => {});
    }

    clearCache(): void {
        console.log('[PersistentStore] clearCache() is a no-op in PostgreSQL-backed mode — delete the repo to clear its cache.');
    }

    // ── Graph Persistence ─────────────────────────────────────────────────────

    async saveGraph(nodes: NodeMetadata[], edges: Edge[], durationMs: number): Promise<void> {
        try {
            // Only persist Java source files — other file types (TS, JS, configs, XML…)
            // are used in-memory for graph rendering but don't need DB storage.
            const javaNodes = nodes.filter(n => n.id.endsWith('.java'));
            const javaNodeIds = new Set(javaNodes.map(n => n.id));
            const javaEdges = edges.filter(e => javaNodeIds.has(e.source) || javaNodeIds.has(e.target));

            if (javaNodes.length === 0) {
                console.log('[PersistentStore] No .java files found — skipping DB persist (non-Java repo).');
                // Still register the repo record for metadata
                await JavaStore.upsertRepository({
                    id:         this.repoId,
                    repoLabel:  nodes[0]?.repoLabel ?? this.repoId,
                    repoPath:   this.repoPath,
                    repoUrl:    nodes[0]?.repoUrl,
                    fileCount:  nodes.length,
                    durationMs,
                }, this.token);
                return;
            }

            await JavaStore.upsertRepository({
                id:         this.repoId,
                repoLabel:  javaNodes[0]?.repoLabel ?? this.repoId,
                repoPath:   this.repoPath,
                repoUrl:    javaNodes[0]?.repoUrl,
                fileCount:  javaNodes.length,
                durationMs,
            }, this.token);
            await JavaStore.saveFileNodes(this.repoId, javaNodes, this.token);
            await JavaStore.saveEdges(this.repoId, javaEdges, this.token);
            console.log(`[PersistentStore] Java files saved to PostgreSQL — ${javaNodes.length} nodes, ${javaEdges.length} edges (filtered from ${nodes.length} total files)`);
        } catch (err: any) {
            console.error(`[PersistentStore] saveGraph failed: ${err?.message}`);
            throw err;
        }
    }

    async loadGraph(): Promise<PersistedGraph | null> {
        try {
            const nodes = await JavaStore.loadFileNodes(this.repoId, this.token);
            if (nodes.length === 0) return null;
            const edges = await JavaStore.loadEdges(this.repoId, this.token);
            return {
                meta: {
                    repoPath:   this.repoPath,
                    repoHash:   this.repoHash,
                    analyzedAt: new Date().toISOString(),
                    fileCount:  nodes.length,
                    durationMs: 0,
                },
                nodes,
                edges,
            };
        } catch (err: any) {
            console.warn(`[PersistentStore] loadGraph failed: ${err?.message}`);
            return null;
        }
    }

    // ── Vector Store Persistence ──────────────────────────────────────────────

    async saveVectors(docs: RichVectorDoc[]): Promise<void> {
        try {
            await JavaStore.saveVectors(this.repoId, docs, this.token);
            console.log(`[PersistentStore] ${docs.length} vectors saved to PostgreSQL`);
        } catch (err: any) {
            console.error(`[PersistentStore] saveVectors failed: ${err?.message}`);
            throw err;
        }
    }

    async loadVectors(): Promise<RichVectorDoc[]> {
        try {
            return await JavaStore.loadVectors(this.repoId, this.token);
        } catch (err: any) {
            console.warn(`[PersistentStore] loadVectors failed: ${err?.message}`);
            return [];
        }
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    async deleteRepository(): Promise<void> {
        try {
            await JavaStore.deleteRepository(this.repoId, this.token);
            console.log(`[PersistentStore] Repository ${this.repoId} deleted from PostgreSQL.`);
        } catch (err: any) {
            console.warn(`[PersistentStore] deleteRepository failed: ${err?.message}`);
        }
    }

    // ── Meta ──────────────────────────────────────────────────────────────────

    saveMeta(_meta: RepoMeta): void {
        // Meta is now saved as part of saveGraph() / upsertRepository()
    }

    loadMeta(): RepoMeta | null {
        // No longer used — meta is embedded in the repository table
        return null;
    }
}

// ─── Singleton management ─────────────────────────────────────────────────────

let _activeStore: PersistentStore | null = null;

export function initStore(repoPath: string, token?: string): PersistentStore {
    _activeStore = new PersistentStore(repoPath, token);
    return _activeStore;
}

export function getStore(): PersistentStore {
    if (!_activeStore) throw new Error('[PersistentStore] Store not initialised. Call initStore(repoPath) first.');
    return _activeStore;
}
