import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { shippingApi } from '../services/api';
import type { Shipment } from '../types';

export default function ShippingPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'create' | 'lookup'>('create');
  const [form, setForm] = useState({ orderId: '', trackingNumber: 'TRK-' + Date.now(), carrier: 'FedEx', recipientName: '', recipientAddress: '' });
  const [lookupVal, setLookupVal] = useState('');
  const [lookupType, setLookupType] = useState<'id' | 'tracking' | 'order'>('id');
  const [result, setResult] = useState<Shipment | null>(null);

  const { data: shipments, isLoading } = useQuery({ queryKey: ['shipments'], queryFn: shippingApi.list });

  const createMut = useMutation({
    mutationFn: shippingApi.create,
    onSuccess: (s) => { qc.invalidateQueries({ queryKey: ['shipments'] }); setResult(s); },
  });

  const deliverMut = useMutation({
    mutationFn: shippingApi.deliver,
    onSuccess: (s) => { qc.invalidateQueries({ queryKey: ['shipments'] }); setResult(s); },
  });

  const lookupMut = useMutation({
    mutationFn: async () => {
      if (lookupType === 'id') return shippingApi.get(lookupVal);
      if (lookupType === 'tracking') { const all = await shippingApi.list(); return all.find((s: Shipment) => s.trackingNumber === lookupVal) || null; }
      const all = await shippingApi.getByOrder(lookupVal); return all[0] || null;
    },
    onSuccess: (s) => setResult(s as Shipment),
  });

  const updateField = (field: string, val: string) => setForm({ ...form, [field]: val });

  return (
    <div>
      <div className="page-header"><h1>🚚 Shipping</h1><p>Create and track shipments</p></div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'create' ? 'btn-primary' : ''}`} onClick={() => setTab('create')}>Create Shipment</button>
        <button className={`btn ${tab === 'lookup' ? 'btn-primary' : ''}`} onClick={() => setTab('lookup')}>Lookup</button>
      </div>

      {tab === 'create' && (
        <div className="card">
          <div className="card-title">📋 New Shipment</div>
          <div className="form-grid mb-3">
            <div className="form-group"><label>Order ID</label><input value={form.orderId} onChange={(e) => updateField('orderId', e.target.value)} placeholder="UUID" /></div>
            <div className="form-group"><label>Tracking Number</label><input value={form.trackingNumber} onChange={(e) => updateField('trackingNumber', e.target.value)} placeholder="TRK-001" /></div>
            <div className="form-group"><label>Carrier</label>
              <select value={form.carrier} onChange={(e) => updateField('carrier', e.target.value)}>
                <option>FedEx</option><option>UPS</option><option>DHL</option><option>USPS</option><option>Amazon Logistics</option>
              </select>
            </div>
            <div className="form-group"><label>Recipient Name</label><input value={form.recipientName} onChange={(e) => updateField('recipientName', e.target.value)} /></div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Recipient Address</label><input value={form.recipientAddress} onChange={(e) => updateField('recipientAddress', e.target.value)} /></div>
          </div>
          <button className="btn btn-primary" onClick={() => createMut.mutate(form)} disabled={createMut.isPending}>{createMut.isPending ? 'Creating...' : '🚚 Create Shipment'}</button>
          {result && <ShipmentDetail shipment={result} onDeliver={() => deliverMut.mutate(result.id)} />}
        </div>
      )}

      {tab === 'lookup' && (
        <div className="card">
          <div className="card-title">🔍 Lookup</div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 16, alignItems: 'center' }}>
            <select value={lookupType} onChange={(e) => setLookupType(e.target.value as any)} style={{ padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}>
              <option value="id">Shipment ID</option><option value="tracking">Tracking #</option><option value="order">Order ID</option>
            </select>
            <input placeholder="Enter value" value={lookupVal} onChange={(e) => setLookupVal(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && lookupVal && lookupMut.mutate()} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
            <button className="btn btn-primary" onClick={() => lookupMut.mutate()} disabled={!lookupVal}>Search</button>
          </div>
          {result && <ShipmentDetail shipment={result} onDeliver={() => deliverMut.mutate(result.id)} />}
          <div className="card-title mt-3" style={{ fontSize: 14 }}>All Shipments</div>
          {isLoading ? <div className="spinner" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Tracking</th><th>Carrier</th><th>Order</th><th>Status</th><th>Recipient</th></tr></thead>
                <tbody>
                  {(shipments || []).map((s: Shipment) => (
                    <tr key={s.id} style={{ cursor: 'pointer' }} onClick={() => setResult(s)}>
                      <td className="font-mono">{s.trackingNumber}</td>
                      <td>{s.carrier || '—'}</td>
                      <td className="font-mono">{s.orderNumber || s.orderId?.slice(0, 8)}...</td>
                      <td><span className={`badge b-${s.status}`}>{s.status}</span></td>
                      <td className="text-sm">{s.shippingAddress?.slice(0, 30)}...</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ShipmentDetail({ shipment, onDeliver }: { shipment: Shipment; onDeliver: () => void }) {
  return (
    <div className="card mt-3" style={{ background: '#f8fafc' }}>
      <div className="flex-between mb-2">
        <div className="card-title" style={{ marginBottom: 0 }}>📦 {shipment.trackingNumber}</div>
        <span className={`badge b-${shipment.status}`}>{shipment.status}</span>
      </div>
      <div className="text-sm">Carrier: <strong>{shipment.carrier || 'N/A'}</strong> | Order: <strong>{shipment.orderNumber || shipment.orderId?.slice(0, 8)}...</strong></div>
      <div className="text-sm mt-2">Address: {shipment.shippingAddress}</div>
      {shipment.status !== 'DELIVERED' && <button className="btn btn-primary btn-sm mt-2" onClick={onDeliver}>✅ Mark Delivered</button>}
    </div>
  );
}
