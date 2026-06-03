import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowDefinitionApi, workflowInstanceApi } from '../services/workflowApi';

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
  const navigate = useNavigate();

  useEffect(() => {
    if (activeTab === 'definitions') {
      loadWorkflows();
    } else {
      loadInstances();
    }
  }, [page, instancesPage, activeTab]);

  const loadWorkflows = async () => {
    setLoading(true);
    try {
      const response = await workflowDefinitionApi.getAllDefinitions({ page, size: pageSize });
      const data = response.data;
      setWorkflows(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载流程列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadInstances = async () => {
    setLoading(true);
    try {
      const response = await workflowInstanceApi.getAllInstances({ page: instancesPage, size: pageSize });
      const data = response.data;
      setInstances(data.content || []);
      setInstancesTotalPages(data.totalPages || 0);
      setInstancesTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载实例列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    if (window.confirm('确定要删除这个流程吗？')) {
      try {
        await workflowDefinitionApi.deleteDefinition(id);
        loadWorkflows();
      } catch (error) {
        console.error('删除流程失败:', error);
        alert('删除失败: ' + (error.response?.data?.error || error.message));
      }
    }
  };

  const handleDeleteInstance = async (id, e) => {
    e.stopPropagation();
    if (window.confirm('确定要删除这个流程实例吗？')) {
      try {
        await workflowInstanceApi.deleteInstance(id);
        loadInstances();
      } catch (error) {
        console.error('删除实例失败:', error);
        alert('删除失败: ' + (error.response?.data?.error || error.message));
      }
    }
  };

  const handleStartInstance = async (definitionId, e) => {
    e.stopPropagation();
    try {
      const response = await workflowInstanceApi.startInstance(definitionId);
      const instanceId = response.data.id;
      navigate(`/workflow/instance/${instanceId}`);
    } catch (error) {
      console.error('启动流程失败:', error);
      alert('启动失败: ' + (error.response?.data?.error || error.message));
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

      {(activeTab === 'definitions' ? workflows.length === 0 : instances.length === 0) && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          {activeTab === 'definitions'
            ? '暂无流程定义，点击"新建流程"创建'
            : '暂无流程实例，在流程定义中点击"启动"创建'}
        </div>
      )}

      {renderPagination()}
    </div>
  );
}

export default WorkflowList;
