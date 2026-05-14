import type { NotificationItem } from "../../types/collaboration";

interface Props {
  notifications: NotificationItem[];
  loading: boolean;
  unreadCount: number;
  onMarkRead: (id: string) => void;
  onMarkAllRead: () => void;
  onClose: () => void;
}

function relTime(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const m = Math.floor(diff / 60_000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

export default function NotificationPanel({
  notifications,
  loading,
  unreadCount,
  onMarkRead,
  onMarkAllRead,
  onClose,
}: Props) {
  return (
    <div
      onClick={(e) => e.stopPropagation()}
      style={{
        position: "fixed",
        top: 0,
        right: 0,
        width: "380px",
        height: "100vh",
        background: "#fff",
        boxShadow: "-4px 0 24px rgba(0,0,0,0.12)",
        zIndex: 501,
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* Header */}
      <div
        style={{
          padding: "16px 18px",
          borderBottom: "1px solid #e2e8f0",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div>
          <h3
            style={{
              margin: 0,
              fontSize: "15px",
              fontWeight: 600,
              color: "#1e293b",
            }}
          >
            Notifications
          </h3>
          {unreadCount > 0 && (
            <div
              style={{ fontSize: "12px", color: "#94a3b8", marginTop: "2px" }}
            >
              {unreadCount} unread
            </div>
          )}
        </div>
        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          {unreadCount > 0 && (
            <button
              onClick={onMarkAllRead}
              style={{
                fontSize: "12px",
                color: "#3b82f6",
                background: "none",
                border: "none",
                cursor: "pointer",
                fontWeight: 500,
              }}
            >
              Mark all read
            </button>
          )}
          <button
            onClick={onClose}
            style={{
              background: "none",
              border: "none",
              cursor: "pointer",
              color: "#94a3b8",
              fontSize: "18px",
              lineHeight: 1,
              padding: "2px",
            }}
          >
            ✕
          </button>
        </div>
      </div>

      {/* Body */}
      <div style={{ flex: 1, overflow: "auto" }}>
        {loading && notifications.length === 0 ? (
          <div style={{ padding: "32px", textAlign: "center" }}>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "8px" }}
            >
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  style={{
                    height: "52px",
                    borderRadius: "8px",
                    background: "#f1f5f9",
                  }}
                />
              ))}
            </div>
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ padding: "60px", textAlign: "center" }}>
            <div style={{ fontSize: "32px", marginBottom: "10px" }}>🔔</div>
            <div style={{ color: "#94a3b8", fontSize: "14px" }}>
              No notifications yet
            </div>
          </div>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.read && onMarkRead(n.id)}
              style={{
                padding: "12px 18px",
                borderBottom: "1px solid #f1f5f9",
                background: n.read ? "#fff" : "#f0f9ff",
                cursor: n.read ? "default" : "pointer",
                display: "flex",
                gap: "10px",
                alignItems: "flex-start",
              }}
            >
              {/* Unread dot */}
              <div
                style={{
                  width: "7px",
                  height: "7px",
                  borderRadius: "50",
                  flexShrink: 0,
                  marginTop: "6px",
                  background: n.read ? "transparent" : "#3b82f6",
                }}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: "13px",
                    fontWeight: n.read ? 400 : 600,
                    color: "#1e293b",
                  }}
                >
                  {n.title}
                </div>
                {n.message && (
                  <div
                    style={{
                      fontSize: "12px",
                      color: "#64748b",
                      marginTop: "2px",
                    }}
                  >
                    {n.message}
                  </div>
                )}
                <div
                  style={{
                    fontSize: "11px",
                    color: "#94a3b8",
                    marginTop: "4px",
                  }}
                >
                  {n.actor?.name && `${n.actor.name} · `}
                  {relTime(n.createdAt)}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Footer */}
      <div
        style={{
          padding: "12px 18px",
          borderTop: "1px solid #e2e8f0",
          fontSize: "12px",
          color: "#94a3b8",
          textAlign: "center",
        }}
      >
        Auto-refreshes every 30s
      </div>
    </div>
  );
}
