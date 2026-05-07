import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { workspacesApi } from '../api/workspaces';
import { PendingApprovalsWidget } from '../components/PendingApprovalsWidget';
import RecentFilesWidget from '../components/dashboard/RecentFilesWidget';
import ActivityFeedWidget from '../components/dashboard/ActivityFeedWidget';
import StorageUsageWidget from '../components/dashboard/StorageUsageWidget';
import SharedItemsWidget from '../components/dashboard/SharedItemsWidget';
import AlertsWidget from '../components/dashboard/AlertsWidget';
import type { Workspace } from '../types/models';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    workspacesApi.list()
      .then((res) => setWorkspaces(res.data.data ?? []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const stats = [
    { label: 'Workspaces', value: workspaces.length, icon: '▤', color: '#3b82f6' },
    { label: 'Active', value: workspaces.filter(w => w.status === 'ACTIVE').length, icon: '●', color: '#10b981' },
    { label: 'Members', value: workspaces.reduce((sum, w) => sum + (w.memberCount || 0), 0), icon: '👥', color: '#8b5cf6' },
  ];

  const handleWorkspaceClick = (ws: Workspace) => {
    localStorage.setItem('lastWorkspaceId', ws.id);
    navigate(`/workspaces/${ws.id}`);
  };

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Welcome Section */}
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{
          fontSize: '28px',
          fontWeight: 700,
          color: '#1e293b',
          margin: 0,
          letterSpacing: '-0.02em',
        }}>
          Welcome back, {user?.firstName}
        </h1>
        <p style={{ color: '#64748b', margin: '8px 0 0', fontSize: '15px' }}>
          {user?.organizationName} • Here's what's happening in your workspaces
        </p>
      </div>

      {/* System Alerts (at top, above all content) */}
      <AlertsWidget />

      {/* Stats Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '16px',
        marginBottom: '32px',
      }}>
        {stats.map((stat) => (
          <div key={stat.label} style={{
            background: '#fff',
            borderRadius: '12px',
            padding: '20px',
            border: '1px solid #e2e8f0',
            display: 'flex',
            alignItems: 'center',
            gap: '16px',
          }}>
            <div style={{
              width: '44px',
              height: '44px',
              borderRadius: '10px',
              background: `${stat.color}15`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '20px',
              color: stat.color,
              flexShrink: 0,
            }}>
              {stat.icon}
            </div>
            <div>
              <div style={{ fontSize: '24px', fontWeight: 700, color: '#1e293b' }}>
                {stat.value}
              </div>
              <div style={{ fontSize: '13px', color: '#64748b' }}>
                {stat.label}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pending Approvals */}
      <div style={{ marginBottom: '32px' }}>
        <PendingApprovalsWidget onViewAll={() => navigate('/approvals')} />
      </div>

      {/* Dashboard Widgets Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
        gap: '24px',
        marginBottom: '32px',
      }}>
        <RecentFilesWidget />
        <ActivityFeedWidget />
      </div>

      {/* Storage Widget */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
        gap: '24px',
        marginBottom: '32px',
      }}>
        <StorageUsageWidget />
        <SharedItemsWidget />
      </div>

      {/* Quick Actions */}
      <div style={{
        display: 'flex',
        gap: '12px',
        marginBottom: '32px',
        flexWrap: 'wrap',
      }}>
        <button
          onClick={() => {
            const last = localStorage.getItem('lastWorkspaceId');
            if (last) navigate(`/workspaces/${last}/search`);
            else if (workspaces.length > 0 && workspaces[0]) navigate(`/workspaces/${workspaces[0].id}/search`);
          }}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 20px',
            background: '#fff',
            border: '1px solid #e2e8f0',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '13px',
            fontWeight: 500,
            color: '#334155',
            transition: 'all 0.15s ease',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.borderColor = '#3b82f6'; e.currentTarget.style.color = '#3b82f6'; }}
          onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#e2e8f0'; e.currentTarget.style.color = '#334155'; }}
        >
          🔍 Search Files
        </button>
      </div>

      {/* Workspaces Section */}
      <div>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '16px',
        }}>
          <h2 style={{ fontSize: '18px', fontWeight: 600, color: '#1e293b', margin: 0 }}>
            Your Workspaces
          </h2>
        </div>

        {loading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#94a3b8' }}>
            Loading workspaces...
          </div>
        ) : workspaces.length === 0 ? (
          <div style={{
            padding: '48px',
            textAlign: 'center',
            background: '#fff',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
          }}>
            <div style={{ fontSize: '40px', marginBottom: '12px' }}>📁</div>
            <div style={{ fontSize: '15px', color: '#64748b' }}>
              No workspaces yet. Create one to get started.
            </div>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '16px',
          }}>
            {workspaces.map((ws) => (
              <div
                key={ws.id}
                onClick={() => handleWorkspaceClick(ws)}
                style={{
                  background: '#fff',
                  borderRadius: '12px',
                  padding: '20px',
                  border: '1px solid #e2e8f0',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = '#3b82f6';
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(59,130,246,0.08)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = '#e2e8f0';
                  e.currentTarget.style.boxShadow = 'none';
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
                  <div style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '10px',
                    background: 'linear-gradient(135deg, #3b82f6, #6366f1)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    fontSize: '16px',
                    fontWeight: 700,
                    flexShrink: 0,
                  }}>
                    {ws.name.charAt(0).toUpperCase()}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      fontSize: '15px',
                      fontWeight: 600,
                      color: '#1e293b',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}>
                      {ws.name}
                    </div>
                    {ws.description && (
                      <div style={{
                        fontSize: '13px',
                        color: '#64748b',
                        marginTop: '4px',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}>
                        {ws.description}
                      </div>
                    )}
                  </div>
                </div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  marginTop: '16px',
                  paddingTop: '12px',
                  borderTop: '1px solid #f1f5f9',
                }}>
                  <span style={{ fontSize: '12px', color: '#64748b' }}>
                    👥 {ws.memberCount} members
                  </span>
                  <span style={{
                    fontSize: '11px',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    background: ws.status === 'ACTIVE' ? '#ecfdf5' : '#fef3c7',
                    color: ws.status === 'ACTIVE' ? '#059669' : '#d97706',
                    fontWeight: 500,
                  }}>
                    {ws.status}
                  </span>
                  {ws.myRole && (
                    <span style={{ fontSize: '12px', color: '#94a3b8', marginLeft: 'auto' }}>
                      {ws.myRole.name}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
