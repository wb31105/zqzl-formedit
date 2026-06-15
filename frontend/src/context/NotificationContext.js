import React, { createContext, useContext, useState, useCallback } from 'react';
import './Notification.css';

const NotificationContext = createContext();

export const useNotification = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within NotificationProvider');
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);
  const [confirmState, setConfirmState] = useState(null);
  const [pageAlert, setPageAlert] = useState(null);

  const addNotification = useCallback((type, message, duration = 3000) => {
    const id = Date.now() + Math.random();
    setNotifications(prev => [...prev, { id, type, message }]);
    if (duration > 0) {
      setTimeout(() => {
        setNotifications(prev => prev.filter(n => n.id !== id));
      }, duration);
    }
    return id;
  }, []);

  const removeNotification = useCallback((id) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  const showError = useCallback((message, duration = 5000) => {
    return addNotification('error', message, duration);
  }, [addNotification]);

  const showSuccess = useCallback((message, duration = 3000) => {
    return addNotification('success', message, duration);
  }, [addNotification]);

  const showInfo = useCallback((message, duration = 3000) => {
    return addNotification('info', message, duration);
  }, [addNotification]);

  const showWarning = useCallback((message, duration = 4000) => {
    return addNotification('warning', message, duration);
  }, [addNotification]);

  const showConfirm = useCallback((message, title = '确认操作') => {
    return new Promise((resolve) => {
      setConfirmState({
        message,
        title,
        resolve,
      });
    });
  }, []);

  const setAlert = useCallback((type, message, duration = 0) => {
    const id = Date.now() + Math.random();
    setPageAlert({ id, type, message });
    if (duration > 0) {
      setTimeout(() => {
        setPageAlert(prev => prev && prev.id === id ? null : prev);
      }, duration);
    }
    return id;
  }, []);

  const clearAlert = useCallback((id) => {
    if (id) {
      setPageAlert(prev => prev && prev.id === id ? null : prev);
    } else {
      setPageAlert(null);
    }
  }, []);

  const handleConfirmOk = useCallback(() => {
    if (confirmState) {
      confirmState.resolve(true);
      setConfirmState(null);
    }
  }, [confirmState]);

  const handleConfirmCancel = useCallback(() => {
    if (confirmState) {
      confirmState.resolve(false);
      setConfirmState(null);
    }
  }, [confirmState]);

  const value = {
    showError,
    showSuccess,
    showInfo,
    showWarning,
    showConfirm,
    removeNotification,
    setAlert,
    clearAlert,
    pageAlert,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
      {pageAlert && (
        <div className="page-alert-container">
          <div className={`page-alert page-alert-${pageAlert.type}`}>
            <span className="page-alert-icon">
              {pageAlert.type === 'error' && '✕'}
              {pageAlert.type === 'success' && '✓'}
              {pageAlert.type === 'info' && 'ℹ'}
              {pageAlert.type === 'warning' && '⚠'}
            </span>
            <span className="page-alert-message">{pageAlert.message}</span>
            <span className="page-alert-close" onClick={() => clearAlert()}>×</span>
          </div>
        </div>
      )}
      <div className="notification-container">
        {notifications.map(notification => (
          <div
            key={notification.id}
            className={`notification notification-${notification.type}`}
            onClick={() => removeNotification(notification.id)}
          >
            <span className="notification-icon">
              {notification.type === 'error' && '✕'}
              {notification.type === 'success' && '✓'}
              {notification.type === 'info' && 'ℹ'}
              {notification.type === 'warning' && '⚠'}
            </span>
            <span className="notification-message">{notification.message}</span>
            <span className="notification-close">×</span>
          </div>
        ))}
      </div>

      {confirmState && (
        <div className="confirm-overlay" onClick={handleConfirmCancel}>
          <div className="confirm-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="confirm-header">
              <h3>{confirmState.title}</h3>
            </div>
            <div className="confirm-body">
              <div className="confirm-icon">⚠️</div>
              <div className="confirm-message">{confirmState.message}</div>
            </div>
            <div className="confirm-footer">
              <button className="btn btn-default" onClick={handleConfirmCancel}>
                取消
              </button>
              <button className="btn btn-primary" onClick={handleConfirmOk}>
                确定
              </button>
            </div>
          </div>
        </div>
      )}
    </NotificationContext.Provider>
  );
};

export default NotificationContext;
