import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '../services/api';
import type { Order, OrderItem } from '../types';

export default function OrdersPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'create' | 'lookup'>('create');
  const [orderId, setOrderId] = useState('');
  const [customerId, setCustomerId] = useState('');
  const [address, setAddress] = useState('');
  const [items, setItems] = useState<OrderItem[]>([{ productId: '', productName: '', quantity: 1, unitPrice: 0 }]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  const { data: orders, isLoading } = useQuery({ queryKey: ['orders'], queryFn: orderApi.list });

  const createMut = useMutation({
    mutationFn: orderApi.create,
    onSuccess: (order) => { qc.invalidateQueries({ queryKey: ['orders'] }); setSelectedOrder(order); setTab('lookup'); },
  });

  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => orderApi.updateStatus(id, status),
    onSuccess: (order) => { qc.invalidateQueries({ queryKey: ['orders'] }); setSelectedOrder(order); },
  });

  const lookupMut = useMutation({ mutationFn: orderApi.get, onSuccess: (o) => setSelectedOrder(o) });

  const addItem = () => setItems([...items, { productId: '', productName: '', quantity: 1, unitPrice: 0 }]);
  const removeItem = (i: number) => setItems(items.filter((_, idx) => idx !== i));
  const updateItem = (i: number, field: string, val: string | number) => {
    const next = [...items]; (next[i] as any)[field] = field === 'quantity' || field === 'unitPrice' ? Number(val) : val; setItems(next);
  };

  const handleCreate = () => {
    createMut.mutate({ customerId: crypto.randomUUID(), shippingAddress: address, items: items.map(i => ({ ...i, unitPrice: Number(i.unitPrice), quantity: Number(i.quantity) })) });
  };

  return (
    <div>
      <div className="page-header"><h1>📦 Orders</h1><p>Create and manage customer orders</p></div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${tab === 'create' ? 'btn-primary' : ''}`} onClick={() => setTab('create')}>Create Order</button>
        <button className={`btn ${tab === 'lookup' ? 'btn-primary' : ''}`} onClick={() => setTab('lookup')}>Lookup</button>
      </div>

      {tab === 'create' && (
        <div className="card">
          <div className="card-title">📝 New Order</div>
          <div className="form-grid mb-3">
            <div className="form-group"><label>Customer ID</label><input value={customerId} onChange={(e) => setCustomerId(e.target.value)} placeholder="CUST-001" /></div>
            <div className="form-group" style={{ gridColumn: 'span 2' }}><label>Shipping Address</label><input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="123 Main St, City, Country" /></div>
          </div>
          <div className="card-title" style={{ fontSize: 14 }}>Items</div>
          {items.map((item, i) => (
            <div key={i} className="form-grid mb-2" style={{ alignItems: 'end' }}>
              <div className="form-group"><label>Product ID</label><input value={item.productId} onChange={(e) => updateItem(i, 'productId', e.target.value)} placeholder="PROD-001" /></div>
              <div className="form-group"><label>Product Name</label><input value={item.productName} onChange={(e) => updateItem(i, 'productName', e.target.value)} placeholder="Widget" /></div>
              <div className="form-group"><label>Qty</label><input type="number" value={item.quantity} onChange={(e) => updateItem(i, 'quantity', e.target.value)} /></div>
              <div className="form-group"><label>Price</label><input type="number" value={item.unitPrice} onChange={(e) => updateItem(i, 'unitPrice', e.target.value)} /></div>
              {items.length > 1 && <button className="btn btn-danger btn-sm" onClick={() => removeItem(i)}>✕</button>}
            </div>
          ))}
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button className="btn btn-sm" onClick={addItem}>+ Add Item</button>
            <button className="btn btn-primary" onClick={handleCreate} disabled={createMut.isPending}>{createMut.isPending ? 'Creating...' : 'Create Order'}</button>
          </div>
          {selectedOrder && tab === 'create' && <OrderDetail order={selectedOrder} onStatus={(s) => statusMut.mutate({ id: selectedOrder.id, status: s })} />}
        </div>
      )}

      {tab === 'lookup' && (
        <div className="card">
          <div className="card-title">🔍 Lookup</div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input placeholder="Order ID" value={orderId} onChange={(e) => setOrderId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && orderId && lookupMut.mutate(orderId)} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
            <button className="btn btn-primary" onClick={() => orderId && lookupMut.mutate(orderId)} disabled={!orderId}>Fetch</button>
          </div>
          {selectedOrder && <OrderDetail order={selectedOrder} onStatus={(s) => statusMut.mutate({ id: selectedOrder.id, status: s })} />}
          <div className="card-title mt-3" style={{ fontSize: 14 }}>All Orders</div>
          {isLoading ? <div className="spinner" /> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>ID</th><th>Customer</th><th>Status</th><th>Total</th><th>Created</th></tr></thead>
                <tbody>
                  {(orders || []).map((o) => (
                    <tr key={o.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedOrder(o)}>
                      <td className="font-mono">{o.orderNumber || o.id.slice(0, 8)}...</td>
                      <td>{o.customerName || o.customerId?.slice(0, 8)}</td>
                      <td><span className={`badge b-${o.status}`}>{o.status}</span></td>
                      <td>${o.totalAmount?.toFixed(2)}</td>
                      <td className="text-sm text-muted">{new Date(o.createdAt).toLocaleString()}</td>
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

function OrderDetail({ order, onStatus }: { order: Order; onStatus: (s: string) => void }) {
  return (
    <div className="card mt-3" style={{ background: '#f8fafc' }}>
      <div className="flex-between mb-2">
        <div className="card-title" style={{ marginBottom: 0 }}>📋 Order {order.id.slice(0, 8)}...</div>
        <span className={`badge b-${order.status}`}>{order.status}</span>
      </div>
      <div className="text-sm mb-2">Customer: <strong>{order.customerId}</strong> | Total: <strong>${order.totalAmount?.toFixed(2)}</strong></div>
      <div className="text-sm mb-2">Address: {order.shippingAddress}</div>
      {order.items && order.items.length > 0 && (
        <table className="mt-2">
          <thead><tr><th>Product</th><th>Qty</th><th>Price</th></tr></thead>
          <tbody>{order.items.map((it, i) => <tr key={i}><td>{it.productName}</td><td>{it.quantity}</td><td>${it.unitPrice?.toFixed(2)}</td></tr>)}</tbody>
        </table>
      )}
      {order.status === 'PLACED' && <button className="btn btn-danger btn-sm mt-2" onClick={() => onStatus('CANCELLED')}>Cancel Order</button>}
    </div>
  );
}
