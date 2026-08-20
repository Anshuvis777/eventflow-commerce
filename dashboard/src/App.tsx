import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DashboardPage from './pages/DashboardPage';
import OrdersPage from './pages/OrdersPage';
import PaymentsPage from './pages/PaymentsPage';
import InventoryPage from './pages/InventoryPage';
import ShippingPage from './pages/ShippingPage';
import EmailPage from './pages/EmailPage';
import ObservabilityPage from './pages/ObservabilityPage';
import AIAnalysisPage from './pages/AIAnalysisPage';

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 10000, retry: 1 } },
});

const navItems = [
  { to: '/', icon: '📊', label: 'Dashboard' },
  { to: '/orders', icon: '📦', label: 'Orders' },
  { to: '/payments', icon: '💳', label: 'Payments' },
  { to: '/inventory', icon: '🏭', label: 'Inventory' },
  { to: '/shipping', icon: '🚚', label: 'Shipping' },
  { to: '/email', icon: '✉️', label: 'Email & Alerts' },
  { to: '/observability', icon: '📈', label: 'Observability' },
  { to: '/ai', icon: '🤖', label: 'AI Analysis' },
];

function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-brand-icon">⚡</span>
        <div>
          <div className="sidebar-brand-title">EventFlow</div>
          <div className="sidebar-brand-sub">Commerce Platform</div>
        </div>
      </div>
      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <span className="sidebar-link-icon">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div className="sidebar-footer-dot" /> All Systems Operational
      </div>
    </aside>
  );
}

function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        <div className="main-inner">{children}</div>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/inventory" element={<InventoryPage />} />
            <Route path="/shipping" element={<ShippingPage />} />
            <Route path="/email" element={<EmailPage />} />
            <Route path="/observability" element={<ObservabilityPage />} />
            <Route path="/ai" element={<AIAnalysisPage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
