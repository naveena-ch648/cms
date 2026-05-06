import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import NotificationBell from '../components/collaboration/NotificationBell';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Dashboard</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button
            onClick={() => {
              const lastWorkspace = localStorage.getItem('lastWorkspaceId');
              if (lastWorkspace) {
                navigate(`/workspaces/${lastWorkspace}/search`);
              }
            }}
            title="Search files"
            style={{ cursor: 'pointer' }}
          >
            🔍 Search
          </button>
          <NotificationBell />
          <button onClick={handleLogout}>Sign Out</button>
        </div>
      </div>
      {user && (
        <div>
          <p>Welcome, {user.firstName} {user.lastName}</p>
          <p>Organization: {user.organizationName}</p>
        </div>
      )}
    </div>
  );
}
