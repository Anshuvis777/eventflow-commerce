import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { paymentApi } from '../services/api';
import type { Payment } from '../types';

export default function PaymentsPage() {
  const [tab, setTab] = useState<'process' | 'lookup'>('process');
  const [orderId, setOrderId] = useState('');
  const [amount, setAmount] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [method, setMethod] = useState('CREDIT_CARD');
  const [paymentId, setPaymentId] = useState('');
  const [result, setResult] = useState<Payment | null>(null);

  const processMut = useMutation({
    mutationFn: paymentApi.process,
    onSuccess: (p) => { setResult(p); setTab('lookup'); },
  });

  const lookupById = useMutation({ mutationFn: paymentApi.get, onSuccess: setResult });
  const lookupByOrder = useMutation({ mutationFn: paymentApi.getByOrder, onSuccess: (ps) => { if (ps.length) setResult(ps[0]); } });

  return (
    <div>
      <div className="page-header"><h1>💳 Payments</h1><p>Process and look up payments</p></div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'process' ? 'btn-primary' : ''}`} onClick={() => setTab('process')}>Process Payment</button>
        <button className={`btn ${tab === 'lookup' ? 'btn-primary' : ''}`} onClick={() => setTab('lookup')}>Lookup</button>
      </div>

      {tab === 'process' && (
        <div className="card">
          <div className="card-title">💸 Process Payment</div>
          <div className="form-grid mb-3">
            <div className="form-group"><label>Order ID</label><input value={orderId} onChange={(e) => setOrderId(e.target.value)} placeholder="UUID" /></div>
            <div className="form-group"><label>Amount</label><input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="99.99" /></div>
            <div className="form-group"><label>Currency</label>
              <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
                <option>USD</option><option>EUR</option><option>GBP</option><option>INR</option>
              </select>
            </div>
            <div className="form-group"><label>Payment Method</label>
              <select value={method} onChange={(e) => setMethod(e.target.value)}>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="DEBIT_CARD">Debit Card</option>
                <option value="PAYPAL">PayPal</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="KAFKA_AUTO">Kafka Auto</option>
              </select>
            </div>
          </div>
          <button className="btn btn-primary" onClick={() => processMut.mutate({ orderId, amount: Number(amount), currency, paymentMethod: method })} disabled={processMut.isPending}>
            {processMut.isPending ? 'Processing...' : '💳 Process Payment'}
          </button>
          {result && <PaymentDetail payment={result} />}
        </div>
      )}

      {tab === 'lookup' && (
        <div className="card">
          <div className="card-title">🔍 Lookup</div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input placeholder="Payment ID" value={paymentId} onChange={(e) => setPaymentId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && paymentId && lookupById.mutate(paymentId)} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
            <button className="btn btn-primary" onClick={() => paymentId && lookupById.mutate(paymentId)} disabled={!paymentId}>By ID</button>
            <input placeholder="Order ID" value={orderId} onChange={(e) => setOrderId(e.target.value)} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
            <button className="btn btn-primary" onClick={() => orderId && lookupByOrder.mutate(orderId)} disabled={!orderId}>By Order</button>
          </div>
          {result && <PaymentDetail payment={result} />}
        </div>
      )}
    </div>
  );
}

function PaymentDetail({ payment }: { payment: Payment }) {
  return (
    <div className="card mt-3" style={{ background: '#f8fafc' }}>
      <div className="flex-between mb-2">
        <div className="card-title" style={{ marginBottom: 0 }}>💰 Payment {payment.id.slice(0, 8)}...</div>
        <span className={`badge b-${payment.status}`}>{payment.status}</span>
      </div>
      <div className="text-sm">Order: <strong>{payment.orderNumber || payment.orderId?.slice(0, 8)}...</strong> | Amount: <strong>${payment.amount?.toFixed(2)} {payment.currency}</strong></div>
      <div className="text-sm mt-2">Method: {payment.paymentMethod || 'N/A'} | Status: <span className={`badge b-${payment.status}`}>{payment.status}</span></div>
    </div>
  );
}
