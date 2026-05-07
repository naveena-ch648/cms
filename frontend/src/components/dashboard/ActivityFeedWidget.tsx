import { useEffect, useState } from 'react';
import { getActivity } from '../../api/dashboard';
import type { ActivityEvent } from '../../types/dashboard';

export default function ActivityFeedWidget() {
  const [events, setEvents] = useState<ActivityEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const loadActivity = (pageNum: number, append = false) => {
    setLoading(true);
    getActivity(pageNum, 10)
      .then((data) => {
        if (append) {
          setEvents((prev) => [...prev, ...data.content]);
        } else {
          setEvents(data.content);
        }
        setHasMore(data.content.length === 10);
      })
      .catch(() => setError('Failed to load activity'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadActivity(0);
  }, []);

  const handleLoadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    loadActivity(nextPage, true);
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

  const getActionIcon = (actionType: string) => {
    switch (actionType) {
      case 'FILE_UPLOADED': return '⬆️';
      case 'FILE_DOWNLOADED': return '⬇️';
      case 'FILE_SHARED': return '🔗';
      case 'FILE_MOVED': return '📦';
      case 'FILE_DELETED': return '🗑️';
      case 'FOLDER_CREATED': return '📁';
      case 'COMMENT_ADDED': return '💬';
      case 'APPROVAL_SUBMITTED': return '📋';
      case 'APPROVAL_DECIDED': return '✅';
      case 'WORKFLOW_TRANSITIONED': return '🔄';
      default: return '📌';
    }
  };

  const getActionLabel = (actionType: string) => {
    switch (actionType) {
      case 'FILE_UPLOADED': return 'uploaded';
      case 'FILE_DOWNLOADED': return 'downloaded';
      case 'FILE_SHARED': return 'shared';
      case 'FILE_MOVED': return 'moved';
      case 'FILE_DELETED': return 'deleted';
      case 'FOLDER_CREATED': return 'created folder';
      case 'COMMENT_ADDED': return 'commented on';
      case 'APPROVAL_SUBMITTED': return 'submitted for approval';
      case 'APPROVAL_DECIDED': return 'reviewed';
      case 'WORKFLOW_TRANSITIONED': return 'transitioned';
      default: return 'updated';
    }
  };

  if (loading && events.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Activity Feed</h3>
        <div className="animate-pulse space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-10 bg-gray-200 rounded" />
          ))}
        </div>
      </div>
    );
  }

  if (error && events.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Activity Feed</h3>
        <p className="text-red-500 text-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold mb-4">Activity Feed</h3>
      {events.length === 0 ? (
        <p className="text-gray-500 text-sm">No recent activity</p>
      ) : (
        <>
          <ul className="space-y-3">
            {events.map((event) => (
              <li key={event.id} className="flex items-start gap-3">
                <span className="text-lg mt-0.5">{getActionIcon(event.actionType)}</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-900">
                    <span className="font-medium">{event.actorName}</span>{' '}
                    {getActionLabel(event.actionType)}{' '}
                    <span className="font-medium">{event.targetName}</span>
                  </p>
                  <p className="text-xs text-gray-500">
                    {event.workspaceName} • {formatTime(event.createdAt)}
                  </p>
                </div>
              </li>
            ))}
          </ul>
          {hasMore && (
            <button
              onClick={handleLoadMore}
              disabled={loading}
              className="mt-4 w-full text-sm text-blue-600 hover:text-blue-800 py-2"
            >
              {loading ? 'Loading...' : 'Load more'}
            </button>
          )}
        </>
      )}
    </div>
  );
}
