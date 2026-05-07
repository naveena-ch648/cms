import React, { useState } from 'react';
import type { FileInfo } from '../types/file';
import { WorkflowStateBadge } from './WorkflowStateBadge';

interface FileListProps {
  files: FileInfo[];
  onFileClick: (file: FileInfo) => void;
  onContextMenu?: (file: FileInfo, e: React.MouseEvent) => void;
  onSummarize?: (file: FileInfo) => void;
  onBulkEdit?: (fileIds: string[]) => void;
  loading?: boolean;
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]!}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

const FileList: React.FC<FileListProps> = ({ files, onFileClick, onContextMenu, onSummarize, onBulkEdit, loading }) => {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const toggleSelect = (fileId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(fileId)) next.delete(fileId);
      else next.add(fileId);
      return next;
    });
  };

  const toggleAll = () => {
    if (selectedIds.size === files.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(files.map(f => f.id)));
    }
  };

  if (loading) {
    return <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>Loading files...</div>;
  }

  if (files.length === 0) {
    return <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>No files in this folder</div>;
  }

  return (
    <div>
      {selectedIds.size > 0 && onBulkEdit && (
        <div style={{ padding: '8px 12px', background: '#e3f2fd', borderRadius: 4, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 13 }}>{selectedIds.size} file(s) selected</span>
          <button
            onClick={() => onBulkEdit(Array.from(selectedIds))}
            style={{ padding: '4px 12px', fontSize: 12, cursor: 'pointer', background: '#1976d2', color: '#fff', border: 'none', borderRadius: 4 }}
          >
            Bulk Edit
          </button>
          <button
            onClick={() => setSelectedIds(new Set())}
            style={{ padding: '4px 12px', fontSize: 12, cursor: 'pointer', background: '#fff', border: '1px solid #ddd', borderRadius: 4 }}
          >
            Clear
          </button>
        </div>
      )}
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
      <thead>
        <tr style={{ borderBottom: '2px solid #eee', textAlign: 'left' }}>
          {onBulkEdit && (
            <th style={{ padding: '8px 4px', width: 32 }}>
              <input type="checkbox" checked={selectedIds.size === files.length && files.length > 0} onChange={toggleAll} />
            </th>
          )}
          <th style={{ padding: '8px 12px' }}>Name</th>
          <th style={{ padding: '8px 12px' }}>Size</th>
          <th style={{ padding: '8px 12px' }}>Status</th>
          <th style={{ padding: '8px 12px' }}>Type</th>
          <th style={{ padding: '8px 12px' }}>Uploaded by</th>
          <th style={{ padding: '8px 12px' }}>Date</th>
          {onSummarize && <th style={{ padding: '8px 12px' }}>Actions</th>}
        </tr>
      </thead>
      <tbody>
        {files.map(file => (
          <tr
            key={file.id}
            onClick={() => onFileClick(file)}
            onContextMenu={(e) => { e.preventDefault(); onContextMenu?.(file, e); }}
            style={{ borderBottom: '1px solid #f0f0f0', cursor: 'pointer', background: selectedIds.has(file.id) ? '#f3f8ff' : undefined }}
          >
            {onBulkEdit && (
              <td style={{ padding: '8px 4px', width: 32 }}>
                <input type="checkbox" checked={selectedIds.has(file.id)} onClick={(e) => toggleSelect(file.id, e)} readOnly />
              </td>
            )}
            <td style={{ padding: '8px 12px' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                {file.thumbnailUrl ? (
                  <img
                    src={file.thumbnailUrl}
                    alt=""
                    style={{ width: 24, height: 24, objectFit: 'cover', borderRadius: 2 }}
                    onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                  />
                ) : (
                  <span>{getMimeIcon(file.mimeType)}</span>
                )}
                <span>{file.name}</span>
              </span>
            </td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{formatSize(file.sizeBytes)}</td>
            <td style={{ padding: '8px 12px' }}>
              {file.workflowState && <WorkflowStateBadge state={file.workflowState} />}
            </td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{file.mimeType.split('/')[1]}</td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{file.uploadedBy.name}</td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{formatDate(file.createdAt)}</td>
            {onSummarize && (
              <td style={{ padding: '8px 12px' }}>
                <button
                  onClick={(e) => { e.stopPropagation(); onSummarize(file); }}
                  style={{
                    padding: '4px 8px',
                    fontSize: '11px',
                    backgroundColor: '#f3f4f6',
                    border: '1px solid #d1d5db',
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                  title="Summarize with AI"
                >
                  Summarize
                </button>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
    </div>
  );
};

function getMimeIcon(mimeType: string): string {
  if (mimeType.startsWith('image/')) return '🖼️';
  if (mimeType === 'application/pdf') return '📄';
  if (mimeType.startsWith('video/')) return '🎬';
  if (mimeType.startsWith('audio/')) return '🎵';
  if (mimeType.startsWith('text/')) return '📝';
  return '📎';
}

export default FileList;
