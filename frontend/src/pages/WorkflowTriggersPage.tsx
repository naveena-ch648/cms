import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { triggersApi } from '../api/triggers';
import type { WorkflowTrigger, WorkflowState } from '../types/workflow';

const STATES: WorkflowState[] = ['DRAFT', 'REVIEW', 'APPROVED', 'PUBLISHED', 'ARCHIVED'];
const TYPES = ['NOTIFICATION', 'PREREQUISITE'];

const WorkflowTriggersPage: React.FC = () => {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const [triggers, setTriggers] = useState<WorkflowTrigger[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ name: '', triggerState: 'PUBLISHED', triggerType: 'NOTIFICATION', config: '' });
  const [editingId, setEditingId] = useState<string | null>(null);

  const fetchTriggers = () => {
    if (!workspaceId) return;
    setLoading(true);
    triggersApi.list(workspaceId)
      .then(res => setTriggers(res.data.data || []))
      .catch(() => setTriggers([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchTriggers(); }, [workspaceId]);

  const handleSubmit = async () => {
    if (!workspaceId) return;
    let config: Record<string, unknown> | undefined;
    if (formData.config.trim()) {
      try {
        config = JSON.parse(formData.config);
      } catch {
        alert('Invalid JSON in config');
        return;
      }
    }

    const data = {
      name: formData.name,
      triggerState: formData.triggerState,
      triggerType: formData.triggerType,
      config,
      enabled: true,
    };

    if (editingId) {
      await triggersApi.update(workspaceId, editingId, data);
    } else {
      await triggersApi.create(workspaceId, data);
    }
    setShowForm(false);
    setEditingId(null);
    setFormData({ name: '', triggerState: 'PUBLISHED', triggerType: 'NOTIFICATION', config: '' });
    fetchTriggers();
  };

  const handleEdit = (trigger: WorkflowTrigger) => {
    setFormData({
      name: trigger.name,
      triggerState: trigger.triggerState,
      triggerType: trigger.triggerType,
      config: trigger.config ? JSON.stringify(trigger.config, null, 2) : '',
    });
    setEditingId(trigger.id);
    setShowForm(true);
  };

  const handleDelete = async (triggerId: string) => {
    if (!workspaceId) return;
    if (!confirm('Delete this trigger?')) return;
    await triggersApi.delete(workspaceId, triggerId);
    fetchTriggers();
  };

  const handleToggle = async (triggerId: string, enabled: boolean) => {
    if (!workspaceId) return;
    await triggersApi.toggle(workspaceId, triggerId, !enabled);
    fetchTriggers();
  };

  return (
    <div style={{ padding: '24px', maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ margin: 0 }}>Workflow Triggers</h2>
        <button
          onClick={() => { setShowForm(true); setEditingId(null); setFormData({ name: '', triggerState: 'PUBLISHED', triggerType: 'NOTIFICATION', config: '' }); }}
          style={{ padding: '8px 16px', fontSize: '13px', backgroundColor: '#3b82f6', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
        >
          + New Trigger
        </button>
      </div>

      {showForm && (
        <div style={{ marginBottom: '24px', padding: '16px', background: '#f9fafb', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
          <h4 style={{ margin: '0 0 12px' }}>{editingId ? 'Edit Trigger' : 'Create Trigger'}</h4>
          <div style={{ display: 'grid', gap: '12px' }}>
            <input
              placeholder="Trigger name"
              value={formData.name}
              onChange={e => setFormData({ ...formData, name: e.target.value })}
              style={{ padding: '8px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px' }}
            />
            <div style={{ display: 'flex', gap: '12px' }}>
              <select
                value={formData.triggerState}
                onChange={e => setFormData({ ...formData, triggerState: e.target.value })}
                style={{ flex: 1, padding: '8px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              >
                {STATES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <select
                value={formData.triggerType}
                onChange={e => setFormData({ ...formData, triggerType: e.target.value })}
                style={{ flex: 1, padding: '8px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px' }}
              >
                {TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <textarea
              placeholder='Config JSON (e.g., {"required_metadata": ["department"]})'
              value={formData.config}
              onChange={e => setFormData({ ...formData, config: e.target.value })}
              rows={3}
              style={{ padding: '8px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px', fontFamily: 'monospace', resize: 'vertical' }}
            />
            <div style={{ display: 'flex', gap: '8px' }}>
              <button onClick={handleSubmit} style={{ padding: '8px 16px', fontSize: '13px', backgroundColor: '#3b82f6', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                {editingId ? 'Update' : 'Create'}
              </button>
              <button onClick={() => { setShowForm(false); setEditingId(null); }} style={{ padding: '8px 16px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', backgroundColor: '#fff' }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {loading && <div style={{ color: '#6b7280' }}>Loading...</div>}

      {!loading && triggers.length === 0 && (
        <div style={{ padding: '24px', textAlign: 'center', color: '#6b7280' }}>
          No triggers configured. Create one to automate workflow behavior.
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {triggers.map(trigger => (
          <div key={trigger.id} style={{ padding: '12px 16px', background: '#fff', border: '1px solid #e5e7eb', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontWeight: 500, fontSize: '14px' }}>{trigger.name}</div>
              <div style={{ fontSize: '12px', color: '#6b7280' }}>
                On entry to <strong>{trigger.triggerState}</strong> · {trigger.triggerType}
                {!trigger.enabled && <span style={{ marginLeft: '8px', color: '#ef4444' }}>(disabled)</span>}
              </div>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button onClick={() => handleToggle(trigger.id, trigger.enabled)} style={{ padding: '4px 8px', fontSize: '11px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', backgroundColor: '#fff' }}>
                {trigger.enabled ? 'Disable' : 'Enable'}
              </button>
              <button onClick={() => handleEdit(trigger)} style={{ padding: '4px 8px', fontSize: '11px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', backgroundColor: '#fff' }}>
                Edit
              </button>
              <button onClick={() => handleDelete(trigger.id)} style={{ padding: '4px 8px', fontSize: '11px', border: '1px solid #fee2e2', borderRadius: '4px', cursor: 'pointer', backgroundColor: '#fff', color: '#dc2626' }}>
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default WorkflowTriggersPage;
