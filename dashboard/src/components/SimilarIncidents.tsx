import { Link } from 'react-router-dom';
import type { SimilarIncident } from '../types';

interface SimilarIncidentsProps {
  incidents: SimilarIncident[];
}

export default function SimilarIncidents({ incidents }: SimilarIncidentsProps) {
  if (incidents.length === 0) {
    return <p className="text-gray-500 text-center py-8">No similar incidents found</p>;
  }

  return (
    <div className="space-y-3">
      {incidents.map((incident) => (
        <Link
          key={incident.incidentId}
          to={`/incidents/${incident.incidentId}`}
          className="block bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-300 hover:shadow transition"
        >
          <div className="flex justify-between items-start">
            <div>
              <h4 className="font-medium text-gray-900">{incident.title}</h4>
              <span className={`text-xs px-2 py-0.5 rounded ${
                incident.severity === 'CRITICAL' ? 'bg-red-100 text-red-800' :
                incident.severity === 'HIGH' ? 'bg-orange-100 text-orange-800' : 'bg-gray-100 text-gray-600'
              }`}>{incident.severity}</span>
            </div>
            <div className="text-right">
              <span className="text-2xl font-bold text-blue-600">
                {(incident.similarityScore * 100).toFixed(0)}%
              </span>
              <p className="text-xs text-gray-500">match</p>
            </div>
          </div>
          {incident.rootCauseSummary && (
            <p className="text-sm text-gray-600 mt-2">{incident.rootCauseSummary}</p>
          )}
        </Link>
      ))}
    </div>
  );
}
