import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

const SYSTEM_ERROR_MESSAGE = '系统异常，请稍后重试';
const NETWORK_ERROR_MESSAGE = '网络连接失败，请检查网络或稍后重试';

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  response => {
    const data = response.data;
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      return { ...response, data: data.data, apiResponse: data };
    }
    return response;
  },
  error => {
    let errorInfo = {
      isSystemError: true,
      code: 50000,
      message: SYSTEM_ERROR_MESSAGE,
    };

    if (!error.response) {
      errorInfo.message = NETWORK_ERROR_MESSAGE;
      errorInfo.code = 50001;
    } else if (error.response.data && typeof error.response.data === 'object') {
      const errData = error.response.data;
      if ('code' in errData) {
        errorInfo.code = errData.code;
        errorInfo.message = errData.message || errorInfo.message;
        if (errData.code >= 50000) {
          errorInfo.isSystemError = true;
        } else {
          errorInfo.isSystemError = false;
        }
        if (errData.fieldErrors) {
          errorInfo.fieldErrors = errData.fieldErrors;
        }
        if (errData.detail) {
          errorInfo.detail = errData.detail;
        }
      }
    } else if (error.response.status === 404) {
      errorInfo.code = 40400;
      errorInfo.message = '资源不存在';
      errorInfo.isSystemError = false;
    } else if (error.response.status >= 500) {
      errorInfo.message = SYSTEM_ERROR_MESSAGE;
    }

    const enhancedError = new Error(errorInfo.message);
    enhancedError.isSystemError = errorInfo.isSystemError;
    enhancedError.errorCode = errorInfo.code;
    enhancedError.errorMessage = errorInfo.message;
    enhancedError.fieldErrors = errorInfo.fieldErrors || null;
    enhancedError.status = error.response ? error.response.status : 0;
    enhancedError.response = error.response;
    enhancedError.originalError = error;

    throw enhancedError;
  }
);

export function getErrorMessage(error, defaultMsg = '操作失败') {
  if (!error) return defaultMsg;
  if (error.errorMessage) return error.errorMessage;
  if (error.message && error.message !== 'Network Error') return error.message;
  return defaultMsg;
}

export function isSystemError(error) {
  return error && error.isSystemError;
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
