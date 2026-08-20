import { useIncidents } from '../hooks/useIncidents';
import IncidentList from '../components/IncidentList';

export default function IncidentListPage() {
  const { data: incidents = [], isLoading } = useIncidents();

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Incidents Dashboard</h1>
        <IncidentList incidents={incidents} isLoading={isLoading} />
      </div>
    </div>
  );
}
