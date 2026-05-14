import { useState } from "react";
import { useNavigate } from "react-router-dom";
import type { SharedItem } from "../../types/dashboard";

interface Props {
  items: SharedItem[];
  loading: boolean;
  direction: "WITH_ME" | "BY_ME";
  onChangeDirection: (d: "WITH_ME" | "BY_ME") => void;
}

function relTime(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 7) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}

function expiresLabel(
  expiresAt: string | null,
): { text: string; color: string; bg: string } | null {
  if (!expiresAt) return null;
  const ms = new Date(expiresAt).getTime() - Date.now();
  if (ms < 0) return { text: "Expired", color: "#dc2626", bg: "#fee2e2" };
  if (ms < 3_600_000)
    return { text: "Expires <1h", color: "#d97706", bg: "#fffbeb" };
  if (ms < 86_400_000)
    return { text: "Expires today", color: "#d97706", bg: "#fffbeb" };
  return null;
}

function SharedRow({
  item,
  direction,
  onClick,
}: {
  item: SharedItem;
  direction: "WITH_ME" | "BY_ME";
  onClick: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  const badge = expiresLabel(item.expiresAt);

  const subtitle =
    direction === "WITH_ME"
      ? `Shared by ${item.sharedBy}${item.workspaceName ? ` · ${item.workspaceName}` : ""}`
      : `${item.workspaceName || "Workspace"} · shared via link`;

  return (
    <li
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: "flex",
        gap: "10px",
        padding: "8px 10px",
        borderRadius: "8px",
        cursor: "pointer",
        background: hovered ? "#f8fafc" : "transparent",
        transition: "background 0.1s",
        alignItems: "center",
        borderBottom: "1px solid #f1f5f9",
      }}
    >
      {/* Icon */}
      <div
        style={{
          width: 34,
          height: 34,
          borderRadius: "8px",
          flexShrink: 0,
          background: direction === "WITH_ME" ? "#eff6ff" : "#f0fdf4",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "16px",
        }}
      >
        {direction === "WITH_ME" ? "📥" : "📤"}
      </div>

      {/* Text */}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div
          style={{
            fontSize: "13px",
            fontWeight: 500,
            color: "#1e293b",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {item.fileName || "Unnamed file"}
        </div>
        <div
          style={{
            fontSize: "11px",
            color: "#94a3b8",
            marginTop: "2px",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {subtitle}
        </div>
      </div>

      {/* Right side: badge + time */}
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "flex-end",
          gap: "3px",
          flexShrink: 0,
        }}
      >
        <span
          style={{ fontSize: "11px", color: "#94a3b8", whiteSpace: "nowrap" }}
        >
          {relTime(item.sharedAt)}
        </span>
        {badge && (
          <span
            style={{
              fontSize: "10px",
              padding: "2px 5px",
              borderRadius: "4px",
              background: badge.bg,
              color: badge.color,
              fontWeight: 500,
              whiteSpace: "nowrap",
            }}
          >
            {badge.text}
          </span>
        )}
      </div>
    </li>
  );
}

export default function SharedItemsWidget({
  items,
  loading,
  direction,
  onChangeDirection,
}: Props) {
  const navigate = useNavigate();

  const handleItemClick = (item: SharedItem) => {
    // Navigate to the workspace — the user can locate the file there.
    // We don't have a standalone /files/:id route.
    if (item.fileId) {
      navigate("/workspaces");
    }
  };

  if (loading) {
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
            alignItems: "center",
            gap: "8px",
            marginBottom: "14px",
          }}
        >
          <span style={{ fontSize: "14px" }}>🔗</span>
          <h3
            style={{
              margin: 0,
              fontSize: "14px",
              fontWeight: 600,
              color: "#1e293b",
            }}
          >
            Shared Files
          </h3>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              style={{
                height: "44px",
                borderRadius: "8px",
                background: "#f1f5f9",
                animation: "pulse 1.5s infinite",
              }}
            />
          ))}
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
      {/* Header */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
          <span style={{ fontSize: "14px" }}>🔗</span>
          <h3
            style={{
              margin: 0,
              fontSize: "14px",
              fontWeight: 600,
              color: "#1e293b",
            }}
          >
            Shared Files
          </h3>
        </div>
        <span style={{ fontSize: "11px", color: "#94a3b8" }}>
          {items.length} item{items.length !== 1 ? "s" : ""}
        </span>
      </div>

      {/* Tabs */}
      <div
        style={{
          display: "flex",
          gap: "4px",
          marginBottom: "12px",
          background: "#f8fafc",
          borderRadius: "8px",
          padding: "3px",
        }}
      >
        {(["WITH_ME", "BY_ME"] as const).map((d) => (
          <button
            key={d}
            onClick={() => onChangeDirection(d)}
            style={{
              flex: 1,
              padding: "5px 10px",
              borderRadius: "6px",
              border: "none",
              cursor: "pointer",
              fontSize: "12px",
              fontWeight: 500,
              transition: "all 0.15s",
              background: direction === d ? "#fff" : "transparent",
              color: direction === d ? "#1e293b" : "#94a3b8",
              boxShadow:
                direction === d ? "0 1px 3px rgba(0,0,0,0.08)" : "none",
            }}
          >
            {d === "WITH_ME" ? "📥 With Me" : "📤 By Me"}
          </button>
        ))}
      </div>

      {/* Content */}
      {items.length === 0 ? (
        <div
          style={{
            padding: "24px 16px",
            textAlign: "center",
            color: "#94a3b8",
            fontSize: "13px",
            background: "#f8fafc",
            borderRadius: "8px",
          }}
        >
          {direction === "WITH_ME"
            ? "No files have been shared with you yet"
            : "You haven't created any shared links yet"}
        </div>
      ) : (
        <ul style={{ margin: 0, padding: 0, listStyle: "none" }}>
          {items.map((item) => (
            <SharedRow
              key={item.id}
              item={item}
              direction={direction}
              onClick={() => handleItemClick(item)}
            />
          ))}
        </ul>
      )}
    </div>
  );
}
