import { useState, useCallback, useRef } from 'react';
import { filesApi } from '../api/files';
import type { UploadProgress } from '../types/file';

const CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
const LARGE_FILE_THRESHOLD = 100 * 1024 * 1024; // 100 MB
const MAX_CONCURRENT_CHUNKS = 3;

interface UploadTask {
  file: File;
  folderId: string;
  sessionId?: string;
  paused: boolean;
  aborted: boolean;
  currentChunk: number;
}

export function useUploadManager() {
  const [uploads, setUploads] = useState<UploadProgress[]>([]);
  const tasksRef = useRef<Map<string, UploadTask>>(new Map());

  const updateProgress = useCallback((fileId: string, update: Partial<UploadProgress>) => {
    setUploads(prev => prev.map(u => u.fileId === fileId ? { ...u, ...update } : u));
  }, []);

  const uploadFile = useCallback(async (file: File, folderId: string) => {
    const fileId = crypto.randomUUID();
    const isLarge = file.size > LARGE_FILE_THRESHOLD;

    const progress: UploadProgress = {
      fileId, fileName: file.name, totalBytes: file.size,
      uploadedBytes: 0, percentage: 0, status: 'uploading', startedAt: Date.now(),
    };
    setUploads(prev => [...prev, progress]);

    const task: UploadTask = { file, folderId, paused: false, aborted: false, currentChunk: 0 };
    tasksRef.current.set(fileId, task);

    try {
      if (isLarge) {
        await uploadChunked(fileId, task);
      } else {
        await uploadSimple(fileId, task);
      }
    } catch (err) {
      if (!task.aborted) {
        updateProgress(fileId, { status: 'failed', error: err instanceof Error ? err.message : 'Upload failed' });
      }
    }
  }, [updateProgress]);

  const uploadSimple = async (fileId: string, task: UploadTask) => {
    await filesApi.uploadFile(task.file, task.folderId, {}, (pct) => {
      updateProgress(fileId, { percentage: pct, uploadedBytes: Math.round(task.file.size * pct / 100) });
    });
    updateProgress(fileId, { status: 'completed', percentage: 100, uploadedBytes: task.file.size });
    tasksRef.current.delete(fileId);
  };

  const uploadChunked = async (fileId: string, task: UploadTask) => {
    const totalChunks = Math.ceil(task.file.size / CHUNK_SIZE);

    // Initiate
    const session = await filesApi.initiateChunkedUpload({
      fileName: task.file.name,
      fileSize: task.file.size,
      mimeType: task.file.type || 'application/octet-stream',
      folderId: task.folderId,
      chunkSize: CHUNK_SIZE,
    });
    task.sessionId = session.sessionId;

    // Upload chunks with concurrency limit
    let completedChunks = 0;
    const chunkQueue: number[] = Array.from({ length: totalChunks }, (_, i) => i);

    const uploadNextChunk = async (): Promise<void> => {
      while (chunkQueue.length > 0) {
        if (task.paused || task.aborted) return;

        const chunkNum = chunkQueue.shift()!;
        const start = chunkNum * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, task.file.size);
        const chunk = await task.file.slice(start, end).arrayBuffer();

        await filesApi.uploadChunk(session.sessionId, chunkNum, chunk);
        completedChunks++;

        const pct = Math.round((completedChunks / totalChunks) * 100);
        const uploaded = Math.min(completedChunks * CHUNK_SIZE, task.file.size);
        const elapsed = (Date.now() - (progress?.startedAt || Date.now())) / 1000;
        const rate = uploaded / elapsed;
        const remaining = rate > 0 ? (task.file.size - uploaded) / rate : undefined;

        updateProgress(fileId, { percentage: pct, uploadedBytes: uploaded, estimatedTimeRemaining: remaining });
      }
    };

    // Access progress for ETA calc
    const progress = uploads.find(u => u.fileId === fileId);

    const workers = Array.from({ length: MAX_CONCURRENT_CHUNKS }, () => uploadNextChunk());
    await Promise.all(workers);

    if (task.aborted) return;

    // Complete
    await filesApi.completeChunkedUpload(session.sessionId);
    updateProgress(fileId, { status: 'completed', percentage: 100, uploadedBytes: task.file.size });
    tasksRef.current.delete(fileId);
  };

  const pause = useCallback((fileId: string) => {
    const task = tasksRef.current.get(fileId);
    if (task) {
      task.paused = true;
      updateProgress(fileId, { status: 'paused' });
    }
  }, [updateProgress]);

  const resume = useCallback((fileId: string) => {
    const task = tasksRef.current.get(fileId);
    if (task) {
      task.paused = false;
      updateProgress(fileId, { status: 'uploading' });
      // Re-trigger chunked upload from current position
      if (task.sessionId) {
        uploadChunked(fileId, task).catch(err => {
          updateProgress(fileId, { status: 'failed', error: err.message });
        });
      }
    }
  }, [updateProgress]);

  const cancel = useCallback((fileId: string) => {
    const task = tasksRef.current.get(fileId);
    if (task) {
      task.aborted = true;
      if (task.sessionId) {
        filesApi.abortUpload(task.sessionId).catch(() => {});
      }
      updateProgress(fileId, { status: 'failed', error: 'Cancelled' });
      tasksRef.current.delete(fileId);
    }
  }, [updateProgress]);

  const dismiss = useCallback((fileId: string) => {
    setUploads(prev => prev.filter(u => u.fileId !== fileId));
  }, []);

  return { uploads, uploadFile, pause, resume, cancel, dismiss };
}
