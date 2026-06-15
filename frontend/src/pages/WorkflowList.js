import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowDefinitionApi, workflowInstanceApi } from '../services/workflowApi';
import { formApi } from '../services/api';
import FieldRenderer from '../components/FieldRenderer';
import PageError from '../components/PageError';
import { useNotification } from '../context/NotificationContext';

function WorkflowList() {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [activeTab, setActiveTab] = useState('definitions');
  const [instances, setInstances] = useState([]);
  const [instancesPage, setInstancesPage] = useState(0);
  const [instancesTotalPages, setInstancesTotalPages] = useState(0);
  const [instancesTotalElements, setInstancesTotalElements] = useState(0);
  const [loadError, setLoadError] = useState('');
  const navigate = useNavigate();
  const { showConfirm, setAlert, clearAlert } = useNotification();

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
      loadWorkflows();
    } else {
      loadInstances();
    }
  }, [page, instancesPage, activeTab]);

  const loadWorkflows = async () => {
    setLoading(true);
    setLoadError('');
    try {
      const response = await workflowDefinitionApi.getAllDefinitions({ page, size: pageSize });
      const data = response.data;
      setWorkflows(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载流程列表失败:', error);
      const msg = '加载流程列表失败: ' + (error.response?.data?.error || error.message || '网络错误');
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const loadInstances = async () => {
    setLoading(true);
    setLoadError('');
    try {
      const response = await workflowInstanceApi.getAllInstances({ page: instancesPage, size: pageSize });
      const data = response.data;
      setInstances(data.content || []);
      setInstancesTotalPages(data.totalPages || 0);
      setInstancesTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载实例列表失败:', error);
      const msg = '加载实例列表失败: ' + (error.response?.data?.error || error.message || '网络错误');
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    const confirmed = await showConfirm('确定要删除这个流程吗？删除后无法恢复。', '删除流程');
    if (confirmed) {
      try {
        await workflowDefinitionApi.deleteDefinition(id);
        setAlert('success', '流程删除成功', 3000);
        loadWorkflows();
      } catch (error) {
        console.error('删除流程失败:', error);
        setAlert('error', '删除失败: ' + (error.response?.data?.error || error.message));
      }
    }
  };

  const handleDeleteInstance = async (id, e) => {
    e.stopPropagation();
    const confirmed = await showConfirm('确定要删除这个流程实例吗？删除后无法恢复。', '删除实例');
    if (confirmed) {
      try {
        await workflowInstanceApi.deleteInstance(id);
        setAlert('success', '实例删除成功', 3000);
        loadInstances();
      } catch (error) {
        console.error('删除实例失败:', error);
        setAlert('error', '删除失败: ' + (error.response?.data?.error || error.message));
      }
    }
  };

  const handleStartInstance = async (definitionId, e) => {
    e.stopPropagation();
    setStartingDefinitionId(definitionId);
    setSelectedFormId(null);
    setSelectedForm(null);
    setFormData({});
    setFormErrors({});

    const workflow = workflows.find(w => w.id === definitionId);
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
        setAlert('error', '加载绑定表单失败: ' + (error.response?.data?.error || error.message));
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
    const errors = {};
    selectedForm.fields?.forEach(field => {
      const value = formData[field.id];
      const stringValue = value ? String(value).trim() : '';
      const isEmpty = value === null || value === undefined || value === '' || 
        (Array.isArray(value) && value.length === 0) || stringValue === '';

      if (field.required && isEmpty) {
        errors[field.id] = `${field.label}不能为空`;
        return;
      }

      if (isEmpty) return;

      const isTextLike = ['text', 'textarea', 'email', 'number'].includes(field.type);

      if (isTextLike && field.minLength && stringValue.length < field.minLength) {
        errors[field.id] = `${field.label}最少需要${field.minLength}个字符`;
      }

      if (isTextLike && field.maxLength && stringValue.length > field.maxLength) {
        errors[field.id] = `${field.label}最多允许${field.maxLength}个字符`;
      }

      if (field.pattern && field.pattern.trim() && stringValue) {
        try {
          const regex = new RegExp(field.pattern);
          if (!regex.test(stringValue)) {
            errors[field.id] = field.patternMessage || `${field.label}格式不正确`;
          }
        } catch (e) {
          console.error('正则表达式错误:', e);
        }
      }
    });
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
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
      setAlert('error', '启动失败: ' + (error.response?.data?.error || error.message));
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
      RUNNING: { label: '运行中', className: 'badge-running' },
      COMPLETED: { label: '已完成', className: 'badge-completed' },
      PENDING: { label: '等待中', className: 'badge-pending' },
    };
    const config = statusMap[status] || { label: status, className: 'badge-default' };
    return <span className={`badge ${config.className}`}>{config.label}</span>;
  };

  const renderDefinitions = () => (
    <div className="workflow-list">
      {workflows.map((workflow) => (
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
      {instances.map((instance) => (
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

  const renderPagination = () => {
    const currentPage = activeTab === 'definitions' ? page : instancesPage;
    const total = activeTab === 'definitions' ? totalPages : instancesTotalPages;
    const totalEl = activeTab === 'definitions' ? totalElements : instancesTotalElements;
    const setCurrentPage = activeTab === 'definitions' ? setPage : setInstancesPage;

    if (total <= 1) return null;

    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(total, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    return (
      <div className="pagination">
        <button
          className="btn btn-default btn-page"
          disabled={currentPage === 0}
          onClick={() => setCurrentPage(0)}
        >
          首页
        </button>
        <button
          className="btn btn-default btn-page"
          disabled={currentPage === 0}
          onClick={() => setCurrentPage(currentPage - 1)}
        >
          上一页
        </button>
        {pages.map((p) => (
          <button
            key={p}
            className={`btn btn-page ${p === currentPage ? 'btn-primary' : 'btn-default'}`}
            onClick={() => setCurrentPage(p)}
          >
            {p + 1}
          </button>
        ))}
        <button
          className="btn btn-default btn-page"
          disabled={currentPage >= total - 1}
          onClick={() => setCurrentPage(currentPage + 1)}
        >
          下一页
        </button>
        <button
          className="btn btn-default btn-page"
          disabled={currentPage >= total - 1}
          onClick={() => setCurrentPage(total - 1)}
        >
          末页
        </button>
        <span className="pagination-info">
          共 {totalEl} 条，第 {currentPage + 1}/{total} 页
        </span>
      </div>
    );
  };

  if (loading && (activeTab === 'definitions' ? workflows.length === 0 : instances.length === 0)) {
    return <div className="workflow-list-container">加载中...</div>;
  }

  if (loadError) {
    return (
      <div className="workflow-list-container">
        <PageError
          title="加载失败"
          message={loadError}
          onRetry={() => activeTab === 'definitions' ? loadWorkflows() : loadInstances()}
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

      {!loadError && (activeTab === 'definitions' ? workflows.length === 0 : instances.length === 0) && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          {activeTab === 'definitions'
            ? '暂无流程定义，点击"新建流程"创建'
            : '暂无流程实例，在流程定义中点击"启动"创建'}
        </div>
      )}

      {renderPagination()}

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
