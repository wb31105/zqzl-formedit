import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkflowCanvas from '../components/WorkflowCanvas';
import { workflowDefinitionApi } from '../services/workflowApi';
import { useNotification } from '../context/NotificationContext';

function WorkflowPreview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workflow, setWorkflow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const { showError } = useNotification();

  useEffect(() => {
    loadWorkflow();
  }, [id]);

  const loadWorkflow = async () => {
    setLoading(true);
    setLoadError('');
    try {
      const response = await workflowDefinitionApi.getDefinitionById(id);
      setWorkflow(response.data);
    } catch (error) {
      console.error('加载流程失败:', error);
      let msg;
      if (error.response?.status === 404) {
        msg = '流程不存在或已被删除';
      } else {
        msg = '加载流程失败: ' + (error.response?.data?.error || error.message || '网络错误');
      }
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="workflow-preview">加载中...</div>;
  }

  if (loadError || !workflow) {
    return (
      <div className="workflow-preview">
        <div style={{
          textAlign: 'center',
          padding: '80px 20px',
          color: '#666'
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
          <h2 style={{ color: '#cf1322', marginBottom: '8px' }}>
            {loadError || '流程不存在'}
          </h2>
          <p style={{ marginBottom: '24px', color: '#999' }}>
            请检查流程ID是否正确，或返回列表选择其他流程
          </p>
          <button className="btn btn-primary" onClick={() => navigate('/workflows')}>
            返回流程列表
          </button>
        </div>
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
          <button className="btn btn-success" onClick={() => navigate(`/workflow/instance/new/${id}`)}>
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
    </div>
  );
}

export default WorkflowPreview;
