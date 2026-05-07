import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

interface AdminRouteProps {
  children: ReactNode;
}

export default function AdminRoute({ children }: AdminRouteProps) {
  const { user } = useAuth();

  if (user?.organizationRole !== 'Admin') {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
