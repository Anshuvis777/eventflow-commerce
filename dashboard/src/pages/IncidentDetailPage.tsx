import { useParams } from 'react-router-dom';
import { useState } from 'react';
import { useIncident, useTimeline, useAnalysis, useTriggerAnalysis, useSimilarIncidents, useLogs } from '../hooks/useIncidents';
import IncidentDetail from '../components/IncidentDetail';

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [logFilters, setLogFilters] = useState<{ correlationId?: string }>({});

  const { data: incident, isLoading: incidentLoading } = useIncident(id || '');
  const { data: timeline } = useTimeline(id || '');
  const { data: analysis } = useAnalysis(id || '');
  const { data: similar = [] } = useSimilarIncidents(id || '');
  const triggerAnalysis = useTriggerAnalysis();
  const { data: logs = [] } = useLogs({ ...logFilters, correlationId: logFilters.correlationId || incident?.correlationId });

  if (incidentLoading) {
    return <div className="flex justify-center items-center h-screen"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div></div>;
  }

  if (!incident) {
    return <div className="text-center py-12"><p className="text-gray-500">Incident not found</p></div>;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        <IncidentDetail
          incident={incident}
          timeline={timeline || null}
          analysis={analysis || null}
          similar={similar}
          logs={logs}
          onAnalyze={() => triggerAnalysis.mutate({ incidentId: id || '' })}
          isAnalyzing={triggerAnalysis.isPending}
          onLogFilterChange={setLogFilters}
        />
      </div>
    </div>
  );
}
