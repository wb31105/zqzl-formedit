import { NODE_TYPES } from '../services/workflowApi';

function NodeLibrary({ onDragStart }) {
  const handleDragStart = (e, nodeType) => {
    e.dataTransfer.setData('nodeType', nodeType.type);
    e.dataTransfer.setData('nodeName', nodeType.name);
    onDragStart && onDragStart(nodeType);
  };

  return (
    <div className="node-library">
      <h3>节点库</h3>
      <div className="node-list">
        {NODE_TYPES.map((nodeType) => (
          <div
            key={nodeType.type}
            className="node-library-item"
            draggable={nodeType.canDelete}
            onDragStart={(e) => handleDragStart(e, nodeType)}
            style={{
              borderLeftColor: nodeType.color,
              cursor: nodeType.canDelete ? 'grab' : 'not-allowed',
              opacity: nodeType.canDelete ? 1 : 0.7,
            }}
          >
            <span
              className="node-icon"
              style={{ backgroundColor: nodeType.color }}
            >
              {nodeType.icon}
            </span>
            <span className="node-name">{nodeType.name}</span>
            {!nodeType.canDelete && (
              <span className="node-hint">画布内自动添加</span>
            )}
          </div>
        ))}
      </div>
      <div className="node-library-tips">
        <p><strong>使用提示：</strong></p>
        <p>1. 从左侧拖拽节点到画布</p>
        <p>2. 点击节点选中，可在右侧编辑属性</p>
        <p>3. 点击节点右侧圆点拖拽到另一节点创建连线</p>
        <p>4. 按 Delete 键删除选中的节点或连线</p>
      </div>
    </div>
  );
}

export default NodeLibrary;
