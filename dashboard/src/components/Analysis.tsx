import type { AnalysisResponse } from '../types';

interface AnalysisProps {
  analysis: AnalysisResponse | null;
  isLoading: boolean;
  onAnalyze: () => void;
  isAnalyzing: boolean;
}

export default function Analysis({ analysis, isLoading, onAnalyze, isAnalyzing }: AnalysisProps) {
  if (isLoading) {
    return <div className="text-center py-8"><p className="text-gray-500">Loading analysis...</p></div>;
  }

  if (!analysis) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500 mb-4">No analysis available</p>
        <button
          onClick={onAnalyze}
          disabled={isAnalyzing}
          className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {isAnalyzing ? 'Analyzing...' : 'Analyze Incident'}
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">Root Cause Analysis</h3>
        <div className="flex items-center gap-4">
          <div className="text-right">
            <span className="text-sm text-gray-500">Confidence: </span>
            <span className="font-bold text-lg" style={{ color: analysis.confidenceScore > 70 ? 'green' : 'orange' }}>
              {analysis.confidenceScore}%
            </span>
          </div>
          <button
            onClick={onAnalyze}
            disabled={isAnalyzing}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 text-sm"
          >
            {isAnalyzing ? 'Re-analyzing...' : 'Re-analyze'}
          </button>
        </div>
      </div>

      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <h4 className="font-semibold text-red-800 mb-2">Root Cause</h4>
        <p className="text-red-700">{analysis.rootCause}</p>
      </div>

      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <h4 className="font-semibold text-yellow-800 mb-2">Impact</h4>
        <p className="text-yellow-700">{analysis.impact}</p>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <h4 className="font-semibold text-blue-800 mb-2">Contributing Factors</h4>
        <ul className="list-disc list-inside text-blue-700 space-y-1">
          {analysis.contributingFactors.map((factor, i) => <li key={i}>{factor}</li>)}
        </ul>
      </div>

      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
        <h4 className="font-semibold text-green-800 mb-2">Recommended Actions</h4>
        <ul className="list-disc list-inside text-green-700 space-y-1">
          {analysis.recommendedActions.map((action, i) => <li key={i}>{action}</li>)}
        </ul>
      </div>

      <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
        <h4 className="font-semibold text-purple-800 mb-2">Prevention Measures</h4>
        <ul className="list-disc list-inside text-purple-700 space-y-1">
          {analysis.preventionMeasures.map((measure, i) => <li key={i}>{measure}</li>)}
        </ul>
      </div>

      <p className="text-xs text-gray-400">Model: {analysis.modelVersion} | Generated: {new Date(analysis.createdAt).toLocaleString()}</p>
    </div>
  );
}
