import { useState } from 'react';
import AdminLayout, { type AdminSection } from '../components/admin/AdminLayout';
import UserManagement from '../components/admin/UserManagement';
import RoleManagement from '../components/admin/RoleManagement';
import GroupManagement from '../components/admin/GroupManagement';
import StoragePolicies from '../components/admin/StoragePolicies';
import AnalyticsDashboard from '../components/admin/AnalyticsDashboard';
import AIConfiguration from '../components/admin/AIConfiguration';

export default function AdminPage() {
  const [activeSection, setActiveSection] = useState<AdminSection>('users');

  const renderSection = () => {
    switch (activeSection) {
      case 'users':
        return <UserManagement />;
      case 'roles':
        return <RoleManagement />;
      case 'groups':
        return <GroupManagement />;
      case 'storage':
        return <StoragePolicies />;
      case 'analytics':
        return <AnalyticsDashboard />;
      case 'ai':
        return <AIConfiguration />;
    }
  };

  return (
    <AdminLayout activeSection={activeSection} onSectionChange={setActiveSection}>
      {renderSection()}
    </AdminLayout>
  );
}
