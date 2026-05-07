import { useEffect, useState } from 'react';
import { notificationsApi } from '../../api/notifications';

interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  targetType: string;
  targetId: string;
  read: boolean;
  createdAt: string;
  actor?: { id: string; name: string };
}

interface Props {
  open: boolean;
  onClose: () => void;
}

export default function NotificationPanel({ open, onClose }: Props) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  const load = (pageNum: number, append = false) => {
    setLoading(true);
    notificationsApi.getNotifications({ page: pageNum, size: 15 })
      .then((res) => {
        const data = res.data.data as unknown as Notification[];
        if (append) {
          setNotifications((prev) => [...prev, ...data]);
        } else {
          setNotifications(data);
        }
        setHasMore(data.length === 15);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (open) {
      setPage(0);
      load(0);
    }
  }, [open]);

  const handleMarkRead = async (id: string) => {
    await notificationsApi.markAsRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const handleMarkAllRead = async () => {
    await notificationsApi.markAllAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleLoadMore = () => {
    const next = page + 1;
    setPage(next);
    load(next, true);
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

  if (!open) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      right: 0,
      width: '400px',
      height: '100vh',
      background: '#fff',
      boxShadow: '-4px 0 20px rgba(0,0,0,0.1)',
      zIndex: 1000,
      display: 'flex',
      flexDirection: 'column',
    }}>
      <div style={{
        padding: '16px 20px',
        borderBottom: '1px solid #e2e8f0',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>Notifications</h3>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <button
            onClick={handleMarkAllRead}
            style={{ fontSize: '12px', color: '#3b82f6', background: 'none', border: 'none', cursor: 'pointer' }}
          >
            Mark all read
          </button>
          <button
            onClick={onClose}
            style={{ fontSize: '18px', background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
          >
            ✕
          </button>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '8px 0' }}>
        {loading && notifications.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>Loading...</div>
        ) : notifications.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
            No notifications yet
          </div>
        ) : (
          <>
            {notifications.map((n) => (
              <div
                key={n.id}
                style={{
                  padding: '12px 20px',
                  borderBottom: '1px solid #f1f5f9',
                  background: n.read ? '#fff' : '#f0f9ff',
                  cursor: 'pointer',
                }}
                onClick={() => !n.read && handleMarkRead(n.id)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ flex: 1 }}>
                    <p style={{ margin: 0, fontSize: '13px', fontWeight: n.read ? 400 : 600, color: '#1e293b' }}>
                      {n.title}
                    </p>
                    {n.message && (
                      <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#64748b' }}>
                        {n.message}
                      </p>
                    )}
                  </div>
                  {!n.read && (
                    <span style={{
                      width: '8px',
                      height: '8px',
                      borderRadius: '50%',
                      background: '#3b82f6',
                      flexShrink: 0,
                      marginTop: '4px',
                    }} />
                  )}
                </div>
                <p style={{ margin: '4px 0 0', fontSize: '11px', color: '#94a3b8' }}>
                  {n.actor?.name && `${n.actor.name} • `}{formatTime(n.createdAt)}
                </p>
              </div>
            ))}
            {hasMore && (
              <button
                onClick={handleLoadMore}
                disabled={loading}
                style={{
                  display: 'block',
                  width: '100%',
                  padding: '12px',
                  fontSize: '13px',
                  color: '#3b82f6',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                }}
              >
                {loading ? 'Loading...' : 'Load more'}
              </button>
            )}
          </>
        )}
      </div>
    </div>
  );
}
