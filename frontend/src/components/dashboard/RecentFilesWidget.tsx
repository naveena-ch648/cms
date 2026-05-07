import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getRecentFiles } from '../../api/dashboard';
import type { RecentFile } from '../../types/dashboard';

export default function RecentFilesWidget() {
  const [files, setFiles] = useState<RecentFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    getRecentFiles(10)
      .then(setFiles)
      .catch(() => setError('Failed to load recent files'))
      .finally(() => setLoading(false));
  }, []);

  const handleFileClick = (file: RecentFile) => {
    navigate(`/workspaces/${file.workspaceId}/folders/${file.folderId}`);
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHr = Math.floor(diffMin / 60);
    if (diffHr < 24) return `${diffHr}h ago`;
    const diffDays = Math.floor(diffHr / 24);
    return `${diffDays}d ago`;
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType?.startsWith('image/')) return '🖼️';
    if (mimeType?.includes('pdf')) return '📄';
    if (mimeType?.includes('spreadsheet') || mimeType?.includes('excel')) return '📊';
    if (mimeType?.includes('presentation') || mimeType?.includes('powerpoint')) return '📽️';
    if (mimeType?.includes('word') || mimeType?.includes('document')) return '📝';
    if (mimeType?.startsWith('video/')) return '🎬';
    return '📁';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Recent Files</h3>
        <div className="animate-pulse space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-10 bg-gray-200 rounded" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Recent Files</h3>
        <p className="text-red-500 text-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Recent Files</h3>
      {files.length === 0 ? (
        <p className="text-gray-500 text-sm">No recent files</p>
      ) : (
        <ul className="space-y-2">
          {files.map((file) => (
            <li
              key={file.id}
              onClick={() => handleFileClick(file)}
              className="flex items-center gap-3 p-2 rounded hover:bg-gray-50 cursor-pointer"
            >
              <span className="text-xl">{getFileIcon(file.mimeType)}</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{file.name}</p>
                <p className="text-xs text-gray-500 truncate">
                  {file.workspaceName} • {file.folderPath}
                </p>
              </div>
              <span className="text-xs text-gray-400 whitespace-nowrap">
                {formatTime(file.lastAccessedAt || file.updatedAt)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
