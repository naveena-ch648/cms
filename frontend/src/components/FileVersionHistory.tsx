import { useState, useEffect, useCallback } from 'react';
import { fileVersionsApi } from '../api/fileVersions';
import type { FileVersion } from '../types/file';

interface FileVersionHistoryProps {
  fileId: string;
  onVersionRestored?: () => void;
}

export default function FileVersionHistory({ fileId, onVersionRestored }: FileVersionHistoryProps) {
  const [versions, setVersions] = useState<FileVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploadingVersion, setUploadingVersion] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

  const loadVersions = useCallback(async () => {
    try {
      setLoading(true);
      const result = await fileVersionsApi.listVersions(fileId);
      setVersions(result.content);
      setError(null);
    } catch {
      setError('Failed to load version history');
    } finally {
      setLoading(false);
    }
  }, [fileId]);

  useEffect(() => {
    loadVersions();
  }, [loadVersions]);

  const handleUploadVersion = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const changeNote = prompt('Enter a change note (optional):') || undefined;

    try {
      setUploadingVersion(true);
      setUploadProgress(0);
      await fileVersionsApi.uploadVersion(fileId, file, changeNote, setUploadProgress);
      await loadVersions();
      onVersionRestored?.();
    } catch {
      setError('Failed to upload new version');
    } finally {
      setUploadingVersion(false);
      e.target.value = '';
    }
  };

  const handleRestore = async (versionId: string) => {
    if (!confirm('Restore this version? This will create a new version with the restored content.')) return;

    try {
      await fileVersionsApi.restoreVersion(fileId, versionId);
      await loadVersions();
      onVersionRestored?.();
    } catch {
      setError('Failed to restore version');
    }
  };

  const handleDownload = async (versionId: string) => {
    try {
      const url = await fileVersionsApi.downloadVersion(versionId, versionId);
      window.open(url, '_blank');
    } catch {
      // Fallback: direct link
      window.open(`/api/files/${fileId}/versions/${versionId}/download`, '_blank');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (loading) {
    return <div className="p-4 text-gray-500">Loading version history...</div>;
  }

  if (error) {
    return <div className="p-4 text-red-500">{error}</div>;
  }

  return (
    <div className="version-history">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-700">
          Version History ({versions.length})
        </h3>
        <label className="cursor-pointer text-sm text-blue-600 hover:text-blue-800">
          {uploadingVersion ? `Uploading... ${uploadProgress}%` : 'Upload New Version'}
          <input
            type="file"
            className="hidden"
            onChange={handleUploadVersion}
            disabled={uploadingVersion}
          />
        </label>
      </div>

      <div className="space-y-2">
        {versions.map((version) => (
          <div
            key={version.id}
            className={`border rounded p-3 ${version.isCurrent ? 'border-blue-300 bg-blue-50' : 'border-gray-200'}`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium">v{version.versionNumber}</span>
                {version.isCurrent && (
                  <span className="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded">Current</span>
                )}
              </div>
              <div className="flex gap-1">
                <button
                  onClick={() => handleDownload(version.id)}
                  className="text-xs text-gray-600 hover:text-gray-800 px-2 py-1"
                  title="Download this version"
                >
                  Download
                </button>
                {!version.isCurrent && (
                  <button
                    onClick={() => handleRestore(version.id)}
                    className="text-xs text-orange-600 hover:text-orange-800 px-2 py-1"
                    title="Restore this version"
                  >
                    Restore
                  </button>
                )}
              </div>
            </div>
            <div className="mt-1 text-xs text-gray-500">
              <span>{version.uploadedBy.name}</span>
              <span className="mx-1">·</span>
              <span>{formatDate(version.createdAt)}</span>
              <span className="mx-1">·</span>
              <span>{formatSize(version.sizeBytes)}</span>
            </div>
            {version.changeNote && (
              <div className="mt-1 text-xs text-gray-600 italic">{version.changeNote}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
