import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../services/api';
import type { Notification } from '../types';

export default function EmailPage() {
  const [tab, setTab] = useState<'history' | 'email' | 'notification'>('history');
  const [emailForm, setEmailForm] = useState({ to: '', subject: '', body: '' });
  const [notifForm, setNotifForm] = useState({ recipient: '', type: 'ORDER_CREATED', title: '', body: '' });
  const [sent, setSent] = useState('');
  
  // Pagination & Filtering state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedNotification, setSelectedNotification] = useState<Notification | null>(null);

  const { data: rawEmails = [], isLoading: emailsLoading, refetch } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(),
    refetchOnWindowFocus: true,
  });

  const handleSendEmail = () => {
    setSent('Email sent to ' + emailForm.to);
    setEmailForm({ to: '', subject: '', body: '' });
    setTimeout(() => setSent(''), 3000);
  };

  const handleSendNotif = () => {
    setSent('Notification sent to ' + notifForm.recipient);
    setNotifForm({ recipient: '', type: 'ORDER_CREATED', title: '', body: '' });
    setTimeout(() => setSent(''), 3000);
  };

  // Filtered notifications
  const filteredEmails = (rawEmails || []).filter((e: Notification) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    return (
      e.recipient?.toLowerCase().includes(term) ||
      e.subject?.toLowerCase().includes(term) ||
      e.eventType?.toLowerCase().includes(term) ||
      e.status?.toLowerCase().includes(term)
    );
  });

  // Calculate pagination bounds
  const totalItems = filteredEmails.length;
  const totalPages = Math.ceil(totalItems / pageSize) || 1;
  const validCurrentPage = Math.min(Math.max(currentPage, 1), totalPages);
  const startIndex = (validCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, totalItems);
  const paginatedEmails = filteredEmails.slice(startIndex, endIndex);

  return (
    <div>
      <div className="page-header">
        <h1>✉️ Email & Alerts</h1>
        <p>View incident alerts, notification audit history, and send emails</p>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'history' ? 'btn-primary' : ''}`} onClick={() => { setTab('history'); setCurrentPage(1); }}>
          📜 Alert & Email History
        </button>
        <button className={`btn ${tab === 'email' ? 'btn-primary' : ''}`} onClick={() => setTab('email')}>
          📧 Send Email
        </button>
        <button className={`btn ${tab === 'notification' ? 'btn-primary' : ''}`} onClick={() => setTab('notification')}>
          🔔 Send Notification
        </button>
      </div>

      {sent && (
        <div className="card mb-3" style={{ background: '#f0fdf4', border: '1px solid #86efac', color: '#166534', padding: '12px 16px', borderRadius: 8 }}>
          {sent}
        </div>
      )}

      {tab === 'history' && (
        <div className="card">
          <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <span>📧 Email & Incident Alert History ({totalItems})</span>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input
                type="text"
                placeholder="Search alerts or recipient..."
                value={searchTerm}
                onChange={(e) => { setSearchTerm(e.target.value); setCurrentPage(1); }}
                style={{ padding: '6px 12px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13, minWidth: 220 }}
              />
              <button className="btn btn-secondary text-sm" onClick={() => refetch()}>
                🔄 Refresh
              </button>
            </div>
          </div>

          {emailsLoading ? (
            <p style={{ padding: 16 }} className="text-muted">Loading alerts history...</p>
          ) : totalItems === 0 ? (
            <p style={{ padding: 16 }} className="text-muted">No notifications or incident alerts recorded yet.</p>
          ) : (
            <>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Recipient</th>
                      <th>Subject</th>
                      <th>Event Type</th>
                      <th>Status</th>
                      <th>Sent At</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedEmails.map((e: Notification) => (
                      <tr key={e.id || Math.random()}>
                        <td className="font-mono text-sm">{e.recipient}</td>
                        <td className="font-medium">{e.subject}</td>
                        <td>
                          <span className={`badge ${e.eventType?.includes('Failed') || e.eventType?.includes('Incident') ? 'b-CRITICAL' : 'b-PENDING'}`}>
                            {e.eventType}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${e.status === 'SENT' ? 'b-ANALYZED' : 'b-OPEN'}`}>
                            {e.status}
                          </span>
                        </td>
                        <td className="text-sm text-muted">
                          {e.sentAt ? new Date(e.sentAt).toLocaleString() : '—'}
                        </td>
                        <td>
                          <button
                            className="btn btn-secondary text-sm"
                            style={{ padding: '4px 10px', fontSize: 12 }}
                            onClick={() => setSelectedNotification(e)}
                          >
                            👁️ View Body
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Pagination Bar */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--border)', flexWrap: 'wrap', gap: 12 }}>
                <div className="text-sm text-muted">
                  Showing <strong>{totalItems > 0 ? startIndex + 1 : 0}</strong> to <strong>{endIndex}</strong> of <strong>{totalItems}</strong> alerts
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }} className="text-sm">
                    <span>Rows per page:</span>
                    <select
                      value={pageSize}
                      onChange={(e) => { setPageSize(Number(e.target.value)); setCurrentPage(1); }}
                      style={{ padding: '4px 8px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13 }}
                    >
                      <option value={5}>5</option>
                      <option value={10}>10</option>
                      <option value={20}>20</option>
                      <option value={50}>50</option>
                    </select>
                  </div>

                  <div style={{ display: 'flex', gap: 4 }}>
                    <button
                      className="btn btn-secondary text-sm"
                      disabled={validCurrentPage <= 1}
                      onClick={() => setCurrentPage(1)}
                      title="First Page"
                    >
                      «
                    </button>
                    <button
                      className="btn btn-secondary text-sm"
                      disabled={validCurrentPage <= 1}
                      onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                    >
                      ‹ Prev
                    </button>
                    
                    {Array.from({ length: totalPages }, (_, i) => i + 1)
                      .filter((p) => p === 1 || p === totalPages || Math.abs(p - validCurrentPage) <= 1)
                      .map((p, idx, arr) => {
                        const prev = arr[idx - 1];
                        return (
                          <span key={p} style={{ display: 'flex', alignItems: 'center' }}>
                            {prev && p - prev > 1 && <span style={{ padding: '0 4px', color: '#94a3b8' }}>...</span>}
                            <button
                              className={`btn text-sm ${validCurrentPage === p ? 'btn-primary' : 'btn-secondary'}`}
                              style={{ minWidth: 32, padding: '4px 8px' }}
                              onClick={() => setCurrentPage(p)}
                            >
                              {p}
                            </button>
                          </span>
                        );
                      })}

                    <button
                      className="btn btn-secondary text-sm"
                      disabled={validCurrentPage >= totalPages}
                      onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                    >
                      Next ›
                    </button>
                    <button
                      className="btn btn-secondary text-sm"
                      disabled={validCurrentPage >= totalPages}
                      onClick={() => setCurrentPage(totalPages)}
                      title="Last Page"
                    >
                      »
                    </button>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {tab === 'email' && (
        <div className="card">
          <div className="card-title">📧 Send Email</div>
          <div className="form-grid mb-3">
            <div className="form-group">
              <label>To</label>
              <input value={emailForm.to} onChange={(e) => setEmailForm({ ...emailForm, to: e.target.value })} placeholder="user@example.com" />
            </div>
            <div className="form-group">
              <label>Subject</label>
              <input value={emailForm.subject} onChange={(e) => setEmailForm({ ...emailForm, subject: e.target.value })} placeholder="Order confirmation" />
            </div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Body</label>
              <textarea value={emailForm.body} onChange={(e) => setEmailForm({ ...emailForm, body: e.target.value })} rows={4} />
            </div>
          </div>
          <button className="btn btn-primary" onClick={handleSendEmail} disabled={!emailForm.to || !emailForm.subject}>
            📧 Send Email
          </button>
        </div>
      )}

      {tab === 'notification' && (
        <div className="card">
          <div className="card-title">🔔 Send Notification</div>
          <div className="form-grid mb-3">
            <div className="form-group">
              <label>Recipient</label>
              <input value={notifForm.recipient} onChange={(e) => setNotifForm({ ...notifForm, recipient: e.target.value })} placeholder="user@example.com" />
            </div>
            <div className="form-group">
              <label>Type</label>
              <select value={notifForm.type} onChange={(e) => setNotifForm({ ...notifForm, type: e.target.value })}>
                <option value="ORDER_CREATED">Order Created</option>
                <option value="PAYMENT_COMPLETED">Payment Completed</option>
                <option value="SHIPMENT_CREATED">Shipment Created</option>
                <option value="INCIDENT">Incident Alert</option>
                <option value="GENERAL">General</option>
              </select>
            </div>
            <div className="form-group">
              <label>Title</label>
              <input value={notifForm.title} onChange={(e) => setNotifForm({ ...notifForm, title: e.target.value })} placeholder="Notification title" />
            </div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Body</label>
              <textarea value={notifForm.body} onChange={(e) => setNotifForm({ ...notifForm, body: e.target.value })} rows={3} />
            </div>
          </div>
          <button className="btn btn-primary" onClick={handleSendNotif} disabled={!notifForm.recipient || !notifForm.title}>
            🔔 Send Notification
          </button>
        </div>
      )}

      {/* Modal / Drawer for Viewing Email Body */}
      {selectedNotification && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 16,
          }}
          onClick={() => setSelectedNotification(null)}
        >
          <div
            className="card"
            style={{ width: '100%', maxWidth: 600, background: '#ffffff', borderRadius: 12, padding: 24 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="card-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>✉️ Email Alert Details</span>
              <button className="btn btn-secondary text-sm" onClick={() => setSelectedNotification(null)}>✕ Close</button>
            </div>

            <div className="mb-3" style={{ fontSize: 14 }}>
              <div style={{ marginBottom: 6 }}><strong>Subject:</strong> {selectedNotification.subject}</div>
              <div style={{ marginBottom: 6 }}><strong>Recipient:</strong> <code style={{ background: '#f1f5f9', padding: '2px 6px', borderRadius: 4 }}>{selectedNotification.recipient}</code></div>
              <div style={{ marginBottom: 6 }}><strong>Event Type:</strong> <span className="badge b-PENDING">{selectedNotification.eventType}</span></div>
              <div style={{ marginBottom: 6 }}><strong>Status:</strong> <span className={`badge b-${selectedNotification.status}`}>{selectedNotification.status}</span></div>
              <div style={{ marginBottom: 6 }}><strong>Sent At:</strong> {selectedNotification.sentAt ? new Date(selectedNotification.sentAt).toLocaleString() : '—'}</div>
            </div>

            <div style={{ marginTop: 16, borderTop: '1px solid var(--border)', paddingTop: 16 }}>
              <strong>Email Message Body:</strong>
              <pre
                style={{
                  background: '#f8fafc',
                  border: '1px solid var(--border)',
                  padding: 12,
                  borderRadius: 8,
                  marginTop: 8,
                  whiteSpace: 'pre-wrap',
                  fontSize: 13,
                  fontFamily: 'inherit',
                }}
              >
                {selectedNotification.body || '(No body content)'}
              </pre>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
