import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ControlLibrary from '../components/ControlLibrary';
import PropertiesPanel from '../components/PropertiesPanel';
import FieldRenderer from '../components/FieldRenderer';
import { createField } from '../utils/fieldTypes';
import { formApi } from '../services/api';

function FormEditor() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNew = id === 'new';
  const hasPersistedId = !isNew && id && !isNaN(Number(id));

  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [fields, setFields] = useState([]);
  const [selectedFieldId, setSelectedFieldId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [currentFormId, setCurrentFormId] = useState(hasPersistedId ? id : null);
  const canvasRef = useRef(null);

  useEffect(() => {
    if (!isNew) {
      loadForm(id);
    }
  }, [id, isNew]);

  const loadForm = async (formId) => {
    try {
      const response = await formApi.getFormById(formId);
      const form = response.data;
      setFormName(form.name);
      setFormDescription(form.description || '');
      setFields(form.fields || []);
      setCurrentFormId(form.id);
    } catch (error) {
      console.error('加载表单失败:', error);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    const fieldType = e.dataTransfer.getData('fieldType');
    if (fieldType) {
      const newField = createField(fieldType);
      setFields([...fields, newField]);
      setSelectedFieldId(newField.id);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
  };

  const handleFieldClick = (fieldId) => {
    setSelectedFieldId(fieldId);
  };

  const handleDeleteField = (fieldId, e) => {
    e.stopPropagation();
    setFields(fields.filter((f) => f.id !== fieldId));
    if (selectedFieldId === fieldId) {
      setSelectedFieldId(null);
    }
  };

  const handleMoveUp = (fieldId, e) => {
    e.stopPropagation();
    const index = fields.findIndex((f) => f.id === fieldId);
    if (index > 0) {
      const newFields = [...fields];
      [newFields[index - 1], newFields[index]] = [newFields[index], newFields[index - 1]];
      setFields(newFields);
    }
  };

  const handleMoveDown = (fieldId, e) => {
    e.stopPropagation();
    const index = fields.findIndex((f) => f.id === fieldId);
    if (index < fields.length - 1) {
      const newFields = [...fields];
      [newFields[index], newFields[index + 1]] = [newFields[index + 1], newFields[index]];
      setFields(newFields);
    }
  };

  const handleUpdateField = (updatedField) => {
    setFields(fields.map((f) => (f.id === updatedField.id ? updatedField : f)));
  };

  const selectedField = fields.find((f) => f.id === selectedFieldId);

  const handleSave = async () => {
    if (!formName.trim()) {
      alert('请输入表单名称');
      return;
    }

    setSaving(true);
    try {
      const formData = {
        name: formName,
        description: formDescription,
        fields: fields,
      };

      if (isNew && !currentFormId) {
        const response = await formApi.createForm(formData);
        setCurrentFormId(response.data.id);
        navigate(`/editor/${response.data.id}`, { replace: true });
      } else {
        const formIdToSave = currentFormId || id;
        await formApi.updateForm(formIdToSave, formData);
      }
      alert('保存成功');
    } catch (error) {
      console.error('保存失败:', error);
      alert('保存失败: ' + (error.response?.data?.message || error.message));
    } finally {
      setSaving(false);
    }
  };

  const canPreview = !!currentFormId;

  return (
    <div className="form-editor">
      <ControlLibrary />

      <div className="editor-main">
        <div className="editor-toolbar">
          <button className="btn btn-default" onClick={() => navigate('/')}>
            ← 返回列表
          </button>
          <input
            type="text"
            className="form-name-input"
            placeholder="表单名称"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
          />
          <div className="spacer" />
          <button
            className="btn btn-default"
            onClick={() => canPreview && navigate(`/preview/${currentFormId}`)}
            disabled={!canPreview}
            title={!canPreview ? '请先保存表单后再预览' : '预览表单'}
          >
            预览
          </button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </button>
        </div>

        <div className="canvas-container">
          <div
            className="canvas"
            ref={canvasRef}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
          >
            <div style={{ marginBottom: 24 }}>
              <input
                type="text"
                placeholder="表单描述（可选）"
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                style={{ width: '100%', border: 'none', outline: 'none', fontSize: 14, color: '#666' }}
              />
            </div>

            {fields.length === 0 ? (
              <div className="canvas-empty">从左侧拖拽控件到此处</div>
            ) : (
              <div className="canvas-grid">
                {fields.map((field) => {
                  const span = field.span || 24;
                  return (
                    <div
                      key={field.id}
                      className={`canvas-field ${selectedFieldId === field.id ? 'selected' : ''}`}
                      style={{ gridColumn: `span ${span}` }}
                      onClick={() => handleFieldClick(field.id)}
                    >
                      <div className="field-header">
                        <span className="field-label">
                          {field.required && <span className="required">*</span>}
                          {field.label}
                          <span className="field-span-badge">{span === 24 ? '整行' : `${span}/24`}</span>
                        </span>
                        <div className="field-actions">
                          <button onClick={(e) => handleMoveUp(field.id, e)}>↑</button>
                          <button onClick={(e) => handleMoveDown(field.id, e)}>↓</button>
                          <button onClick={(e) => handleDeleteField(field.id, e)}>删除</button>
                        </div>
                      </div>
                      <FieldRenderer field={field} value={field.defaultValue} onChange={() => {}} />
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      <PropertiesPanel field={selectedField} onUpdate={handleUpdateField} />
    </div>
  );
}

export default FormEditor;
