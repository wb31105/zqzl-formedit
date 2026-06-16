import api from './api';
import { NODE_TYPES, getNodeTypeConfig } from '../constants/workflowConstants';

export { NODE_TYPES, getNodeTypeConfig };

export const workflowDefinitionApi = {
  getAllDefinitions: (params) => api.get('/workflow-definitions', { params }),
  getDefinitionById: (id) => api.get(`/workflow-definitions/${id}`),
  createDefinition: (data) => api.post('/workflow-definitions', data),
  updateDefinition: (id, data) => api.put(`/workflow-definitions/${id}`, data),
  deleteDefinition: (id) => api.delete(`/workflow-definitions/${id}`),
  validateDefinition: (data) => api.post('/workflow-definitions/validate', data),
};

export const workflowInstanceApi = {
  getAllInstances: (params) => api.get('/workflow-instances', { params }),
  getInstancesByDefinitionId: (definitionId) => api.get(`/workflow-instances/definition/${definitionId}`),
  getInstanceById: (id) => api.get(`/workflow-instances/${id}`),
  getExecutionLogs: (id) => api.get(`/workflow-instances/${id}/logs`),
  getInstanceTasks: (id) => api.get(`/workflow-instances/${id}/tasks`),
  getPendingTasks: (id) => api.get(`/workflow-instances/${id}/pending-tasks`),
  startInstance: (definitionId) => api.post(`/workflow-instances/start/${definitionId}`),
  startInstanceWithForm: (definitionId, data) => api.post(`/workflow-instances/start-with-form/${definitionId}`, data),
  completeTask: (instanceId, data) => api.post(`/workflow-instances/${instanceId}/complete-task`, data),
  deleteInstance: (id) => api.delete(`/workflow-instances/${id}`),
};
