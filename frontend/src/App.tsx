import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import AppLayout from "./components/AppLayout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import WorkspacePage from "./pages/WorkspacePage";
import WorkspaceListPage from "./pages/WorkspaceListPage";
import SearchPage from "./pages/SearchPage";
import QAPage from "./pages/QAPage";
import PendingApprovalsPage from "./pages/PendingApprovalsPage";
import WorkflowTriggersPage from "./pages/WorkflowTriggersPage";
import AuditPage from "./pages/AuditPage";
import AdminPage from "./pages/AdminPage";
import AdminRoute from "./components/admin/AdminRoute";
import IntegrationsPage from "./pages/IntegrationsPage";
import SharedFilesPage from "./pages/SharedFilesPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/approvals" element={<PendingApprovalsPage />} />
              <Route path="/audit" element={<AuditPage />} />
              <Route path="/integrations" element={<IntegrationsPage />} />
              <Route path="/workspaces" element={<WorkspaceListPage />} />
              <Route path="/shared" element={<SharedFilesPage />} />
              <Route
                path="/workspaces/:workspaceId"
                element={<WorkspacePage />}
              />
              <Route
                path="/workspaces/:workspaceId/search"
                element={<SearchPage />}
              />
              <Route path="/workspaces/:workspaceId/qa" element={<QAPage />} />
              <Route
                path="/workspaces/:workspaceId/workflow-triggers"
                element={<WorkflowTriggersPage />}
              />
              <Route
                path="/admin"
                element={
                  <AdminRoute>
                    <AdminPage />
                  </AdminRoute>
                }
              />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
