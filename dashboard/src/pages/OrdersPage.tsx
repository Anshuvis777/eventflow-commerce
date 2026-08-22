import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi, inventoryApi } from '../services/api';
import type { Order, OrderItem, InventoryProduct } from '../types';

export default function OrdersPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'all' | 'create' | 'lookup'>('all');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  // New Order Form state
  const [customerName, setCustomerName] = useState('John Doe');
  const [customerEmail, setCustomerEmail] = useState('john.doe@example.com');
  const [address, setAddress] = useState('123 Tech Street, Silicon Valley');
  const [items, setItems] = useState<OrderItem[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  // Lookup state
  const [lookupOrderId, setLookupOrderId] = useState('');

  // Fetch Orders and Inventory Products
  const { data: orders = [], isLoading: isLoadingOrders, refetch } = useQuery({
    queryKey: ['orders'],
    queryFn: orderApi.list,
  });

  const { data: products = [], isLoading: isLoadingProducts } = useQuery({
    queryKey: ['inventory'],
    queryFn: inventoryApi.list,
  });

  const createMut = useMutation({
    mutationFn: orderApi.create,
    onSuccess: (order) => {
      qc.invalidateQueries({ queryKey: ['orders'] });
      qc.invalidateQueries({ queryKey: ['inventory'] });
      setSelectedOrder(order);
      setTab('all');
      setItems([]);
    },
  });

  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => orderApi.updateStatus(id, status),
    onSuccess: (order) => {
      qc.invalidateQueries({ queryKey: ['orders'] });
      setSelectedOrder(order);
    },
  });

  const lookupMut = useMutation({
    mutationFn: orderApi.get,
    onSuccess: (o) => setSelectedOrder(o),
  });

  // Handle product selection from dropdown or catalog
  const handleSelectProduct = (product: InventoryProduct) => {
    // Check if already in items
    const existingIndex = items.findIndex((i) => i.productId === product.productId);
    if (existingIndex >= 0) {
      const next = [...items];
      next[existingIndex].quantity += 1;
      setItems(next);
    } else {
      setItems([
        ...items,
        {
          productId: product.productId,
          productName: product.productName,
          quantity: 1,
          unitPrice: product.price || 49.99, // default fallback price
        },
      ]);
    }
  };

  const removeItem = (index: number) => {
    setItems(items.filter((_, idx) => idx !== index));
  };

  const updateQuantity = (index: number, qty: number) => {
    const next = [...items];
    next[index].quantity = Math.max(1, qty);
    setItems(next);
  };

  const handleCreate = () => {
    if (items.length === 0) return;
    createMut.mutate({
      customerId: crypto.randomUUID(),
      customerName,
      customerEmail,
      shippingAddress: address,
      items: items.map((i) => ({
        productId: i.productId,
        productName: i.productName,
        quantity: Number(i.quantity),
        unitPrice: Number(i.unitPrice),
      })),
    });
  };

  // Filtered orders list
  const filteredOrders = useMemo(() => {
    return orders.filter((o) => {
      const matchesSearch =
        search === '' ||
        o.orderNumber?.toLowerCase().includes(search.toLowerCase()) ||
        o.customerName?.toLowerCase().includes(search.toLowerCase()) ||
        o.id?.toLowerCase().includes(search.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || o.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [orders, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / itemsPerPage));
  const currentOrders = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filteredOrders.slice(start, start + itemsPerPage);
  }, [filteredOrders, currentPage, itemsPerPage]);

  const totalOrderAmount = items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);

  return (
    <div>
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <h1>📦 Customer Orders</h1>
          <p>Browse, track, and create customer orders with real-time inventory validation</p>
        </div>
        <button className="btn btn-secondary" onClick={() => refetch()}>
          🔄 Refresh
        </button>
      </div>

      {/* Tabs Navigation */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
        <button className={`btn ${tab === 'all' ? 'btn-primary' : ''}`} onClick={() => setTab('all')}>
          📋 All Orders ({orders.length})
        </button>
        <button className={`btn ${tab === 'create' ? 'btn-primary' : ''}`} onClick={() => setTab('create')}>
          ➕ Create New Order
        </button>
        <button className={`btn ${tab === 'lookup' ? 'btn-primary' : ''}`} onClick={() => setTab('lookup')}>
          🔍 Order Lookup
        </button>
      </div>

      {/* TAB 1: ALL ORDERS TABLE */}
      {tab === 'all' && (
        <div className="card">
          {/* Controls Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', gap: 8, flex: 1, minWidth: 260 }}>
              <input
                type="text"
                placeholder="🔍 Search order #, customer name, ID..."
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
                <option value="PLACED">PLACED</option>
                <option value="CONFIRMED">CONFIRMED</option>
                <option value="CANCELLED">CANCELLED</option>
              </select>
            </div>
            <div style={{ fontSize: 13, color: '#64748b', alignSelf: 'center' }}>
              Showing <strong>{filteredOrders.length ? (currentPage - 1) * itemsPerPage + 1 : 0}</strong> - <strong>{Math.min(currentPage * itemsPerPage, filteredOrders.length)}</strong> of <strong>{filteredOrders.length}</strong> orders
            </div>
          </div>

          {isLoadingOrders ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#64748b' }}>Loading orders...</div>
          ) : currentOrders.length === 0 ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#64748b' }}>No orders found</div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>Order Number</th>
                    <th>Customer Name</th>
                    <th>Status</th>
                    <th>Total Amount</th>
                    <th>Shipping Address</th>
                    <th>Created At</th>
                  </tr>
                </thead>
                <tbody>
                  {currentOrders.map((o) => (
                    <tr key={o.id} style={{ cursor: 'pointer' }} onClick={() => { setSelectedOrder(o); setTab('lookup'); }}>
                      <td style={{ fontWeight: 600, color: 'var(--primary)' }}>
                        {o.orderNumber || o.id.substring(0, 8)}
                      </td>
                      <td>{o.customerName || 'Customer'}</td>
                      <td><span className={`badge b-${o.status?.toLowerCase()}`}>{o.status}</span></td>
                      <td><strong>${o.totalAmount?.toFixed(2)}</strong></td>
                      <td style={{ fontSize: 13, color: '#475569' }}>{o.shippingAddress}</td>
                      <td style={{ fontSize: 12, color: '#64748b' }}>{new Date(o.createdAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16, paddingTop: 12, borderTop: '1px solid var(--border)' }}>
              <button className="btn btn-secondary" disabled={currentPage === 1} onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}>
                ◀ Previous
              </button>
              <span style={{ fontSize: 13, color: '#475569' }}>
                Page <strong>{currentPage}</strong> of <strong>{totalPages}</strong>
              </span>
              <button className="btn btn-secondary" disabled={currentPage === totalPages} onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}>
                Next ▶
              </button>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: CREATE NEW ORDER (WITH INVENTORY PRODUCT SELECTOR) */}
      {tab === 'create' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 20 }}>
          {/* Main Form */}
          <div className="card">
            <div className="card-title">🛍️ Place New Customer Order</div>

            <div className="form-grid mb-3">
              <div className="form-group">
                <label>Customer Name</label>
                <input value={customerName} onChange={(e) => setCustomerName(e.target.value)} placeholder="e.g. John Doe" />
              </div>
              <div className="form-group">
                <label>Customer Email</label>
                <input type="email" value={customerEmail} onChange={(e) => setCustomerEmail(e.target.value)} placeholder="e.g. john.doe@example.com" />
              </div>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label>Shipping Address</label>
                <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="e.g. 123 Main St, Silicon Valley" />
              </div>
            </div>

            {/* Select Product Dropdown */}
            <div className="card mb-3" style={{ background: '#f8fafc', border: '1px solid var(--border)' }}>
              <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 8, color: '#334155' }}>
                👇 Choose Product from Available Inventory
              </div>
              <select
                onChange={(e) => {
                  const prod = products.find((p) => p.productId === e.target.value);
                  if (prod) handleSelectProduct(prod);
                  e.target.value = '';
                }}
                defaultValue=""
                style={{ width: '100%', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 14 }}
              >
                <option value="" disabled>-- Select a Product from Inventory Catalog --</option>
                {products.map((p) => {
                  const available = p.quantity - p.reserved;
                  return (
                    <option key={p.id} value={p.productId} disabled={available <= 0}>
                      {p.productName} ({p.productId}) — Available: {available} units (Price: ${p.price || 49.99})
                    </option>
                  );
                })}
              </select>
            </div>

            {/* Selected Items List */}
            <div className="card-title" style={{ fontSize: 14, marginBottom: 12 }}>
              🛒 Order Items ({items.length})
            </div>

            {items.length === 0 ? (
              <div style={{ padding: 20, textAlign: 'center', border: '2px dashed #cbd5e1', borderRadius: 8, color: '#64748b', marginBottom: 16 }}>
                No items added yet. Pick a product from the dropdown above or catalog on the right.
              </div>
            ) : (
              <div className="mb-3">
                {items.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      background: '#fff',
                      border: '1px solid var(--border)',
                      borderRadius: 8,
                      marginBottom: 8,
                    }}
                  >
                    <div>
                      <strong>{item.productName}</strong>
                      <span style={{ marginLeft: 8, fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>
                        ({item.productId})
                      </span>
                      <div style={{ fontSize: 13, color: '#475569' }}>
                        ${item.unitPrice.toFixed(2)} each
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <label style={{ fontSize: 12, color: '#64748b' }}>Qty:</label>
                        <input
                          type="number"
                          min="1"
                          value={item.quantity}
                          onChange={(e) => updateQuantity(idx, Number(e.target.value))}
                          style={{ width: 60, padding: '4px 8px', borderRadius: 6, border: '1px solid var(--border)' }}
                        />
                      </div>
                      <div style={{ fontWeight: 600, width: 80, textAlign: 'right' }}>
                        ${(item.unitPrice * item.quantity).toFixed(2)}
                      </div>
                      <button className="btn btn-danger btn-sm" onClick={() => removeItem(idx)}>
                        ✕
                      </button>
                    </div>
                  </div>
                ))}

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 14px', background: '#f1f5f9', borderRadius: 8, fontWeight: 700, fontSize: 16 }}>
                  <span>Total Amount:</span>
                  <span style={{ color: 'var(--primary)' }}>${totalOrderAmount.toFixed(2)} USD</span>
                </div>
              </div>
            )}

            <button
              className="btn btn-primary"
              onClick={handleCreate}
              disabled={createMut.isPending || items.length === 0}
              style={{ width: '100%', padding: '12px', fontSize: 15, fontWeight: 600 }}
            >
              {createMut.isPending ? 'Processing Order...' : '🚀 Place Order Now'}
            </button>
          </div>

          {/* Right Sidebar: Available Inventory Catalog Quick Select */}
          <div className="card">
            <div className="card-title" style={{ fontSize: 14 }}>🏬 Live Inventory Catalog</div>
            {isLoadingProducts ? (
              <div style={{ padding: 20, textAlign: 'center', color: '#64748b' }}>Loading inventory...</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {products.map((p) => {
                  const available = p.quantity - p.reserved;
                  return (
                    <div
                      key={p.id}
                      style={{
                        padding: '10px 12px',
                        border: '1px solid var(--border)',
                        borderRadius: 8,
                        background: available > 0 ? '#fff' : '#f8fafc',
                      }}
                    >
                      <div style={{ fontWeight: 600, fontSize: 13 }}>{p.productName}</div>
                      <div style={{ fontSize: 12, color: '#64748b', fontFamily: 'monospace' }}>{p.productId}</div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                        <span className={`badge ${available > 0 ? 'b-completed' : 'b-failed'}`} style={{ fontSize: 11 }}>
                          {available > 0 ? `${available} In Stock` : 'Out of Stock'}
                        </span>
                        <button
                          className="btn btn-secondary btn-sm"
                          disabled={available <= 0}
                          onClick={() => handleSelectProduct(p)}
                        >
                          + Add
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 3: LOOKUP */}
      {tab === 'lookup' && (
        <div className="card">
          <div className="card-title">🔍 Order Lookup</div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 16, maxWidth: 500 }}>
            <input
              placeholder="Enter Order ID (UUID)..."
              value={lookupOrderId}
              onChange={(e) => setLookupOrderId(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && lookupOrderId && lookupMut.mutate(lookupOrderId)}
              style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }}
            />
            <button className="btn btn-primary" onClick={() => lookupOrderId && lookupMut.mutate(lookupOrderId)} disabled={!lookupOrderId || lookupMut.isPending}>
              {lookupMut.isPending ? 'Fetching...' : 'Fetch Order'}
            </button>
          </div>

          {selectedOrder && (
            <OrderDetail
              order={selectedOrder}
              onStatus={(s) => statusMut.mutate({ id: selectedOrder.id, status: s })}
            />
          )}
        </div>
      )}
    </div>
  );
}

function OrderDetail({ order, onStatus }: { order: Order; onStatus: (s: string) => void }) {
  return (
    <div className="card mt-3" style={{ background: '#f8fafc', border: '1px solid var(--border)' }}>
      <div className="flex-between mb-2">
        <div className="card-title" style={{ marginBottom: 0 }}>📋 Order {order.orderNumber || order.id.slice(0, 8)}</div>
        <span className={`badge b-${order.status?.toLowerCase()}`}>{order.status}</span>
      </div>
      <div className="text-sm mb-2">Customer: <strong>{order.customerName || order.customerId}</strong> | Total: <strong>${order.totalAmount?.toFixed(2)}</strong></div>
      <div className="text-sm mb-2">Shipping Address: {order.shippingAddress}</div>
      {order.items && order.items.length > 0 && (
        <table className="table mt-2">
          <thead>
            <tr><th>Product</th><th>Qty</th><th>Unit Price</th><th>Subtotal</th></tr>
          </thead>
          <tbody>
            {order.items.map((it, i) => (
              <tr key={i}>
                <td>{it.productName} ({it.productId})</td>
                <td>{it.quantity}</td>
                <td>${it.unitPrice?.toFixed(2)}</td>
                <td><strong>${(it.quantity * it.unitPrice).toFixed(2)}</strong></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {order.status === 'PLACED' && (
        <button className="btn btn-danger btn-sm mt-3" onClick={() => onStatus('CANCELLED')}>
          Cancel Order
        </button>
      )}
    </div>
  );
}
