import type { DashboardSummary } from "../../types/dashboard";

interface Props {
  summary: DashboardSummary | null;
  loading: boolean;
}

function formatBytes(bytes: number) {
  if (!bytes) return "0 B";
  const k = 1024,
    sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`;
}

export default function StorageUsageWidget({ summary, loading }: Props) {
  const pct = summary?.storagePercentage ?? 0;
  const barColor = pct >= 95 ? "#ef4444" : pct >= 80 ? "#f59e0b" : "#3b82f6";
  const statusLabel =
    pct >= 95
      ? "🚨 Critical — storage almost full"
      : pct >= 80
        ? "⚠️ Warning — storage getting full"
        : null;

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
          💾 Storage Usage
        </h3>
        <div
          style={{ height: "72px", borderRadius: "8px", background: "#f1f5f9" }}
        />
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
      <h3
        style={{
          margin: "0 0 16px",
          fontSize: "14px",
          fontWeight: 600,
          color: "#1e293b",
        }}
      >
        💾 Storage Usage
      </h3>

      {/* Percentage display */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "baseline",
          marginBottom: "8px",
        }}
      >
        <span style={{ fontSize: "13px", color: "#64748b" }}>
          {formatBytes(summary?.storageUsedBytes ?? 0)} used
          <span style={{ color: "#94a3b8" }}>
            {" "}
            of {formatBytes(summary?.storageMaxBytes ?? 0)}
          </span>
        </span>
        <span style={{ fontSize: "16px", fontWeight: 700, color: barColor }}>
          {pct.toFixed(1)}%
        </span>
      </div>

      {/* Progress bar */}
      <div
        style={{
          height: "10px",
          background: "#f1f5f9",
          borderRadius: "999px",
          overflow: "hidden",
          marginBottom: "10px",
        }}
      >
        <div
          style={{
            height: "100%",
            borderRadius: "999px",
            width: `${Math.min(pct, 100)}%`,
            background:
              pct >= 95
                ? "linear-gradient(90deg, #ef4444, #dc2626)"
                : pct >= 80
                  ? "linear-gradient(90deg, #f59e0b, #d97706)"
                  : "linear-gradient(90deg, #3b82f6, #2563eb)",
            transition: "width 0.6s ease",
          }}
        />
      </div>

      {/* Markers */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontSize: "10px",
          color: "#cbd5e1",
          marginBottom: "8px",
        }}
      >
        <span>0%</span>
        <span style={{ color: pct >= 80 ? "#f59e0b" : "#cbd5e1" }}>80%</span>
        <span style={{ color: pct >= 95 ? "#ef4444" : "#cbd5e1" }}>95%</span>
        <span>100%</span>
      </div>

      {statusLabel && (
        <div
          style={{
            padding: "8px 12px",
            borderRadius: "8px",
            fontSize: "12px",
            fontWeight: 500,
            background: pct >= 95 ? "#fef2f2" : "#fffbeb",
            color: pct >= 95 ? "#dc2626" : "#d97706",
            border: `1px solid ${pct >= 95 ? "#fecaca" : "#fed7aa"}`,
          }}
        >
          {statusLabel}
        </div>
      )}
    </div>
  );
}
