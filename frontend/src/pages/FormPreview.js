import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import FieldRenderer from '../components/FieldRenderer';
import { formApi } from '../services/api';

function FormPreview() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState(null);
  const [formData, setFormData] = useState({});
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    loadForm();
  }, [id]);

  const loadForm = async () => {
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
    const newErrors = {};

    form.fields?.forEach((field) => {
      const value = formData[field.id];
      const stringValue = value ? String(value).trim() : '';
      const isEmpty = !value || (Array.isArray(value) && value.length === 0) || stringValue === '';

      if (field.required && isEmpty) {
        newErrors[field.id] = `${field.label}不能为空`;
        return;
      }

      if (isEmpty) return;

      const isTextLike = ['text', 'textarea', 'email', 'number'].includes(field.type);

      if (isTextLike && field.minLength && stringValue.length < field.minLength) {
        newErrors[field.id] = `${field.label}最少需要${field.minLength}个字符`;
      }

      if (isTextLike && field.maxLength && stringValue.length > field.maxLength) {
        newErrors[field.id] = `${field.label}最多允许${field.maxLength}个字符`;
      }

      if (field.pattern && field.pattern.trim() && stringValue) {
        try {
          const regex = new RegExp(field.pattern);
          if (!regex.test(stringValue)) {
            newErrors[field.id] = field.patternMessage || `${field.label}格式不正确`;
          }
        } catch (e) {
          console.error('正则表达式错误:', e);
        }
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
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
      } else {
        setErrors(response.data.errors || {});
      }
    } catch (error) {
      console.error('验证失败:', error);
      alert('后端验证失败');
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

  if (!form) {
    return <div className="preview-container">表单不存在</div>;
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
