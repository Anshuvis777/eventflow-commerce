import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../services/api';
import type { Notification } from '../types';

export default function EmailPage() {
  const [tab, setTab] = useState<'email' | 'notification' | 'history'>('email');
  const [emailForm, setEmailForm] = useState({ to: '', subject: '', body: '' });
  const [notifForm, setNotifForm] = useState({ recipient: '', type: 'ORDER_CREATED', title: '', body: '' });
  const [sent, setSent] = useState('');

  const { data: emails, isLoading: emailsLoading } = useQuery({ queryKey: ['notifications'], queryFn: () => notificationApi.list(), refetchOnWindowFocus: true });

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

  return (
    <div>
      <div className="page-header"><h1>✉️ Email & Alerts</h1><p>Send emails and notifications</p></div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'email' ? 'btn-primary' : ''}`} onClick={() => setTab('email')}>Send Email</button>
        <button className={`btn ${tab === 'notification' ? 'btn-primary' : ''}`} onClick={() => setTab('notification')}>Send Notification</button>
        <button className={`btn ${tab === 'history' ? 'btn-primary' : ''}`} onClick={() => setTab('history')}>History</button>
      </div>

      {sent && <div className="card" style={{ background: '#f0fdf4', border: '1px solid #86efac' }}>{sent}</div>}

      {tab === 'email' && (
        <div className="card">
          <div className="card-title">📧 Send Email</div>
          <div className="form-grid mb-3">
            <div className="form-group"><label>To</label><input value={emailForm.to} onChange={(e) => setEmailForm({ ...emailForm, to: e.target.value })} placeholder="user@example.com" /></div>
            <div className="form-group"><label>Subject</label><input value={emailForm.subject} onChange={(e) => setEmailForm({ ...emailForm, subject: e.target.value })} placeholder="Order confirmation" /></div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Body</label><textarea value={emailForm.body} onChange={(e) => setEmailForm({ ...emailForm, body: e.target.value })} rows={4} /></div>
          </div>
          <button className="btn btn-primary" onClick={handleSendEmail} disabled={!emailForm.to || !emailForm.subject}>📧 Send Email</button>
        </div>
      )}

      {tab === 'notification' && (
        <div className="card">
          <div className="card-title">🔔 Send Notification</div>
          <div className="form-grid mb-3">
            <div className="form-group"><label>Recipient</label><input value={notifForm.recipient} onChange={(e) => setNotifForm({ ...notifForm, recipient: e.target.value })} placeholder="user@example.com" /></div>
            <div className="form-group"><label>Type</label>
              <select value={notifForm.type} onChange={(e) => setNotifForm({ ...notifForm, type: e.target.value })}>
                <option value="ORDER_CREATED">Order Created</option>
                <option value="PAYMENT_COMPLETED">Payment Completed</option>
                <option value="SHIPMENT_CREATED">Shipment Created</option>
                <option value="INCIDENT">Incident</option>
                <option value="GENERAL">General</option>
              </select>
            </div>
            <div className="form-group"><label>Title</label><input value={notifForm.title} onChange={(e) => setNotifForm({ ...notifForm, title: e.target.value })} placeholder="Notification title" /></div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Body</label><textarea value={notifForm.body} onChange={(e) => setNotifForm({ ...notifForm, body: e.target.value })} rows={3} /></div>
          </div>
          <button className="btn btn-primary" onClick={handleSendNotif} disabled={!notifForm.recipient || !notifForm.title}>🔔 Send Notification</button>
        </div>
      )}

      {tab === 'history' && (
        <div className="card">
          <div className="card-title">📧 Email History</div>
          {emailsLoading && <p style={{ padding: 16 }}>Loading...</p>}
          <div className="table-wrap">
            <table>
              <thead><tr><th>Recipient</th><th>Subject</th><th>Event</th><th>Status</th><th>Sent</th></tr></thead>
              <tbody>
                {(emails || []).map((e: Notification) => (
                  <tr key={e.id}><td>{e.recipient}</td><td>{e.subject}</td><td><span className="badge b-PENDING">{e.eventType}</span></td><td><span className={`badge b-${e.status}`}>{e.status}</span></td><td className="text-sm text-muted">{e.sentAt ? new Date(e.sentAt).toLocaleString() : '—'}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
