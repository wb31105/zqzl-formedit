import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  response => response,
  error => {
    throw error;
  }
);

export function getErrorMessage(error, defaultMsg = '操作失败') {
  return error?.response?.data?.error || error?.message || defaultMsg;
}

export const formApi = {
  getAllForms: (params) => api.get('/forms', { params }),
  getFormsList: () => api.get('/forms/list'),
  getFormById: (id) => api.get(`/forms/${id}`),
  createForm: (data) => api.post('/forms', data),
  updateForm: (id, data) => api.put(`/forms/${id}`, data),
  deleteForm: (id) => api.delete(`/forms/${id}`),
  validateForm: (formId, data) => api.post('/forms/validate', { formId, data }),
};

export default api;
