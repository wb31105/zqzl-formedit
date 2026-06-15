import React from 'react';
import { useNavigate } from 'react-router-dom';

function PageError({ title, message, onRetry, showBack = true, backTo = '/', backText = '返回首页' }) {
  const navigate = useNavigate();

  const handleBack = () => {
    navigate(backTo);
  };

  return (
    <div className="page-error-container">
      <div className="page-error-content">
        <div className="page-error-icon">⚠️</div>
        <h2 className="page-error-title">{title || '加载失败'}</h2>
        <p className="page-error-message">{message || '页面加载出现问题，请稍后重试'}</p>
        <div className="page-error-actions">
          {onRetry && (
            <button className="btn btn-primary" onClick={onRetry}>
              重新加载
            </button>
          )}
          {showBack && (
            <button className="btn btn-default" onClick={handleBack}>
              {backText}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default PageError;
