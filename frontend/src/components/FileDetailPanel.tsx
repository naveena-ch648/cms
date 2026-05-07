import React, { useEffect, useState, useCallback } from 'react';
import type { FileInfo } from '../types/file';
import type { WorkflowStateInfo } from '../types/workflow';
import FileVersionHistory from './FileVersionHistory';
import MetadataEditor from './metadata/MetadataEditor';
import TagInput from './metadata/TagInput';
import { WorkflowStateBadge } from './WorkflowStateBadge';
import { WorkflowTransitionMenu } from './WorkflowTransitionMenu';
import { WorkflowHistoryPanel } from './WorkflowHistoryPanel';
import { workflowApi } from '../api/workflow';
import apiClient from '../api/client';

interface EmbeddingStatus {
  status: string;
  indexed: boolean;
  chunkCount?: number;
  embeddingModel?: string;
  lastUpdated?: string;
  error?: string;
}

interface FileDetailPanelProps {
  file: FileInfo;
  onClose: () => void;
  onDownload: (file: FileInfo) => void;
  onPreview: (file: FileInfo) => void;
  onFileUpdated?: () => void;
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]!}`;
}

const FileDetailPanel: React.FC<FileDetailPanelProps> = ({ file, onClose, onDownload, onPreview, onFileUpdated }) => {
  const [embeddingStatus, setEmbeddingStatus] = useState<EmbeddingStatus | null>(null);
  const [workflowState, setWorkflowState] = useState<WorkflowStateInfo | null>(null);

  const fetchWorkflowState = useCallback(() => {
    workflowApi.getState(file.id)
      .then(res => setWorkflowState(res.data.data))
      .catch(() => setWorkflowState(null));
  }, [file.id]);

  useEffect(() => {
    apiClient.get<EmbeddingStatus>(`/api/qa/embedding-status/${file.id}`)
      .then(res => setEmbeddingStatus(res.data))
      .catch(() => setEmbeddingStatus(null));
    fetchWorkflowState();
  }, [file.id, fetchWorkflowState]);

  const getStatusBadge = () => {
    if (!embeddingStatus) return null;
    const { status } = embeddingStatus;
    const colors: Record<string, { bg: string; text: string }> = {
      COMPLETED: { bg: '#e6f4ea', text: '#1e7e34' },
      PROCESSING: { bg: '#fff3cd', text: '#856404' },
      PENDING: { bg: '#e2e3e5', text: '#383d41' },
      FAILED: { bg: '#f8d7da', text: '#721c24' },
    };
    const style = colors[status] || colors.PENDING!;
    return (
      <span style={{ padding: '2px 8px', borderRadius: 12, fontSize: 11, fontWeight: 600, backgroundColor: style!.bg, color: style!.text }}>
        {status === 'COMPLETED' ? '✓ Indexed' : status === 'PROCESSING' ? '⟳ Indexing' : status === 'FAILED' ? '✗ Failed' : '◌ Pending'}
      </span>
    );
  };

  return (
    <div style={{
      width: 320, borderLeft: '1px solid #e0e0e0', padding: 16, overflowY: 'auto',
      display: 'flex', flexDirection: 'column', gap: 12,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0, fontSize: 16 }}>{file.name}</h3>
        <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: 18 }}>✕</button>
      </div>

      <div style={{ fontSize: 13, color: '#666', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div><strong>Size:</strong> {formatSize(file.sizeBytes)}</div>
        <div><strong>Type:</strong> {file.mimeType}</div>
        <div><strong>Uploaded by:</strong> {file.uploadedBy.name}</div>
        <div><strong>Date:</strong> {new Date(file.createdAt).toLocaleString()}</div>
        {file.description && <div><strong>Description:</strong> {file.description}</div>}
        {file.tags && file.tags.length > 0 && (
          <div><strong>Tags:</strong> {file.tags.join(', ')}</div>
        )}
        <div><strong>Downloads:</strong> {file.downloadCount}</div>
      </div>

      {workflowState && (
        <div style={{ padding: 10, background: '#f8f9fa', borderRadius: 6, fontSize: 13 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <strong>Workflow</strong>
            <WorkflowStateBadge state={workflowState.currentState} />
          </div>
          <WorkflowTransitionMenu
            fileId={file.id}
            workspaceId={file.workspaceId}
            stateInfo={workflowState}
            onTransitioned={() => { fetchWorkflowState(); onFileUpdated?.(); }}
          />
        </div>
      )}

      {embeddingStatus && (
        <div style={{ padding: 10, background: '#f8f9fa', borderRadius: 6, fontSize: 13 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <strong>AI Index</strong>
            {getStatusBadge()}
          </div>
          {embeddingStatus.chunkCount != null && embeddingStatus.status === 'COMPLETED' && (
            <div style={{ color: '#666', fontSize: 12 }}>{embeddingStatus.chunkCount} chunks indexed</div>
          )}
          {embeddingStatus.error && (
            <div style={{ color: '#dc3545', fontSize: 12, marginTop: 4 }}>{embeddingStatus.error}</div>
          )}
        </div>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button onClick={() => onDownload(file)} style={{ flex: 1, padding: '8px 12px', cursor: 'pointer' }}>
          ⬇️ Download
        </button>
        {file.previewable && (
          <button onClick={() => onPreview(file)} style={{ flex: 1, padding: '8px 12px', cursor: 'pointer' }}>
            👁️ Preview
          </button>
        )}
      </div>

      <div style={{ marginTop: 16, borderTop: '1px solid #e0e0e0', paddingTop: 12 }}>
        <MetadataEditor fileId={file.id} workspaceId={file.workspaceId} />
      </div>

      <div style={{ marginTop: 16, borderTop: '1px solid #e0e0e0', paddingTop: 12 }}>
        <TagInput fileId={file.id} workspaceId={file.workspaceId} />
      </div>

      <div style={{ marginTop: 16, borderTop: '1px solid #e0e0e0', paddingTop: 12 }}>
        <FileVersionHistory fileId={file.id} onVersionRestored={onFileUpdated} />
      </div>

      <div style={{ marginTop: 16, borderTop: '1px solid #e0e0e0', paddingTop: 12 }}>
        <WorkflowHistoryPanel fileId={file.id} />
      </div>
    </div>
  );
};

export default FileDetailPanel;
