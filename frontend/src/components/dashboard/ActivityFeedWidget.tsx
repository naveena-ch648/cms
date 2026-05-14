import { useState } from "react";
import type { ActivityEvent } from "../../types/dashboard";

interface Props {
  events: ActivityEvent[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
  newCount: number;
  onLoadMore: () => void;
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

const ACTION_META: Record<
  string,
  { icon: string; label: string; color: string }
> = {
  FILE_UPLOADED: { icon: "⬆️", label: "uploaded", color: "#3b82f6" },
  FILE_DOWNLOADED: { icon: "⬇️", label: "downloaded", color: "#10b981" },
  FILE_SHARED: { icon: "🔗", label: "shared", color: "#8b5cf6" },
  FILE_MOVED: { icon: "📦", label: "moved", color: "#f59e0b" },
  FILE_DELETED: { icon: "🗑️", label: "deleted", color: "#ef4444" },
  FOLDER_CREATED: { icon: "📁", label: "created folder", color: "#10b981" },
  COMMENT_ADDED: { icon: "💬", label: "commented on", color: "#6366f1" },
  APPROVAL_SUBMITTED: {
    icon: "📋",
    label: "submitted for approval",
    color: "#f59e0b",
  },
  APPROVAL_DECIDED: { icon: "✅", label: "reviewed", color: "#10b981" },
  WORKFLOW_TRANSITIONED: {
    icon: "🔄",
    label: "transitioned",
    color: "#8b5cf6",
  },
};

function EventRow({ event }: { event: ActivityEvent }) {
  const [hovered, setHovered] = useState(false);
  const meta = ACTION_META[event.actionType] ?? {
    icon: "📌",
    label: "updated",
    color: "#64748b",
  };
  return (
    <li
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: "flex",
        gap: "10px",
        padding: "8px 10px",
        borderRadius: "8px",
        background: hovered ? "#f8fafc" : "transparent",
        transition: "background 0.1s",
      }}
    >
      <span
        style={{
          width: "28px",
          height: "28px",
          borderRadius: "6px",
          flexShrink: 0,
          background: `${meta.color}15`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "14px",
          marginTop: "1px",
        }}
      >
        {meta.icon}
      </span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: "13px", color: "#1e293b" }}>
          <strong style={{ fontWeight: 600 }}>{event.actorName}</strong>{" "}
          {meta.label}{" "}
          <strong style={{ fontWeight: 600 }}>{event.targetName}</strong>
        </div>
        <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "2px" }}>
          {event.workspaceName} · {relTime(event.createdAt)}
        </div>
      </div>
    </li>
  );
}

export default function ActivityFeedWidget({
  events,
  loading,
  error,
  hasMore,
  newCount,
  onLoadMore,
}: Props) {
  if (loading && events.length === 0) {
    return (
      <div
        style={{
          background: "#fff",
          borderRadius: "12px",
          border: "1px solid #e2e8f0",
          padding: "20px",
        }}
      >
        <h3
          style={{
            margin: "0 0 14px",
            fontSize: "14px",
            fontWeight: 600,
            color: "#1e293b",
          }}
        >
          ⚡ Activity Feed
        </h3>
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {[1, 2, 3, 4, 5].map((i) => (
            <div
              key={i}
              style={{
                height: "40px",
                borderRadius: "8px",
                background: "#f1f5f9",
              }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (error && events.length === 0) {
    return (
      <div
        style={{
          background: "#fff",
          borderRadius: "12px",
          border: "1px solid #e2e8f0",
          padding: "20px",
        }}
      >
        <h3
          style={{
            margin: "0 0 14px",
            fontSize: "14px",
            fontWeight: 600,
            color: "#1e293b",
          }}
        >
          ⚡ Activity Feed
        </h3>
        <div
          style={{
            padding: "12px 14px",
            background: "#fef2f2",
            borderRadius: "8px",
            fontSize: "13px",
            color: "#dc2626",
          }}
        >
          {error}
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        background: "#fff",
        borderRadius: "12px",
        border: "1px solid #e2e8f0",
        padding: "20px",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
        }}
      >
        <h3
          style={{
            margin: 0,
            fontSize: "14px",
            fontWeight: 600,
            color: "#1e293b",
          }}
        >
          ⚡ Activity Feed
        </h3>
        <span style={{ fontSize: "11px", color: "#94a3b8" }}>
          Live · refreshes every 30s
        </span>
      </div>

      {/* New items banner */}
      {newCount > 0 && (
        <div
          style={{
            marginBottom: "10px",
            padding: "6px 12px",
            background: "#eff6ff",
            border: "1px solid #bfdbfe",
            borderRadius: "8px",
            fontSize: "12px",
            color: "#1d4ed8",
            fontWeight: 500,
            display: "flex",
            alignItems: "center",
            gap: "6px",
          }}
        >
          <span
            style={{
              width: "6px",
              height: "6px",
              borderRadius: "50%",
              background: "#3b82f6",
              animation: "pulse-dot 2s infinite",
            }}
          />
          {newCount} new update{newCount !== 1 ? "s" : ""} available
        </div>
      )}

      {events.length === 0 ? (
        <div
          style={{
            padding: "24px",
            textAlign: "center",
            color: "#94a3b8",
            fontSize: "13px",
          }}
        >
          No recent activity
        </div>
      ) : (
        <>
          <ul style={{ margin: 0, padding: 0, listStyle: "none" }}>
            {events.map((event) => (
              <EventRow key={event.id} event={event} />
            ))}
          </ul>
          {hasMore && (
            <button
              onClick={onLoadMore}
              disabled={loading}
              style={{
                marginTop: "10px",
                width: "100%",
                padding: "8px",
                background: "none",
                border: "1px solid #e2e8f0",
                borderRadius: "8px",
                fontSize: "12px",
                color: "#3b82f6",
                cursor: "pointer",
                fontWeight: 500,
              }}
            >
              {loading ? "Loading..." : "Load more"}
            </button>
          )}
        </>
      )}
    </div>
  );
}
