import React, { useEffect, useState } from 'react';
import { auditApi } from '../../api/audit';
import { AuditAlertRule } from '../../types/models';

export const AlertRulesPanel: React.FC = () => {
  const [rules, setRules] = useState<AuditAlertRule[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', eventType: '', thresholdCount: 5, timeWindowMinutes: 5, enabled: true });

  const fetchRules = async () => {
    try {
      const res = await auditApi.listAlertRules();
      setRules(res.data);
    } catch (err) {
      console.error('Failed to fetch alert rules', err);
    }
  };

  useEffect(() => { fetchRules(); }, []);

  const handleCreate = async () => {
    try {
      await auditApi.createAlertRule(form);
      setShowForm(false);
      setForm({ name: '', description: '', eventType: '', thresholdCount: 5, timeWindowMinutes: 5, enabled: true });
      fetchRules();
    } catch (err) {
      console.error('Failed to create rule', err);
    }
  };

  const handleDelete = async (ruleId: string) => {
    try {
      await auditApi.deleteAlertRule(ruleId);
      fetchRules();
    } catch (err) {
      console.error('Failed to delete rule', err);
    }
  };

  const inputStyle: React.CSSProperties = {
    padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: '4px', fontSize: '13px', width: '100%',
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h3 style={{ margin: 0, fontSize: '16px' }}>Alert Rules</h3>
        <button
          onClick={() => setShowForm(!showForm)}
          style={{ padding: '6px 14px', background: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
        >
          {showForm ? 'Cancel' : 'New Rule'}
        </button>
      </div>

      {showForm && (
        <div style={{ padding: '16px', border: '1px solid #e5e7eb', borderRadius: '8px', marginBottom: '16px', display: 'grid', gap: '10px', gridTemplateColumns: '1fr 1fr' }}>
          <input style={inputStyle} placeholder="Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
          <input style={inputStyle} placeholder="Event type (e.g. LOGIN_FAILED)" value={form.eventType} onChange={e => setForm({ ...form, eventType: e.target.value })} />
          <input style={inputStyle} type="number" placeholder="Threshold count" value={form.thresholdCount} onChange={e => setForm({ ...form, thresholdCount: Number(e.target.value) })} />
          <input style={inputStyle} type="number" placeholder="Time window (minutes)" value={form.timeWindowMinutes} onChange={e => setForm({ ...form, timeWindowMinutes: Number(e.target.value) })} />
          <input style={{ ...inputStyle, gridColumn: '1 / -1' }} placeholder="Description" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
          <button onClick={handleCreate} style={{ padding: '8px 16px', background: '#059669', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
            Create Rule
          </button>
        </div>
      )}

      <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #e5e7eb' }}>
        <thead>
          <tr>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Name</th>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Event Type</th>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Threshold</th>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Window</th>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Status</th>
            <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {rules.map(rule => (
            <tr key={rule.id}>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{rule.name}</td>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}><code>{rule.eventType}</code></td>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{rule.thresholdCount}</td>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{rule.timeWindowMinutes}m</td>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                <span style={{ color: rule.enabled ? '#059669' : '#6b7280' }}>{rule.enabled ? 'Active' : 'Disabled'}</span>
              </td>
              <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                <button onClick={() => handleDelete(rule.id)} style={{ color: '#dc2626', background: 'none', border: 'none', cursor: 'pointer', fontSize: '13px' }}>Delete</button>
              </td>
            </tr>
          ))}
          {rules.length === 0 && (
            <tr><td colSpan={6} style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>No alert rules configured.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
};
