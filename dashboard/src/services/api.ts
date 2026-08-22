import axios from 'axios';
import type {
  ApiResponse, Incident, TimelineResponse, AnalysisResponse,
  SimilarIncident, LogEntry, LogStatsResponse, HealthStatus,
  Order, Payment, InventoryProduct, Shipment, Notification,
} from '../types';

const api = axios.create({ baseURL: '/api/v1', headers: { 'Content-Type': 'application/json' } });

// ─── Orders ───────────────────────────────────────────
export const orderApi = {
  list: async () => (await api.get<ApiResponse<Order[]>>('/orders')).data.data,
  get: async (id: string) => (await api.get<ApiResponse<Order>>(`/orders/${id}`)).data.data,
  create: async (data: { customerId: string; customerName?: string; customerEmail?: string; shippingAddress: string; items: { productId: string; productName: string; quantity: number; unitPrice: number }[] }) =>
    (await api.post<ApiResponse<Order>>('/orders', data)).data.data,
  updateStatus: async (id: string, status: string) =>
    (await api.patch<ApiResponse<Order>>(`/orders/${id}/status?status=${status}`)).data.data,
};

// ─── Payments ─────────────────────────────────────────
export const paymentApi = {
  list: async () => (await api.get<ApiResponse<Payment[]>>('/payments')).data.data,
  get: async (id: string) => (await api.get<ApiResponse<Payment>>(`/payments/${id}`)).data.data,
  getByOrder: async (orderId: string) => (await api.get<ApiResponse<Payment[]>>(`/payments/order/${orderId}`)).data.data,
  process: async (data: { orderId: string; amount: number; currency: string; paymentMethod: string }) =>
    (await api.post<ApiResponse<Payment>>('/payments/process', data)).data.data,
};

// ─── Inventory ────────────────────────────────────────
export const inventoryApi = {
  list: async () => (await api.get<ApiResponse<InventoryProduct[]>>('/inventory')).data.data,
  get: async (id: string) => (await api.get<ApiResponse<InventoryProduct>>(`/inventory/${id}`)).data.data,
  add: async (data: { productId?: string; productName: string; quantity: number; warehouseLocation?: string }) =>
    (await api.post<ApiResponse<InventoryProduct>>('/inventory', data)).data.data,
  reserve: async (data: { orderId: string; items: { productId: string; quantity: number }[] }) =>
    (await api.post<ApiResponse<string>>('/inventory/reserve', data)).data.data,
  release: async (orderId: string) =>
    (await api.post<ApiResponse<string>>(`/inventory/release?orderId=${orderId}`)).data.data,
};

// ─── Shipping ─────────────────────────────────────────
export const shippingApi = {
  list: async () => (await api.get<ApiResponse<Shipment[]>>('/shipments')).data.data,
  get: async (id: string) => (await api.get<ApiResponse<Shipment>>(`/shipments/${id}`)).data.data,
  getByOrder: async (orderId: string) => (await api.get<ApiResponse<Shipment[]>>(`/shipments/order/${orderId}`)).data.data,
  create: async (data: { orderId: string; trackingNumber: string; carrier: string; recipientName: string; recipientAddress: string }) =>
    (await api.post<ApiResponse<Shipment>>('/shipments', { ...data, shippingAddress: data.recipientAddress })).data.data,
  deliver: async (id: string) => (await api.post<ApiResponse<Shipment>>(`/shipments/${id}/deliver`)).data.data,
};

// ─── Incidents ────────────────────────────────────────
export const incidentApi = {
  list: async (status?: string, severity?: string) => {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (severity) params.append('severity', severity);
    return (await api.get<ApiResponse<Incident[]>>(`/incidents?${params.toString()}`)).data.data;
  },
  get: async (id: string) => (await api.get<ApiResponse<Incident>>(`/incidents/${id}`)).data.data,
  update: async (id: string, updates: { status?: string; title?: string }) =>
    (await api.patch<ApiResponse<Incident>>(`/incidents/${id}`, updates)).data.data,
};

// ─── Timeline ─────────────────────────────────────────
export const timelineApi = {
  get: async (incidentId: string) => (await api.get<ApiResponse<TimelineResponse>>(`/incidents/${incidentId}/timeline`)).data.data,
};

// ─── Analysis ─────────────────────────────────────────
export const analysisApi = {
  get: async (incidentId: string) => (await api.get<ApiResponse<AnalysisResponse>>(`/incidents/${incidentId}/analysis`)).data.data,
  trigger: async (incidentId: string, force = false) =>
    (await api.post<ApiResponse<AnalysisResponse>>(`/incidents/${incidentId}/analysis`, { force })).data.data,
};

// ─── Similar ──────────────────────────────────────────
export const similarApi = {
  get: async (incidentId: string, limit = 10) =>
    (await api.get<ApiResponse<SimilarIncident[]>>(`/incidents/${incidentId}/similar?limit=${limit}`)).data.data,
};

// ─── Logs ─────────────────────────────────────────────
export const logApi = {
  query: async (params: { correlationId?: string; serviceName?: string; level?: string; startTime?: string; endTime?: string }) => {
    const sp = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v) sp.append(k, v); });
    return (await api.get<ApiResponse<LogEntry[]>>(`/logs?${sp.toString()}`)).data.data;
  },
  errorStats: async (startTime: string, endTime: string) =>
    (await api.get<ApiResponse<LogStatsResponse>>(`/logs/errors/stats?startTime=${startTime}&endTime=${endTime}`)).data.data,
};

// ─── Health ───────────────────────────────────────────
export const healthApi = {
  check: async () => (await api.get<ApiResponse<HealthStatus>>('/health')).data.data,
};

// ─── Notifications ────────────────────────────────────
export const notificationApi = {
  list: async (filters?: { correlationId?: string; status?: string; eventType?: string }) => {
    const sp = new URLSearchParams();
    if (filters) Object.entries(filters).forEach(([k, v]) => { if (v) sp.append(k, v); });
    const qs = sp.toString();
    return (await api.get<ApiResponse<Notification[]>>(`/notifications${qs ? '?' + qs : ''}`)).data.data;
  },
  get: async (id: string) => (await api.get<ApiResponse<Notification>>(`/notifications/${id}`)).data.data,
  health: async () => (await api.get<ApiResponse<{ status: string }>>('/notifications/health')).data.data,
};
