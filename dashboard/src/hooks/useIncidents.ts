import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { incidentApi, timelineApi, analysisApi, similarApi, logApi } from '../services/api';

export const useIncidents = (status?: string, severity?: string) => {
  return useQuery({
    queryKey: ['incidents', status, severity],
    queryFn: () => incidentApi.list(status, severity),
  });
};

export const useIncident = (id: string) => {
  return useQuery({
    queryKey: ['incident', id],
    queryFn: () => incidentApi.get(id),
    enabled: !!id,
  });
};

export const useTimeline = (incidentId: string) => {
  return useQuery({
    queryKey: ['timeline', incidentId],
    queryFn: () => timelineApi.get(incidentId),
    enabled: !!incidentId,
  });
};

export const useAnalysis = (incidentId: string) => {
  return useQuery({
    queryKey: ['analysis', incidentId],
    queryFn: () => analysisApi.get(incidentId),
    enabled: !!incidentId,
    retry: false,
  });
};

export const useTriggerAnalysis = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ incidentId, force }: { incidentId: string; force?: boolean }) =>
      analysisApi.trigger(incidentId, force),
    onSuccess: (_, { incidentId }) => {
      queryClient.invalidateQueries({ queryKey: ['analysis', incidentId] });
    },
  });
};

export const useSimilarIncidents = (incidentId: string) => {
  return useQuery({
    queryKey: ['similar', incidentId],
    queryFn: () => similarApi.get(incidentId),
    enabled: !!incidentId,
  });
};

export const useLogs = (params: { correlationId?: string; serviceName?: string; level?: string; startTime?: string; endTime?: string }) => {
  return useQuery({
    queryKey: ['logs', params],
    queryFn: () => logApi.query(params),
  });
};
