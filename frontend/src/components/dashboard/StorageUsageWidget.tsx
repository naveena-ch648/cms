import { useEffect, useState } from 'react';
import { getDashboardSummary } from '../../api/dashboard';

export default function StorageUsageWidget() {
  const [storageUsedBytes, setStorageUsedBytes] = useState(0);
  const [storageMaxBytes, setStorageMaxBytes] = useState(0);
  const [storagePercentage, setStoragePercentage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboardSummary()
      .then((summary) => {
        setStorageUsedBytes(summary.storageUsedBytes);
        setStorageMaxBytes(summary.storageMaxBytes);
        setStoragePercentage(summary.storagePercentage);
      })
      .catch(() => setError('Failed to load storage info'))
      .finally(() => setLoading(false));
  }, []);

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  const getBarColor = () => {
    if (storagePercentage >= 95) return '#ef4444';
    if (storagePercentage >= 80) return '#f59e0b';
    return '#3b82f6';
  };

  const getStatusText = () => {
    if (storagePercentage >= 95) return 'Critical — storage almost full';
    if (storagePercentage >= 80) return 'Warning — storage is getting full';
    return '';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Storage Usage</h3>
        <div className="animate-pulse h-16 bg-gray-200 rounded" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Storage Usage</h3>
        <p className="text-red-500 text-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Storage Usage</h3>
      <div className="space-y-3">
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">
            {formatBytes(storageUsedBytes)} of {formatBytes(storageMaxBytes)} used
          </span>
          <span className="font-medium" style={{ color: getBarColor() }}>
            {storagePercentage.toFixed(1)}%
          </span>
        </div>
        <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-500"
            style={{
              width: `${Math.min(storagePercentage, 100)}%`,
              backgroundColor: getBarColor(),
            }}
          />
        </div>
        {getStatusText() && (
          <p className="text-xs" style={{ color: getBarColor() }}>
            {getStatusText()}
          </p>
        )}
      </div>
    </div>
  );
}
