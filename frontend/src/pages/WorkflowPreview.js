import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkflowCanvas from '../components/WorkflowCanvas';
import { workflowDefinitionApi } from '../services/workflowApi';

function WorkflowPreview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [workflow, setWorkflow] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadWorkflow();
  }, [id]);

  const loadWorkflow = async () => {
    setLoading(true);
    try {
      const response = await workflowDefinitionApi.getDefinitionById(id);
      setWorkflow(response.data);
    } catch (error) {
      console.error('加载流程失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="workflow-preview">加载中...</div>;
  }

  if (!workflow) {
    return <div className="workflow-preview">流程不存在</div>;
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
