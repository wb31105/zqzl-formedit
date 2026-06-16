import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowDefinitionApi, workflowInstanceApi } from '../services/workflowApi';
import { formApi, getErrorMessage } from '../services/api';
import FieldRenderer from '../components/FieldRenderer';
import PageError from '../components/PageError';
import { useNotification } from '../context/NotificationContext';
import usePaginatedList from '../hooks/usePaginatedList';
import { validateFormFields } from '../utils/formValidation';
import { APPROVAL_ACTION, APPROVAL_ACTION_LABEL, INSTANCE_STATUS, STATUS_LABEL } from '../constants/workflowConstants';

function WorkflowList() {
  const [activeTab, setActiveTab] = useState('definitions');
  const navigate = useNavigate();
  const { showConfirm, setAlert, clearAlert } = useNotification();

  const definitionList = usePaginatedList({ fetchFunction: (params) => workflowDefinitionApi.getAllDefinitions(params), pageSize: 10 });
  const instanceList = usePaginatedList({ fetchFunction: (params) => workflowInstanceApi.getAllInstances(params), pageSize: 10 });

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const [showStartModal, setShowStartModal] = useState(false);
  const [startingDefinitionId, setStartingDefinitionId] = useState(null);
  const [selectedFormId, setSelectedFormId] = useState(null);
  const [selectedForm, setSelectedForm] = useState(null);
  const [formData, setFormData] = useState({});
  const [formErrors, setFormErrors] = useState({});
  const [startingInstance, setStartingInstance] = useState(false);

  useEffect(() => {
    if (activeTab === 'definitions') {
      definitionList.reload();
    } else {
      instanceList.reload();
    }
  }, [activeTab]);

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    await definitionList.deleteItem(workflowDefinitionApi.deleteDefinition, id, '确定要删除这个流程吗？删除后无法恢复。', '流程删除成功');
  };

  const handleDeleteInstance = async (id, e) => {
    e.stopPropagation();
    await instanceList.deleteItem(workflowInstanceApi.deleteInstance, id, '确定要删除这个流程实例吗？删除后无法恢复。', '实例删除成功');
  };

  const handleStartInstance = async (definitionId, e) => {
    e.stopPropagation();
    setStartingDefinitionId(definitionId);
    setSelectedFormId(null);
    setSelectedForm(null);
    setFormData({});
    setFormErrors({});

    const workflow = definitionList.items.find(w => w.id === definitionId);
    const boundFormId = workflow?.formId;
    const boundFormName = workflow?.formName;

    if (boundFormId) {
      try {
        const response = await formApi.getFormById(boundFormId);
        setSelectedFormId(boundFormId);
        setSelectedForm(response.data);
        setShowStartModal(true);
      } catch (error) {
        console.error('加载绑定表单失败:', error);
        setAlert('error', '加载绑定表单失败: ' + getErrorMessage(error));
      }
    } else {
      const confirmed = await showConfirm('此流程未绑定表单，是否直接启动？', '启动确认');
      if (confirmed) {
        doStartInstance(definitionId, null, null);
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

  const doStartInstance = async (definitionId, formId, data) => {
    setStartingInstance(true);
    try {
      let response;
      if (formId) {
        response = await workflowInstanceApi.startInstanceWithForm(definitionId, {
          formId,
          formData: data
        });
      } else {
        response = await workflowInstanceApi.startInstance(definitionId);
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
    if (selectedFormId && selectedForm) {
      if (!validateForm()) {
        return;
      }
    }
    doStartInstance(startingDefinitionId, selectedFormId || null, 
      selectedFormId ? formData : null);
  };

  const handleCloseModal = () => {
    if (!startingInstance) {
      setShowStartModal(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      RUNNING: { label: STATUS_LABEL.RUNNING, className: 'badge-running' },
      COMPLETED: { label: STATUS_LABEL.COMPLETED, className: 'badge-completed' },
      PENDING: { label: STATUS_LABEL.PENDING, className: 'badge-pending' },
    };
    const config = statusMap[status] || { label: STATUS_LABEL[status] || status, className: 'badge-default' };
    return <span className={`badge ${config.className}`}>{config.label}</span>;
  };

  const renderDefinitions = () => (
    <div className="workflow-list">
      {definitionList.items.map((workflow) => (
        <div key={workflow.id} className="workflow-card">
          <h3>{workflow.name}</h3>
          <p className="workflow-desc">{workflow.description || '暂无描述'}</p>
          <div className="workflow-meta">
            <div>创建时间: {formatDate(workflow.createdAt)}</div>
            <div>更新时间: {formatDate(workflow.updatedAt)}</div>
          </div>
          {workflow.formName && (
            <div className="workflow-meta" style={{ marginTop: '8px' }}>
              <div style={{ color: '#1890ff' }}>
                📝 绑定表单：{workflow.formName}
              </div>
            </div>
          )}
          <div className="workflow-actions">
            <button className="btn btn-default" onClick={() => navigate(`/workflow/editor/${workflow.id}`)}>
              编辑
            </button>
            <button className="btn btn-primary" onClick={() => navigate(`/workflow/preview/${workflow.id}`)}>
              预览
            </button>
            <button className="btn btn-success" onClick={(e) => handleStartInstance(workflow.id, e)}>
              启动
            </button>
            <button className="btn btn-danger" onClick={(e) => handleDelete(workflow.id, e)}>
              删除
            </button>
          </div>
        </div>
      ))}
    </div>
  );

  const renderInstances = () => (
    <div className="workflow-list">
      {instanceList.items.map((instance) => (
        <div key={instance.id} className="workflow-card">
          <h3>
            {instance.definitionName}
            <span style={{ marginLeft: '10px' }}>{getStatusBadge(instance.status)}</span>
          </h3>
          <p className="workflow-desc">
            流程定义ID: {instance.definitionId} | 实例ID: {instance.id}
          </p>
          <div className="workflow-meta">
            <div>当前节点: {instance.currentNodeName || '-'}</div>
            <div>开始时间: {formatDate(instance.startedAt)}</div>
            {instance.endedAt && <div>结束时间: {formatDate(instance.endedAt)}</div>}
          </div>
          <div className="workflow-actions">
            <button className="btn btn-primary" onClick={() => navigate(`/workflow/instance/${instance.id}`)}>
              查看详情
            </button>
            <button className="btn btn-danger" onClick={(e) => handleDeleteInstance(instance.id, e)}>
              删除
            </button>
          </div>
        </div>
      ))}
    </div>
  );

  const currentList = activeTab === 'definitions' ? definitionList : instanceList;

  if (currentList.loading && (activeTab === 'definitions' ? definitionList.items.length === 0 : instanceList.items.length === 0)) {
    return <div className="workflow-list-container">加载中...</div>;
  }

  if (currentList.loadError) {
    return (
      <div className="workflow-list-container">
        <PageError
          title="加载失败"
          message={currentList.loadError}
          onRetry={() => (activeTab === 'definitions' ? definitionList.reload() : instanceList.reload())}
          backTo="/"
          backText="返回首页"
        />
      </div>
    );
  }

  return (
    <div className="workflow-list-container">
      <div className="page-header">
        <h1>工作流管理</h1>
        <button className="btn btn-primary" onClick={() => navigate('/workflow/editor/new')}>
          + 新建流程
        </button>
      </div>

      <div className="tabs">
        <button
          className={`tab-btn ${activeTab === 'definitions' ? 'active' : ''}`}
          onClick={() => setActiveTab('definitions')}
        >
          流程定义
        </button>
        <button
          className={`tab-btn ${activeTab === 'instances' ? 'active' : ''}`}
          onClick={() => setActiveTab('instances')}
        >
          流程实例
        </button>
      </div>

      {activeTab === 'definitions' ? renderDefinitions() : renderInstances()}

      {!currentList.loadError && (activeTab === 'definitions' ? definitionList.items.length === 0 : instanceList.items.length === 0) && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          {activeTab === 'definitions'
            ? '暂无流程定义，点击"新建流程"创建'
            : '暂无流程实例，在流程定义中点击"启动"创建'}
        </div>
      )}

      {activeTab === 'definitions' ? definitionList.renderPagination() : instanceList.renderPagination()}

      {showStartModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>启动流程</h3>
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

              {!selectedForm && (
                <div style={{ 
                  textAlign: 'center', 
                  padding: '40px 20px', 
                  color: '#999',
                  backgroundColor: '#fafafa',
                  borderRadius: '8px',
                  border: '1px dashed #d9d9d9'
                }}>
                  请填写表单后再启动流程。
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

export default WorkflowList;
