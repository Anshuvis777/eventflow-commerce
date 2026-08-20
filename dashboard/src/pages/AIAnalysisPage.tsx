import { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { incidentApi, analysisApi, similarApi } from '../services/api';
import type { Incident, AnalysisResponse, SimilarIncident } from '../types';

export default function AIAnalysisPage() {
  const [correlationId, setCorrelationId] = useState('');
  const [incident, setIncident] = useState<Incident | null>(null);
  const [analysis, setAnalysis] = useState<AnalysisResponse | null>(null);
  const [similar, setSimilar] = useState<SimilarIncident[]>([]);
  const [processing, setProcessing] = useState(false);
  const [pollCount, setPollCount] = useState(0);

  const analyzeMut = useMutation({
    mutationFn: async (cid: string) => {
      const incidents = await incidentApi.list();
      const inc = incidents.find((i: any) => i.correlationId === cid);
      if (!inc) throw new Error('No incident found');
      return { incident: inc, analysis: await analysisApi.trigger(inc.id, true) };
    },
    onSuccess: (data) => {
      setIncident(data.incident);
      setAnalysis(data.analysis);
      setProcessing(true);
      setPollCount(0);
    },
    onError: () => { setProcessing(false); },
  });

  // Poll for results after triggering analysis
  useEffect(() => {
    if (!processing || !incident || pollCount > 10) return;
    const timer = setTimeout(async () => {
      try {
        const result = await analysisApi.get(incident.id);
        if (result && result.confidenceScore > 0) {
          setAnalysis(result);
          setProcessing(false);
          const sim = await similarApi.get(incident.id).catch(() => []);
          setSimilar(sim);
        } else {
          setPollCount(p => p + 1);
        }
      } catch {
        setPollCount(p => p + 1);
      }
    }, 3000);
    return () => clearTimeout(timer);
  }, [processing, incident, pollCount]);

  return (
    <div>
      <div className="page-header"><h1>🤖 AI Analysis</h1><p>AI-powered root cause analysis using Gemini</p></div>

      <div className="card">
        <div className="card-title">🧠 Analyze Incident</div>
        <div style={{ display: 'flex', gap: 8 }}>
          <input placeholder="Enter Correlation ID" value={correlationId} onChange={(e) => setCorrelationId(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && correlationId && analyzeMut.mutate(correlationId)} style={{ flex: 1, padding: '9px 12px', border: '1px solid var(--border)', borderRadius: 8, fontSize: 14 }} />
          <button className="btn btn-primary" onClick={() => correlationId && analyzeMut.mutate(correlationId)} disabled={analyzeMut.isPending || !correlationId}>
            {analyzeMut.isPending ? 'Analyzing...' : '🤖 Analyze with AI'}
          </button>
        </div>
        {processing && <div className="mt-2 text-sm text-muted">⏳ Processing analysis... ({pollCount * 3}s elapsed)</div>}
        {analyzeMut.isError && <div className="mt-2 text-sm" style={{ color: 'var(--red)' }}>No incident found for this correlation ID</div>}
      </div>

      {incident && (
        <div className="stat-grid">
          <div className="stat-card">
            <div className="stat-icon blue">🔧</div>
            <div><div className="stat-value text-sm">{incident.affectedServices || '—'}</div><div className="stat-label">Service Name</div></div>
          </div>
          <div className="stat-card">
            <div className="stat-icon red">⚡</div>
            <div><div className="stat-value"><span className={`badge b-${incident.severity}`}>{incident.severity}</span></div><div className="stat-label">Severity</div></div>
          </div>
          <div className="stat-card">
            <div className="stat-icon green">✅</div>
            <div><div className="stat-value">{analysis ? 'Yes' : 'No'}</div><div className="stat-label">Analyzed</div></div>
          </div>
          <div className="stat-card">
            <div className="stat-icon purple">📊</div>
            <div><div className="stat-value">{analysis?.confidenceScore ?? '—'}%</div><div className="stat-label">Confidence Score</div></div>
          </div>
        </div>
      )}

      {analysis && (
        <>
          <div className="card">
            <div className="card-title">🔍 Root Cause Analysis</div>
            <div className="mb-3"><strong>Root Cause:</strong><div className="text-sm mt-2">{analysis.rootCause}</div></div>
            <div className="mb-3"><strong>Impact Assessment:</strong><div className="text-sm mt-2">{analysis.impact}</div></div>
            <div className="mb-3">
              <strong>Contributing Factors:</strong>
              <ul className="mt-2">{analysis.contributingFactors?.map((f, i) => <li key={i} className="text-sm" style={{ marginLeft: 16, marginBottom: 4 }}>• {f}</li>)}</ul>
            </div>
            <div className="mb-3">
              <strong>Recommended Actions:</strong>
              <ul className="mt-2">{analysis.recommendedActions?.map((a, i) => <li key={i} className="text-sm" style={{ marginLeft: 16, marginBottom: 4 }}>• {a}</li>)}</ul>
            </div>
            <div>
              <strong>Prevention Measures:</strong>
              <ul className="mt-2">{analysis.preventionMeasures?.map((p, i) => <li key={i} className="text-sm" style={{ marginLeft: 16, marginBottom: 4 }}>• {p}</li>)}</ul>
            </div>
          </div>

          {similar.length > 0 && (
            <div className="card">
              <div className="card-title">🔗 Similar Incidents</div>
              <div className="table-wrap">
                <table>
                  <thead><tr><th>Title</th><th>Severity</th><th>Match</th><th>Root Cause</th></tr></thead>
                  <tbody>
                    {similar.map((s, i) => (
                      <tr key={i}>
                        <td>{s.title}</td>
                        <td><span className={`badge b-${s.severity}`}>{s.severity}</span></td>
                        <td><strong>{(s.similarityScore * 100).toFixed(0)}%</strong></td>
                        <td className="text-sm">{s.rootCauseSummary}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="card">
            <div className="card-title">📋 Analysis History</div>
            <div className="text-sm">
              <div>Model: <strong>{analysis.modelVersion}</strong></div>
              <div>Status: <span className="badge b-ANALYZED">COMPLETED</span></div>
              <div>Confidence: <strong>{analysis.confidenceScore}%</strong></div>
              <div>Analyzed at: {new Date(analysis.createdAt).toLocaleString()}</div>
            </div>
          </div>
        </>
      )}

      {!incident && !analyzeMut.isPending && (
        <div className="empty-state">
          <div className="empty-state-icon">🤖</div>
          <p>Enter a Correlation ID to run AI-powered root cause analysis</p>
        </div>
      )}
    </div>
  );
}
