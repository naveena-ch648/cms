import React from 'react';
import type { SensitivitySuggestion } from '../../api/ai';

interface SensitivityBadgeProps {
  sensitivity: SensitivitySuggestion;
}

const severityColors: Record<string, { bg: string; border: string; text: string }> = {
  CRITICAL: { bg: '#f8d7da', border: '#dc3545', text: '#721c24' },
  HIGH: { bg: '#f8d7da', border: '#e35d6a', text: '#842029' },
  MEDIUM: { bg: '#fff3cd', border: '#ffc107', text: '#664d03' },
  LOW: { bg: '#cfe2ff', border: '#6ea8fe', text: '#084298' },
  NONE: { bg: '#d1e7dd', border: '#198754', text: '#0f5132' },
};

const SensitivityBadge: React.FC<SensitivityBadgeProps> = ({ sensitivity }) => {
  if (!sensitivity || sensitivity.status !== 'COMPLETED') return null;
  if (!sensitivity.hasSensitiveData) {
    return (
      <span style={{ fontSize: 10, padding: '2px 6px', borderRadius: 4, background: '#d1e7dd', color: '#0f5132', border: '1px solid #198754' }}>
        ✓ No sensitive data
      </span>
    );
  }

  const colors = (severityColors[sensitivity.severity] ?? severityColors.MEDIUM)!;

  return (
    <div style={{ padding: 10, background: colors.bg, borderRadius: 6, border: `1px solid ${colors.border}`, fontSize: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
        <span style={{ fontWeight: 600, color: colors.text }}>
          🔒 Sensitive Data Detected
        </span>
        <span style={{ fontSize: 10, padding: '1px 5px', borderRadius: 3, background: colors.border, color: '#fff', fontWeight: 600 }}>
          {sensitivity.severity}
        </span>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
        {sensitivity.detections.map((d, i) => (
          <span key={i} style={{ fontSize: 10, padding: '2px 6px', borderRadius: 3, background: '#fff', border: `1px solid ${colors.border}`, color: colors.text }}>
            {d.type}: {d.count} occurrence{d.count > 1 ? 's' : ''}
          </span>
        ))}
      </div>
    </div>
  );
};

export default SensitivityBadge;
