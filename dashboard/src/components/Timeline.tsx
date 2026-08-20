import type { Event } from '../types';

interface TimelineProps {
  events: Event[];
}

export default function Timeline({ events }: TimelineProps) {
  if (events.length === 0) {
    return <p className="text-gray-500 text-center py-8">No events recorded</p>;
  }

  return (
    <div className="space-y-4">
      {events.map((event, index) => (
        <div key={event.id} className="flex gap-4">
          <div className="flex flex-col items-center">
            <div className="w-3 h-3 rounded-full bg-blue-500"></div>
            {index < events.length - 1 && <div className="w-0.5 h-full bg-gray-200"></div>}
          </div>
          <div className="pb-4">
            <div className="flex items-center gap-2">
              <span className="font-medium text-gray-900">{event.eventType}</span>
              <span className="text-sm text-gray-500">{event.serviceName}</span>
              <span className={`text-xs px-2 py-0.5 rounded ${
                event.severity === 'ERROR' || event.severity === 'CRITICAL' 
                  ? 'bg-red-100 text-red-800' 
                  : 'bg-gray-100 text-gray-600'
              }`}>{event.severity}</span>
            </div>
            <p className="text-sm text-gray-600 mt-1">{event.message}</p>
            <p className="text-xs text-gray-400 mt-1">{new Date(event.timestamp).toLocaleString()}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
