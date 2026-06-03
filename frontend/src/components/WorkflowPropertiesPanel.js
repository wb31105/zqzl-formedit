import { useState, useEffect } from 'react';
import { getNodeTypeConfig } from '../services/workflowApi';

function WorkflowPropertiesPanel({ selectedNode, selectedEdge, nodes, edges, onUpdateNode, onUpdateEdge, onDeleteNode, onDeleteEdge }) {
  const [nodeName, setNodeName] = useState('');
  const [edgeLabel, setEdgeLabel] = useState('');
  const [properties, setProperties] = useState({});

  useEffect(() => {
    if (selectedNode) {
      setNodeName(selectedNode.name || '');
      setProperties(selectedNode.properties || {});
    }
  }, [selectedNode]);

  useEffect(() => {
    if (selectedEdge) {
      setEdgeLabel(selectedEdge.label || '');
    }
  }, [selectedEdge]);

  if (!selectedNode && !selectedEdge) {
    return (
      <div className="properties-panel">
        <h3>属性面板</h3>
        <div className="properties-empty">
          <p>选择节点或连线以编辑属性</p>
        </div>
      </div>
    );
  }

  if (selectedEdge) {
    const sourceNode = nodes.find(n => n.id === selectedEdge.source);
    const targetNode = nodes.find(n => n.id === selectedEdge.target);

    return (
      <div className="properties-panel">
        <h3>连线属性</h3>
        <div className="properties-section">
          <label>起点</label>
          <div className="properties-value">{sourceNode?.name || selectedEdge.source}</div>
        </div>
        <div className="properties-section">
          <label>终点</label>
          <div className="properties-value">{targetNode?.name || selectedEdge.target}</div>
        </div>
        <div className="properties-section">
          <label>标签（可选）</label>
          <input
            type="text"
            value={edgeLabel}
            onChange={(e) => setEdgeLabel(e.target.value)}
            onBlur={() => onUpdateEdge({ ...selectedEdge, label: edgeLabel })}
            placeholder="如：是/否"
          />
        </div>
        <div className="properties-actions">
          <button className="btn btn-danger" onClick={() => onDeleteEdge(selectedEdge.id)}>
            删除连线
          </button>
        </div>
      </div>
    );
  }

  const nodeConfig = getNodeTypeConfig(selectedNode.type);
  const canDelete = nodeConfig.canDelete;

  const handleNameChange = (e) => {
    const newName = e.target.value;
    setNodeName(newName);
  };

  const handleNameBlur = () => {
    if (nodeName !== selectedNode.name) {
      onUpdateNode({ ...selectedNode, name: nodeName });
    }
  };

  const handlePropertyChange = (key, value) => {
    const newProperties = { ...properties, [key]: value };
    setProperties(newProperties);
    onUpdateNode({ ...selectedNode, properties: newProperties });
  };

  return (
    <div className="properties-panel">
      <h3>节点属性</h3>
      <div className="properties-section">
        <label>节点类型</label>
        <div className="properties-value" style={{ color: nodeConfig.color }}>
          <span style={{ marginRight: '8px' }}>{nodeConfig.icon}</span>
          {nodeConfig.name}
        </div>
      </div>
      <div className="properties-section">
        <label>节点名称</label>
        <input
          type="text"
          value={nodeName}
          onChange={handleNameChange}
          onBlur={handleNameBlur}
          placeholder="请输入节点名称"
        />
      </div>

      {selectedNode.type === 'approval' && (
        <>
          <div className="properties-section">
            <label>审批人</label>
            <input
              type="text"
              value={properties.approver || ''}
              onChange={(e) => handlePropertyChange('approver', e.target.value)}
              placeholder="请输入审批人"
            />
          </div>
          <div className="properties-section">
            <label>操作类型</label>
            <select
              value={properties.actionType || 'approval'}
              onChange={(e) => handlePropertyChange('actionType', e.target.value)}
            >
              <option value="approval">审批（批准/拒绝）</option>
              <option value="submit">提交申请（仅提交）</option>
              <option value="review">审核（同意/退回）</option>
              <option value="custom">自定义</option>
            </select>
          </div>
          {properties.actionType === 'custom' && (
            <>
              <div className="properties-section">
                <label>按钮1文本（批准）</label>
                <input
                  type="text"
                  value={properties.approveText || '批准'}
                  onChange={(e) => handlePropertyChange('approveText', e.target.value)}
                  placeholder="如：同意、通过"
                />
              </div>
              <div className="properties-section">
                <label>按钮2文本（拒绝）</label>
                <input
                  type="text"
                  value={properties.rejectText || '拒绝'}
                  onChange={(e) => handlePropertyChange('rejectText', e.target.value)}
                  placeholder="如：退回、驳回"
                />
              </div>
            </>
          )}
          <div className="properties-section">
            <label>意见框标题</label>
            <input
              type="text"
              value={properties.commentLabel || ''}
              onChange={(e) => handlePropertyChange('commentLabel', e.target.value)}
              placeholder="如：审批意见、请假理由（留空使用默认）"
            />
          </div>
          <div className="properties-section">
            <label>审批说明</label>
            <textarea
              value={properties.description || ''}
              onChange={(e) => handlePropertyChange('description', e.target.value)}
              placeholder="请输入审批说明"
              rows={3}
            />
          </div>
        </>
      )}

      {selectedNode.type === 'countersign' && (
        <>
          <div className="properties-section">
            <label>审批人（多个，用逗号分隔）</label>
            <input
              type="text"
              value={properties.approvers || ''}
              onChange={(e) => handlePropertyChange('approvers', e.target.value)}
              placeholder="如：张三,李四,王五"
            />
          </div>
          <div className="properties-section">
            <label>会签方式</label>
            <select
              value={properties.countersignType || 'all'}
              onChange={(e) => handlePropertyChange('countersignType', e.target.value)}
            >
              <option value="all">全部同意才通过</option>
              <option value="veto">一票否决</option>
              <option value="majority">过半通过</option>
            </select>
          </div>
          <div className="properties-section">
            <label>操作类型</label>
            <select
              value={properties.actionType || 'approval'}
              onChange={(e) => handlePropertyChange('actionType', e.target.value)}
            >
              <option value="approval">审批（批准/拒绝）</option>
              <option value="review">审核（同意/退回）</option>
              <option value="custom">自定义</option>
            </select>
          </div>
          {properties.actionType === 'custom' && (
            <>
              <div className="properties-section">
                <label>按钮1文本（批准）</label>
                <input
                  type="text"
                  value={properties.approveText || '批准'}
                  onChange={(e) => handlePropertyChange('approveText', e.target.value)}
                  placeholder="如：同意、通过"
                />
              </div>
              <div className="properties-section">
                <label>按钮2文本（拒绝）</label>
                <input
                  type="text"
                  value={properties.rejectText || '拒绝'}
                  onChange={(e) => handlePropertyChange('rejectText', e.target.value)}
                  placeholder="如：退回、驳回"
                />
              </div>
            </>
          )}
          <div className="properties-section">
            <label>意见框标题</label>
            <input
              type="text"
              value={properties.commentLabel || ''}
              onChange={(e) => handlePropertyChange('commentLabel', e.target.value)}
              placeholder="如：会签意见（留空使用默认）"
            />
          </div>
          <div className="properties-section">
            <label>会签说明</label>
            <textarea
              value={properties.description || ''}
              onChange={(e) => handlePropertyChange('description', e.target.value)}
              placeholder="请输入会签说明"
              rows={3}
            />
          </div>
        </>
      )}

      {selectedNode.type === 'condition' && (
        <>
          <div className="properties-section">
            <label>条件表达式</label>
            <input
              type="text"
              value={properties.expression || ''}
              onChange={(e) => handlePropertyChange('expression', e.target.value)}
              placeholder="如：amount > 1000"
            />
          </div>
          <div className="properties-section">
            <label>条件说明</label>
            <textarea
              value={properties.description || ''}
              onChange={(e) => handlePropertyChange('description', e.target.value)}
              placeholder="请输入条件说明"
              rows={3}
            />
          </div>
        </>
      )}

      {selectedNode.type === 'auto' && (
        <>
          <div className="properties-section">
            <label>任务类型</label>
            <select
              value={properties.taskType || 'notification'}
              onChange={(e) => handlePropertyChange('taskType', e.target.value)}
            >
              <option value="notification">发送通知</option>
              <option value="webhook">调用 Webhook</option>
              <option value="script">执行脚本</option>
            </select>
          </div>
          <div className="properties-section">
            <label>任务配置</label>
            <textarea
              value={properties.config || ''}
              onChange={(e) => handlePropertyChange('config', e.target.value)}
              placeholder="请输入任务配置"
              rows={3}
            />
          </div>
        </>
      )}

      <div className="properties-section">
        <label>位置</label>
        <div className="properties-value">
          X: {Math.round(selectedNode.x)}, Y: {Math.round(selectedNode.y)}
        </div>
      </div>

      {canDelete && (
        <div className="properties-actions">
          <button className="btn btn-danger" onClick={() => onDeleteNode(selectedNode.id)}>
            删除节点
          </button>
        </div>
      )}
    </div>
  );
}

export default WorkflowPropertiesPanel;
