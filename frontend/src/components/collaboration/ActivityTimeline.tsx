import { useState, useEffect, useCallback } from 'react';
import { activityApi } from '../../api/activity';
import type { ActivityEvent } from '../../types/collaboration';

interface ActivityTimelineProps {
  fileId?: string;
  folderId?: string;
}

export default function ActivityTimeline({ fileId, folderId }: ActivityTimelineProps) {
  const [events, setEvents] = useState<ActivityEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const loadActivity = useCallback(async (pageNum = 0, append = false) => {
    try {
      if (!append) setLoading(true);
      let res;
      if (fileId) {
        res = await activityApi.getFileActivity(fileId, pageNum, 20);
      } else if (folderId) {
        res = await activityApi.getFolderActivity(folderId, pageNum, 20);
      } else return;

      const data = res.data.data;
      if (append) {
        setEvents(prev => [...prev, ...data.content]);
      } else {
        setEvents(data.content);
      }
      setHasMore(data.number < data.totalPages - 1);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  }, [fileId, folderId]);

  useEffect(() => {
    setEvents([]);
    setPage(0);
    loadActivity(0);
  }, [loadActivity]);

  const handleLoadMore = () => {
    const nextPage = page + 1;
    setPage(nextPage);
    loadActivity(nextPage, true);
  };

  if (loading && events.length === 0) {
    return <div style={{ padding: 16, color: '#888', textAlign: 'center' }}>Loading activity...</div>;
  }

  if (events.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: 24, color: '#888' }}>
        <p style={{ margin: 0 }}>No activity yet</p>
      </div>
    );
  }

  return (
    <div style={{ overflowY: 'auto', height: '100%', padding: '12px' }}>
      {events.map(event => (
        <div
          key={event.id}
          style={{
            padding: '10px 12px',
            borderLeft: '2px solid #e0e0e0',
            marginLeft: 8,
            marginBottom: 4,
            position: 'relative',
          }}
        >
          <div style={{
            position: 'absolute',
            left: -6,
            top: 14,
            width: 8,
            height: 8,
            borderRadius: '50%',
            background: getCategoryColor(event.category),
          }} />
          <div style={{ fontSize: 13, fontWeight: 500, color: '#333' }}>
            {event.description}
          </div>
          <div style={{ fontSize: 11, color: '#888', marginTop: 2 }}>
            {event.actor && `${event.actor.name} • `}
            {formatTime(event.createdAt)}
          </div>
        </div>
      ))}
      {hasMore && (
        <button
          onClick={handleLoadMore}
          style={{ display: 'block', margin: '8px auto', padding: '6px 16px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 4, background: '#fff', fontSize: 12 }}
        >
          Load more
        </button>
      )}
    </div>
  );
}

function getCategoryColor(category: string): string {
  switch (category) {
    case 'file': return '#3182ce';
    case 'comment': return '#38a169';
    case 'task': return '#d69e2e';
    case 'share': return '#805ad5';
    default: return '#a0aec0';
  }
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDays = Math.floor(diffHr / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}
