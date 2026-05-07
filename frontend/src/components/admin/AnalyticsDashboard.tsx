import { useState, useEffect, useCallback, type CSSProperties } from 'react';
import { adminApi, type AdminAnalytics } from '../../api/admin';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

const DAY_OPTIONS = [7, 14, 30, 60, 90];

export default function AnalyticsDashboard() {
  const [days, setDays] = useState(30);
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await adminApi.getAnalytics(days);
      setAnalytics(res.data.data);
    } catch {
      setError('Failed to load analytics');
    }
    setLoading(false);
  }, [days]);

  useEffect(() => { fetchAnalytics(); }, [fetchAnalytics]);

  const maxUpload = analytics ? Math.max(...analytics.uploadTrend.map(t => t.count), 1) : 1;
  const maxStorage = analytics ? Math.max(...analytics.storageTrend.map(t => t.totalBytes), 1) : 1;
  const maxRoleDist = analytics ? Math.max(...analytics.roleDistribution.map(r => r.userCount), 1) : 1;

  return (
    <div>
      <div style={styles.header}>
        <h2 style={styles.title}>Analytics Dashboard</h2>
        <div style={styles.daySelector}>
          {DAY_OPTIONS.map(d => (
            <button key={d} onClick={() => setDays(d)} style={{ ...styles.dayBtn, ...(days === d ? styles.dayBtnActive : {}) }}>
              {d}d
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={styles.loading}>Loading analytics...</div>
      ) : error ? (
        <div style={styles.error}>{error}<button onClick={fetchAnalytics} style={{ ...styles.primaryBtn, marginLeft: '12px' }}>Retry</button></div>
      ) : analytics ? (
        <>
          {/* Summary Cards */}
          <div style={styles.cardGrid}>
            <div style={styles.card}>
              <div style={styles.cardLabel}>Total Users</div>
              <div style={styles.cardValue}>{analytics.summary.totalUsers}</div>
              <div style={styles.cardSub}>
                {analytics.summary.activeUsers} active · {analytics.summary.inactiveUsers} inactive
                {analytics.summary.lockedUsers > 0 && ` · ${analytics.summary.lockedUsers} locked`}
              </div>
            </div>
            <div style={styles.card}>
              <div style={styles.cardLabel}>Total Files</div>
              <div style={styles.cardValue}>{analytics.summary.totalFiles}</div>
            </div>
            <div style={styles.card}>
              <div style={styles.cardLabel}>Storage Used</div>
              <div style={styles.cardValue}>{analytics.summary.storageUsedPercent.toFixed(1)}%</div>
              <div style={styles.cardSub}>
                {formatBytes(analytics.summary.totalStorageUsedBytes)} / {formatBytes(analytics.summary.totalStorageMaxBytes)}
              </div>
            </div>
            <div style={styles.card}>
              <div style={styles.cardLabel}>Active Users (30d)</div>
              <div style={styles.cardValue}>{analytics.summary.activeUsersLast30Days}</div>
            </div>
            <div style={styles.card}>
              <div style={styles.cardLabel}>Workspaces</div>
              <div style={styles.cardValue}>{analytics.summary.totalWorkspaces}</div>
            </div>
          </div>

          {/* Charts Row */}
          <div style={styles.chartRow}>
            {/* Role Distribution */}
            <div style={styles.chartCard}>
              <h3 style={styles.chartTitle}>Role Distribution</h3>
              {analytics.roleDistribution.map(r => (
                <div key={r.roleName} style={{ marginBottom: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#475569', marginBottom: '3px' }}>
                    <span>{r.roleName}</span>
                    <span>{r.userCount}</span>
                  </div>
                  <div style={{ height: '6px', background: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${(r.userCount / maxRoleDist) * 100}%`, background: '#6366f1', borderRadius: '3px' }} />
                  </div>
                </div>
              ))}
              {analytics.roleDistribution.length === 0 && <div style={styles.empty}>No data</div>}
            </div>

            {/* Upload Trend */}
            <div style={styles.chartCard}>
              <h3 style={styles.chartTitle}>Upload Trend</h3>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '2px', height: '120px' }}>
                {analytics.uploadTrend.map((t, i) => (
                  <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column' as const, alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
                    <div style={{ width: '100%', maxWidth: '24px', background: '#3b82f6', borderRadius: '2px 2px 0 0', height: `${Math.max((t.count / maxUpload) * 100, 2)}%`, minHeight: '2px' }} title={`${t.date}: ${t.count}`} />
                  </div>
                ))}
              </div>
              {analytics.uploadTrend.length > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: '#94a3b8', marginTop: '4px' }}>
                  <span>{analytics.uploadTrend[0]?.date}</span>
                  <span>{analytics.uploadTrend[analytics.uploadTrend.length - 1]?.date}</span>
                </div>
              )}
              {analytics.uploadTrend.length === 0 && <div style={styles.empty}>No data</div>}
            </div>
          </div>

          {/* Storage Growth & Top Users */}
          <div style={styles.chartRow}>
            {/* Storage Growth */}
            <div style={styles.chartCard}>
              <h3 style={styles.chartTitle}>Storage Growth</h3>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: '2px', height: '120px' }}>
                {analytics.storageTrend.map((t, i) => (
                  <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column' as const, alignItems: 'center', justifyContent: 'flex-end', height: '100%' }}>
                    <div style={{ width: '100%', maxWidth: '24px', background: '#22c55e', borderRadius: '2px 2px 0 0', height: `${Math.max((t.totalBytes / maxStorage) * 100, 2)}%`, minHeight: '2px' }} title={`${t.date}: ${formatBytes(t.totalBytes)}`} />
                  </div>
                ))}
              </div>
              {analytics.storageTrend.length > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: '#94a3b8', marginTop: '4px' }}>
                  <span>{analytics.storageTrend[0]?.date}</span>
                  <span>{analytics.storageTrend[analytics.storageTrend.length - 1]?.date}</span>
                </div>
              )}
              {analytics.storageTrend.length === 0 && <div style={styles.empty}>No data</div>}
            </div>

            {/* Top Active Users */}
            <div style={styles.chartCard}>
              <h3 style={styles.chartTitle}>Top Active Users</h3>
              <table style={{ width: '100%', borderCollapse: 'collapse' as const }}>
                <thead>
                  <tr>
                    <th style={styles.miniTh}>#</th>
                    <th style={styles.miniTh}>User</th>
                    <th style={{ ...styles.miniTh, textAlign: 'right' as const }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {analytics.topActiveUsers.map((u, i) => (
                    <tr key={u.userId}>
                      <td style={styles.miniTd}>{i + 1}</td>
                      <td style={styles.miniTd}>{u.name}</td>
                      <td style={{ ...styles.miniTd, textAlign: 'right' as const, fontWeight: 600 }}>{u.actionCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {analytics.topActiveUsers.length === 0 && <div style={styles.empty}>No data</div>}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' },
  title: { margin: 0, fontSize: '20px', fontWeight: 700, color: '#1e293b' },
  daySelector: { display: 'flex', border: '1px solid #e2e8f0', borderRadius: '6px', overflow: 'hidden' },
  dayBtn: { padding: '6px 12px', border: 'none', background: '#fff', cursor: 'pointer', fontSize: '12px', color: '#475569', fontWeight: 500 },
  dayBtnActive: { background: '#3b82f6', color: '#fff' },
  primaryBtn: { padding: '8px 16px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: 500 },
  cardGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '12px', marginBottom: '20px' },
  card: { background: '#fff', borderRadius: '8px', padding: '16px', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  cardLabel: { fontSize: '12px', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' as const, letterSpacing: '0.05em', marginBottom: '4px' },
  cardValue: { fontSize: '28px', fontWeight: 700, color: '#1e293b' },
  cardSub: { fontSize: '11px', color: '#94a3b8', marginTop: '4px' },
  chartRow: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' },
  chartCard: { background: '#fff', borderRadius: '8px', padding: '16px', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  chartTitle: { margin: '0 0 12px', fontSize: '14px', fontWeight: 600, color: '#1e293b' },
  miniTh: { textAlign: 'left' as const, padding: '4px 6px', fontSize: '11px', fontWeight: 600, color: '#94a3b8', borderBottom: '1px solid #f1f5f9' },
  miniTd: { padding: '4px 6px', fontSize: '12px', color: '#334155', borderBottom: '1px solid #f8fafc' },
  loading: { textAlign: 'center' as const, padding: '40px', color: '#64748b', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '40px', color: '#dc2626', fontSize: '14px' },
  empty: { textAlign: 'center' as const, padding: '20px', color: '#94a3b8', fontSize: '13px' },
};
