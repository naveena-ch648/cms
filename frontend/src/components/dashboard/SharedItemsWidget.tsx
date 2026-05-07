import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSharedItems } from '../../api/dashboard';
import type { SharedItem } from '../../types/dashboard';

export default function SharedItemsWidget() {
  const [items, setItems] = useState<SharedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [direction, setDirection] = useState<'WITH_ME' | 'BY_ME'>('WITH_ME');
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    getSharedItems(direction, 10)
      .then(setItems)
      .catch(() => setError('Failed to load shared items'))
      .finally(() => setLoading(false));
  }, [direction]);

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / 86400000);
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Shared Items</h3>
        <div className="animate-pulse space-y-3">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="h-10 bg-gray-200 rounded" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Shared Items</h3>
        <p className="text-red-500 text-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Shared Items</h3>

      {/* Tabs */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setDirection('WITH_ME')}
          className={`px-3 py-1.5 text-sm rounded-md font-medium ${
            direction === 'WITH_ME'
              ? 'bg-blue-100 text-blue-700'
              : 'text-gray-500 hover:bg-gray-100'
          }`}
        >
          With Me
        </button>
        <button
          onClick={() => setDirection('BY_ME')}
          className={`px-3 py-1.5 text-sm rounded-md font-medium ${
            direction === 'BY_ME'
              ? 'bg-blue-100 text-blue-700'
              : 'text-gray-500 hover:bg-gray-100'
          }`}
        >
          By Me
        </button>
      </div>

      {items.length === 0 ? (
        <p className="text-gray-500 text-sm">
          {direction === 'WITH_ME' ? 'No files shared with you' : 'You haven\'t shared any files'}
        </p>
      ) : (
        <ul className="space-y-2">
          {items.map((item) => (
            <li
              key={item.id}
              onClick={() => item.fileId && navigate(`/files/${item.fileId}`)}
              className="flex items-center gap-3 p-2 rounded hover:bg-gray-50 cursor-pointer"
            >
              <span className="text-lg">🔗</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {item.fileName || 'Unnamed file'}
                </p>
                <p className="text-xs text-gray-500">
                  {direction === 'WITH_ME' ? `From ${item.sharedBy}` : `Shared with ${item.sharedWith || 'link'}`}
                  {' • '}{formatTime(item.sharedAt)}
                </p>
              </div>
              {item.expiresAt && (
                <span className="text-xs text-amber-500">
                  Expires {formatTime(item.expiresAt)}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
