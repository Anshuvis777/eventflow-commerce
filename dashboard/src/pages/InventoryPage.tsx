import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { inventoryApi } from '../services/api';
import type { InventoryProduct } from '../types';

export default function InventoryPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'list' | 'reserve' | 'release'>('list');
  const [orderId, setOrderId] = useState('');
  const [reserveItems, setReserveItems] = useState([{ productId: '', quantity: 1 }]);
  const [msg, setMsg] = useState('');

  const { data: products, isLoading } = useQuery({ queryKey: ['inventory'], queryFn: inventoryApi.list, refetchOnWindowFocus: true });

  const reserveMut = useMutation({
    mutationFn: inventoryApi.reserve,
    onSuccess: (m) => { setMsg(m); qc.invalidateQueries({ queryKey: ['inventory'] }); },
    onError: (e: any) => setMsg(e.response?.data?.message || 'Error'),
  });

  const releaseMut = useMutation({
    mutationFn: inventoryApi.release,
    onSuccess: (m) => { setMsg(m); qc.invalidateQueries({ queryKey: ['inventory'] }); },
    onError: (e: any) => setMsg(e.response?.data?.message || 'Error'),
  });

  const addReserveItem = () => setReserveItems([...reserveItems, { productId: '', quantity: 1 }]);
  const updateReserveItem = (i: number, field: string, val: string | number) => {
    const next = [...reserveItems]; (next[i] as any)[field] = val; setReserveItems(next);
  };

  return (
    <div>
      <div className="page-header"><h1>🏭 Inventory</h1><p>Manage stock, reserve and release inventory</p></div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'list' ? 'btn-primary' : ''}`} onClick={() => setTab('list')}>All Products</button>
        <button className={`btn ${tab === 'reserve' ? 'btn-primary' : ''}`} onClick={() => setTab('reserve')}>Reserve</button>
        <button className={`btn ${tab === 'release' ? 'btn-primary' : ''}`} onClick={() => setTab('release')}>Release</button>
      </div>

      {msg && <div className="card" style={{ background: '#f0fdf4', border: '1px solid #86efac' }}>{msg} <button className="btn btn-sm" onClick={() => setMsg('')} style={{ marginLeft: 8 }}>✕</button></div>}

      {tab === 'list' && (
        <div className="card">
          <div className="card-title">📦 All Products</div>
          {isLoading ? <div className="spinner" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Product Name</th><th>Product ID</th><th>Location</th><th>Quantity</th><th>Reserved</th></tr></thead>
                <tbody>
                  {(products || []).map((p: InventoryProduct) => (
                    <tr key={p.id}>
                      <td><strong>{p.productName}</strong></td>
                      <td className="font-mono">{p.productId}</td>
                      <td className="font-mono">{p.warehouseLocation || '—'}</td>
                      <td><span className="badge b-UP">{p.quantity}</span></td>
                      <td><span className="badge b-RESERVED">{p.reserved}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {tab === 'reserve' && (
        <div className="card">
          <div className="card-title">📥 Reserve Stock</div>
          <div className="form-group mb-3" style={{ maxWidth: 400 }}><label>Order ID</label><input value={orderId} onChange={(e) => setOrderId(e.target.value)} placeholder="UUID" /></div>
          {reserveItems.map((it, i) => (
            <div key={i} className="form-grid mb-2" style={{ maxWidth: 600 }}>
              <div className="form-group"><label>Product ID</label><input value={it.productId} onChange={(e) => updateReserveItem(i, 'productId', e.target.value)} /></div>
              <div className="form-group"><label>Qty</label><input type="number" value={it.quantity} onChange={(e) => updateReserveItem(i, 'quantity', e.target.value)} /></div>
            </div>
          ))}
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button className="btn btn-sm" onClick={addReserveItem}>+ Add Item</button>
            <button className="btn btn-primary" onClick={() => reserveMut.mutate({ orderId, items: reserveItems.map(i => ({ ...i, quantity: Number(i.quantity) })) })} disabled={reserveMut.isPending}>
              {reserveMut.isPending ? 'Reserving...' : 'Reserve'}
            </button>
          </div>
        </div>
      )}

      {tab === 'release' && (
        <div className="card">
          <div className="card-title">📤 Release Stock</div>
          <div className="form-group mb-3" style={{ maxWidth: 400 }}><label>Order ID</label><input value={orderId} onChange={(e) => setOrderId(e.target.value)} placeholder="UUID" /></div>
          <button className="btn btn-danger" onClick={() => releaseMut.mutate(orderId)} disabled={releaseMut.isPending || !orderId}>
            {releaseMut.isPending ? 'Releasing...' : 'Release Stock'}
          </button>
        </div>
      )}
    </div>
  );
}
