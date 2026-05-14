import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { listSharedWithMe, revokeFileShare } from "../api/fileShares";
import type { SharedWithMeFile } from "../types/fileShare";

function relTime(iso: string) {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const h = Math.floor(mins / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  if (d === 1) return "Yesterday";
  if (d < 7) return `${d}d ago`;
  return new Date(iso).toLocaleDateString();
}

function formatSize(bytes: number) {
  if (bytes === 0) return "0 B";
  const u = ["B", "KB", "MB", "GB"] as const;
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${u[i]}`;
}

function mimeIcon(mime: string) {
  if (mime.startsWith("image/")) return "🖼️";
  if (mime.startsWith("video/")) return "🎬";
  if (mime.startsWith("audio/")) return "🎵";
  if (mime.includes("pdf")) return "📄";
  if (mime.includes("word") || mime.includes("document")) return "📝";
  if (mime.includes("sheet") || mime.includes("excel")) return "📊";
  if (mime.includes("presentation") || mime.includes("powerpoint")) return "📋";
  if (mime.includes("zip") || mime.includes("archive")) return "🗜️";
  return "📁";
}

export default function SharedFilesPage() {
  const navigate = useNavigate();
  const [files, setFiles] = useState<SharedWithMeFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [revoking, setRevoking] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listSharedWithMe(page, 20);
      setFiles(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch {
      setFiles([]);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const handleRemoveAccess = async (shareUuid: string) => {
    setRevoking(shareUuid);
    try {
      await revokeFileShare(shareUuid);
      setFiles((prev) => prev.filter((f) => f.shareUuid !== shareUuid));
      setTotalElements((n) => n - 1);
    } catch {
      /* ignore */
    } finally {
      setRevoking(null);
    }
  };

  return (
    <div style={{ padding: "28px 32px", maxWidth: "1100px", margin: "0 auto" }}>

      {/* Header */}
      <div style={{ marginBottom: "24px" }}>
        <h1 style={{ margin: "0 0 4px", fontSize: "22px", fontWeight: 700, color: "#1e293b" }}>
          👥 Shared with Me
        </h1>
        <p style={{ margin: 0, fontSize: "14px", color: "#64748b" }}>
          Files that other CMS users have shared with you directly.
        </p>
      </div>

      {/* Stats strip */}
      {!loading && (
        <div style={{ display: "flex", gap: "14px", marginBottom: "20px" }}>
          <div
            style={{
              background: "#eff6ff",
              borderRadius: "10px",
              padding: "12px 20px",
              display: "flex",
              flexDirection: "column",
              gap: "2px",
            }}
          >
            <span style={{ fontSize: "22px", fontWeight: 700, color: "#2563eb" }}>
              {totalElements}
            </span>
            <span style={{ fontSize: "12px", color: "#3b82f6" }}>
              Files shared with me
            </span>
          </div>
        </div>
      )}

      {/* Content */}
      <div
        style={{
          background: "#fff",
          borderRadius: "14px",
          border: "1px solid #e2e8f0",
          overflow: "hidden",
        }}
      >
        {loading ? (
          <div style={{ display: "flex", flexDirection: "column" }}>
            {[1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                style={{
                  height: "72px",
                  background: i % 2 === 0 ? "#f8fafc" : "#fff",
                  borderBottom: "1px solid #f1f5f9",
                  display: "flex",
                  alignItems: "center",
                  padding: "0 20px",
                  gap: "14px",
                }}
              >
                <div
                  style={{
                    width: "38px",
                    height: "38px",
                    borderRadius: "8px",
                    background: "#e2e8f0",
                  }}
                />
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      height: "13px",
                      width: "40%",
                      background: "#e2e8f0",
                      borderRadius: "4px",
                      marginBottom: "6px",
                    }}
                  />
                  <div
                    style={{
                      height: "11px",
                      width: "25%",
                      background: "#f1f5f9",
                      borderRadius: "4px",
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        ) : files.length === 0 ? (
          <div style={{ padding: "72px 40px", textAlign: "center", color: "#94a3b8" }}>
            <div style={{ fontSize: "48px", marginBottom: "16px" }}>👥</div>
            <div
              style={{
                fontSize: "17px",
                fontWeight: 600,
                color: "#64748b",
                marginBottom: "8px",
              }}
            >
              Nothing shared with you yet
            </div>
            <div
              style={{ fontSize: "13px", maxWidth: "340px", margin: "0 auto" }}
            >
              When someone shares a file with you from the CMS workspace, it
              will appear here instantly.
            </div>
            <button
              onClick={() => navigate("/workspaces")}
              style={{
                marginTop: "20px",
                padding: "10px 22px",
                background: "#2563eb",
                color: "#fff",
                border: "none",
                borderRadius: "9px",
                cursor: "pointer",
                fontSize: "13px",
                fontWeight: 600,
              }}
            >
              Browse Workspaces
            </button>
          </div>
        ) : (
          <>
            {/* Table header */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 150px 110px 110px 90px",
                padding: "10px 20px",
                background: "#f8fafc",
                borderBottom: "1px solid #e2e8f0",
                fontSize: "11px",
                fontWeight: 700,
                color: "#64748b",
                textTransform: "uppercase",
                letterSpacing: "0.05em",
                gap: "8px",
              }}
            >
              <span>File</span>
              <span>Shared by</span>
              <span>Permission</span>
              <span>Shared</span>
              <span style={{ textAlign: "right" }}>Actions</span>
            </div>

            {/* Rows */}
            {files.map((item) => (
              <div
                key={item.shareUuid}
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 150px 110px 110px 90px",
                  padding: "14px 20px",
                  borderBottom: "1px solid #f1f5f9",
                  alignItems: "center",
                  gap: "8px",
                  transition: "background 0.1s",
                }}
                onMouseEnter={(e) =>
                  ((e.currentTarget as HTMLElement).style.background = "#f8fafc")
                }
                onMouseLeave={(e) =>
                  ((e.currentTarget as HTMLElement).style.background = "")
                }
              >
                {/* File info */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "12px",
                    minWidth: 0,
                  }}
                >
                  <div
                    style={{
                      width: "40px",
                      height: "40px",
                      borderRadius: "9px",
                      background: "#f1f5f9",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "20px",
                      flexShrink: 0,
                    }}
                  >
                    {mimeIcon(item.fileMimeType)}
                  </div>
                  <div style={{ minWidth: 0 }}>
                    <div
                      style={{
                        fontSize: "14px",
                        fontWeight: 600,
                        color: "#1e293b",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {item.fileName}
                    </div>
                    <div style={{ fontSize: "11px", color: "#94a3b8" }}>
                      {formatSize(item.fileSizeBytes)}
                      {item.expiresAt && (
                        <span
                          style={{
                            color:
                              new Date(item.expiresAt) < new Date()
                                ? "#dc2626"
                                : "#d97706",
                          }}
                        >
                          {" · expires "}
                          {new Date(item.expiresAt).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Shared by */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "7px",
                    minWidth: 0,
                  }}
                >
                  <div
                    style={{
                      width: "26px",
                      height: "26px",
                      borderRadius: "50%",
                      background: "linear-gradient(135deg,#3b82f6,#8b5cf6)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "#fff",
                      fontWeight: 700,
                      fontSize: "10px",
                      flexShrink: 0,
                    }}
                  >
                    {item.sharedByFirstName[0]?.toUpperCase()}
                    {item.sharedByLastName[0]?.toUpperCase()}
                  </div>
                  <div
                    style={{
                      fontSize: "12px",
                      color: "#374151",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {item.sharedByFirstName} {item.sharedByLastName}
                  </div>
                </div>

                {/* Permission badge */}
                <div>
                  <span
                    style={{
                      fontSize: "11px",
                      padding: "3px 10px",
                      borderRadius: "6px",
                      fontWeight: 700,
                      background:
                        item.permission === "EDITOR" ? "#eff6ff" : "#f0fdf4",
                      color:
                        item.permission === "EDITOR" ? "#2563eb" : "#16a34a",
                    }}
                  >
                    {item.permission === "EDITOR" ? "✏️ Editor" : "👁 Viewer"}
                  </span>
                </div>

                {/* Shared at */}
                <div style={{ fontSize: "12px", color: "#94a3b8" }}>
                  {relTime(item.sharedAt)}
                </div>

                {/* Actions */}
                <div
                  style={{
                    display: "flex",
                    justifyContent: "flex-end",
                    gap: "6px",
                  }}
                >
                  {item.fileWorkspaceId && (
                    <button
                      onClick={() =>
                        navigate(
                          `/workspaces/${item.fileWorkspaceId}?folder=${item.fileFolderId ?? ""}`
                        )
                      }
                      title="Open in workspace"
                      style={{
                        padding: "5px 10px",
                        fontSize: "12px",
                        fontWeight: 500,
                        background: "#eff6ff",
                        color: "#2563eb",
                        border: "1px solid #bfdbfe",
                        borderRadius: "7px",
                        cursor: "pointer",
                        whiteSpace: "nowrap",
                      }}
                    >
                      Open
                    </button>
                  )}
                  <button
                    onClick={() => handleRemoveAccess(item.shareUuid)}
                    disabled={revoking === item.shareUuid}
                    title="Remove from shared list"
                    style={{
                      padding: "5px 8px",
                      fontSize: "12px",
                      background: "#fef2f2",
                      color: "#ef4444",
                      border: "1px solid #fecaca",
                      borderRadius: "7px",
                      cursor:
                        revoking === item.shareUuid
                          ? "not-allowed"
                          : "pointer",
                    }}
                  >
                    {revoking === item.shareUuid ? "…" : "✕"}
                  </button>
                </div>
              </div>
            ))}

            {/* Pagination */}
            {totalPages > 1 && (
              <div
                style={{
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  gap: "12px",
                  padding: "16px",
                  borderTop: "1px solid #f1f5f9",
                }}
              >
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  style={{
                    padding: "7px 16px",
                    fontSize: "13px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    cursor: page === 0 ? "not-allowed" : "pointer",
                    background: page === 0 ? "#f8fafc" : "#fff",
                    color: page === 0 ? "#94a3b8" : "#374151",
                  }}
                >
                  ← Prev
                </button>
                <span style={{ fontSize: "13px", color: "#64748b" }}>
                  {page + 1} / {totalPages}
                </span>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  style={{
                    padding: "7px 16px",
                    fontSize: "13px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    cursor:
                      page >= totalPages - 1 ? "not-allowed" : "pointer",
                    background:
                      page >= totalPages - 1 ? "#f8fafc" : "#fff",
                    color:
                      page >= totalPages - 1 ? "#94a3b8" : "#374151",
                  }}
                >
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
