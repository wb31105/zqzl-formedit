import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import FieldRenderer from '../components/FieldRenderer';
import { formApi, getErrorMessage } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import PageError from '../components/PageError';
import { validateFormFields } from '../utils/formValidation';

function FormPreview() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [formData, setFormData] = useState({});
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitted, setSubmitted] = useState(false);
  const [loadError, setLoadError] = useState('');
  const { setAlert, clearAlert } = useNotification();

  useEffect(() => {
    loadForm();
  }, [id]);

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const loadForm = async () => {
    setLoading(true);
    setLoadError('');
    clearAlert();
    try {
      const response = await formApi.getFormById(id);
      setForm(response.data);
      const initialData = {};
      response.data.fields?.forEach((field) => {
        initialData[field.id] = field.defaultValue || (field.type === 'checkbox' ? [] : '');
      });
      setFormData(initialData);
    } catch (error) {
      console.error('加载表单失败:', error);
      let msg;
      if (error.response?.status === 404) {
        msg = '表单不存在或已被删除';
      } else {
        msg = '加载表单失败: ' + getErrorMessage(error, '网络错误');
      }
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleFieldChange = (fieldId, value) => {
    setFormData({ ...formData, [fieldId]: value });
    if (errors[fieldId]) {
      const newErrors = { ...errors };
      delete newErrors[fieldId];
      setErrors(newErrors);
    }
  };

  const validateForm = () => {
    const result = validateFormFields(form.fields, formData);
    setErrors(result.errors || {});
    return result.valid;
  };

  const handleSubmit = async () => {
    const isValid = validateForm();
    if (!isValid) {
      return;
    }

    try {
      const response = await formApi.validateForm(parseInt(id), formData);
      if (response.data.valid) {
        setSubmitted(true);
        console.log('表单数据:', formData);
        setAlert('success', '表单提交成功', 3000);
      } else {
        setErrors(response.data.errors || {});
        setAlert('error', '表单验证未通过，请检查填写内容');
      }
    } catch (error) {
      console.error('验证失败:', error);
      setAlert('error', '后端验证失败: ' + getErrorMessage(error));
    }
  };

  const handleReset = () => {
    const initialData = {};
    form.fields?.forEach((field) => {
      initialData[field.id] = field.defaultValue || (field.type === 'checkbox' ? [] : '');
    });
    setFormData(initialData);
    setErrors({});
    setSubmitted(false);
  };

  if (loading) {
    return <div className="preview-container">加载中...</div>;
  }

  if (loadError || !form) {
    return (
      <div className="preview-container">
        <PageError
          title="加载失败"
          message={loadError || '表单不存在或已被删除'}
          onRetry={loadForm}
          backTo="/"
          backText="返回表单列表"
        />
      </div>
    );
  }

  return (
    <div className="preview-container">
      <div className="preview-header">
        <button className="btn btn-default" onClick={() => navigate('/')}>
          ← 返回列表
        </button>
        <h1>表单预览</h1>
        <button className="btn btn-default" onClick={() => navigate(`/editor/${id}`)}>
          编辑表单
        </button>
      </div>

      <div className="preview-form">
        <h2>{form.name}</h2>
        {form.description && <p className="form-description">{form.description}</p>}

        {submitted ? (
          <div className="success-message">
            <h3>🎉 提交成功！</h3>
            <p>表单数据已成功提交</p>
            <button className="btn btn-primary" onClick={handleReset} style={{ marginTop: 16 }}>
              重新填写
            </button>
          </div>
        ) : (
          <>
            <div className="preview-grid">
              {form.fields?.map((field) => {
                const span = field.span || 24;
                return (
                  <div key={field.id} className="preview-field" style={{ gridColumn: `span ${span}` }}>
                    <label>
                      {field.required && <span className="required">*</span>}
                      {field.label}
                    </label>
                    <FieldRenderer
                      field={field}
                      value={formData[field.id]}
                      onChange={handleFieldChange}
                      errors={errors}
                    />
                  </div>
                );
              })}
            </div>

            <div className="preview-actions">
              <button className="btn btn-primary" onClick={handleSubmit}>
                提交
              </button>
              <button className="btn btn-default" onClick={handleReset}>
                重置
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default FormPreview;
