import { useState, useEffect, useCallback, type CSSProperties } from 'react';
import { adminApi, type StorageQuotaDetail, type StorageQuotaUpdate } from '../../api/admin';
import { organizationApi } from '../../api/organizations';
import apiClient from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';
import type { ApiResponse } from '../../types/api';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

export default function StoragePolicies() {
  const { user } = useAuth();
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Storage quota
  const [quota, setQuota] = useState<StorageQuotaDetail | null>(null);
  const [quotaLoading, setQuotaLoading] = useState(true);
  const [quotaForm, setQuotaForm] = useState<StorageQuotaUpdate>({});
  const [quotaSaving, setQuotaSaving] = useState(false);

  // Policies
  const [policies, setPolicies] = useState<Record<string, any>>({});
  const [policiesLoading, setPoliciesLoading] = useState(true);
  const [policiesSaving, setPoliciesSaving] = useState(false);

  const showNotif = useCallback((type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const fetchQuota = useCallback(async () => {
    setQuotaLoading(true);
    try {
      const res = await adminApi.getStorageQuota();
      const q = res.data.data;
      setQuota(q);
      setQuotaForm({
        maxStorageBytes: q.maxStorageBytes,
        maxFileSizeBytes: q.maxFileSizeBytes,
        allowedExtensions: q.allowedExtensions,
        blockedExtensions: q.blockedExtensions,
        trashRetentionDays: q.trashRetentionDays,
      });
    } catch { /* ignore */ }
    setQuotaLoading(false);
  }, []);

  const fetchPolicies = useCallback(async () => {
    setPoliciesLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<{ effectivePolicies: Record<string, any>; customOverrides: Record<string, any> }>>('/policies');
      const data = res.data.data;
      setPolicies(data.effectivePolicies || {});
    } catch { /* ignore */ }
    setPoliciesLoading(false);
  }, []);

  useEffect(() => { fetchQuota(); }, [fetchQuota]);
  useEffect(() => { fetchPolicies(); }, [fetchPolicies]);

  const handleQuotaSave = async () => {
    setQuotaSaving(true);
    try {
      await adminApi.updateStorageQuota(quotaForm);
      showNotif('success', 'Storage quota updated');
      fetchQuota();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to update quota');
    }
    setQuotaSaving(false);
  };

  const handlePoliciesSave = async () => {
    if (!user?.organizationId) return;
    setPoliciesSaving(true);
    try {
      await organizationApi.updatePolicies(user.organizationId, policies);
      showNotif('success', 'Organization policies updated');
      fetchPolicies();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to update policies');
    }
    setPoliciesSaving(false);
  };

  return (
    <div>
      <h2 style={styles.pageTitle}>Storage & Policies</h2>

      {notification && (
        <div style={{ ...styles.notification, background: notification.type === 'success' ? '#dcfce7' : '#fee2e2', color: notification.type === 'success' ? '#166534' : '#991b1b' }}>
          {notification.message}
        </div>
      )}

      {/* Storage Quota Section */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>Storage Quota</h3>
        {quotaLoading ? (
          <div style={styles.loading}>Loading quota...</div>
        ) : quota ? (
          <div>
            {/* Usage bar */}
            <div style={{ marginBottom: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#475569', marginBottom: '6px' }}>
                <span>Used: {formatBytes(quota.usedStorageBytes)}</span>
                <span>Max: {formatBytes(quota.maxStorageBytes)}</span>
              </div>
              <div style={{ height: '8px', background: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${Math.min(quota.usedPercent, 100)}%`, background: quota.usedPercent > 90 ? '#dc2626' : quota.usedPercent > 70 ? '#f59e0b' : '#22c55e', borderRadius: '4px', transition: 'width 0.3s' }} />
              </div>
              <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '4px' }}>{quota.usedPercent.toFixed(1)}% used</div>
              {quota.warning && <div style={{ fontSize: '12px', color: '#dc2626', marginTop: '4px' }}>{quota.warning}</div>}
            </div>

            <div style={styles.formGrid}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Max Storage (bytes)</label>
                <input type="number" style={styles.input} value={quotaForm.maxStorageBytes ?? ''} onChange={e => setQuotaForm(f => ({ ...f, maxStorageBytes: Number(e.target.value) }))} />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Max File Size (bytes)</label>
                <input type="number" style={styles.input} value={quotaForm.maxFileSizeBytes ?? ''} onChange={e => setQuotaForm(f => ({ ...f, maxFileSizeBytes: Number(e.target.value) }))} />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Allowed Extensions (comma-separated)</label>
                <input style={styles.input} value={quotaForm.allowedExtensions?.join(', ') ?? ''} onChange={e => setQuotaForm(f => ({ ...f, allowedExtensions: e.target.value ? e.target.value.split(',').map(s => s.trim()) : null }))} placeholder="e.g. pdf, docx, jpg" />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Blocked Extensions (comma-separated)</label>
                <input style={styles.input} value={quotaForm.blockedExtensions?.join(', ') ?? ''} onChange={e => setQuotaForm(f => ({ ...f, blockedExtensions: e.target.value ? e.target.value.split(',').map(s => s.trim()) : null }))} placeholder="e.g. exe, bat, sh" />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Trash Retention (days)</label>
                <input type="number" style={styles.input} value={quotaForm.trashRetentionDays ?? ''} onChange={e => setQuotaForm(f => ({ ...f, trashRetentionDays: Number(e.target.value) }))} min={1} max={365} />
              </div>
            </div>
            <button onClick={handleQuotaSave} style={styles.primaryBtn} disabled={quotaSaving}>{quotaSaving ? 'Saving...' : 'Save Quota'}</button>
          </div>
        ) : (
          <div style={styles.loading}>No quota data available</div>
        )}
      </div>

      {/* Organization Policies Section */}
      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>Organization Policies</h3>
        {policiesLoading ? (
          <div style={styles.loading}>Loading policies...</div>
        ) : (
          <div>
            <div style={styles.formGrid}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Minimum Password Length</label>
                <input type="number" style={styles.input} value={policies.minPasswordLength ?? 8} onChange={e => setPolicies(p => ({ ...p, minPasswordLength: Number(e.target.value) }))} min={6} max={32} />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Require Uppercase</label>
                <select style={styles.input} value={String(policies.requireUppercase ?? true)} onChange={e => setPolicies(p => ({ ...p, requireUppercase: e.target.value === 'true' }))}>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Require Lowercase</label>
                <select style={styles.input} value={String(policies.requireLowercase ?? true)} onChange={e => setPolicies(p => ({ ...p, requireLowercase: e.target.value === 'true' }))}>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Require Number</label>
                <select style={styles.input} value={String(policies.requireNumber ?? true)} onChange={e => setPolicies(p => ({ ...p, requireNumber: e.target.value === 'true' }))}>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Require Special Character</label>
                <select style={styles.input} value={String(policies.requireSpecial ?? false)} onChange={e => setPolicies(p => ({ ...p, requireSpecial: e.target.value === 'true' }))}>
                  <option value="true">Yes</option>
                  <option value="false">No</option>
                </select>
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Session Timeout (minutes)</label>
                <input type="number" style={styles.input} value={policies.sessionTimeoutMinutes ?? 60} onChange={e => setPolicies(p => ({ ...p, sessionTimeoutMinutes: Number(e.target.value) }))} min={5} max={1440} />
              </div>
            </div>
            <button onClick={handlePoliciesSave} style={styles.primaryBtn} disabled={policiesSaving}>{policiesSaving ? 'Saving...' : 'Save Policies'}</button>
          </div>
        )}
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  pageTitle: { margin: '0 0 20px', fontSize: '20px', fontWeight: 700, color: '#1e293b' },
  section: { background: '#fff', borderRadius: '8px', padding: '20px', marginBottom: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  sectionTitle: { margin: '0 0 16px', fontSize: '16px', fontWeight: 600, color: '#1e293b' },
  formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '16px' },
  formGroup: { display: 'flex', flexDirection: 'column' as const },
  label: { fontSize: '13px', fontWeight: 500, color: '#374151', marginBottom: '4px' },
  input: { padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' as const },
  primaryBtn: { padding: '8px 16px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: 500 },
  notification: { padding: '10px 16px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: 500 },
  loading: { textAlign: 'center' as const, padding: '24px', color: '#64748b', fontSize: '14px' },
};
