import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { timelineApi, logApi, incidentApi } from '../services/api';
import type { TimelineResponse, LogEntry, Incident } from '../types';

export default function ObservabilityPage() {
  const [correlationId, setCorrelationId] = useState('');
  const [timeline, setTimeline] = useState<TimelineResponse | null>(null);
  const [logs, setLogs] = useState<LogEntry[]>([]);

  const { data: pastIncidents = [], isLoading: isLoadingIncidents, refetch: refetchIncidents } = useQuery({
    queryKey: ['incidents-list-obs'],
    queryFn: () => incidentApi.list(),
  });

  const timelineMut = useMutation({
    mutationFn: async (cid: string) => {
      const cleanCid = cid.trim();
      const incidents = await incidentApi.list();
      const incident = incidents.find((i: any) =>
        i.correlationId?.trim() === cleanCid ||
        i.id === cleanCid ||
        i.correlationId?.toLowerCase() === cleanCid.toLowerCase()
      );
      if (!incident) throw new Error('No incident found for this correlation ID');
      return { incident, timelineData: await timelineApi.get(incident.id) };
    },
    onSuccess: async (res) => {
      setTimeline(res.timelineData);
      const logData = await logApi.query({ correlationId: res.incident.correlationId }).catch(() => []);
      setLogs(logData || []);
    },
  });

  const handleSelectIncident = (inc: Incident) => {
    setCorrelationId(inc.correlationId);
    timelineMut.mutate(inc.correlationId);
  };

  return (
    <div>
      <div className="page-header"><h1>📈 Observability</h1><p>Timeline reconstruction and log analysis</p></div>

      <div className="card">
        <div className="card-title">🔍 Load Timeline</div>
        <div style={{ display: 'flex', gap: 8 }}>
          <input placeholder="Enter Correlation ID or Incident ID" value={correlationId} onChange={(e) => setCorrelationId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && correlationId && timelineMut.mutate(correlationId)} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
          <button className="btn btn-primary" onClick={() => correlationId && timelineMut.mutate(correlationId)} disabled={timelineMut.isPending || !correlationId}>
            {timelineMut.isPending ? 'Loading...' : '📈 Load Timeline'}
          </button>
        </div>
        {timelineMut.isError && <div className="mt-2 text-sm" style={{ color: 'var(--red)' }}>No data found for this correlation ID</div>}
      </div>

      <div className="card">
        <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>📋 Recorded System Incidents ({pastIncidents.length})</span>
          <button className="btn btn-secondary text-sm" onClick={() => refetchIncidents()}>🔄 Refresh</button>
        </div>
        {isLoadingIncidents ? (
          <div className="text-sm text-muted">Loading past incidents...</div>
        ) : pastIncidents.length === 0 ? (
          <div className="text-sm text-muted">No recorded incidents found yet.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Correlation ID</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Services</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {pastIncidents.map((inc) => (
                  <tr key={inc.id}>
                    <td className="font-medium">{inc.title}</td>
                    <td className="text-sm font-mono">{inc.correlationId}</td>
                    <td><span className={`badge b-${inc.severity}`}>{inc.severity}</span></td>
                    <td><span className={`badge b-${inc.status}`}>{inc.status}</span></td>
                    <td className="text-sm">{Array.isArray(inc.affectedServices) ? inc.affectedServices.join(', ') : inc.affectedServices}</td>
                    <td>
                      <button className="btn btn-secondary text-sm" onClick={() => handleSelectIncident(inc)}>
                        📈 View Timeline
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {timeline && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-icon blue">📊</div>
              <div><div className="stat-value">{timeline.totalEvents ?? timeline.events?.length ?? 0}</div><div className="stat-label">Total Events</div></div>
            </div>
            <div className="stat-card">
              <div className="stat-icon purple">🔧</div>
              <div><div className="stat-value">{timeline.affectedServices?.length || 0}</div><div className="stat-label">Affected Services</div></div>
            </div>
            <div className="stat-card">
              <div className="stat-icon yellow">⏱️</div>
              <div><div className="stat-value">{timeline.duration || '—'}</div><div className="stat-label">Duration</div></div>
            </div>
          </div>

          {timeline.affectedServices?.length > 0 && (
            <div className="card">
              <div className="card-title">🔧 Service Flow</div>
              <div className="timeline-flow">
                {timeline.affectedServices.map((svc, i) => (
                  <span key={svc}>
                    <span className="badge b-OPEN">{svc}</span>
                    {i < timeline.affectedServices.length - 1 && <span className="timeline-arrow">→</span>}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="card">
            <div className="card-title">⛓️ Event Chain</div>
            <div className="table-wrap">
              <table>
                <thead><tr><th>Time</th><th>Event Type</th><th>Service</th><th>Level</th></tr></thead>
                <tbody>
                  {(timeline.events || []).map((ev, i) => (
                    <tr key={i}>
                      <td className="text-sm font-mono">{new Date(ev.timestamp).toLocaleTimeString()}</td>
                      <td><span className="badge b-PENDING">{ev.eventType}</span></td>
                      <td>{ev.serviceName}</td>
                      <td><span className={`badge b-${ev.severity || 'LOW'}`}>{ev.severity || 'INFO'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {logs.length > 0 && (
        <div className="card">
          <div className="card-title">📋 Logs ({logs.length})</div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Level</th><th>Service</th><th>Message</th><th>Time</th></tr></thead>
              <tbody>
                {logs.slice(0, 30).map((log, i) => (
                  <tr key={i}>
                    <td><span className={`badge b-${log.level === 'ERROR' ? 'FAILED' : log.level === 'WARN' ? 'RESERVED' : 'UP'}`}>{log.level}</span></td>
                    <td>{log.serviceName}</td>
                    <td className="text-sm font-mono" style={{ maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{log.message}</td>
                    <td className="text-sm text-muted">{new Date(log.timestamp).toLocaleTimeString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {!timeline && !timelineMut.isPending && (
        <div className="empty-state">
          <div className="empty-state-icon">📈</div>
          <p>Enter a Correlation ID to view the event timeline</p>
        </div>
      )}
    </div>
  );
}
