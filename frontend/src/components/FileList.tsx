import React from 'react';
import type { FileInfo } from '../types/file';

interface FileListProps {
  files: FileInfo[];
  onFileClick: (file: FileInfo) => void;
  onContextMenu?: (file: FileInfo, e: React.MouseEvent) => void;
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

const FileList: React.FC<FileListProps> = ({ files, onFileClick, onContextMenu, loading }) => {
  if (loading) {
    return <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>Loading files...</div>;
  }

  if (files.length === 0) {
    return <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>No files in this folder</div>;
  }

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
      <thead>
        <tr style={{ borderBottom: '2px solid #eee', textAlign: 'left' }}>
          <th style={{ padding: '8px 12px' }}>Name</th>
          <th style={{ padding: '8px 12px' }}>Size</th>
          <th style={{ padding: '8px 12px' }}>Type</th>
          <th style={{ padding: '8px 12px' }}>Uploaded by</th>
          <th style={{ padding: '8px 12px' }}>Date</th>
        </tr>
      </thead>
      <tbody>
        {files.map(file => (
          <tr
            key={file.id}
            onClick={() => onFileClick(file)}
            onContextMenu={(e) => { e.preventDefault(); onContextMenu?.(file, e); }}
            style={{ borderBottom: '1px solid #f0f0f0', cursor: 'pointer' }}
          >
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
            <td style={{ padding: '8px 12px', color: '#666' }}>{file.mimeType.split('/')[1]}</td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{file.uploadedBy.name}</td>
            <td style={{ padding: '8px 12px', color: '#666' }}>{formatDate(file.createdAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
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
