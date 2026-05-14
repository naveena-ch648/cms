import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  shareFile,
  listFileShares,
  revokeFileShare,
  updateFileShare,
  searchUsers,
} from "../api/fileShares";
import type { FileShare, CmsUser } from "../types/fileShare";

interface InternalShareDialogProps {
  fileUuid: string;
  fileName: string;
  open: boolean;
  onClose: () => void;
}

const badge = (permission: string) => ({
  background: permission === "EDITOR" ? "#eff6ff" : "#f0fdf4",
  color: permission === "EDITOR" ? "#2563eb" : "#16a34a",
});

function formatExpiry(iso: string | null) {
  if (!iso) return "Never";
  return new Date(iso).toLocaleDateString();
}

const InternalShareDialog: React.FC<InternalShareDialogProps> = ({
  fileUuid,
  fileName,
  open,
  onClose,
}) => {
  // ── state ──────────────────────────────────────────────────────────────────
  const [shares, setShares] = useState<FileShare[]>([]);
  const [loadingShares, setLoadingShares] = useState(false);

  // User search
  const [query, setQuery] = useState("");
  const [userResults, setUserResults] = useState<CmsUser[]>([]);
  const [searching, setSearching] = useState(false);
  const [selectedUser, setSelectedUser] = useState<CmsUser | null>(null);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  // Share options
  const [permission, setPermission] = useState<"VIEWER" | "EDITOR">("VIEWER");
  const [allowDownload, setAllowDownload] = useState(true);
  const [watermark, setWatermark] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");

  // UI state
  const [sharing, setSharing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const searchRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // ── load existing shares ───────────────────────────────────────────────────
  const loadShares = useCallback(async () => {
    if (!open) return;
    setLoadingShares(true);
    try {
      const data = await listFileShares(fileUuid);
      setShares(data);
    } catch {
      /* ignore */
    } finally {
      setLoadingShares(false);
    }
  }, [fileUuid, open]);

  useEffect(() => {
    loadShares();
  }, [loadShares]);

  // ── user search with debounce ──────────────────────────────────────────────
  useEffect(() => {
    if (query.trim().length < 2) {
      setUserResults([]);
      setDropdownOpen(false);
      return;
    }
    if (searchRef.current) clearTimeout(searchRef.current);
    searchRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const results = await searchUsers(query.trim());
        setUserResults(results);
        setDropdownOpen(results.length > 0);
      } catch {
        setUserResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => {
      if (searchRef.current) clearTimeout(searchRef.current);
    };
  }, [query]);

  // ── handlers ──────────────────────────────────────────────────────────────
  const handleSelectUser = (user: CmsUser) => {
    setSelectedUser(user);
    setQuery(`${user.firstName} ${user.lastName} (${user.email})`);
    setDropdownOpen(false);
    setUserResults([]);
  };

  const handleShare = async () => {
    if (!selectedUser) {
      setError("Please select a user to share with.");
      return;
    }
    setError("");
    setSharing(true);
    try {
      await shareFile(fileUuid, {
        sharedWithUserUuid: selectedUser.id,
        permission,
        allowDownload,
        watermarkEnabled: watermark,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null,
      });
      setSuccess(`Shared with ${selectedUser.firstName} ${selectedUser.lastName}`);
      setSelectedUser(null);
      setQuery("");
      setPermission("VIEWER");
      setAllowDownload(true);
      setWatermark(false);
      setExpiresAt("");
      await loadShares();
      setTimeout(() => setSuccess(""), 3000);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { error?: { message?: string }; message?: string } } };
      setError(
        e.response?.data?.error?.message ||
          e.response?.data?.message ||
          "Failed to share file."
      );
    } finally {
      setSharing(false);
    }
  };

  const handleRevoke = async (shareUuid: string) => {
    try {
      await revokeFileShare(shareUuid);
      setShares((prev) => prev.filter((s) => s.uuid !== shareUuid));
    } catch {
      /* ignore */
    }
  };

  const handlePermissionChange = async (
    shareUuid: string,
    newPermission: "VIEWER" | "EDITOR"
  ) => {
    try {
      const updated = await updateFileShare(shareUuid, { permission: newPermission });
      setShares((prev) =>
        prev.map((s) => (s.uuid === shareUuid ? updated : s))
      );
    } catch {
      /* ignore */
    }
  };

  const handleClose = () => {
    setQuery("");
    setSelectedUser(null);
    setUserResults([]);
    setDropdownOpen(false);
    setError("");
    setSuccess("");
    setPermission("VIEWER");
    setAllowDownload(true);
    setWatermark(false);
    setExpiresAt("");
    onClose();
  };

  if (!open) return null;

  // ── render ─────────────────────────────────────────────────────────────────
  return (
    <div
      onClick={handleClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.5)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1100,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: "#fff",
          borderRadius: "16px",
          width: "100%",
          maxWidth: "520px",
          maxHeight: "90vh",
          overflow: "hidden",
          display: "flex",
          flexDirection: "column",
          boxShadow: "0 24px 64px rgba(0,0,0,0.18)",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "18px 22px",
            borderBottom: "1px solid #e2e8f0",
            flexShrink: 0,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <div
              style={{
                width: "36px",
                height: "36px",
                borderRadius: "10px",
                background: "linear-gradient(135deg,#3b82f6,#8b5cf6)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "17px",
                flexShrink: 0,
              }}
            >
              👥
            </div>
            <div>
              <div style={{ fontSize: "15px", fontWeight: 700, color: "#1e293b" }}>
                Share File
              </div>
              <div
                style={{
                  fontSize: "12px",
                  color: "#64748b",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  maxWidth: "320px",
                }}
              >
                {fileName}
              </div>
            </div>
          </div>
          <button
            onClick={handleClose}
            style={{
              background: "none",
              border: "none",
              fontSize: "20px",
              cursor: "pointer",
              color: "#94a3b8",
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        </div>

        {/* Scrollable body */}
        <div style={{ overflowY: "auto", flex: 1, padding: "20px 22px" }}>

          {/* Error / success banners */}
          {error && (
            <div
              style={{
                padding: "10px 14px",
                background: "#fef2f2",
                border: "1px solid #fecaca",
                borderRadius: "8px",
                fontSize: "13px",
                color: "#dc2626",
                marginBottom: "14px",
              }}
            >
              {error}
            </div>
          )}
          {success && (
            <div
              style={{
                padding: "10px 14px",
                background: "#f0fdf4",
                border: "1px solid #bbf7d0",
                borderRadius: "8px",
                fontSize: "13px",
                color: "#16a34a",
                marginBottom: "14px",
              }}
            >
              ✅ {success}
            </div>
          )}

          {/* ── User selector ── */}
          <section style={{ marginBottom: "18px" }}>
            <label
              style={{
                display: "block",
                fontSize: "12px",
                fontWeight: 600,
                color: "#374151",
                marginBottom: "6px",
              }}
            >
              Share with a person
            </label>
            <div style={{ position: "relative" }}>
              <input
                ref={inputRef}
                type="text"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setSelectedUser(null);
                }}
                placeholder="Search by name or email..."
                style={{
                  width: "100%",
                  padding: "10px 14px",
                  fontSize: "13px",
                  border: "1.5px solid #e2e8f0",
                  borderRadius: "10px",
                  outline: "none",
                  boxSizing: "border-box",
                  color: "#1e293b",
                }}
              />
              {searching && (
                <span
                  style={{
                    position: "absolute",
                    right: "12px",
                    top: "50%",
                    transform: "translateY(-50%)",
                    fontSize: "12px",
                    color: "#94a3b8",
                  }}
                >
                  …
                </span>
              )}
              {dropdownOpen && userResults.length > 0 && (
                <div
                  style={{
                    position: "absolute",
                    top: "100%",
                    left: 0,
                    right: 0,
                    background: "#fff",
                    border: "1px solid #e2e8f0",
                    borderRadius: "10px",
                    boxShadow: "0 8px 24px rgba(0,0,0,0.12)",
                    zIndex: 200,
                    overflow: "hidden",
                    marginTop: "4px",
                  }}
                >
                  {userResults.map((u) => (
                    <div
                      key={u.id}
                      onClick={() => handleSelectUser(u)}
                      style={{
                        padding: "10px 14px",
                        cursor: "pointer",
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        borderBottom: "1px solid #f1f5f9",
                      }}
                      onMouseEnter={(e) =>
                        ((e.currentTarget as HTMLElement).style.background = "#f8fafc")
                      }
                      onMouseLeave={(e) =>
                        ((e.currentTarget as HTMLElement).style.background = "")
                      }
                    >
                      <div
                        style={{
                          width: "32px",
                          height: "32px",
                          borderRadius: "50%",
                          background: "linear-gradient(135deg,#3b82f6,#8b5cf6)",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          color: "#fff",
                          fontWeight: 700,
                          fontSize: "13px",
                          flexShrink: 0,
                        }}
                      >
                        {u.firstName[0]?.toUpperCase()}
                        {u.lastName[0]?.toUpperCase()}
                      </div>
                      <div>
                        <div
                          style={{ fontSize: "13px", fontWeight: 600, color: "#1e293b" }}
                        >
                          {u.firstName} {u.lastName}
                        </div>
                        <div style={{ fontSize: "11px", color: "#64748b" }}>
                          {u.email}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>

          {/* ── Permission + options ── */}
          <div
            style={{
              background: "#f8fafc",
              borderRadius: "12px",
              padding: "14px 16px",
              marginBottom: "18px",
              display: "flex",
              flexDirection: "column",
              gap: "12px",
            }}
          >
            {/* Permission level */}
            <div>
              <label
                style={{
                  fontSize: "12px",
                  fontWeight: 600,
                  color: "#374151",
                  display: "block",
                  marginBottom: "6px",
                }}
              >
                Permission
              </label>
              <div style={{ display: "flex", gap: "8px" }}>
                {(["VIEWER", "EDITOR"] as const).map((p) => (
                  <button
                    key={p}
                    onClick={() => setPermission(p)}
                    style={{
                      flex: 1,
                      padding: "8px",
                      borderRadius: "8px",
                      border: permission === p ? "2px solid #3b82f6" : "2px solid transparent",
                      background: permission === p ? "#eff6ff" : "#e2e8f0",
                      color: permission === p ? "#2563eb" : "#64748b",
                      fontWeight: 600,
                      fontSize: "13px",
                      cursor: "pointer",
                    }}
                  >
                    {p === "VIEWER" ? "👁 Viewer" : "✏️ Editor"}
                  </button>
                ))}
              </div>
              <div style={{ fontSize: "11px", color: "#94a3b8", marginTop: "4px" }}>
                {permission === "VIEWER"
                  ? "Can view and (if enabled) download the file"
                  : "Can view, download, and collaborate on the file"}
              </div>
            </div>

            {/* Toggles */}
            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              {[
                {
                  label: "⬇️ Allow download",
                  checked: allowDownload,
                  set: setAllowDownload,
                },
                {
                  label: "💧 Enable watermark",
                  checked: watermark,
                  set: setWatermark,
                },
              ].map(({ label, checked, set }) => (
                <label
                  key={label}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                    fontSize: "13px",
                    color: "#374151",
                    cursor: "pointer",
                  }}
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(e) => set(e.target.checked)}
                    style={{ accentColor: "#2563eb", cursor: "pointer" }}
                  />
                  {label}
                </label>
              ))}
            </div>

            {/* Expiry */}
            <div>
              <label
                style={{
                  fontSize: "12px",
                  fontWeight: 600,
                  color: "#374151",
                  display: "block",
                  marginBottom: "4px",
                }}
              >
                Expires at{" "}
                <span style={{ color: "#9ca3af", fontWeight: 400 }}>(optional)</span>
              </label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                style={{
                  width: "100%",
                  padding: "8px 12px",
                  fontSize: "12px",
                  border: "1px solid #e2e8f0",
                  borderRadius: "8px",
                  outline: "none",
                  boxSizing: "border-box",
                  color: "#1e293b",
                }}
              />
            </div>
          </div>

          {/* Share button */}
          <button
            onClick={handleShare}
            disabled={sharing || !selectedUser}
            style={{
              width: "100%",
              padding: "12px",
              background: sharing || !selectedUser ? "#cbd5e1" : "#2563eb",
              color: "#fff",
              border: "none",
              borderRadius: "10px",
              cursor: sharing || !selectedUser ? "not-allowed" : "pointer",
              fontSize: "14px",
              fontWeight: 700,
              marginBottom: "22px",
            }}
          >
            {sharing ? "Sharing…" : "👥 Share with this person"}
          </button>

          {/* ── Existing shares list ── */}
          {loadingShares ? (
            <div style={{ fontSize: "13px", color: "#94a3b8", textAlign: "center" }}>
              Loading…
            </div>
          ) : shares.length > 0 ? (
            <div>
              <div
                style={{
                  fontSize: "12px",
                  fontWeight: 700,
                  color: "#374151",
                  marginBottom: "10px",
                  textTransform: "uppercase",
                  letterSpacing: "0.05em",
                }}
              >
                People with access ({shares.length})
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {shares.map((share) => (
                  <div
                    key={share.uuid}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "10px",
                      padding: "10px 12px",
                      background: "#f8fafc",
                      borderRadius: "10px",
                      border: "1px solid #e2e8f0",
                    }}
                  >
                    {/* Avatar */}
                    <div
                      style={{
                        width: "34px",
                        height: "34px",
                        borderRadius: "50%",
                        background: "linear-gradient(135deg,#3b82f6,#8b5cf6)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        color: "#fff",
                        fontWeight: 700,
                        fontSize: "12px",
                        flexShrink: 0,
                      }}
                    >
                      {share.sharedWith.firstName[0]?.toUpperCase()}
                      {share.sharedWith.lastName[0]?.toUpperCase()}
                    </div>

                    {/* Name + email */}
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div
                        style={{
                          fontSize: "13px",
                          fontWeight: 600,
                          color: "#1e293b",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {share.sharedWith.firstName} {share.sharedWith.lastName}
                      </div>
                      <div
                        style={{
                          fontSize: "11px",
                          color: "#64748b",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {share.sharedWith.email}
                        {share.expiresAt && ` · expires ${formatExpiry(share.expiresAt)}`}
                      </div>
                    </div>

                    {/* Permission selector */}
                    <select
                      value={share.permission}
                      onChange={(e) =>
                        handlePermissionChange(
                          share.uuid,
                          e.target.value as "VIEWER" | "EDITOR"
                        )
                      }
                      style={{
                        padding: "4px 8px",
                        fontSize: "12px",
                        borderRadius: "6px",
                        border: "1px solid #e2e8f0",
                        background: badge(share.permission).background,
                        color: badge(share.permission).color,
                        fontWeight: 600,
                        cursor: "pointer",
                      }}
                    >
                      <option value="VIEWER">Viewer</option>
                      <option value="EDITOR">Editor</option>
                    </select>

                    {/* Revoke */}
                    <button
                      onClick={() => handleRevoke(share.uuid)}
                      title="Revoke access"
                      style={{
                        background: "none",
                        border: "none",
                        cursor: "pointer",
                        fontSize: "16px",
                        color: "#ef4444",
                        padding: "0 4px",
                        flexShrink: 0,
                      }}
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div
              style={{
                textAlign: "center",
                fontSize: "13px",
                color: "#94a3b8",
                padding: "12px",
              }}
            >
              No one else has access to this file yet.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default InternalShareDialog;
