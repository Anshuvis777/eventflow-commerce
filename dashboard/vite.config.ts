import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/v1/orders': { target: 'http://localhost:8081', changeOrigin: true },
      '/api/v1/payments': { target: 'http://localhost:8082', changeOrigin: true },
      '/api/v1/inventory': { target: 'http://localhost:8083', changeOrigin: true },
      '/api/v1/shipments': { target: 'http://localhost:8084', changeOrigin: true },
      '/api/v1/incidents': { target: 'http://localhost:8091', changeOrigin: true },
      '/api/v1/logs': { target: 'http://localhost:8091', changeOrigin: true },
      '/api/v1/health': { target: 'http://localhost:8091', changeOrigin: true },
    },
  },
});
