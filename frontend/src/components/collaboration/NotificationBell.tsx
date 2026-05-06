import { useState, useEffect, useCallback } from 'react';
import { notificationsApi } from '../../api/notifications';
import type { NotificationItem } from '../../types/collaboration';

export default function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await notificationsApi.getUnreadCount();
      setUnreadCount(res.data.data.unreadCount);
    } catch {
      // silent
    }
  }, []);

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000); // Poll every 30s
    return () => clearInterval(interval);
  }, [fetchUnreadCount]);

  const openDropdown = async () => {
    setShowDropdown(true);
    setLoading(true);
    try {
      const res = await notificationsApi.getNotifications({ page: 0, size: 10 });
      setNotifications(res.data.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (notificationId: string) => {
    try {
      await notificationsApi.markAsRead(notificationId);
      setNotifications(prev => prev.map(n => n.id === notificationId ? { ...n, read: true } : n));
      setUnreadCount(c => Math.max(0, c - 1));
    } catch {
      // silent
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationsApi.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch {
      // silent
    }
  };

  return (
    <div style={{ position: 'relative' }}>
      <button
        onClick={() => showDropdown ? setShowDropdown(false) : openDropdown()}
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          position: 'relative',
          padding: '8px',
          fontSize: '18px',
        }}
        title="Notifications"
      >
        🔔
        {unreadCount > 0 && (
          <span style={{
            position: 'absolute',
            top: '2px',
            right: '2px',
            background: '#e53e3e',
            color: '#fff',
            borderRadius: '50%',
            width: '18px',
            height: '18px',
            fontSize: '11px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 600,
          }}>
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {showDropdown && (
        <div style={{
          position: 'absolute',
          top: '100%',
          right: 0,
          width: '360px',
          maxHeight: '400px',
          overflow: 'auto',
          background: '#fff',
          border: '1px solid #ddd',
          borderRadius: '8px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          zIndex: 1000,
        }}>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px 16px',
            borderBottom: '1px solid #eee',
          }}>
            <span style={{ fontWeight: 600, fontSize: '14px' }}>Notifications</span>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#3182ce',
                  cursor: 'pointer',
                  fontSize: '12px',
                }}
              >
                Mark all read
              </button>
            )}
          </div>
          {loading ? (
            <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>Loading...</div>
          ) : notifications.length === 0 ? (
            <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>No notifications</div>
          ) : (
            notifications.map(n => (
              <div
                key={n.id}
                onClick={() => !n.read && handleMarkAsRead(n.id)}
                style={{
                  padding: '12px 16px',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: n.read ? 'default' : 'pointer',
                  background: n.read ? '#fff' : '#f7fafc',
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: n.read ? 400 : 600 }}>
                  {n.title}
                </div>
                <div style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>
                  {n.message}
                </div>
                <div style={{ fontSize: '11px', color: '#999', marginTop: '4px' }}>
                  {new Date(n.createdAt).toLocaleString()}
                  {n.actor && ` • ${n.actor.name}`}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
