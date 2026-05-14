import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { workspacesApi } from "../api/workspaces";
import { useDashboard } from "../hooks/useDashboard";
import { PendingApprovalsWidget } from "../components/PendingApprovalsWidget";
import RecentFilesWidget from "../components/dashboard/RecentFilesWidget";
import ActivityFeedWidget from "../components/dashboard/ActivityFeedWidget";
import StorageUsageWidget from "../components/dashboard/StorageUsageWidget";
import SharedItemsWidget from "../components/dashboard/SharedItemsWidget";
import AlertsWidget from "../components/dashboard/AlertsWidget";
import NotificationPanel from "../components/dashboard/NotificationPanel";
import type { Workspace } from "../types/models";

/* ─── colour tokens ─────────────────────────────────────────────────────── */
const C = {
  bg: "#f0f4f8",
  card: "#ffffff",
  border: "#e2e8f0",
  blue: "#3b82f6",
  green: "#10b981",
  amber: "#f59e0b",
  red: "#ef4444",
  purple: "#8b5cf6",
  text: "#1e293b",
  muted: "#64748b",
  subtle: "#94a3b8",
};

function relativeTime(dateStr: string) {
  const diff = Date.now() - new Date(dateStr).getTime();
  const m = Math.floor(diff / 60_000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

function formatBytes(bytes: number) {
  if (!bytes) return "0 B";
  const k = 1024,
    sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
}

/* ─── LiveBadge ─────────────────────────────────────────────────────────── */
function LiveBadge() {
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "5px",
        padding: "3px 9px",
        background: "#dcfce7",
        borderRadius: "20px",
        fontSize: "11px",
        fontWeight: 600,
        color: "#15803d",
        letterSpacing: "0.05em",
      }}
    >
      <span
        style={{
          width: "6px",
          height: "6px",
          borderRadius: "50%",
          background: "#22c55e",
          animation: "pulse-dot 2s infinite",
        }}
      />
      LIVE
    </span>
  );
}

/* ─── StatCard ───────────────────────────────────────────────────────────── */
interface StatCardProps {
  label: string;
  value: string | number;
  icon: string;
  color: string;
  loading?: boolean;
  sub?: string;
}
function StatCard({ label, value, icon, color, loading, sub }: StatCardProps) {
  return (
    <div
      style={{
        background: C.card,
        borderRadius: "12px",
        padding: "18px 20px",
        border: `1px solid ${C.border}`,
        display: "flex",
        gap: "14px",
        alignItems: "center",
      }}
    >
      <div
        style={{
          width: "44px",
          height: "44px",
          borderRadius: "10px",
          background: `${color}18`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "20px",
          flexShrink: 0,
        }}
      >
        {icon}
      </div>
      <div style={{ minWidth: 0 }}>
        {loading ? (
          <div
            style={{
              width: "48px",
              height: "22px",
              borderRadius: "4px",
              background: "#e2e8f0",
            }}
          />
        ) : (
          <div
            style={{
              fontSize: "22px",
              fontWeight: 700,
              color: C.text,
              lineHeight: 1,
            }}
          >
            {value}
          </div>
        )}
        <div style={{ fontSize: "12px", color: C.muted, marginTop: "3px" }}>
          {label}
        </div>
        {sub && (
          <div style={{ fontSize: "11px", color: C.subtle, marginTop: "1px" }}>
            {sub}
          </div>
        )}
      </div>
    </div>
  );
}

/* ─── WorkspaceCard ──────────────────────────────────────────────────────── */
function WorkspaceCard({
  ws,
  onClick,
}: {
  ws: Workspace;
  onClick: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  return (
    <div
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: C.card,
        borderRadius: "12px",
        padding: "16px 18px",
        border: `1px solid ${hovered ? C.blue : C.border}`,
        cursor: "pointer",
        transition: "border-color 0.15s, box-shadow 0.15s",
        boxShadow: hovered ? "0 4px 14px rgba(59,130,246,0.10)" : "none",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
        <div
          style={{
            width: "38px",
            height: "38px",
            borderRadius: "8px",
            flexShrink: 0,
            background: "linear-gradient(135deg, #3b82f6, #8b5cf6)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "16px",
            fontWeight: 700,
            color: "#fff",
          }}
        >
          {ws.name.charAt(0).toUpperCase()}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div
            style={{
              fontWeight: 600,
              fontSize: "14px",
              color: C.text,
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {ws.name}
          </div>
          <div
            style={{
              display: "flex",
              gap: "8px",
              alignItems: "center",
              marginTop: "4px",
              flexWrap: "wrap",
            }}
          >
            <span
              style={{
                fontSize: "11px",
                padding: "1px 7px",
                borderRadius: "20px",
                fontWeight: 500,
                background: ws.status === "ACTIVE" ? "#dcfce7" : "#f1f5f9",
                color: ws.status === "ACTIVE" ? "#15803d" : C.muted,
              }}
            >
              {ws.status}
            </span>
            {ws.memberCount !== undefined && (
              <span style={{ fontSize: "12px", color: C.muted }}>
                👥 {ws.memberCount}
              </span>
            )}
            {ws.myRole && (
              <span
                style={{
                  fontSize: "11px",
                  color: C.subtle,
                  marginLeft: "auto",
                }}
              >
                {ws.myRole.name}
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ─── DashboardPage ──────────────────────────────────────────────────────── */
export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [wsLoading, setWsLoading] = useState(true);
  const [notifPanelOpen, setNotifPanelOpen] = useState(false);

  const db = useDashboard();

  useEffect(() => {
    workspacesApi
      .list()
      .then((res) => setWorkspaces(res.data.data ?? []))
      .catch(() => {})
      .finally(() => setWsLoading(false));
  }, []);

  const storagePercent = db.summary?.storagePercentage ?? 0;
  const storageColor =
    storagePercent >= 95 ? C.red : storagePercent >= 80 ? C.amber : C.green;

  return (
    <>
      {/* Keyframe animation */}
      <style>{`
        @keyframes pulse-dot {
          0%, 100% { opacity: 1; transform: scale(1); }
          50%       { opacity: 0.4; transform: scale(0.75); }
        }
      `}</style>

      <div
        style={{ padding: "24px 28px", maxWidth: "1280px", margin: "0 auto" }}
      >
        {/* ── Header ──────────────────────────────────────────────────── */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-start",
            marginBottom: "20px",
            gap: "12px",
            flexWrap: "wrap",
          }}
        >
          <div>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "10px",
                marginBottom: "4px",
              }}
            >
              <h1
                style={{
                  fontSize: "22px",
                  fontWeight: 700,
                  color: C.text,
                  margin: 0,
                }}
              >
                Welcome back, {user?.firstName} 👋
              </h1>
              <LiveBadge />
            </div>
            <p style={{ color: C.muted, margin: 0, fontSize: "13px" }}>
              {user?.organizationName}
              {db.lastUpdated && (
                <span style={{ color: C.subtle }}>
                  {" "}
                  · Updated {relativeTime(db.lastUpdated.toISOString())}
                </span>
              )}
            </p>
          </div>

          {/* Header actions */}
          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            <button
              onClick={db.refreshAll}
              style={{
                padding: "8px 14px",
                background: C.card,
                border: `1px solid ${C.border}`,
                borderRadius: "8px",
                cursor: "pointer",
                fontSize: "13px",
                color: C.muted,
                fontWeight: 500,
                display: "flex",
                alignItems: "center",
                gap: "5px",
              }}
            >
              ↻ Refresh
            </button>

            {/* Notification bell */}
            <button
              onClick={() => setNotifPanelOpen((o) => !o)}
              style={{
                position: "relative",
                padding: "8px 12px",
                background: notifPanelOpen ? "#eff6ff" : C.card,
                border: `1px solid ${notifPanelOpen ? C.blue : C.border}`,
                borderRadius: "8px",
                cursor: "pointer",
                fontSize: "17px",
                lineHeight: 1,
              }}
              title="Notifications"
            >
              🔔
              {db.unreadCount > 0 && (
                <span
                  style={{
                    position: "absolute",
                    top: "5px",
                    right: "5px",
                    background: C.red,
                    color: "#fff",
                    borderRadius: "50%",
                    width: "16px",
                    height: "16px",
                    fontSize: "10px",
                    fontWeight: 700,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {db.unreadCount > 9 ? "9+" : db.unreadCount}
                </span>
              )}
            </button>

            <button
              onClick={() => {
                const last = localStorage.getItem("lastWorkspaceId");
                if (last) navigate(`/workspaces/${last}/search`);
                else if (workspaces[0])
                  navigate(`/workspaces/${workspaces[0].id}/search`);
              }}
              style={{
                padding: "8px 16px",
                background: C.blue,
                border: "none",
                borderRadius: "8px",
                cursor: "pointer",
                fontSize: "13px",
                color: "#fff",
                fontWeight: 500,
                display: "flex",
                alignItems: "center",
                gap: "6px",
              }}
            >
              🔍 Search Files
            </button>
          </div>
        </div>

        {/* ── Alerts ──────────────────────────────────────────────────── */}
        <AlertsWidget
          alerts={db.alerts}
          loading={db.alertsLoading}
          onDismiss={db.dismissAlert}
        />

        {/* ── Stats ───────────────────────────────────────────────────── */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
            gap: "12px",
            marginBottom: "20px",
          }}
        >
          <StatCard
            label="Recent Files"
            icon="📁"
            color={C.blue}
            loading={db.summaryLoading}
            value={db.summary?.recentFilesCount ?? "—"}
          />
          <StatCard
            label="Notifications"
            icon="🔔"
            color={C.purple}
            loading={db.notifLoading}
            value={db.unreadCount > 0 ? `${db.unreadCount} unread` : "All read"}
          />
          <StatCard
            label="Pending Approvals"
            icon="📋"
            color={C.amber}
            loading={db.summaryLoading}
            value={db.summary?.pendingApprovals ?? "—"}
          />
          <StatCard
            label="Storage Used"
            icon="💾"
            color={storageColor}
            loading={db.summaryLoading}
            value={`${storagePercent.toFixed(0)}%`}
            sub={
              db.summary
                ? `of ${formatBytes(db.summary.storageMaxBytes)}`
                : undefined
            }
          />
          <StatCard
            label="Active Alerts"
            icon="⚡"
            color={C.red}
            loading={db.summaryLoading}
            value={db.summary?.activeAlertsCount ?? 0}
          />
          <StatCard
            label="Workspaces"
            icon="▤"
            color={C.green}
            loading={wsLoading}
            value={workspaces.length}
            sub={`${workspaces.filter((w) => w.status === "ACTIVE").length} active`}
          />
        </div>

        {/* ── Main widget row ──────────────────────────────────────────── */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))",
            gap: "16px",
            marginBottom: "16px",
          }}
        >
          <RecentFilesWidget
            files={db.recentFiles}
            loading={db.filesLoading}
            error={db.filesError}
          />
          <ActivityFeedWidget
            events={db.activity}
            loading={db.activityLoading}
            error={db.activityError}
            hasMore={db.activityHasMore}
            newCount={db.newActivityCount}
            onLoadMore={db.loadMoreActivity}
          />
        </div>

        {/* ── Secondary widget row ─────────────────────────────────────── */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
            gap: "16px",
            marginBottom: "16px",
          }}
        >
          <StorageUsageWidget
            summary={db.summary}
            loading={db.summaryLoading}
          />
          <SharedItemsWidget
            items={db.sharedItems}
            loading={db.sharedLoading}
            direction={db.sharedDirection}
            onChangeDirection={db.setSharedDirection}
          />
        </div>

        {/* ── Pending Approvals ────────────────────────────────────────── */}
        <div style={{ marginBottom: "16px" }}>
          <PendingApprovalsWidget onViewAll={() => navigate("/approvals")} />
        </div>

        {/* ── Workspaces grid ──────────────────────────────────────────── */}
        <div
          style={{
            background: C.card,
            borderRadius: "14px",
            border: `1px solid ${C.border}`,
            padding: "18px 20px",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "14px",
            }}
          >
            <h2
              style={{
                margin: 0,
                fontSize: "15px",
                fontWeight: 600,
                color: C.text,
              }}
            >
              Your Workspaces
            </h2>
            <button
              onClick={() => navigate("/workspaces")}
              style={{
                fontSize: "13px",
                color: C.blue,
                background: "none",
                border: "none",
                cursor: "pointer",
                fontWeight: 500,
              }}
            >
              View all →
            </button>
          </div>

          {wsLoading ? (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
                gap: "10px",
              }}
            >
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  style={{
                    height: "68px",
                    borderRadius: "10px",
                    background: "#f1f5f9",
                  }}
                />
              ))}
            </div>
          ) : workspaces.length === 0 ? (
            <div style={{ padding: "40px", textAlign: "center" }}>
              <div style={{ fontSize: "32px", marginBottom: "8px" }}>📁</div>
              <div style={{ color: C.muted, fontSize: "14px" }}>
                No workspaces yet. Create one to get started.
              </div>
            </div>
          ) : (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))",
                gap: "10px",
              }}
            >
              {workspaces.map((ws) => (
                <WorkspaceCard
                  key={ws.id}
                  ws={ws}
                  onClick={() => {
                    localStorage.setItem("lastWorkspaceId", ws.id);
                    navigate(`/workspaces/${ws.id}`);
                  }}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ── Notification side panel ──────────────────────────────────── */}
      {notifPanelOpen && (
        <div
          onClick={(e) => {
            if (e.target === e.currentTarget) setNotifPanelOpen(false);
          }}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 500,
            background: "rgba(0,0,0,0.18)",
          }}
        >
          <NotificationPanel
            notifications={db.notifications}
            loading={db.notifLoading}
            unreadCount={db.unreadCount}
            onMarkRead={db.markNotifRead}
            onMarkAllRead={db.markAllNotifRead}
            onClose={() => setNotifPanelOpen(false)}
          />
        </div>
      )}
    </>
  );
}
