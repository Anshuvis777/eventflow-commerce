import { useState } from 'react';
import type { Incident, TimelineResponse, AnalysisResponse, SimilarIncident, LogEntry } from '../types';
import Timeline from './Timeline';
import Analysis from './Analysis';
import SimilarIncidents from './SimilarIncidents';
import LogViewer from './LogViewer';

interface IncidentDetailProps {
  incident: Incident;
  timeline: TimelineResponse | null;
  analysis: AnalysisResponse | null;
  similar: SimilarIncident[];
  logs: LogEntry[];
  onAnalyze: () => void;
  isAnalyzing: boolean;
  onLogFilterChange: (filters: { correlationId?: string }) => void;
}

type Tab = 'overview' | 'timeline' | 'analysis' | 'similar' | 'logs';

export default function IncidentDetail({
  incident, timeline, analysis, similar, logs, onAnalyze, isAnalyzing, onLogFilterChange,
}: IncidentDetailProps) {
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  const tabs: { id: Tab; label: string }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'timeline', label: 'Timeline' },
    { id: 'analysis', label: 'Analysis' },
    { id: 'similar', label: 'Similar' },
    { id: 'logs', label: 'Logs' },
  ];

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{incident.title}</h1>
        <div className="flex gap-2 mt-2">
          <span className={`px-2 py-1 text-xs font-medium rounded-full ${
            incident.severity === 'CRITICAL' ? 'bg-red-100 text-red-800' :
            incident.severity === 'HIGH' ? 'bg-orange-100 text-orange-800' :
            incident.severity === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
            'bg-green-100 text-green-800'
          }`}>{incident.severity}</span>
          <span className={`px-2 py-1 text-xs font-medium rounded-full ${
            incident.status === 'OPEN' ? 'bg-blue-100 text-blue-800' :
            incident.status === 'ANALYZING' ? 'bg-purple-100 text-purple-800' :
            incident.status === 'ANALYZED' ? 'bg-indigo-100 text-indigo-800' :
            'bg-gray-100 text-gray-800'
          }`}>{incident.status}</span>
        </div>
      </div>

      <div className="flex border-b mb-6">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 font-medium text-sm border-b-2 transition ${
              activeTab === tab.id ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        {activeTab === 'overview' && (
          <div className="space-y-4">
            <div><h3 className="font-semibold">Description</h3><p className="text-gray-600">{incident.description || 'No description'}</p></div>
            <div><h3 className="font-semibold">Correlation ID</h3><p className="text-gray-600 font-mono text-sm">{incident.correlationId}</p></div>
            <div><h3 className="font-semibold">Affected Services</h3><p className="text-gray-600">{incident.affectedServices || 'N/A'}</p></div>
            <div><h3 className="font-semibold">Created</h3><p className="text-gray-600">{new Date(incident.createdAt).toLocaleString()}</p></div>
          </div>
        )}
        {activeTab === 'timeline' && <Timeline events={timeline?.events || []} />}
        {activeTab === 'analysis' && <Analysis analysis={analysis} isLoading={false} onAnalyze={onAnalyze} isAnalyzing={isAnalyzing} />}
        {activeTab === 'similar' && <SimilarIncidents incidents={similar} />}
        {activeTab === 'logs' && <LogViewer logs={logs} onFilterChange={(f) => onLogFilterChange(f)} />}
      </div>
    </div>
  );
}
