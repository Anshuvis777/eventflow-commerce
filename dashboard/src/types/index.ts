export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  correlationId: string;
}

// ─── E-Commerce Types ─────────────────────────────────
export interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  customerName: string;
  status: string;
  totalAmount: number;
  currency: string;
  shippingAddress: string;
  items?: OrderItem[];
  createdAt: string;
}

export interface OrderItem {
  id?: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface Payment {
  id: string;
  paymentNumber: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  status: string;
  transactionId: string;
  createdAt: string;
}

export interface InventoryProduct {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  reserved: number;
  warehouseLocation: string;
  price?: number;
}

export interface Shipment {
  id: string;
  trackingNumber: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  carrier: string;
  shippingAddress: string;
  status: string;
  estimatedDelivery: string;
  actualDelivery: string;
  createdAt: string;
}

export interface Email {
  id: string;
  to: string;
  subject: string;
  body: string;
  status: string;
  createdAt: string;
}

export interface Notification {
  id: string;
  eventId: string;
  correlationId: string;
  eventType: string;
  recipient: string;
  subject: string;
  body?: string;
  status: string;
  sentAt: string;
}

export interface ServiceHealth {
  name: string;
  port: number;
  url: string;
  status: 'UP' | 'DOWN';
}

// ─── Incident Types ───────────────────────────────────
export interface Incident {
  id: string;
  correlationId: string;
  title: string;
  description: string;
  status: string;
  severity: string;
  affectedServices: string;
  createdAt: string;
  updatedAt: string;
}

export interface TimelineEvent {
  id: string;
  incidentId: string;
  correlationId: string;
  eventType: string;
  serviceName: string;
  severity: string;
  message: string;
  timestamp: string;
}

export interface TimelineResponse {
  incidentId: string;
  events: TimelineEvent[];
  duration: string;
  affectedServices: string[];
  totalEvents: number;
}

export interface AnalysisResponse {
  rootCause: string;
  impact: string;
  contributingFactors: string[];
  recommendedActions: string[];
  preventionMeasures: string[];
  confidenceScore: number;
  modelVersion: string;
  createdAt: string;
}

export interface SimilarIncident {
  incidentId: string;
  title: string;
  severity: string;
  status: string;
  similarityScore: number;
  matchedOn: string;
  rootCauseSummary: string;
}

export interface LogEntry {
  correlationId: string;
  serviceName: string;
  level: string;
  message: string;
  timestamp: string;
  traceId: string;
}

export interface LogStatsResponse {
  startTime: string;
  endTime: string;
  services: { serviceName: string; errorCount: number }[];
}

export interface HealthStatus {
  status: string;
  service: string;
  postgres: string;
  kafka: string;
  chromadb: string;
}

export interface Event {
  id: string;
  eventType: string;
  correlationId: string;
  payload?: Record<string, unknown>;
  timestamp: string;
  serviceName?: string;
  severity?: string;
  message?: string;
}
