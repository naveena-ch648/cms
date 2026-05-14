import React, { useState } from "react";
import { createShareLink } from "../api/sharing";
import type { CreateShareLinkRequest, ShareLink } from "../types/sharing";

interface ShareLinkDialogProps {
  workspaceId: string;
  resourceType: "FILE" | "FOLDER";
  resourceUuid: string;
  resourceName: string;
  open: boolean;
  onClose: () => void;
  onCreated?: (link: ShareLink) => void;
}

const ShareLinkDialog: React.FC<ShareLinkDialogProps> = ({
  workspaceId,
  resourceType,
  resourceUuid,
  resourceName,
  open,
  onClose,
  onCreated,
}) => {
  const [password, setPassword] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [allowDownload, setAllowDownload] = useState(true);
  const [watermarkEnabled, setWatermarkEnabled] = useState(false);
  const [shareWithEmails, setShareWithEmails] = useState("");
  const [loading, setLoading] = useState(false);
  const [createdLink, setCreatedLink] = useState<ShareLink | null>(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  const handleCreate = async () => {
    setLoading(true);
    setError("");
    try {
      const request: CreateShareLinkRequest = {
        resourceType,
        allowDownload,
        watermarkEnabled,
        ...(resourceType === "FILE"
          ? { fileUuid: resourceUuid }
          : { folderUuid: resourceUuid }),
        ...(password ? { password } : {}),
        ...(expiresAt ? { expiresAt: new Date(expiresAt).toISOString() } : {}),
      };
      const link = await createShareLink(workspaceId, request);
      setCreatedLink(link);
      onCreated?.(link);
    } catch (err: unknown) {
      const e = err as {
        response?: { data?: { message?: string; error?: string } };
        message?: string;
      };
      const msg =
        e.response?.data?.message ||
        e.response?.data?.error ||
        e.message ||
        "Failed to create share link. Make sure you have Editor or Admin access.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (createdLink) {
      navigator.clipboard.writeText(createdLink.url).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      });
    }
  };

  const handleShareViaEmail = () => {
    if (!createdLink) return;
    const emails = shareWithEmails
      .split(/[,;\s]+/)
      .map((e) => e.trim())
      .filter(Boolean)
      .join(",");
    const subject = encodeURIComponent(`Shared with you: ${resourceName}`);
    const body = encodeURIComponent(
      `Hi,\n\nI've shared "${resourceName}" with you.\n\nAccess it here: ${createdLink.url}\n\n${createdLink.hasPassword ? "Note: This link is password protected.\n\n" : ""}${createdLink.expiresAt ? `Link expires on: ${new Date(createdLink.expiresAt).toLocaleDateString()}\n\n` : ""}`
    );
    window.open(`mailto:${emails}?subject=${subject}&body=${body}`);
  };

  const handleShareViaWhatsApp = () => {
    if (!createdLink) return;
    const text = encodeURIComponent(
      `I've shared "${resourceName}" with you: ${createdLink.url}`
    );
    window.open(`https://wa.me/?text=${text}`, "_blank");
  };

  const handleShareViaSlack = () => {
    if (!createdLink) return;
    const text = encodeURIComponent(createdLink.url);
    window.open(
      `https://slack.com/intl/en-in/share?text=${text}`,
      "_blank"
    );
  };

  const handleClose = () => {
    setCreatedLink(null);
    setPassword("");
    setExpiresAt("");
    setShareWithEmails("");
    setAllowDownload(true);
    setWatermarkEnabled(false);
    setError("");
    onClose();
  };

  if (!open) return null;

  return (
    <div
      onClick={handleClose}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(0,0,0,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          background: "#fff",
          borderRadius: "14px",
          width: "100%",
          maxWidth: "440px",
          boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
          overflow: "hidden",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "16px 20px",
            borderBottom: "1px solid #e2e8f0",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span style={{ fontSize: "18px" }}>🔗</span>
            <div>
              <div
                style={{ fontSize: "14px", fontWeight: 600, color: "#1e293b" }}
              >
                Share File
              </div>
              <div
                style={{
                  fontSize: "12px",
                  color: "#64748b",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  maxWidth: "280px",
                }}
              >
                {resourceName}
              </div>
            </div>
          </div>
          <button
            onClick={handleClose}
            style={{
              background: "none",
              border: "none",
              fontSize: "18px",
              cursor: "pointer",
              color: "#94a3b8",
              width: "28px",
              height: "28px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              borderRadius: "6px",
            }}
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <div style={{ padding: "20px" }}>
          {createdLink ? (
            /* ── Success state ── */
            <div>
              <div
                style={{
                  background: "#f0fdf4",
                  border: "1px solid #bbf7d0",
                  borderRadius: "10px",
                  padding: "14px",
                  marginBottom: "16px",
                  display: "flex",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span style={{ fontSize: "20px" }}>✅</span>
                <div>
                  <div
                    style={{
                      fontSize: "13px",
                      fontWeight: 600,
                      color: "#16a34a",
                    }}
                  >
                    Link created!
                  </div>
                  <div
                    style={{
                      fontSize: "12px",
                      color: "#15803d",
                      marginTop: "2px",
                    }}
                  >
                    Anyone with this link can view the file.
                  </div>
                </div>
              </div>

              {/* URL copy box */}
              <div
                style={{ display: "flex", gap: "8px", marginBottom: "14px" }}
              >
                <input
                  readOnly
                  value={createdLink.url}
                  style={{
                    flex: 1,
                    padding: "9px 12px",
                    fontSize: "12px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    background: "#f8fafc",
                    color: "#475569",
                    outline: "none",
                  }}
                />
                <button
                  onClick={handleCopy}
                  style={{
                    padding: "9px 16px",
                    background: copied ? "#16a34a" : "#2563eb",
                    color: "#fff",
                    border: "none",
                    borderRadius: "8px",
                    cursor: "pointer",
                    fontSize: "13px",
                    fontWeight: 500,
                    transition: "background 0.2s",
                    whiteSpace: "nowrap",
                  }}
                >
                  {copied ? "✓ Copied" : "Copy"}
                </button>
              </div>

              {/* Link metadata */}
              <div
                style={{
                  display: "flex",
                  gap: "10px",
                  flexWrap: "wrap",
                  marginBottom: "16px",
                }}
              >
                {createdLink.hasPassword && (
                  <span
                    style={{
                      fontSize: "11px",
                      padding: "3px 8px",
                      borderRadius: "6px",
                      background: "#fffbeb",
                      color: "#d97706",
                      fontWeight: 500,
                    }}
                  >
                    🔒 Password protected
                  </span>
                )}
                {createdLink.allowDownload && (
                  <span
                    style={{
                      fontSize: "11px",
                      padding: "3px 8px",
                      borderRadius: "6px",
                      background: "#eff6ff",
                      color: "#2563eb",
                      fontWeight: 500,
                    }}
                  >
                    ⬇️ Download allowed
                  </span>
                )}
                {createdLink.expiresAt && (
                  <span
                    style={{
                      fontSize: "11px",
                      padding: "3px 8px",
                      borderRadius: "6px",
                      background: "#f8fafc",
                      color: "#64748b",
                      fontWeight: 500,
                    }}
                  >
                    ⏱ Expires{" "}
                    {new Date(createdLink.expiresAt).toLocaleDateString()}
                  </span>
                )}
              </div>

              {/* Share with people (email) */}
              <div style={{ marginBottom: "14px" }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 500,
                    color: "#374151",
                    marginBottom: "5px",
                  }}
                >
                  Share with people
                  <span style={{ color: "#9ca3af", fontWeight: 400 }}>
                    {" "}(enter emails, comma-separated)
                  </span>
                </label>
                <div style={{ display: "flex", gap: "8px" }}>
                  <input
                    type="text"
                    value={shareWithEmails}
                    onChange={(e) => setShareWithEmails(e.target.value)}
                    placeholder="name@example.com, another@example.com"
                    style={{
                      flex: 1,
                      padding: "8px 12px",
                      fontSize: "12px",
                      border: "1px solid #e2e8f0",
                      borderRadius: "8px",
                      outline: "none",
                      color: "#1e293b",
                    }}
                  />
                  <button
                    onClick={handleShareViaEmail}
                    disabled={!shareWithEmails.trim()}
                    style={{
                      padding: "8px 14px",
                      background: shareWithEmails.trim() ? "#2563eb" : "#cbd5e1",
                      color: "#fff",
                      border: "none",
                      borderRadius: "8px",
                      cursor: shareWithEmails.trim() ? "pointer" : "not-allowed",
                      fontSize: "12px",
                      fontWeight: 500,
                      whiteSpace: "nowrap",
                    }}
                  >
                    ✉️ Send
                  </button>
                </div>
              </div>

              {/* Share destinations */}
              <div>
                <div
                  style={{
                    fontSize: "12px",
                    fontWeight: 500,
                    color: "#374151",
                    marginBottom: "8px",
                  }}
                >
                  Share via
                </div>
                <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                  <button
                    onClick={handleShareViaWhatsApp}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      padding: "8px 14px",
                      background: "#25d366",
                      color: "#fff",
                      border: "none",
                      borderRadius: "8px",
                      cursor: "pointer",
                      fontSize: "12px",
                      fontWeight: 500,
                    }}
                  >
                    💬 WhatsApp
                  </button>
                  <button
                    onClick={() => handleShareViaEmail()}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      padding: "8px 14px",
                      background: "#475569",
                      color: "#fff",
                      border: "none",
                      borderRadius: "8px",
                      cursor: "pointer",
                      fontSize: "12px",
                      fontWeight: 500,
                    }}
                  >
                    ✉️ Email
                  </button>
                  <button
                    onClick={handleShareViaSlack}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "6px",
                      padding: "8px 14px",
                      background: "#4A154B",
                      color: "#fff",
                      border: "none",
                      borderRadius: "8px",
                      cursor: "pointer",
                      fontSize: "12px",
                      fontWeight: 500,
                    }}
                  >
                    💼 Slack
                  </button>
                </div>
              </div>

              <button
                onClick={handleClose}
                style={{
                  width: "100%",
                  padding: "10px",
                  background: "#f1f5f9",
                  border: "none",
                  borderRadius: "8px",
                  cursor: "pointer",
                  fontSize: "13px",
                  color: "#475569",
                  fontWeight: 500,
                  marginTop: "4px",
                }}
              >
                Done
              </button>
            </div>
          ) : (
            /* ── Create form ── */
            <div
              style={{ display: "flex", flexDirection: "column", gap: "14px" }}
            >
              {error && (
                <div
                  style={{
                    padding: "10px 14px",
                    background: "#fef2f2",
                    border: "1px solid #fecaca",
                    borderRadius: "8px",
                    fontSize: "13px",
                    color: "#dc2626",
                  }}
                >
                  {error}
                </div>
              )}

              {/* Share with people */}
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 500,
                    color: "#374151",
                    marginBottom: "5px",
                  }}
                >
                  Share with{" "}
                  <span style={{ color: "#9ca3af", fontWeight: 400 }}>
                    (emails — link will open in their mail client)
                  </span>
                </label>
                <input
                  type="text"
                  value={shareWithEmails}
                  onChange={(e) => setShareWithEmails(e.target.value)}
                  placeholder="name@example.com, another@example.com"
                  style={{
                    width: "100%",
                    padding: "9px 12px",
                    fontSize: "13px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    outline: "none",
                    boxSizing: "border-box",
                    color: "#1e293b",
                  }}
                />
              </div>

              {/* Password */}
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 500,
                    color: "#374151",
                    marginBottom: "5px",
                  }}
                >
                  Password{" "}
                  <span style={{ color: "#9ca3af", fontWeight: 400 }}>
                    (optional)
                  </span>
                </label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Leave empty for no password"
                  style={{
                    width: "100%",
                    padding: "9px 12px",
                    fontSize: "13px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    outline: "none",
                    boxSizing: "border-box",
                    color: "#1e293b",
                  }}
                />
              </div>

              {/* Expiry */}
              <div>
                <label
                  style={{
                    display: "block",
                    fontSize: "12px",
                    fontWeight: 500,
                    color: "#374151",
                    marginBottom: "5px",
                  }}
                >
                  Expires at{" "}
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
                    padding: "9px 12px",
                    fontSize: "13px",
                    border: "1px solid #e2e8f0",
                    borderRadius: "8px",
                    outline: "none",
                    boxSizing: "border-box",
                    color: "#1e293b",
                  }}
                />
              </div>

              {/* Toggles */}
              <div
                style={{
                  background: "#f8fafc",
                  borderRadius: "8px",
                  padding: "12px 14px",
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                {[
                  {
                    label: "⬇️ Allow download",
                    checked: allowDownload,
                    onChange: setAllowDownload,
                  },
                  {
                    label: "💧 Enable watermark",
                    checked: watermarkEnabled,
                    onChange: setWatermarkEnabled,
                  },
                ].map(({ label, checked, onChange }) => (
                  <label
                    key={label}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "10px",
                      cursor: "pointer",
                      fontSize: "13px",
                      color: "#374151",
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={(e) => onChange(e.target.checked)}
                      style={{
                        width: "15px",
                        height: "15px",
                        accentColor: "#2563eb",
                        cursor: "pointer",
                      }}
                    />
                    {label}
                  </label>
                ))}
              </div>

              <button
                onClick={handleCreate}
                disabled={loading}
                style={{
                  padding: "11px",
                  background: loading ? "#93c5fd" : "#2563eb",
                  color: "#fff",
                  border: "none",
                  borderRadius: "8px",
                  cursor: loading ? "not-allowed" : "pointer",
                  fontSize: "14px",
                  fontWeight: 600,
                  transition: "background 0.2s",
                }}
              >
                {loading ? "Creating…" : "🔗 Create Share Link"}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ShareLinkDialog;
