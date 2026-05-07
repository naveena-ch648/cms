import React, { useState } from 'react';
import { auditApi } from '../../api/audit';

interface ComplianceReportDialogProps {
  open: boolean;
  onClose: () => void;
  onGenerated: () => void;
}

export const ComplianceReportDialog: React.FC<ComplianceReportDialogProps> = ({ open, onClose, onGenerated }) => {
  const [reportType, setReportType] = useState('FULL_AUDIT');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [loading, setLoading] = useState(false);

  if (!open) return null;

  const handleGenerate = async () => {
    if (!dateFrom || !dateTo) return;
    setLoading(true);
    try {
      await auditApi.generateReport(reportType, new Date(dateFrom).toISOString(), new Date(dateTo).toISOString());
      onGenerated();
      onClose();
    } catch (err) {
      console.error('Failed to generate report', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: 'white', borderRadius: '12px', padding: '24px', width: '400px', boxShadow: '0 4px 24px rgba(0,0,0,0.15)' }}>
        <h3 style={{ margin: '0 0 16px', fontSize: '18px' }}>Generate Compliance Report</h3>

        <div style={{ marginBottom: '12px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, marginBottom: '4px' }}>Report Type</label>
          <select
            value={reportType}
            onChange={e => setReportType(e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #d1d5db', borderRadius: '6px' }}
          >
            <option value="FULL_AUDIT">Full Audit</option>
            <option value="AUTHENTICATION">Authentication Only</option>
            <option value="FILE_OPERATIONS">File Operations</option>
            <option value="PERMISSION_CHANGES">Permission Changes</option>
          </select>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, marginBottom: '4px' }}>From</label>
          <input type="date" value={dateFrom} onChange={e => setDateFrom(e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #d1d5db', borderRadius: '6px' }} />
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, marginBottom: '4px' }}>To</label>
          <input type="date" value={dateTo} onChange={e => setDateTo(e.target.value)} style={{ width: '100%', padding: '8px', border: '1px solid #d1d5db', borderRadius: '6px' }} />
        </div>

        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button onClick={onClose} style={{ padding: '8px 16px', border: '1px solid #d1d5db', borderRadius: '6px', cursor: 'pointer', background: 'white' }}>
            Cancel
          </button>
          <button onClick={handleGenerate} disabled={loading || !dateFrom || !dateTo} style={{ padding: '8px 16px', background: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', opacity: loading ? 0.6 : 1 }}>
            {loading ? 'Generating...' : 'Generate'}
          </button>
        </div>
      </div>
    </div>
  );
};
