import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { inventoryApi } from '../services/api';
import type { InventoryProduct, ApiResponse } from '../types';

export default function InventoryPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'list' | 'add' | 'reserve' | 'release'>('list');
  const [msg, setMsg] = useState('');

  // Add product form state
  const [addProductName, setAddProductName] = useState('');
  const [addProductId, setAddProductId] = useState('');
  const [addQuantity, setAddQuantity] = useState(50);
  const [addLocation, setAddLocation] = useState('Warehouse-A');

  // Reserve/Release states
  const [orderId, setOrderId] = useState('');
  const [reserveItems, setReserveItems] = useState<{ productId: string; quantity: number }[]>([
    { productId: '', quantity: 1 },
  ]);

  const { data: products = [], isLoading, refetch } = useQuery({
    queryKey: ['inventory'],
    queryFn: inventoryApi.list,
  });

  const addMut = useMutation({
    mutationFn: inventoryApi.add,
    onSuccess: (data) => {
      setMsg(`Success: Added/updated product "${data.productName}" (${data.productId})!`);
      qc.invalidateQueries({ queryKey: ['inventory'] });
      setTab('list');
      setAddProductName('');
      setAddProductId('');
      setAddQuantity(50);
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      setMsg(e.response?.data?.message || 'Error adding product');
    },
  });

  const reserveMut = useMutation({
    mutationFn: inventoryApi.reserve,
    onSuccess: (m) => {
      setMsg(m || 'Stock reserved successfully');
      qc.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      setMsg(e.response?.data?.message || 'Error reserving stock');
    },
  });

  const releaseMut = useMutation({
    mutationFn: inventoryApi.release,
    onSuccess: (m) => {
      setMsg(m || 'Stock released successfully');
      qc.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      setMsg(e.response?.data?.message || 'Error releasing stock');
    },
  });

  const addReserveItem = () => setReserveItems([...reserveItems, { productId: '', quantity: 1 }]);

  const updateReserveItem = (i: number, field: 'productId' | 'quantity', val: string | number) => {
    const next = [...reserveItems];
    if (field === 'productId') {
      next[i].productId = String(val);
    } else if (field === 'quantity') {
      next[i].quantity = Number(val);
    }
    setReserveItems(next);
  };

  return (
    <div>
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <h1>🏭 Inventory Management</h1>
          <p>Add new products, manage stock levels, reserve and release warehouse inventory</p>
        </div>
        <button className="btn btn-secondary" onClick={() => refetch()}>
          🔄 Refresh
        </button>
      </div>

      {/* Tabs Navigation */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
        <button className={`btn ${tab === 'list' ? 'btn-primary' : ''}`} onClick={() => setTab('list')}>
          📦 All Products ({products.length})
        </button>
        <button className={`btn ${tab === 'add' ? 'btn-primary' : ''}`} onClick={() => setTab('add')}>
          ➕ Add Product / Replenish Stock
        </button>
        <button className={`btn ${tab === 'reserve' ? 'btn-primary' : ''}`} onClick={() => setTab('reserve')}>
          📥 Reserve Stock
        </button>
        <button className={`btn ${tab === 'release' ? 'btn-primary' : ''}`} onClick={() => setTab('release')}>
          📤 Release Stock
        </button>
      </div>

      {msg && (
        <div
          className="card mb-3"
          style={{
            background: '#f0fdf4',
            border: '1px solid #86efac',
            color: '#166534',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <span>{msg}</span>
          <button className="btn btn-sm" onClick={() => setMsg('')}>
            ✕
          </button>
        </div>
      )}

      {/* TAB 1: ALL PRODUCTS LIST */}
      {tab === 'list' && (
        <div className="card">
          <div className="card-title">📦 Warehouse Catalog & Stock Levels</div>
          {isLoading ? (
            <div style={{ padding: 40, textAlign: 'center', color: '#64748b' }}>Loading products...</div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>Product Name</th>
                    <th>Product ID</th>
                    <th>Warehouse Location</th>
                    <th>Available Quantity</th>
                    <th>Reserved Units</th>
                    <th>Stock Status</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((p: InventoryProduct) => {
                    const available = p.quantity - p.reserved;
                    return (
                      <tr key={p.id}>
                        <td>
                          <strong>{p.productName}</strong>
                        </td>
                        <td>
                          <span style={{ fontFamily: 'monospace', fontSize: 13, color: '#475569' }}>
                            {p.productId}
                          </span>
                        </td>
                        <td>
                          <span style={{ fontFamily: 'monospace', fontSize: 13, color: '#64748b' }}>
                            {p.warehouseLocation || 'Warehouse-A'}
                          </span>
                        </td>
                        <td>
                          <strong>{p.quantity}</strong>
                        </td>
                        <td>
                          <span className="badge" style={{ background: '#fef3c7', color: '#92400e' }}>
                            {p.reserved}
                          </span>
                        </td>
                        <td>
                          <span className={`badge ${available > 0 ? 'b-completed' : 'b-failed'}`}>
                            {available > 0 ? `${available} In Stock` : 'Out of Stock'}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: ADD PRODUCT / REPLENISH STOCK */}
      {tab === 'add' && (
        <div className="card" style={{ maxWidth: 600 }}>
          <div className="card-title">➕ Add New Product / Replenish Stock</div>
          <p style={{ fontSize: 13, color: '#64748b', marginBottom: 16 }}>
            Add a brand new product to the warehouse catalog or add stock to an existing Product ID.
          </p>

          <div className="form-grid mb-3">
            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>
                Product Name <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                value={addProductName}
                onChange={(e) => setAddProductName(e.target.value)}
                placeholder="e.g. Wireless Gaming Mouse"
              />
            </div>

            <div className="form-group">
              <label>Product ID (Optional)</label>
              <input
                value={addProductId}
                onChange={(e) => setAddProductId(e.target.value)}
                placeholder="e.g. PROD-006 (Leave blank to auto-generate)"
              />
            </div>

            <div className="form-group">
              <label>
                Quantity to Add <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input
                type="number"
                min="1"
                value={addQuantity}
                onChange={(e) => setAddQuantity(Number(e.target.value))}
                placeholder="50"
              />
            </div>

            <div className="form-group" style={{ gridColumn: 'span 2' }}>
              <label>Warehouse Location</label>
              <select value={addLocation} onChange={(e) => setAddLocation(e.target.value)}>
                <option value="Warehouse-A">Warehouse-A (Main Hub)</option>
                <option value="Warehouse-B">Warehouse-B (West Coast)</option>
                <option value="Warehouse-C">Warehouse-C (East Coast)</option>
              </select>
            </div>
          </div>

          <button
            className="btn btn-primary"
            onClick={() =>
              addMut.mutate({
                productName: addProductName,
                productId: addProductId || undefined,
                quantity: addQuantity,
                warehouseLocation: addLocation,
              })
            }
            disabled={addMut.isPending || !addProductName}
            style={{ padding: '10px 20px', fontWeight: 600 }}
          >
            {addMut.isPending ? 'Adding to Inventory...' : '➕ Add Product to Inventory'}
          </button>
        </div>
      )}

      {/* TAB 3: RESERVE STOCK */}
      {tab === 'reserve' && (
        <div className="card">
          <div className="card-title">📥 Reserve Stock for Order</div>
          <div className="form-group mb-3" style={{ maxWidth: 400 }}>
            <label>Order ID</label>
            <input
              value={orderId}
              onChange={(e) => setOrderId(e.target.value)}
              placeholder="UUID e.g. 3cd0358b-bd68-4ae0-b3ae-2f8d7ce09a46"
            />
          </div>

          {reserveItems.map((it, i) => (
            <div key={i} className="form-grid mb-2" style={{ maxWidth: 600 }}>
              <div className="form-group">
                <label>Product ID</label>
                <select
                  value={it.productId}
                  onChange={(e) => updateReserveItem(i, 'productId', e.target.value)}
                >
                  <option value="">-- Select Product --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.productId}>
                      {p.productName} ({p.productId})
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Quantity</label>
                <input
                  type="number"
                  min="1"
                  value={it.quantity}
                  onChange={(e) => updateReserveItem(i, 'quantity', Number(e.target.value))}
                />
              </div>
            </div>
          ))}

          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button className="btn btn-secondary btn-sm" onClick={addReserveItem}>
              + Add Item
            </button>
            <button
              className="btn btn-primary"
              onClick={() =>
                reserveMut.mutate({
                  orderId,
                  items: reserveItems.map((i) => ({ ...i, quantity: Number(i.quantity) })),
                })
              }
              disabled={reserveMut.isPending || !orderId}
            >
              {reserveMut.isPending ? 'Reserving...' : '📥 Reserve Stock'}
            </button>
          </div>
        </div>
      )}

      {/* TAB 4: RELEASE STOCK */}
      {tab === 'release' && (
        <div className="card" style={{ maxWidth: 500 }}>
          <div className="card-title">📤 Release Stock</div>
          <div className="form-group mb-3">
            <label>Order ID</label>
            <input value={orderId} onChange={(e) => setOrderId(e.target.value)} placeholder="UUID" />
          </div>
          <button
            className="btn btn-danger"
            onClick={() => releaseMut.mutate(orderId)}
            disabled={releaseMut.isPending || !orderId}
          >
            {releaseMut.isPending ? 'Releasing...' : '📤 Release Reserved Stock'}
          </button>
        </div>
      )}
    </div>
  );
}
