import type { LogEntry } from '../types';

interface LogViewerProps {
  logs: LogEntry[];
  onFilterChange: (filters: { correlationId?: string; serviceName?: string; level?: string }) => void;
}

const levelColors: Record<string, string> = {
  ERROR: 'bg-red-100 text-red-800',
  CRITICAL: 'bg-red-200 text-red-900',
  WARN: 'bg-yellow-100 text-yellow-800',
  INFO: 'bg-blue-100 text-blue-800',
  DEBUG: 'bg-gray-100 text-gray-600',
  TRACE: 'bg-gray-50 text-gray-500',
};

export default function LogViewer({ logs, onFilterChange }: LogViewerProps) {
  return (
    <div>
      <div className="flex gap-2 mb-4">
        <select onChange={(e) => onFilterChange({ level: e.target.value || undefined })} className="border rounded px-3 py-1">
          <option value="">All Levels</option>
          <option value="ERROR">ERROR</option>
          <option value="CRITICAL">CRITICAL</option>
          <option value="WARN">WARN</option>
          <option value="INFO">INFO</option>
          <option value="DEBUG">DEBUG</option>
        </select>
      </div>

      <div className="bg-gray-900 rounded-lg p-4 font-mono text-sm max-h-96 overflow-y-auto">
        {logs.length === 0 ? (
          <p className="text-gray-400">No logs found</p>
        ) : (
          logs.map((log, i) => (
            <div key={i} className="py-1 border-b border-gray-800 flex gap-2">
              <span className="text-gray-500">{new Date(log.timestamp).toLocaleTimeString()}</span>
              <span className={`px-1 rounded text-xs ${levelColors[log.level] || ''}`}>{log.level}</span>
              <span className="text-blue-400">[{log.serviceName}]</span>
              <span className="text-gray-300 flex-1">{log.message}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
