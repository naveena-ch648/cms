import { useState, useEffect } from 'react';
import { webhooksApi, Webhook, WebhookDelivery, WEBHOOK_EVENT_TYPES } from '../../api/webhooks';

export default function WebhookManagement() {
  const [webhooks, setWebhooks] = useState<Webhook[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedWebhook, setSelectedWebhook] = useState<Webhook | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);

  useEffect(() => {
    loadWebhooks();
  }, []);

  const loadWebhooks = async () => {
    try {
      const data = await webhooksApi.list();
      setWebhooks(data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (webhookId: string) => {
    try {
      await webhooksApi.delete(webhookId);
      setWebhooks(prev => prev.filter(w => w.id !== webhookId));
      if (selectedWebhook?.id === webhookId) setSelectedWebhook(null);
    } catch {
      // silent
    }
  };

  const handleTest = async (webhookId: string) => {
    try {
      await webhooksApi.test(webhookId);
    } catch {
      // silent
    }
  };

  const handleViewDeliveries = async (webhook: Webhook) => {
    setSelectedWebhook(webhook);
    try {
      const data = await webhooksApi.getDeliveries(webhook.id);
      setDeliveries(data);
    } catch {
      setDeliveries([]);
    }
  };

  if (loading) {
    return <div style={{ padding: '24px', color: '#64748b' }}>Loading webhooks...</div>;
  }

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>Webhooks</h2>
        <button
          onClick={() => setShowCreate(true)}
          style={{
            padding: '8px 16px',
            background: '#3b82f6',
            color: '#fff',
            border: 'none',
            borderRadius: '8px',
            fontSize: '13px',
            fontWeight: 500,
            cursor: 'pointer',
          }}
        >
          + Create Webhook
        </button>
      </div>

      {/* Webhook List */}
      {webhooks.length === 0 ? (
        <div style={{
          padding: '40px',
          textAlign: 'center',
          color: '#64748b',
          background: '#f8fafc',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
        }}>
          No webhooks configured. Create one to receive event notifications.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {webhooks.map(webhook => (
            <div key={webhook.id} style={{
              padding: '16px 20px',
              background: '#fff',
              border: '1px solid #e2e8f0',
              borderRadius: '12px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: '14px', color: '#1e293b' }}>{webhook.name}</div>
                  <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>{webhook.url}</div>
                  <div style={{ display: 'flex', gap: '4px', marginTop: '8px', flexWrap: 'wrap' }}>
                    {webhook.eventTypes.map(evt => (
                      <span key={evt} style={{
                        padding: '2px 8px',
                        background: '#f1f5f9',
                        borderRadius: '4px',
                        fontSize: '11px',
                        color: '#475569',
                      }}>
                        {evt}
                      </span>
                    ))}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '6px',
                    fontSize: '11px',
                    fontWeight: 600,
                    background: webhook.status === 'ACTIVE' ? '#ecfdf5' : '#fef2f2',
                    color: webhook.status === 'ACTIVE' ? '#065f46' : '#991b1b',
                  }}>
                    {webhook.status}
                  </span>
                  <button
                    onClick={() => handleViewDeliveries(webhook)}
                    style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '12px', cursor: 'pointer' }}
                  >
                    Deliveries
                  </button>
                  <button
                    onClick={() => handleTest(webhook.id)}
                    style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '12px', cursor: 'pointer' }}
                  >
                    Test
                  </button>
                  <button
                    onClick={() => handleDelete(webhook.id)}
                    style={{ padding: '6px 10px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', fontSize: '12px', color: '#dc2626', cursor: 'pointer' }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Deliveries panel */}
      {selectedWebhook && (
        <div style={{ marginTop: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#1e293b' }}>
              Deliveries for "{selectedWebhook.name}"
            </h3>
            <button
              onClick={() => setSelectedWebhook(null)}
              style={{ padding: '4px 8px', background: 'none', border: 'none', color: '#64748b', cursor: 'pointer', fontSize: '12px' }}
            >
              Close
            </button>
          </div>
          {deliveries.length === 0 ? (
            <div style={{ padding: '20px', color: '#64748b', fontSize: '13px', background: '#f8fafc', borderRadius: '8px' }}>
              No deliveries yet.
            </div>
          ) : (
            <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden' }}>
              {deliveries.map((d, idx) => (
                <div key={d.id} style={{
                  padding: '12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  borderBottom: idx < deliveries.length - 1 ? '1px solid #f1f5f9' : 'none',
                  fontSize: '13px',
                }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: d.status === 'SUCCESS' ? '#10b981' : d.status === 'FAILED' ? '#ef4444' : '#f59e0b',
                  }} />
                  <span style={{ fontWeight: 500, color: '#1e293b' }}>{d.eventType}</span>
                  <span style={{ color: '#94a3b8' }}>
                    {d.responseStatus ? `HTTP ${d.responseStatus}` : '—'}
                  </span>
                  <span style={{ color: '#94a3b8' }}>
                    {d.responseTimeMs ? `${d.responseTimeMs}ms` : ''}
                  </span>
                  <span style={{ marginLeft: 'auto', color: '#94a3b8', fontSize: '11px' }}>
                    {new Date(d.createdAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <CreateWebhookModal
          onClose={() => setShowCreate(false)}
          onCreated={(webhook) => {
            setWebhooks(prev => [webhook, ...prev]);
            setShowCreate(false);
          }}
        />
      )}
    </div>
  );
}

function CreateWebhookModal({ onClose, onCreated }: { onClose: () => void; onCreated: (w: Webhook) => void }) {
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [secret, setSecret] = useState('');
  const [eventTypes, setEventTypes] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
    if (!name || !url || eventTypes.length === 0) {
      setError('Name, URL, and at least one event type are required');
      return;
    }
    setCreating(true);
    setError(null);
    try {
      const webhook = await webhooksApi.create({ name, url, secret: secret || undefined, eventTypes });
      onCreated(webhook);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to create webhook');
    } finally {
      setCreating(false);
    }
  };

  const toggleEvent = (evt: string) => {
    setEventTypes(prev =>
      prev.includes(evt) ? prev.filter(e => e !== evt) : [...prev, evt]
    );
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
    }}>
      <div style={{
        background: '#fff',
        borderRadius: '16px',
        width: '480px',
        padding: '24px',
        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)',
      }}>
        <h3 style={{ margin: '0 0 20px', fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>
          Create Webhook
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '4px' }}>Name</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="My webhook"
              style={{ width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '4px' }}>URL</label>
            <input
              value={url}
              onChange={e => setUrl(e.target.value)}
              placeholder="https://example.com/webhook"
              style={{ width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '4px' }}>Secret (optional)</label>
            <input
              value={secret}
              onChange={e => setSecret(e.target.value)}
              placeholder="Signing secret for HMAC verification"
              type="password"
              style={{ width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '8px' }}>Events</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {WEBHOOK_EVENT_TYPES.map(evt => (
                <button
                  key={evt}
                  onClick={() => toggleEvent(evt)}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    border: eventTypes.includes(evt) ? '1px solid #3b82f6' : '1px solid #e2e8f0',
                    background: eventTypes.includes(evt) ? '#eff6ff' : '#fff',
                    color: eventTypes.includes(evt) ? '#1d4ed8' : '#475569',
                    cursor: 'pointer',
                  }}
                >
                  {evt}
                </button>
              ))}
            </div>
          </div>
        </div>

        {error && (
          <div style={{ marginTop: '12px', color: '#dc2626', fontSize: '13px' }}>{error}</div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '24px' }}>
          <button
            onClick={onClose}
            style={{ padding: '8px 16px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', cursor: 'pointer' }}
          >
            Cancel
          </button>
          <button
            onClick={handleCreate}
            disabled={creating}
            style={{
              padding: '8px 16px',
              background: creating ? '#94a3b8' : '#3b82f6',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: creating ? 'wait' : 'pointer',
            }}
          >
            {creating ? 'Creating...' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
}
