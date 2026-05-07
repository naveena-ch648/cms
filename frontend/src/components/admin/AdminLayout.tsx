import { type CSSProperties, type ReactNode } from 'react';

export type AdminSection = 'users' | 'roles' | 'groups' | 'storage' | 'analytics' | 'ai';

interface AdminLayoutProps {
  activeSection: AdminSection;
  onSectionChange: (section: AdminSection) => void;
  children: ReactNode;
}

const navItems: { key: AdminSection; icon: string; label: string }[] = [
  { key: 'users', icon: '👥', label: 'Users' },
  { key: 'roles', icon: '🔑', label: 'Roles' },
  { key: 'groups', icon: '📁', label: 'Groups' },
  { key: 'storage', icon: '💾', label: 'Storage & Policies' },
  { key: 'analytics', icon: '📊', label: 'Analytics' },
  { key: 'ai', icon: '🤖', label: 'AI Automation' },
];

export default function AdminLayout({ activeSection, onSectionChange, children }: AdminLayoutProps) {
  return (
    <div style={styles.container}>
      <aside style={styles.sidebar}>
        <div style={styles.sidebarHeader}>
          <span style={{ fontSize: '20px' }}>⚙</span>
          <span style={styles.sidebarTitle}>Admin Console</span>
        </div>
        <nav style={styles.nav}>
          {navItems.map((item) => {
            const isActive = activeSection === item.key;
            return (
              <button
                key={item.key}
                onClick={() => onSectionChange(item.key)}
                style={{
                  ...styles.navItem,
                  background: isActive ? '#eff6ff' : 'transparent',
                  color: isActive ? '#1d4ed8' : '#475569',
                  fontWeight: isActive ? 600 : 400,
                  borderLeft: isActive ? '3px solid #3b82f6' : '3px solid transparent',
                }}
              >
                <span style={{ fontSize: '16px', width: '24px', textAlign: 'center' as const }}>{item.icon}</span>
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </aside>
      <main style={styles.content}>
        {children}
      </main>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  container: {
    display: 'flex',
    height: '100%',
    background: '#f8fafc',
  },
  sidebar: {
    width: '220px',
    background: '#fff',
    borderRight: '1px solid #e2e8f0',
    display: 'flex',
    flexDirection: 'column',
    flexShrink: 0,
  },
  sidebarHeader: {
    padding: '20px 16px',
    borderBottom: '1px solid #e2e8f0',
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  sidebarTitle: {
    fontSize: '15px',
    fontWeight: 700,
    color: '#1e293b',
    letterSpacing: '-0.01em',
  },
  nav: {
    padding: '8px 0',
    flex: 1,
  },
  navItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    width: '100%',
    padding: '10px 16px',
    border: 'none',
    cursor: 'pointer',
    fontSize: '14px',
    textAlign: 'left' as const,
    transition: 'all 0.15s ease',
  },
  content: {
    flex: 1,
    overflow: 'auto',
    padding: '24px',
  },
};
