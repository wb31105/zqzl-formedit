import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkflowCanvas from '../components/WorkflowCanvas';
import {
  workflowDefinitionApi,
  workflowInstanceApi,
  getNodeTypeConfig,
} from '../services/workflowApi';

function WorkflowInstance() {
  const params = useParams();
  const navigate = useNavigate();
  const [instance, setInstance] = useState(null);
  const [definition, setDefinition] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [comment, setComment] = useState('');

  const id = params.id;
  const definitionId = params.definitionId;
  const isNewInstance = !!definitionId;

  useEffect(() => {
    if (isNewInstance && definitionId) {
      startNewInstance();
    } else if (id && !isNewInstance) {
      loadInstance();
    }
  }, [id, definitionId]);

  const startNewInstance = async () => {
    setLoading(true);
    try {
      const response = await workflowInstanceApi.startInstance(definitionId);
      setInstance(response.data);
      await loadDefinition(definitionId);
    } catch (error) {
      console.error('启动流程失败:', error);
      alert('启动失败: ' + (error.response?.data?.error || error.message));
      navigate('/workflows');
    } finally {
      setLoading(false);
    }
  };

  const loadInstance = async () => {
    setLoading(true);
    try {
      const response = await workflowInstanceApi.getInstanceById(id);
      setInstance(response.data);
      await loadDefinition(response.data.definitionId);
    } catch (error) {
      console.error('加载实例失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadDefinition = async (defId) => {
    try {
      const response = await workflowDefinitionApi.getDefinitionById(defId);
      setDefinition(response.data);
    } catch (error) {
      console.error('加载流程定义失败:', error);
    }
  };

  const getCurrentNode = () => {
    if (!instance?.pendingTasks?.length || !definition?.nodes) return null;
    const pendingTask = instance.pendingTasks[0];
    return definition.nodes.find(n => n.id === pendingTask.nodeId);
  };

  const getButtonConfig = () => {
    const currentNode = getCurrentNode();
    const props = currentNode?.properties || {};
    const actionType = props.actionType || 'approval';

    switch (actionType) {
      case 'submit':
        return {
          showApprove: true,
          showReject: false,
          approveText: props.approveText || '提交',
          rejectText: props.rejectText || '拒绝',
          commentLabel: props.commentLabel || '申请理由',
          confirmApprove: '确定要提交吗？',
          confirmReject: '确定要拒绝吗？',
        };
      case 'review':
        return {
          showApprove: true,
          showReject: true,
          approveText: props.approveText || '同意',
          rejectText: props.rejectText || '退回',
          commentLabel: props.commentLabel || '审核意见',
          confirmApprove: '确定要同意吗？',
          confirmReject: '确定要退回吗？',
        };
      case 'custom':
        return {
          showApprove: true,
          showReject: true,
          approveText: props.approveText || '批准',
          rejectText: props.rejectText || '拒绝',
          commentLabel: props.commentLabel || '处理意见',
          confirmApprove: `确定要${props.approveText || '批准'}吗？`,
          confirmReject: `确定要${props.rejectText || '拒绝'}吗？`,
        };
      case 'approval':
      default:
        return {
          showApprove: true,
          showReject: true,
          approveText: props.approveText || '批准',
          rejectText: props.rejectText || '拒绝',
          commentLabel: props.commentLabel || '处理意见',
          confirmApprove: '确定要批准吗？',
          confirmReject: '确定要拒绝吗？',
        };
    }
  };

  const handleTaskAction = async (action) => {
    if (!instance?.pendingTasks?.length) return;

    const pendingTask = instance.pendingTasks[0];
    const buttonConfig = getButtonConfig();
    const confirmMsg = action === 'approve' ? buttonConfig.confirmApprove : buttonConfig.confirmReject;

    if (!window.confirm(confirmMsg)) {
      return;
    }

    setProcessing(true);
    try {
      const response = await workflowInstanceApi.completeTask(instance.id, {
        taskId: pendingTask.id,
        action,
        comment,
        assignee: '当前用户',
      });
      setInstance(response.data);
      setComment('');
    } catch (error) {
      console.error('处理任务失败:', error);
      alert('处理失败: ' + (error.response?.data?.error || error.message));
    } finally {
      setProcessing(false);
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

  const getHighlightNodeIds = () => {
    if (!instance?.logs) return [];
    return instance.logs.map((log) => log.nodeId);
  };

  if (loading) {
    return <div className="workflow-instance">加载中...</div>;
  }

  if (!instance || !definition) {
    return <div className="workflow-instance">数据加载失败</div>;
  }

  const pendingTask = instance.pendingTasks?.[0];
  const buttonConfig = pendingTask ? getButtonConfig() : null;

  return (
    <div className="workflow-instance">
      <div className="instance-header">
        <button className="btn btn-default" onClick={() => navigate('/workflows')}>
          ← 返回列表
        </button>
        <h2>
          {instance.definitionName}
          <span style={{ marginLeft: '10px' }}>{getStatusBadge(instance.status)}</span>
        </h2>
        <div className="instance-info">
          <span>实例ID: {instance.id}</span>
          <span>开始时间: {formatDate(instance.startedAt)}</span>
          {instance.endedAt && <span>结束时间: {formatDate(instance.endedAt)}</span>}
        </div>
      </div>

      <div className="instance-body">
        <div className="instance-canvas-section">
          <h3>流程图</h3>
          <div className="instance-canvas">
            <WorkflowCanvas
              nodes={definition.nodes || []}
              edges={definition.edges || []}
              selectedNodeId={instance.currentNodeId}
              selectedEdgeId={null}
              onSelectNode={() => {}}
              onSelectEdge={() => {}}
              onAddNode={() => {}}
              onUpdateNode={() => {}}
              onAddEdge={() => {}}
              onCanvasClick={() => {}}
              highlightNodeIds={getHighlightNodeIds()}
              readOnly={true}
            />
          </div>
        </div>

        <div className="instance-side-panel">
          {pendingTask && instance.status === 'RUNNING' && buttonConfig && (
            <div className="task-panel">
              <h3>待办任务</h3>
              <div className="task-card">
                <div className="task-header">
                  <span className="task-name">{pendingTask.nodeName}</span>
                  <span className="badge badge-pending">待处理</span>
                </div>
                <div className="task-info">
                  <div>创建时间: {formatDate(pendingTask.createdAt)}</div>
                </div>
                <div className="task-comment">
                  <label>{buttonConfig.commentLabel}：</label>
                  <textarea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder={`请输入${buttonConfig.commentLabel}（可选）`}
                    rows={3}
                  />
                </div>
                <div className="task-actions">
                  {buttonConfig.showApprove && (
                    <button
                      className="btn btn-success"
                      onClick={() => handleTaskAction('approve')}
                      disabled={processing}
                    >
                      {processing ? '处理中...' : `✓ ${buttonConfig.approveText}`}
                    </button>
                  )}
                  {buttonConfig.showReject && (
                    <button
                      className="btn btn-danger"
                      onClick={() => handleTaskAction('reject')}
                      disabled={processing}
                    >
                      {processing ? '处理中...' : `✕ ${buttonConfig.rejectText}`}
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}

          <div className="history-panel">
            <h3>执行历史</h3>
            <div className="history-list">
              {instance.logs?.length === 0 && (
                <div className="history-empty">暂无执行记录</div>
              )}
              {instance.logs?.map((log, index) => {
                const nodeConfig = getNodeTypeConfig(log.nodeType);
                return (
                  <div key={log.id || index} className="history-item">
                    <div
                      className="history-icon"
                      style={{ backgroundColor: nodeConfig.color }}
                    >
                      {nodeConfig.icon}
                    </div>
                    <div className="history-content">
                      <div className="history-title">
                        <span className="history-node-name">{log.nodeName}</span>
                        <span className="history-action">{log.action}</span>
                      </div>
                      {log.comment && (
                        <div className="history-comment">{log.comment}</div>
                      )}
                      <div className="history-time">{formatDate(log.createdAt)}</div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="tasks-panel">
            <h3>所有任务</h3>
            <div className="tasks-list">
              {instance.tasks?.length === 0 && (
                <div className="tasks-empty">暂无任务</div>
              )}
              {instance.tasks?.map((task) => (
                <div key={task.id} className="task-item">
                  <div className="task-item-header">
                    <span>{task.nodeName}</span>
                    <span
                      className={`badge ${
                        task.status === 'COMPLETED' ? 'badge-completed' : 'badge-pending'
                      }`}
                    >
                      {task.status === 'COMPLETED' ? '已完成' : '待处理'}
                    </span>
                  </div>
                  <div className="task-item-meta">
                    <div>创建: {formatDate(task.createdAt)}</div>
                    {task.completedAt && <div>完成: {formatDate(task.completedAt)}</div>}
                    {task.assignee && <div>处理人: {task.assignee}</div>}
                  </div>
                  {task.comment && (
                    <div className="task-item-comment">意见: {task.comment}</div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default WorkflowInstance;
