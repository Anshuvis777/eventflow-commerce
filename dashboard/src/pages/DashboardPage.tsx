import { useState, useEffect } from 'react';
import type { ServiceHealth } from '../types';

const SERVICES: ServiceHealth[] = [
  { name: 'Order Service', port: 8081, url: '/api/v1/orders', status: 'DOWN' },
  { name: 'Payment Service', port: 8082, url: '/api/v1/payments/order/test', status: 'DOWN' },
  { name: 'Inventory Service', port: 8083, url: '/api/v1/inventory', status: 'DOWN' },
  { name: 'Shipping Service', port: 8084, url: '/api/v1/shipments', status: 'DOWN' },
  { name: 'Notification Service', port: 8085, url: '/api/v1/notifications/health', status: 'DOWN' },
  { name: 'Incident Query', port: 8091, url: '/api/v1/incidents', status: 'DOWN' },
  { name: 'Incident Detector', port: 8092, url: '/api/v1/health', status: 'DOWN' },
  { name: 'Incident Analyzer', port: 8093, url: '/api/v1/incidents', status: 'DOWN' },
  { name: 'Dashboard', port: 3000, url: '/', status: 'UP' },
];

export default function DashboardPage() {
  const [services, setServices] = useState<ServiceHealth[]>(SERVICES);

  const checkHealth = async () => {
    const results = await Promise.all(
      services.map(async (svc) => {
        try {
          const res = await fetch(svc.url, { signal: AbortSignal.timeout(3000) });
          return { ...svc, status: res.ok ? 'UP' as const : 'DOWN' as const };
        } catch {
          return { ...svc, status: 'DOWN' as const };
        }
      })
    );
    setServices(results);
  };

  useEffect(() => { checkHealth(); const iv = setInterval(checkHealth, 15000); return () => clearInterval(iv); }, []);

  const upCount = services.filter((s) => s.status === 'UP').length;

  return (
    <div>
      <div className="page-header">
        <h1>📊 Dashboard</h1>
        <p>System overview and health monitoring</p>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon green">🖥️</div>
          <div><div className="stat-value">{upCount}/{services.length}</div><div className="stat-label">Services Up</div></div>
        </div>
        <div className="stat-card">
          <div className="stat-icon blue">📦</div>
          <div><div className="stat-value">5</div><div className="stat-label">Products in Catalog</div></div>
        </div>
        <div className="stat-card">
          <div className="stat-icon yellow">✉️</div>
          <div><div className="stat-value">—</div><div className="stat-label">Emails Sent</div></div>
        </div>
        <div className="stat-card">
          <div className="stat-icon purple">🔔</div>
          <div><div className="stat-value">—</div><div className="stat-label">Notifications</div></div>
        </div>
      </div>

      <div className="card">
        <div className="card-title">
          <span>❤️</span> Service Health
          <button className="btn btn-sm btn-primary" onClick={checkHealth} style={{ marginLeft: 'auto' }}>🔄 Refresh</button>
        </div>
        <div className="health-grid">
          {services.map((svc) => (
            <div className="health-card" key={svc.name}>
              <div className="health-card-left">
                <div className={`health-dot ${svc.status === 'UP' ? 'up' : 'down'}`} />
                <div>
                  <div className="health-name">{svc.name}</div>
                  <div className="health-port">:{svc.port}</div>
                </div>
              </div>
              <span className={`badge b-${svc.status}`}>{svc.status}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-title">🚀 Quick Actions</div>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <a href="/orders" className="btn btn-primary">📦 Place New Order</a>
          <a href="/ai" className="btn btn-primary">🤖 Run AI Analysis</a>
          <a href="/observability" className="btn btn-primary">📈 Explore Timeline</a>
        </div>
      </div>
    </div>
  );
}
