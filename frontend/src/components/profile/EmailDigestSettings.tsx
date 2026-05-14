import { useEffect, useState } from 'react';
import { emailDigestApi, type EmailDigestPreference, type UpdateDigestPreferenceRequest } from '../../api/emailDigest';

export default function EmailDigestSettings() {
  const [prefs, setPrefs] = useState<EmailDigestPreference | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    emailDigestApi.getPreferences()
      .then((res) => setPrefs(res.data.data))
      .catch(() => setError('Failed to load email preferences'));
  }, []);

  async function handleSave() {
    if (!prefs) return;
    setSaving(true);
    setError(null);
    try {
      const payload: UpdateDigestPreferenceRequest = {
        digestEnabled: prefs.digestEnabled,
        digestFrequency: prefs.digestFrequency,
        includeSharedFiles: prefs.includeSharedFiles,
        includePendingApprovals: prefs.includePendingApprovals,
        includeStorageUsage: prefs.includeStorageUsage,
        includeRecentActivity: prefs.includeRecentActivity,
      };
      const res = await emailDigestApi.updatePreferences(payload);
      setPrefs(res.data.data);
      setSaved(true);
      setTimeout(() => setSaved(false), 2500);
    } catch {
      setError('Failed to save preferences');
    } finally {
      setSaving(false);
    }
  }

  function toggle(field: keyof EmailDigestPreference) {
    if (!prefs) return;
    setPrefs({ ...prefs, [field]: !prefs[field as keyof EmailDigestPreference] });
  }

  if (!prefs) {
    return (
      <div style={{ padding: 24, color: '#5f6368' }}>
        {error ?? 'Loading email preferences…'}
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 520, padding: 24 }}>
      <h2 style={{ fontSize: 18, fontWeight: 600, marginBottom: 4 }}>Email Digest</h2>
      <p style={{ color: '#5f6368', fontSize: 14, marginBottom: 20 }}>
        Receive a periodic email summary of your workspace activity.
      </p>

      {/* Master toggle */}
      <label style={rowStyle}>
        <span style={labelStyle}>Enable email digest</span>
        <input
          type="checkbox"
          checked={prefs.digestEnabled}
          onChange={() => toggle('digestEnabled')}
          style={{ width: 16, height: 16 }}
        />
      </label>

      {prefs.digestEnabled && (
        <>
          {/* Frequency */}
          <label style={rowStyle}>
            <span style={labelStyle}>Frequency</span>
            <select
              value={prefs.digestFrequency}
              onChange={(e) => setPrefs({ ...prefs, digestFrequency: e.target.value as 'DAILY' | 'WEEKLY' })}
              style={selectStyle}
            >
              <option value="DAILY">Daily (07:00 UTC)</option>
              <option value="WEEKLY">Weekly (Monday 08:00 UTC)</option>
            </select>
          </label>

          <p style={{ fontSize: 13, fontWeight: 600, color: '#3c4043', margin: '16px 0 8px' }}>
            Include in digest:
          </p>

          <label style={rowStyle}>
            <span style={labelStyle}>Pending approvals</span>
            <input type="checkbox" checked={prefs.includePendingApprovals}
              onChange={() => toggle('includePendingApprovals')} style={{ width: 16, height: 16 }} />
          </label>

          <label style={rowStyle}>
            <span style={labelStyle}>Files shared with me</span>
            <input type="checkbox" checked={prefs.includeSharedFiles}
              onChange={() => toggle('includeSharedFiles')} style={{ width: 16, height: 16 }} />
          </label>

          <label style={rowStyle}>
            <span style={labelStyle}>Storage usage</span>
            <input type="checkbox" checked={prefs.includeStorageUsage}
              onChange={() => toggle('includeStorageUsage')} style={{ width: 16, height: 16 }} />
          </label>

          <label style={rowStyle}>
            <span style={labelStyle}>Recent activity</span>
            <input type="checkbox" checked={prefs.includeRecentActivity}
              onChange={() => toggle('includeRecentActivity')} style={{ width: 16, height: 16 }} />
          </label>
        </>
      )}

      {error && <p style={{ color: '#ea4335', fontSize: 13, marginTop: 12 }}>{error}</p>}
      {saved && <p style={{ color: '#34a853', fontSize: 13, marginTop: 12 }}>Preferences saved.</p>}

      <button onClick={handleSave} disabled={saving} style={btnStyle}>
        {saving ? 'Saving…' : 'Save preferences'}
      </button>
    </div>
  );
}

const rowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '10px 0',
  borderBottom: '1px solid #f1f3f4',
};

const labelStyle: React.CSSProperties = {
  fontSize: 14,
  color: '#3c4043',
};

const selectStyle: React.CSSProperties = {
  fontSize: 13,
  padding: '4px 8px',
  borderRadius: 4,
  border: '1px solid #dadce0',
};

const btnStyle: React.CSSProperties = {
  marginTop: 20,
  padding: '10px 24px',
  background: '#1a73e8',
  color: '#fff',
  border: 'none',
  borderRadius: 6,
  fontWeight: 600,
  fontSize: 14,
  cursor: 'pointer',
};
