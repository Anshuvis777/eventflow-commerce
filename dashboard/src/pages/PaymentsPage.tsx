import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { paymentApi } from '../services/api';
import type { Payment } from '../types';

export default function PaymentsPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'all' | 'process' | 'lookup'>('all');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  // Process form states
  const [orderId, setOrderId] = useState('');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [method, setMethod] = useState('CREDIT_CARD');

  // Lookup states
  const [lookupPaymentId, setLookupPaymentId] = useState('');
  const [lookupOrderId, setLookupOrderId] = useState('');
  const [lookupResult, setLookupResult] = useState<Payment | null>(null);

  // Fetch all payments query
  const { data: payments = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['payments'],
    queryFn: paymentApi.list,
  });

  const processMut = useMutation({
    mutationFn: paymentApi.process,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      setTab('all');
      setOrderId('');
      setAmount('');
    },
  });

  const lookupById = useMutation({ mutationFn: paymentApi.get, onSuccess: setLookupResult });
  const lookupByOrder = useMutation({ mutationFn: paymentApi.getByOrder, onSuccess: (ps) => { if (ps.length) setLookupResult(ps[0]); } });

  // Filtered & Paginated payments list
  const filteredPayments = useMemo(() => {
    return payments.filter((p) => {
      const matchesSearch =
        search === '' ||
        p.paymentNumber?.toLowerCase().includes(search.toLowerCase()) ||
        p.orderNumber?.toLowerCase().includes(search.toLowerCase()) ||
        p.orderId?.toLowerCase().includes(search.toLowerCase()) ||
        p.transactionId?.toLowerCase().includes(search.toLowerCase()) ||
        p.id?.toLowerCase().includes(search.toLowerCase());

      const matchesStatus = statusFilter === 'ALL' || p.status?.toUpperCase() === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [payments, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredPayments.length / itemsPerPage));
  const currentPayments = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filteredPayments.slice(start, start + itemsPerPage);
  }, [filteredPayments, currentPage, itemsPerPage]);

  return (
    <div>
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <h1>💳 Payment Transactions</h1>
          <p>View, manage, and process payment transactions with live status tracking</p>
        </div>
        <button className="btn btn-secondary" onClick={() => refetch()}>
          🔄 Refresh
        </button>
      </div>

      {/* Tab Navigation */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
        <button
          className={`btn ${tab === 'all' ? 'btn-primary' : ''}`}
          onClick={() => setTab('all')}
        >
          📋 All Payments ({payments.length})
        </button>
        <button
          className={`btn ${tab === 'process' ? 'btn-primary' : ''}`}
          onClick={() => setTab('process')}
        >
          💸 Process Payment
        </button>
        <button
          className={`btn ${tab === 'lookup' ? 'btn-primary' : ''}`}
          onClick={() => setTab('lookup')}
        >
          🔍 ID Lookup
        </button>
      </div>

      {/* TAB 1: ALL PAYMENTS TABLE WITH PAGINATION */}
      {tab === 'all' && (
        <div className="card">
          {/* Controls Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', gap: 8, flex: 1, minWidth: 260 }}>
              <input
                type="text"
                placeholder="🔍 Search payment #, order #, transaction ID..."
                value={search}
                onChange={(e) => { setSearch(e.target.value); setCurrentPage(1); }}
                style={{ flex: 1, padding: '8px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
              />
              <select
                value={statusFilter}
                onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }}
                style={{ padding: '8px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
              >
                <option value="ALL">All Statuses</option>
                <option value="COMPLETED">COMPLETED</option>
                <option value="FAILED">FAILED</option>
                <option value="PENDING">PENDING</option>
              </select>
            </div>
            <div style={{ fontSize: 13, color: '#64748b', alignSelf: 'center' }}>
              Showing <strong>{filteredPayments.length ? (currentPage - 1) * itemsPerPage + 1 : 0}</strong> - <strong>{Math.min(currentPage * itemsPerPage, filteredPayments.length)}</strong> of <strong>{filteredPayments.length}</strong> payments
            </div>
          </div>

          {/* Payments Table */}
          {isLoading ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#64748b' }}>Loading payment records...</div>
          ) : isError ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#ef4444' }}>Failed to load payment records</div>
          ) : currentPayments.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#64748b' }}>No payments found matching criteria</div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>Payment Number</th>
                    <th>Order Number</th>
                    <th>Amount</th>
                    <th>Method</th>
                    <th>Transaction ID</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {currentPayments.map((p) => (
                    <tr key={p.id}>
                      <td style={{ fontWeight: 600, color: 'var(--primary)' }}>
                        {p.paymentNumber || p.id.substring(0, 8)}
                      </td>
                      <td>
                        <span style={{ fontFamily: 'monospace', fontSize: 13 }}>
                          {p.orderNumber || p.orderId?.substring(0, 8) || 'N/A'}
                        </span>
                      </td>
                      <td>
                        <strong>${p.amount?.toFixed(2)}</strong> <span style={{ fontSize: 12, color: '#64748b' }}>{p.currency}</span>
                      </td>
                      <td>
                        <span className="badge" style={{ background: '#e2e8f0', color: '#334155' }}>
                          {p.paymentMethod || 'CREDIT_CARD'}
                        </span>
                      </td>
                      <td>
                        <span style={{ fontFamily: 'monospace', fontSize: 12, color: '#475569' }}>
                          {p.transactionId || '—'}
                        </span>
                      </td>
                      <td>
                        <span className={`badge b-${p.status?.toLowerCase()}`}>
                          {p.status}
                        </span>
                      </td>
                      <td style={{ fontSize: 12, color: '#64748b' }}>
                        {p.createdAt ? new Date(p.createdAt).toLocaleString() : 'Just now'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16, paddingTop: 12, borderTop: '1px solid var(--border)' }}>
              <button
                className="btn btn-secondary"
                disabled={currentPage === 1}
                onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
              >
                ◀ Previous
              </button>
              <span style={{ fontSize: 13, color: '#475569' }}>
                Page <strong>{currentPage}</strong> of <strong>{totalPages}</strong>
              </span>
              <button
                className="btn btn-secondary"
                disabled={currentPage === totalPages}
                onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
              >
                Next ▶
              </button>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: PROCESS PAYMENT */}
      {tab === 'process' && (
        <div className="card">
          <div className="card-title">💸 Process New Payment</div>
          <div className="form-grid mb-3">
            <div className="form-group">
              <label>Order ID</label>
              <input
                value={orderId}
                onChange={(e) => setOrderId(e.target.value)}
                placeholder="UUID e.g. 3cd0358b-bd68-4ae0-b3ae-2f8d7ce09a46"
              />
            </div>
            <div className="form-group">
              <label>Amount ($)</label>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="99.99"
              />
            </div>
            <div className="form-group">
              <label>Currency</label>
              <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
                <option>USD</option>
                <option>EUR</option>
                <option>GBP</option>
                <option>INR</option>
              </select>
            </div>
            <div className="form-group">
              <label>Payment Method</label>
              <select value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="DEBIT_CARD">Debit Card</option>
                <option value="PAYPAL">PayPal</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
              </select>
            </div>
          </div>
          <button
            className="btn btn-primary"
            onClick={() => processMut.mutate({ orderId, amount: Number(amount), currency, paymentMethod: method })}
            disabled={processMut.isPending || !orderId || !amount}
          >
            {processMut.isPending ? 'Processing...' : '💳 Process Payment'}
          </button>
        </div>
      )}

      {/* TAB 3: ID LOOKUP */}
      {tab === 'lookup' && (
        <div className="card">
          <div className="card-title">🔍 Quick Search by ID</div>
          <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', gap: 8, flex: 1 }}>
              <input
                placeholder="Payment ID (UUID)"
                value={lookupPaymentId}
                onChange={(e) => setLookupPaymentId(e.target.value)}
                style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
              />
              <button className="btn btn-primary" onClick={() => lookupPaymentId && lookupById.mutate(lookupPaymentId)} disabled={!lookupPaymentId}>
                Search Payment ID
              </button>
            </div>
            <div style={{ display: 'flex', gap: 8, flex: 1 }}>
              <input
                placeholder="Order ID (UUID)"
                value={lookupOrderId}
                onChange={(e) => setLookupOrderId(e.target.value)}
                style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
              />
              <button className="btn btn-primary" onClick={() => lookupOrderId && lookupByOrder.mutate(lookupOrderId)} disabled={!lookupOrderId}>
                Search Order ID
              </button>
            </div>
          </div>
          {lookupResult && <PaymentDetail payment={lookupResult} />}
        </div>
      )}
    </div>
  );
}

function PaymentDetail({ payment }: { payment: Payment }) {
  return (
    <div className="card mt-3" style={{ background: '#f8fafc', border: '1px solid var(--border)' }}>
      <div className="flex-between mb-2">
        <div className="card-title" style={{ marginBottom: 0 }}>💰 Payment {payment.paymentNumber || payment.id.slice(0, 8)}</div>
        <span className={`badge b-${payment.status?.toLowerCase()}`}>{payment.status}</span>
      </div>
      <div className="text-sm">Order: <strong>{payment.orderNumber || payment.orderId?.slice(0, 8)}...</strong> | Amount: <strong>${payment.amount?.toFixed(2)} {payment.currency}</strong></div>
      <div className="text-sm mt-2">Method: {payment.paymentMethod || 'N/A'} | Txn ID: <strong>{payment.transactionId || 'N/A'}</strong></div>
    </div>
  );
}
