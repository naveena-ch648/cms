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

// ── Helpers ───────────────────────────────────────────────────────────────────

function permBadgeStyle(permission: string) {
  return permission === "EDITOR"
    ? { background: "#eff6ff", color: "#2563eb" }
    : { background: "#f0fdf4", color: "#16a34a" };
}

function formatExpiry(iso: string | null) {
  if (!iso) return null;
  const d = new Date(iso);
  return d < new Date()
    ? `expired ${d.toLocaleDateString()}`
    : `expires ${d.toLocaleDateString()}`;
}

function initials(first: string, last: string) {
  return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
}

// ── Component ─────────────────────────────────────────────────────────────────

const InternalShareDialog: React.FC<InternalShareDialogProps> = ({
  fileUuid,
  fileName,
  open,
  onClose,
}) => {
  // ── Existing shares ────────────────────────────────────────────────────────
  const [shares, setShares] = useState<FileShare[]>([]);
  const [loadingShares, setLoadingShares] = useState(false);
  const [sharesError, setSharesError] = useState<string>("");

  // ── User search ────────────────────────────────────────────────────────────
  const [query, setQuery] = useState("");
  const [userResults, setUserResults] = useState<CmsUser[]>([]);
  const [searching, setSearching] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<CmsUser | null>(null);
  const [hoveredUserId, setHoveredUserId] = useState<string | null>(null);

  // ── Share options ──────────────────────────────────────────────────────────
  const [permission, setPermission] = useState<"VIEWER" | "EDITOR">("VIEWER");
  const [allowDownload, setAllowDownload] = useState(true);
  const [watermark, setWatermark] = useState(false);
  const [expiresAt, setExpiresAt] = useState("");

  // ── UI ─────────────────────────────────────────────────────────────────────
  const [sharing, setSharing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const searchContainerRef = useRef<HTMLDivElement>(null);

  // ── Load existing shares ───────────────────────────────────────────────────
  const loadShares = useCallback(async () => {
    if (!open) return;
    setLoadingShares(true);
    setSharesError("");
    try {
      const data = await listFileShares(fileUuid);
      setShares(data);
    } catch (err: unknown) {
      const e = err as { response?: { status?: number } };
      if (e.response?.status === 403) {
        setSharesError("no_permission");
      }
      // other errors: silently show empty list
    } finally {
      setLoadingShares(false);
    }
  }, [fileUuid, open]);

  useEffect(() => {
    if (open) loadShares();
    else {
      // reset all state when dialog closes
      setShares([]);
      setSharesError("");
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
    }
  }, [open, loadShares]);

  // ── Close dropdown on outside click ───────────────────────────────────────
  useEffect(() => {
    if (!dropdownOpen) return;
    const handler = (e: MouseEvent) => {
      if (
        searchContainerRef.current &&
        !searchContainerRef.current.contains(e.target as Node)
      ) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [dropdownOpen]);

  // ── Debounced user search ──────────────────────────────────────────────────
  useEffect(() => {
    if (selectedUser) return; // user already selected — don't re-search
    const trimmed = query.trim();

    if (trimmed.length < 2) {
      setUserResults([]);
      setDropdownOpen(false);
      return;
    }

    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const results = await searchUsers(trimmed);
        setUserResults(results);
        setDropdownOpen(results.length > 0);
      } catch {
        setUserResults([]);
        setDropdownOpen(false);
      } finally {
        setSearching(false);
      }
    }, 280);

    return () => {
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    };
  }, [query, selectedUser]);

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleSelectUser = (user: CmsUser) => {
    setSelectedUser(user);
    setQuery(`${user.firstName} ${user.lastName}  ·  ${user.email}`);
    setDropdownOpen(false);
    setUserResults([]);
    setError("");
  };

  const handleClearUser = () => {
    setSelectedUser(null);
    setQuery("");
    setUserResults([]);
    setDropdownOpen(false);
  };

  const handleShare = async () => {
    if (!selectedUser) {
      setError("Select a user first.");
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
      setSuccess(
        `✅ Shared with ${selectedUser.firstName} ${selectedUser.lastName}`
      );
      handleClearUser();
      await loadShares();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err: unknown) {
      const e = err as {
        response?: { data?: { error?: { message?: string }; message?: string } };
        message?: string;
      };
      setError(
        e.response?.data?.error?.message ||
          e.response?.data?.message ||
          e.message ||
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
    newPerm: "VIEWER" | "EDITOR"
  ) => {
    try {
      const updated = await updateFileShare(shareUuid, { permission: newPerm });
      setShares((prev) => prev.map((s) => (s.uuid === shareUuid ? updated : s)));
    } catch {
      /* ignore */
    }
  };

  if (!open) return null;

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div
      onClick={onClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.55)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1100,
        backdropFilter: "blur(2px)",
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: "#fff",
          borderRadius: "18px",
          width: "100%",
          maxWidth: "500px",
          maxHeight: "88vh",
          display: "flex",
          flexDirection: "column",
          boxShadow: "0 32px 80px rgba(0,0,0,0.22)",
          overflow: "hidden",
        }}
      >
        {/* ── Header ── */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "12px",
            padding: "18px 22px 16px",
            borderBottom: "1px solid #f1f5f9",
            flexShrink: 0,
          }}
        >
          <div
            style={{
              width: "38px",
              height: "38px",
              borderRadius: "10px",
              background: "linear-gradient(135deg,#3b82f6,#8b5cf6)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "18px",
              flexShrink: 0,
            }}
          >
            👥
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{ fontSize: "15px", fontWeight: 700, color: "#0f172a" }}
            >
              Share with people
            </div>
            <div
              style={{
                fontSize: "12px",
                color: "#64748b",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
              title={fileName}
            >
              {fileName}
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: "#f1f5f9",
              border: "none",
              borderRadius: "8px",
              width: "30px",
              height: "30px",
              cursor: "pointer",
              fontSize: "14px",
              color: "#64748b",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            ✕
          </button>
        </div>

        {/* ── Scrollable body ── */}
        <div style={{ overflowY: "auto", flex: 1, padding: "20px 22px" }}>

          {/* Banners */}
          {success && (
            <div
              style={{
                padding: "10px 14px",
                background: "#f0fdf4",
                border: "1px solid #86efac",
                borderRadius: "9px",
                fontSize: "13px",
                color: "#166534",
                marginBottom: "16px",
                fontWeight: 500,
              }}
            >
              {success}
            </div>
          )}
          {error && (
            <div
              style={{
                padding: "10px 14px",
                background: "#fef2f2",
                border: "1px solid #fca5a5",
                borderRadius: "9px",
                fontSize: "13px",
                color: "#dc2626",
                marginBottom: "16px",
              }}
            >
              {error}
            </div>
          )}

          {/* ── Step 1: User search ── */}
          <div style={{ marginBottom: "16px" }}>
            <label
              style={{
                display: "block",
                fontSize: "12px",
                fontWeight: 700,
                color: "#374151",
                marginBottom: "6px",
                textTransform: "uppercase",
                letterSpacing: "0.04em",
              }}
            >
              1 · Select person
            </label>

            <div ref={searchContainerRef} style={{ position: "relative" }}>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  border: selectedUser
                    ? "2px solid #3b82f6"
                    : "1.5px solid #e2e8f0",
                  borderRadius: "10px",
                  background: "#fff",
                  padding: "0 12px",
                  gap: "8px",
                  transition: "border-color 0.15s",
                }}
              >
                {selectedUser ? (
                  /* Avatar when user is selected */
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
                    {initials(selectedUser.firstName, selectedUser.lastName)}
                  </div>
                ) : (
                  <span style={{ color: "#94a3b8", fontSize: "15px" }}>🔍</span>
                )}

                <input
                  type="text"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    if (selectedUser) setSelectedUser(null);
                  }}
                  onFocus={() => {
                    if (userResults.length > 0) setDropdownOpen(true);
                  }}
                  placeholder="Type name or email to search..."
                  style={{
                    flex: 1,
                    padding: "11px 0",
                    fontSize: "13px",
                    border: "none",
                    outline: "none",
                    color: "#0f172a",
                    background: "transparent",
                    minWidth: 0,
                  }}
                />

                {searching && (
                  <span style={{ fontSize: "12px", color: "#94a3b8" }}>…</span>
                )}
                {selectedUser && (
                  <button
                    onClick={handleClearUser}
                    style={{
                      background: "none",
                      border: "none",
                      cursor: "pointer",
                      color: "#94a3b8",
                      fontSize: "14px",
                      padding: "0 2px",
                      lineHeight: 1,
                    }}
                  >
                    ✕
                  </button>
                )}
              </div>

              {/* Dropdown */}
              {dropdownOpen && userResults.length > 0 && (
                <div
                  style={{
                    position: "absolute",
                    top: "calc(100% + 4px)",
                    left: 0,
                    right: 0,
                    background: "#fff",
                    border: "1px solid #e2e8f0",
                    borderRadius: "12px",
                    boxShadow: "0 12px 32px rgba(0,0,0,0.14)",
                    zIndex: 300,
                    overflow: "hidden",
                    maxHeight: "220px",
                    overflowY: "auto",
                  }}
                >
                  {userResults.map((u) => (
                    <div
                      key={u.id}
                      onMouseDown={(e) => {
                        // onMouseDown fires before onBlur so we can select before dropdown closes
                        e.preventDefault();
                        handleSelectUser(u);
                      }}
                      onMouseEnter={() => setHoveredUserId(u.id)}
                      onMouseLeave={() => setHoveredUserId(null)}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        padding: "10px 14px",
                        cursor: "pointer",
                        background:
                          hoveredUserId === u.id ? "#f8fafc" : "#fff",
                        borderBottom: "1px solid #f1f5f9",
                        transition: "background 0.1s",
                      }}
                    >
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
                        {initials(u.firstName, u.lastName)}
                      </div>
                      <div style={{ minWidth: 0 }}>
                        <div
                          style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#0f172a",
                          }}
                        >
                          {u.firstName} {u.lastName}
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
                          {u.email}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* No results hint */}
              {!searching &&
                query.trim().length >= 2 &&
                userResults.length === 0 &&
                !selectedUser && (
                  <div
                    style={{
                      marginTop: "6px",
                      fontSize: "12px",
                      color: "#94a3b8",
                    }}
                  >
                    No users found for "{query.trim()}"
                  </div>
                )}
            </div>
          </div>

          {/* ── Step 2: Permission + options ── */}
          <div style={{ marginBottom: "16px" }}>
            <label
              style={{
                display: "block",
                fontSize: "12px",
                fontWeight: 700,
                color: "#374151",
                marginBottom: "10px",
                textTransform: "uppercase",
                letterSpacing: "0.04em",
              }}
            >
              2 · Set permission
            </label>

            <div
              style={{
                background: "#f8fafc",
                borderRadius: "12px",
                padding: "14px 16px",
                display: "flex",
                flexDirection: "column",
                gap: "12px",
              }}
            >
              {/* Permission toggle */}
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "8px",
                }}
              >
                {(["VIEWER", "EDITOR"] as const).map((p) => (
                  <button
                    key={p}
                    onClick={() => setPermission(p)}
                    style={{
                      padding: "10px 8px",
                      borderRadius: "9px",
                      border:
                        permission === p
                          ? "2px solid #3b82f6"
                          : "2px solid #e2e8f0",
                      background: permission === p ? "#eff6ff" : "#fff",
                      color: permission === p ? "#2563eb" : "#64748b",
                      fontWeight: 700,
                      fontSize: "13px",
                      cursor: "pointer",
                      transition: "all 0.15s",
                    }}
                  >
                    {p === "VIEWER" ? "👁 Viewer" : "✏️ Editor"}
                  </button>
                ))}
              </div>
              <p
                style={{
                  margin: 0,
                  fontSize: "11px",
                  color: "#94a3b8",
                }}
              >
                {permission === "VIEWER"
                  ? "Can view the file and preview content"
                  : "Can view, comment, and collaborate on the file"}
              </p>

              {/* Toggles */}
              <div style={{ display: "flex", gap: "20px" }}>
                {[
                  { label: "⬇️ Allow download", val: allowDownload, set: setAllowDownload },
                  { label: "💧 Watermark", val: watermark, set: setWatermark },
                ].map(({ label, val, set }) => (
                  <label
                    key={label}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "7px",
                      fontSize: "13px",
                      color: "#374151",
                      cursor: "pointer",
                      userSelect: "none",
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={val}
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
                    marginBottom: "5px",
                  }}
                >
                  Expires{" "}
                  <span style={{ color: "#9ca3af", fontWeight: 400 }}>
                    (optional)
                  </span>
                </label>
                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "8px 12px",
                    fontSize: "12px",
                    border: "1.5px solid #e2e8f0",
                    borderRadius: "8px",
                    outline: "none",
                    boxSizing: "border-box",
                    color: "#0f172a",
                    background: "#fff",
                  }}
                />
              </div>
            </div>
          </div>

          {/* ── Step 3: Share button ── */}
          <button
            onClick={handleShare}
            disabled={sharing || !selectedUser}
            style={{
              width: "100%",
              padding: "13px",
              background:
                sharing || !selectedUser
                  ? "#e2e8f0"
                  : "linear-gradient(135deg,#2563eb,#4f46e5)",
              color: sharing || !selectedUser ? "#94a3b8" : "#fff",
              border: "none",
              borderRadius: "11px",
              cursor: sharing || !selectedUser ? "not-allowed" : "pointer",
              fontSize: "14px",
              fontWeight: 700,
              marginBottom: "24px",
              transition: "opacity 0.15s",
              letterSpacing: "0.01em",
            }}
          >
            {sharing
              ? "Sharing…"
              : selectedUser
              ? `Share with ${selectedUser.firstName} ${selectedUser.lastName}`
              : "Select a person above to share"}
          </button>

          {/* ── Existing shares ── */}
          <div>
            <div
              style={{
                fontSize: "12px",
                fontWeight: 700,
                color: "#374151",
                textTransform: "uppercase",
                letterSpacing: "0.04em",
                marginBottom: "10px",
              }}
            >
              People with access
              {shares.length > 0 && (
                <span
                  style={{
                    marginLeft: "6px",
                    background: "#e2e8f0",
                    color: "#475569",
                    borderRadius: "10px",
                    padding: "1px 8px",
                    fontSize: "11px",
                    fontWeight: 600,
                  }}
                >
                  {shares.length}
                </span>
              )}
            </div>

            {loadingShares ? (
              <div style={{ textAlign: "center", padding: "16px", color: "#94a3b8", fontSize: "13px" }}>
                Loading…
              </div>
            ) : sharesError === "no_permission" ? (
              <div
                style={{
                  padding: "12px 16px",
                  background: "#fffbeb",
                  border: "1px solid #fde68a",
                  borderRadius: "9px",
                  fontSize: "13px",
                  color: "#92400e",
                }}
              >
                You need Editor or Admin access to view and manage shares for this file.
              </div>
            ) : shares.length === 0 ? (
              <div
                style={{
                  padding: "18px",
                  textAlign: "center",
                  fontSize: "13px",
                  color: "#94a3b8",
                  background: "#f8fafc",
                  borderRadius: "10px",
                }}
              >
                No one else has access yet.
              </div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {shares.map((share) => {
                  const exp = formatExpiry(share.expiresAt);
                  return (
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
                          width: "36px",
                          height: "36px",
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
                        {initials(
                          share.sharedWith.firstName,
                          share.sharedWith.lastName
                        )}
                      </div>

                      {/* Name + meta */}
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div
                          style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#0f172a",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {share.sharedWith.firstName}{" "}
                          {share.sharedWith.lastName}
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
                          {exp && (
                            <span
                              style={{
                                color: exp.startsWith("expired")
                                  ? "#dc2626"
                                  : "#d97706",
                              }}
                            >
                              {" · "}{exp}
                            </span>
                          )}
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
                          borderRadius: "7px",
                          border: "1px solid #e2e8f0",
                          ...permBadgeStyle(share.permission),
                          fontWeight: 700,
                          cursor: "pointer",
                          outline: "none",
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
                          color: "#ef4444",
                          fontSize: "16px",
                          padding: "0 2px",
                          lineHeight: 1,
                          flexShrink: 0,
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default InternalShareDialog;
