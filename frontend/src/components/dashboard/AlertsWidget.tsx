import type { Alert } from "../../types/dashboard";

interface Props {
  alerts: Alert[];
  loading: boolean;
  onDismiss: (id: string) => void;
}

type SevStyle = { bg: string; border: string; text: string; icon: string };

const SEV: { [key: string]: SevStyle } = {
  CRITICAL: { bg: "#fef2f2", border: "#fecaca", text: "#dc2626", icon: "🚨" },
  WARNING: { bg: "#fffbeb", border: "#fed7aa", text: "#d97706", icon: "⚠️" },
  INFO: { bg: "#eff6ff", border: "#bfdbfe", text: "#2563eb", icon: "ℹ️" },
};

const DEFAULT_SEV: SevStyle = {
  bg: "#eff6ff",
  border: "#bfdbfe",
  text: "#2563eb",
  icon: "ℹ️",
};

function getSev(severity: string): SevStyle {
  return SEV[severity] ?? DEFAULT_SEV;
}

export default function AlertsWidget({ alerts, loading, onDismiss }: Props) {
  if (loading || alerts.length === 0) return null;

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: "8px",
        marginBottom: "16px",
      }}
    >
      {alerts.map((alert) => {
        const s = getSev(alert.severity);
        return (
          <div
            key={alert.id}
            style={{
              display: "flex",
              alignItems: "flex-start",
              gap: "12px",
              padding: "12px 16px",
              borderRadius: "10px",
              background: s.bg,
              border: `1px solid ${s.border}`,
            }}
          >
            <span style={{ fontSize: "16px", flexShrink: 0, marginTop: "1px" }}>
              {s.icon}
            </span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: "13px", fontWeight: 600, color: s.text }}>
                {alert.title}
              </div>
              <div
                style={{ fontSize: "12px", color: "#64748b", marginTop: "2px" }}
              >
                {alert.message}
              </div>
            </div>
            <button
              onClick={() => onDismiss(alert.id)}
              title="Dismiss"
              style={{
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "#94a3b8",
                fontSize: "16px",
                padding: "2px",
                flexShrink: 0,
                lineHeight: 1,
              }}
            >
              ✕
            </button>
          </div>
        );
      })}
    </div>
  );
}
