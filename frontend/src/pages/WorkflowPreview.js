import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkflowCanvas from '../components/WorkflowCanvas';
import FieldRenderer from '../components/FieldRenderer';
import { workflowDefinitionApi, workflowInstanceApi } from '../services/workflowApi';
import { formApi, getErrorMessage } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import PageError from '../components/PageError';
import { validateFormFields } from '../utils/formValidation';

function WorkflowPreview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workflow, setWorkflow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const { showConfirm, setAlert, clearAlert } = useNotification();

  const [showStartModal, setShowStartModal] = useState(false);
  const [selectedForm, setSelectedForm] = useState(null);
  const [formData, setFormData] = useState({});
  const [formErrors, setFormErrors] = useState({});
  const [startingInstance, setStartingInstance] = useState(false);

  useEffect(() => {
    loadWorkflow();
  }, [id]);

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const loadWorkflow = async () => {
    setLoading(true);
    setLoadError('');
    clearAlert();
    try {
      const response = await workflowDefinitionApi.getDefinitionById(id);
      setWorkflow(response.data);
    } catch (error) {
      console.error('加载流程失败:', error);
      let msg;
      if (error.response?.status === 404) {
        msg = '流程不存在或已被删除';
      } else {
        msg = '加载流程失败: ' + getErrorMessage(error, '网络错误');
      }
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleStartInstance = async (e) => {
    e.stopPropagation();
    if (!workflow) return;

    const boundFormId = workflow.formId;

    if (boundFormId) {
      try {
        const response = await formApi.getFormById(boundFormId);
        setSelectedForm(response.data);
        setFormData({});
        setFormErrors({});
        setShowStartModal(true);
      } catch (error) {
        console.error('加载绑定表单失败:', error);
        setAlert('error', '加载绑定表单失败: ' + getErrorMessage(error));
      }
    } else {
      const confirmed = await showConfirm('此流程未绑定表单，是否直接启动？', '启动确认');
      if (confirmed) {
        navigate(`/workflow/instance/new/${id}`);
      }
    }
  };

  const handleFormFieldChange = (fieldId, value) => {
    setFormData(prev => ({
      ...prev,
      [fieldId]: value
    }));
    if (formErrors[fieldId]) {
      setFormErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[fieldId];
        return newErrors;
      });
    }
  };

  const validateForm = () => {
    if (!selectedForm) return true;
    const result = validateFormFields(selectedForm.fields, formData);
    setFormErrors(result.errors || {});
    return result.valid;
  };

  const doStartInstance = async () => {
    setStartingInstance(true);
    try {
      let response;
      if (workflow.formId) {
        response = await workflowInstanceApi.startInstanceWithForm(id, {
          formId: workflow.formId,
          formData: formData
        });
      } else {
        response = await workflowInstanceApi.startInstance(id);
      }
      const instanceId = response.data.id;
      setShowStartModal(false);
      setAlert('success', '流程启动成功', 3000);
      navigate(`/workflow/instance/${instanceId}`);
    } catch (error) {
      console.error('启动流程失败:', error);
      setAlert('error', '启动失败: ' + getErrorMessage(error));
    } finally {
      setStartingInstance(false);
    }
  };

  const handleConfirmStart = () => {
    if (workflow.formId && selectedForm) {
      if (!validateForm()) {
        return;
      }
    }
    doStartInstance();
  };

  const handleCloseModal = () => {
    if (!startingInstance) {
      setShowStartModal(false);
    }
  };

  if (loading) {
    return <div className="workflow-preview">加载中...</div>;
  }

  if (loadError || !workflow) {
    return (
      <div className="workflow-preview">
        <PageError
          title="加载失败"
          message={loadError || '流程不存在或已被删除'}
          onRetry={loadWorkflow}
          backTo="/workflows"
          backText="返回流程列表"
        />
      </div>
    );
  }

  return (
    <div className="workflow-preview">
      <div className="preview-header">
        <button className="btn btn-default" onClick={() => navigate('/workflows')}>
          ← 返回列表
        </button>
        <h2>流程预览: {workflow.name}</h2>
        <div className="preview-actions">
          <button className="btn btn-default" onClick={() => navigate(`/workflow/editor/${id}`)}>
            编辑
          </button>
          <button className="btn btn-success" onClick={handleStartInstance}>
            ▶ 启动流程
          </button>
        </div>
      </div>

      {workflow.description && (
        <div className="preview-description">
          <strong>描述：</strong>{workflow.description}
        </div>
      )}

      <div className="preview-canvas-container">
        <WorkflowCanvas
          nodes={workflow.nodes || []}
          edges={workflow.edges || []}
          selectedNodeId={null}
          selectedEdgeId={null}
          onSelectNode={() => {}}
          onSelectEdge={() => {}}
          onAddNode={() => {}}
          onUpdateNode={() => {}}
          onAddEdge={() => {}}
          onCanvasClick={() => {}}
          readOnly={true}
        />
      </div>

      <div className="preview-legend">
        <h4>图例说明</h4>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#52c41a' }}></span>
            <span>开始节点</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#1890ff' }}></span>
            <span>审批节点</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#fa8c16' }}></span>
            <span>条件分支</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#722ed1' }}></span>
            <span>自动任务</span>
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{ backgroundColor: '#f5222d' }}></span>
            <span>结束节点</span>
          </div>
        </div>
      </div>

      {showStartModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>启动流程 - 填写表单</h3>
              <button className="modal-close" onClick={handleCloseModal} disabled={startingInstance}>×</button>
            </div>
            <div className="modal-body">
              {selectedForm && (
                <div className="form-preview-section">
                  <h4 style={{ marginBottom: '16px', color: '#1890ff' }}>
                    📝 {selectedForm.name}
                  </h4>
                  {selectedForm.description && (
                    <p style={{ color: '#666', marginBottom: '16px' }}>
                      {selectedForm.description}
                    </p>
                  )}
                  <div className="form-fields-container" style={{
                    maxHeight: '400px',
                    overflowY: 'auto',
                    padding: '16px',
                    backgroundColor: '#fafafa',
                    borderRadius: '8px',
                    border: '1px solid #e8e8e8'
                  }}>
                    <div className="form-render-grid" style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(24, 1fr)',
                      gap: '16px'
                    }}>
                      {selectedForm.fields?.map(field => (
                        <div
                          key={field.id}
                          style={{
                            gridColumn: `span ${field.span || 24}`,
                            marginBottom: '16px'
                          }}
                          className="form-item"
                        >
                          <label style={{
                            display: 'block',
                            marginBottom: '8px',
                            fontWeight: '500',
                            color: '#333'
                          }}>
                            {field.label}
                            {field.required && <span style={{ color: '#f5222d', marginLeft: '4px' }}>*</span>}
                          </label>
                          <FieldRenderer
                            field={field}
                            value={formData[field.id]}
                            onChange={handleFormFieldChange}
                            errors={formErrors}
                            disabled={startingInstance}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer" style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '12px',
              padding: '16px 20px',
              borderTop: '1px solid #e8e8e8'
            }}>
              <button
                className="btn btn-default"
                onClick={handleCloseModal}
                disabled={startingInstance}
              >
                取消
              </button>
              <button
                className="btn btn-primary"
                onClick={handleConfirmStart}
                disabled={startingInstance}
              >
                {startingInstance ? '启动中...' : '确定启动'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default WorkflowPreview;
