import { useState } from "react";
import { useNavigate } from "react-router-dom";
import type { RecentFile } from "../../types/dashboard";

interface Props {
  files: RecentFile[];
  loading: boolean;
  error: string | null;
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

function fileIcon(mimeType: string) {
  if (mimeType?.startsWith("image/")) return "🖼️";
  if (mimeType?.includes("pdf")) return "📄";
  if (mimeType?.includes("spreadsheet") || mimeType?.includes("excel"))
    return "📊";
  if (mimeType?.includes("presentation") || mimeType?.includes("powerpoint"))
    return "📽️";
  if (mimeType?.includes("word") || mimeType?.includes("document")) return "📝";
  if (mimeType?.startsWith("video/")) return "🎬";
  return "📁";
}

function FileRow({ file, onClick }: { file: RecentFile; onClick: () => void }) {
  const [hovered, setHovered] = useState(false);
  return (
    <li
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: "flex",
        alignItems: "center",
        gap: "10px",
        padding: "8px 10px",
        borderRadius: "8px",
        cursor: "pointer",
        background: hovered ? "#f8fafc" : "transparent",
        transition: "background 0.1s",
      }}
    >
      <span style={{ fontSize: "18px", flexShrink: 0 }}>
        {fileIcon(file.mimeType)}
      </span>
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
          {file.name}
        </div>
        <div
          style={{
            fontSize: "11px",
            color: "#94a3b8",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
        >
          {file.workspaceName}
          {file.folderPath ? ` · ${file.folderPath}` : ""}
        </div>
      </div>
      <span
        style={{
          fontSize: "11px",
          color: "#94a3b8",
          whiteSpace: "nowrap",
          flexShrink: 0,
        }}
      >
        {relTime(file.lastAccessedAt || file.updatedAt)}
      </span>
    </li>
  );
}

const PREVIEW_LIMIT = 5;

export default function RecentFilesWidget({ files, loading, error }: Props) {
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState(false);

  // Sort most-recently accessed first, oldest entries at top when expanded
  const sorted = [...files].sort((a, b) => {
    const aDate = new Date(a.lastAccessedAt || a.updatedAt).getTime();
    const bDate = new Date(b.lastAccessedAt || b.updatedAt).getTime();
    return bDate - aDate; // newest first
  });

  const visible = expanded ? sorted : sorted.slice(0, PREVIEW_LIMIT);
  const hiddenCount = sorted.length - PREVIEW_LIMIT;

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
        <h3
          style={{
            margin: "0 0 14px",
            fontSize: "14px",
            fontWeight: 600,
            color: "#1e293b",
          }}
        >
          📁 Recent Files
        </h3>
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {[1, 2, 3, 4, 5].map((i) => (
            <div
              key={i}
              style={{
                height: "44px",
                borderRadius: "8px",
                background: "#f1f5f9",
              }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
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
          📁 Recent Files
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
      {/* Header */}
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
          📁 Recent Files
          <span
            style={{
              marginLeft: "8px",
              fontSize: "11px",
              fontWeight: 400,
              color: "#94a3b8",
            }}
          >
            {sorted.length} files · newest first
          </span>
        </h3>
      </div>

      {sorted.length === 0 ? (
        <div
          style={{
            padding: "24px",
            textAlign: "center",
            color: "#94a3b8",
            fontSize: "13px",
          }}
        >
          No recently accessed files
        </div>
      ) : (
        <>
          <ul
            style={{
              margin: 0,
              padding: 0,
              listStyle: "none",
              display: "flex",
              flexDirection: "column",
              gap: "2px",
            }}
          >
            {visible.map((file) => (
              <FileRow
                key={file.id}
                file={file}
                onClick={() =>
                  navigate(
                    `/workspaces/${file.workspaceId}/folders/${file.folderId}`,
                  )
                }
              />
            ))}
          </ul>

          {/* View all / Collapse toggle */}
          {hiddenCount > 0 && (
            <button
              onClick={() => setExpanded((e) => !e)}
              style={{
                marginTop: "10px",
                width: "100%",
                padding: "8px 12px",
                background: expanded ? "#f8fafc" : "#eff6ff",
                border: `1px solid ${expanded ? "#e2e8f0" : "#bfdbfe"}`,
                borderRadius: "8px",
                cursor: "pointer",
                fontSize: "12px",
                fontWeight: 500,
                color: expanded ? "#64748b" : "#2563eb",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: "6px",
              }}
            >
              {expanded ? (
                <>
                  <span>↑</span> Collapse
                </>
              ) : (
                <>
                  <span>↓</span> View all {hiddenCount} more file
                  {hiddenCount !== 1 ? "s" : ""}
                </>
              )}
            </button>
          )}
        </>
      )}
    </div>
  );
}
