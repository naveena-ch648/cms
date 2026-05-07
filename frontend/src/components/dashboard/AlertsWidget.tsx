import { useEffect, useState } from 'react';
import { getAlerts, dismissAlert } from '../../api/dashboard';
import type { Alert } from '../../types/dashboard';

export default function AlertsWidget() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAlerts()
      .then(setAlerts)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleDismiss = async (alertId: string) => {
    try {
      await dismissAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.id !== alertId));
    } catch {
      // silent
    }
  };

  const getSeverityStyles = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return { bg: '#fef2f2', border: '#fecaca', text: '#dc2626', icon: '🚨' };
      case 'WARNING':
        return { bg: '#fffbeb', border: '#fed7aa', text: '#d97706', icon: '⚠️' };
      default:
        return { bg: '#eff6ff', border: '#bfdbfe', text: '#2563eb', icon: 'ℹ️' };
    }
  };

  if (loading) return null;
  if (alerts.length === 0) return null;

  return (
    <div style={{ marginBottom: '24px' }}>
      <div className="space-y-3">
        {alerts.map((alert) => {
          const styles = getSeverityStyles(alert.severity);
          return (
            <div
              key={alert.id}
              style={{
                background: styles.bg,
                border: `1px solid ${styles.border}`,
                borderRadius: '8px',
                padding: '12px 16px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
              }}
            >
              <span style={{ fontSize: '18px', flexShrink: 0 }}>{styles.icon}</span>
              <div style={{ flex: 1 }}>
                <p style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: styles.text }}>
                  {alert.title}
                </p>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#64748b' }}>
                  {alert.message}
                </p>
              </div>
              <button
                onClick={() => handleDismiss(alert.id)}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  color: '#94a3b8',
                  fontSize: '16px',
                  padding: '4px',
                }}
                title="Dismiss"
              >
                ✕
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
