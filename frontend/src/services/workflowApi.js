import api from './api';

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
  completeTask: (instanceId, data) => api.post(`/workflow-instances/${instanceId}/complete-task`, data),
  deleteInstance: (id) => api.delete(`/workflow-instances/${id}`),
};

export const NODE_TYPES = [
  { type: 'start', name: '开始节点', icon: '▶', color: '#52c41a', canDelete: false },
  { type: 'approval', name: '审批节点', icon: '✓', color: '#1890ff', canDelete: true },
  { type: 'condition', name: '条件分支', icon: '◆', color: '#fa8c16', canDelete: true },
  { type: 'auto', name: '自动任务', icon: '⚙', color: '#722ed1', canDelete: true },
  { type: 'end', name: '结束节点', icon: '■', color: '#f5222d', canDelete: false },
];

export const getNodeTypeConfig = (type) => {
  return NODE_TYPES.find(t => t.type === type) || NODE_TYPES[1];
};
