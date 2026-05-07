import { useState, useEffect, type CSSProperties } from 'react';
import { aiApi, type AIConfig } from '../../api/ai';

const defaultFeatures = ['TAG', 'SUMMARIZE', 'CLASSIFY', 'DETECT_DUPLICATES', 'DETECT_SENSITIVE', 'RECOMMEND_WORKFLOW'];

export default function AIConfiguration() {
  const [config, setConfig] = useState<AIConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = async () => {
    try {
      setLoading(true);
      const res = await aiApi.getConfig();
      setConfig(res.data.data);
    } catch {
      setError('Failed to load AI configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!config) return;
    try {
      setSaving(true);
      setError('');
      await aiApi.updateConfig(config);
      setSuccess('Configuration saved successfully');
      setTimeout(() => setSuccess(''), 3000);
    } catch {
      setError('Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const toggleFeature = (feature: string) => {
    if (!config) return;
    const enabled = config.enabledFeatures || [];
    const updated = enabled.includes(feature)
      ? enabled.filter(f => f !== feature)
      : [...enabled, feature];
    setConfig({ ...config, enabledFeatures: updated });
  };

  const updateThreshold = (value: string) => {
    if (!config) return;
    const num = parseFloat(value);
    if (!isNaN(num) && num >= 0 && num <= 100) {
      setConfig({ ...config, confidenceThreshold: num });
    }
  };

  if (loading) return <div style={styles.loading}>Loading AI configuration...</div>;

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h2 style={styles.title}>AI Automation Configuration</h2>
        <p style={styles.subtitle}>Configure AI-powered features for your organization</p>
      </div>

      {error && <div style={styles.error}>{error}</div>}
      {success && <div style={styles.success}>{success}</div>}

      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>Enabled Features</h3>
        <p style={styles.sectionDesc}>Select which AI features are active for new uploads</p>
        <div style={styles.featureGrid}>
          {defaultFeatures.map(feature => {
            const isEnabled = config?.enabledFeatures?.includes(feature) ?? false;
            return (
              <label key={feature} style={{ ...styles.featureItem, background: isEnabled ? '#eff6ff' : '#f8fafc', border: `1px solid ${isEnabled ? '#3b82f6' : '#e2e8f0'}` }}>
                <input
                  type="checkbox"
                  checked={isEnabled}
                  onChange={() => toggleFeature(feature)}
                  style={{ marginRight: 8 }}
                />
                <span style={{ fontWeight: isEnabled ? 600 : 400 }}>{formatFeatureName(feature)}</span>
              </label>
            );
          })}
        </div>
      </div>

      <div style={styles.section}>
        <h3 style={styles.sectionTitle}>Confidence Threshold</h3>
        <p style={styles.sectionDesc}>Minimum confidence (%) to auto-apply suggestions</p>
        <input
          type="number"
          min={0}
          max={100}
          step={5}
          value={config?.confidenceThreshold ?? 70}
          onChange={e => updateThreshold(e.target.value)}
          style={styles.input}
        />
      </div>

      <div style={styles.actions}>
        <button onClick={handleSave} disabled={saving} style={styles.saveBtn}>
          {saving ? 'Saving...' : 'Save Configuration'}
        </button>
      </div>
    </div>
  );
}

function formatFeatureName(feature: string): string {
  const names: Record<string, string> = {
    TAG: 'Auto-Tagging',
    SUMMARIZE: 'Document Summarization',
    CLASSIFY: 'Classification',
    DETECT_DUPLICATES: 'Duplicate Detection',
    DETECT_SENSITIVE: 'Sensitive Data Detection',
    RECOMMEND_WORKFLOW: 'Workflow Recommendations',
  };
  return names[feature] || feature;
}

const styles: Record<string, CSSProperties> = {
  container: { padding: 24 },
  header: { marginBottom: 24 },
  title: { fontSize: 20, fontWeight: 700, color: '#1e293b', margin: 0 },
  subtitle: { fontSize: 13, color: '#64748b', marginTop: 4 },
  section: { marginBottom: 24, padding: 16, background: '#fff', borderRadius: 8, border: '1px solid #e2e8f0' },
  sectionTitle: { fontSize: 14, fontWeight: 600, color: '#334155', margin: '0 0 4px' },
  sectionDesc: { fontSize: 12, color: '#64748b', margin: '0 0 12px' },
  featureGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 8 },
  featureItem: { display: 'flex', alignItems: 'center', padding: '10px 12px', borderRadius: 6, cursor: 'pointer', fontSize: 13 },
  input: { padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, width: 100 },
  actions: { display: 'flex', justifyContent: 'flex-end' },
  saveBtn: { padding: '10px 20px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 600, cursor: 'pointer', fontSize: 14 },
  loading: { padding: 24, color: '#64748b' },
  error: { padding: 12, background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 6, color: '#991b1b', marginBottom: 16, fontSize: 13 },
  success: { padding: 12, background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 6, color: '#166534', marginBottom: 16, fontSize: 13 },
};
