import React, { useCallback, useEffect, useState } from 'react';
import { AuditSearchBar } from '../components/audit/AuditSearchBar';
import { AuditFilters } from '../components/audit/AuditFilters';
import { AuditLogTable } from '../components/audit/AuditLogTable';
import { AuditEventDetail } from '../components/audit/AuditEventDetail';
import { AlertRulesPanel } from '../components/audit/AlertRulesPanel';
import { ComplianceReportDialog } from '../components/audit/ComplianceReportDialog';
import { auditApi } from '../api/audit';
import { AuditAlertInstance, AuditEvent, ComplianceReport } from '../types/models';

type Tab = 'events' | 'reports' | 'alerts';

const AuditPage: React.FC = () => {
  const [tab, setTab] = useState<Tab>('events');
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);

  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [eventType, setEventType] = useState('');
  const [outcome, setOutcome] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  // Reports
  const [reports, setReports] = useState<ComplianceReport[]>([]);
  const [showReportDialog, setShowReportDialog] = useState(false);

  // Alerts
  const [alertInstances, setAlertInstances] = useState<AuditAlertInstance[]>([]);

  const size = 20;

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const res = await auditApi.searchEvents({
        query: query || undefined,
        category: category as any || undefined,
        eventType: eventType || undefined,
        outcome: outcome || undefined,
        dateFrom: dateFrom ? new Date(dateFrom).toISOString() : undefined,
        dateTo: dateTo ? new Date(dateTo).toISOString() : undefined,
        page,
        size,
      });
      setEvents(res.data?.events || []);
      setTotal(res.data?.total || 0);
    } catch (err) {
      console.error('Failed to fetch audit events', err);
    } finally {
      setLoading(false);
    }
  }, [query, category, eventType, outcome, dateFrom, dateTo, page]);

  const fetchReports = async () => {
    try {
      const res = await auditApi.listReports();
      setReports(res.data || []);
    } catch (err) {
      console.error('Failed to fetch reports', err);
    }
  };

  const fetchAlertInstances = async () => {
    try {
      const res = await auditApi.listAlertInstances(0, 50);
      setAlertInstances(res.data?.content || []);
    } catch (err) {
      console.error('Failed to fetch alert instances', err);
    }
  };

  useEffect(() => {
    if (tab === 'events') fetchEvents();
    else if (tab === 'reports') fetchReports();
    else if (tab === 'alerts') fetchAlertInstances();
  }, [tab, fetchEvents]);

  const handleSearch = (q: string) => { setQuery(q); setPage(0); };
  const handleFilterChange = (filters: Record<string, string | undefined>) => {
    if ('category' in filters) setCategory(filters.category || '');
    if ('eventType' in filters) setEventType(filters.eventType || '');
    if ('outcome' in filters) setOutcome(filters.outcome || '');
    if ('dateFrom' in filters) setDateFrom(filters.dateFrom || '');
    if ('dateTo' in filters) setDateTo(filters.dateTo || '');
    setPage(0);
  };

  const handleAcknowledge = async (id: string) => {
    try {
      await auditApi.acknowledgeAlert(id);
      fetchAlertInstances();
    } catch (err) {
      console.error('Failed to acknowledge alert', err);
    }
  };

  const tabStyle = (active: boolean): React.CSSProperties => ({
    padding: '8px 20px',
    border: 'none',
    borderBottom: active ? '2px solid #2563eb' : '2px solid transparent',
    background: 'none',
    cursor: 'pointer',
    fontWeight: active ? 600 : 400,
    color: active ? '#2563eb' : '#6b7280',
    fontSize: '14px',
  });

  return (
    <div style={{ padding: '24px', maxWidth: '1400px', margin: '0 auto' }}>
      <h1 style={{ fontSize: '24px', fontWeight: 600, marginBottom: '12px' }}>Audit & Compliance</h1>

      <div style={{ borderBottom: '1px solid #e5e7eb', marginBottom: '20px' }}>
        <button style={tabStyle(tab === 'events')} onClick={() => setTab('events')}>Events</button>
        <button style={tabStyle(tab === 'reports')} onClick={() => setTab('reports')}>Reports</button>
        <button style={tabStyle(tab === 'alerts')} onClick={() => setTab('alerts')}>Alerts</button>
      </div>

      {tab === 'events' && (
        <>
          <AuditSearchBar onSearch={handleSearch} initialQuery={query} />
          <AuditFilters category={category} eventType={eventType} outcome={outcome} dateFrom={dateFrom} dateTo={dateTo} onFilterChange={handleFilterChange} />
          <AuditLogTable events={events} total={total} page={page} size={size} onPageChange={setPage} onEventClick={setSelectedEvent} loading={loading} />
          <AuditEventDetail event={selectedEvent} onClose={() => setSelectedEvent(null)} />
        </>
      )}

      {tab === 'reports' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h3 style={{ margin: 0 }}>Compliance Reports</h3>
            <button onClick={() => setShowReportDialog(true)} style={{ padding: '6px 14px', background: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
              Generate Report
            </button>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #e5e7eb' }}>
            <thead>
              <tr>
                <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Type</th>
                <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Period</th>
                <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Status</th>
                <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Events</th>
                <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {reports.map(r => (
                <tr key={r.id}>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{r.reportType}</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{r.dateFrom?.slice(0, 10)} → {r.dateTo?.slice(0, 10)}</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                    <span style={{ color: r.status === 'COMPLETED' ? '#059669' : r.status === 'FAILED' ? '#dc2626' : '#d97706' }}>{r.status}</span>
                  </td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{r.totalEvents ?? '—'}</td>
                  <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                    {r.status === 'COMPLETED' && (
                      <button onClick={() => auditApi.downloadReport(r.id)} style={{ color: '#2563eb', background: 'none', border: 'none', cursor: 'pointer' }}>Download</button>
                    )}
                  </td>
                </tr>
              ))}
              {reports.length === 0 && <tr><td colSpan={5} style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>No reports generated yet.</td></tr>}
            </tbody>
          </table>
          <ComplianceReportDialog open={showReportDialog} onClose={() => setShowReportDialog(false)} onGenerated={fetchReports} />
        </div>
      )}

      {tab === 'alerts' && (
        <div>
          <AlertRulesPanel />
          <div style={{ marginTop: '24px' }}>
            <h3 style={{ fontSize: '16px', marginBottom: '12px' }}>Triggered Alerts</h3>
            <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #e5e7eb' }}>
              <thead>
                <tr>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Rule</th>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>User</th>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Count</th>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Window</th>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Status</th>
                  <th style={{ padding: '8px 12px', background: '#f9fafb', textAlign: 'left', fontSize: '13px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {alertInstances.map(a => (
                  <tr key={a.id}>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{a.ruleName}</td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{a.triggeredByUser || '—'}</td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{a.eventCount}</td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>{new Date(a.windowStart).toLocaleString()}</td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                      {a.acknowledged ? <span style={{ color: '#059669' }}>Acknowledged</span> : <span style={{ color: '#dc2626' }}>Unacknowledged</span>}
                    </td>
                    <td style={{ padding: '8px 12px', borderBottom: '1px solid #e5e7eb', fontSize: '13px' }}>
                      {!a.acknowledged && <button onClick={() => handleAcknowledge(a.id)} style={{ color: '#2563eb', background: 'none', border: 'none', cursor: 'pointer' }}>Acknowledge</button>}
                    </td>
                  </tr>
                ))}
                {alertInstances.length === 0 && <tr><td colSpan={6} style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>No alerts triggered.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default AuditPage;
