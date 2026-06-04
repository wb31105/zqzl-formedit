import { useRef, useState, useEffect } from 'react';
import { getNodeTypeConfig } from '../services/workflowApi';

function WorkflowCanvas({
  nodes,
  edges,
  selectedNodeId,
  selectedEdgeId,
  onSelectNode,
  onSelectEdge,
  onAddNode,
  onUpdateNode,
  onAddEdge,
  onCanvasClick,
  highlightNodeIds = [],
  readOnly = false,
}) {
  const canvasRef = useRef(null);
  const [draggingNode, setDraggingNode] = useState(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [connecting, setConnecting] = useState(null);
  const [tempLine, setTempLine] = useState(null);

  const NODE_WIDTH = 140;
  const NODE_HEIGHT = 60;

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (draggingNode) {
        const rect = canvasRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left - dragOffset.x;
        const y = e.clientY - rect.top - dragOffset.y;
        onUpdateNode({ ...draggingNode, x: Math.max(0, x), y: Math.max(0, y) });
      }

      if (connecting && canvasRef.current) {
        const rect = canvasRef.current.getBoundingClientRect();
        setTempLine({
          x1: connecting.x,
          y1: connecting.y,
          x2: e.clientX - rect.left,
          y2: e.clientY - rect.top,
        });
      }
    };

    const handleMouseUp = (e) => {
      if (draggingNode) {
        setDraggingNode(null);
      }

      if (connecting) {
        const targetNode = findNodeAtPosition(e.clientX, e.clientY);
        if (targetNode && targetNode.id !== connecting.nodeId) {
          const existingEdge = edges.find(
            (edge) => edge.source === connecting.nodeId && edge.target === targetNode.id
          );
          if (!existingEdge) {
            onAddEdge(connecting.nodeId, targetNode.id);
          }
        }
        setConnecting(null);
        setTempLine(null);
      }
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [draggingNode, dragOffset, connecting, edges, onUpdateNode, onAddEdge]);

  const findNodeAtPosition = (clientX, clientY) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const y = clientY - rect.top;

    return nodes.find((node) => {
      return (
        x >= node.x &&
        x <= node.x + NODE_WIDTH &&
        y >= node.y &&
        y <= node.y + NODE_HEIGHT
      );
    });
  };

  const handleDragOver = (e) => {
    if (readOnly) return;
    e.preventDefault();
  };

  const handleDrop = (e) => {
    if (readOnly) return;
    e.preventDefault();
    const nodeType = e.dataTransfer.getData('nodeType');
    const nodeName = e.dataTransfer.getData('nodeName');

    if (nodeType) {
      const rect = canvasRef.current.getBoundingClientRect();
      const x = e.clientX - rect.left - NODE_WIDTH / 2;
      const y = e.clientY - rect.top - NODE_HEIGHT / 2;
      onAddNode(nodeType, nodeName, Math.max(0, x), Math.max(0, y));
    }
  };

  const handleNodeMouseDown = (e, node) => {
    if (readOnly) return;
    e.preventDefault();
    e.stopPropagation();
    onSelectNode(node.id);

    const rect = canvasRef.current.getBoundingClientRect();
    setDragOffset({
      x: e.clientX - rect.left - node.x,
      y: e.clientY - rect.top - node.y,
    });
    setDraggingNode(node);
  };

  const handleNodeClick = (e, node) => {
    if (readOnly) return;
    e.preventDefault();
    e.stopPropagation();
    onSelectNode(node.id);
  };

  const handleConnectorMouseDown = (e, node) => {
    if (readOnly) return;
    e.stopPropagation();
    const rect = canvasRef.current.getBoundingClientRect();
    setConnecting({
      nodeId: node.id,
      x: node.x + NODE_WIDTH,
      y: node.y + NODE_HEIGHT / 2,
    });
  };

  const handleEdgeClick = (e, edgeId) => {
    if (readOnly) return;
    e.stopPropagation();
    onSelectEdge(edgeId);
  };

  const getEdgePath = (sourceNode, targetNode) => {
    const x1 = sourceNode.x + NODE_WIDTH;
    const y1 = sourceNode.y + NODE_HEIGHT / 2;
    const x2 = targetNode.x;
    const y2 = targetNode.y + NODE_HEIGHT / 2;

    const midX = (x1 + x2) / 2;

    return `M ${x1} ${y1} C ${midX} ${y1}, ${midX} ${y2}, ${x2} ${y2}`;
  };

  const getEdgeLabelPosition = (sourceNode, targetNode) => {
    return {
      x: (sourceNode.x + NODE_WIDTH + targetNode.x) / 2,
      y: (sourceNode.y + NODE_HEIGHT / 2 + targetNode.y + NODE_HEIGHT / 2) / 2 - 10,
    };
  };

  const renderNode = (node) => {
    const config = getNodeTypeConfig(node.type);
    const isSelected = selectedNodeId === node.id;
    const isHighlighted = highlightNodeIds.includes(node.id);

    return (
      <g key={node.id}>
        <foreignObject
          x={node.x}
          y={node.y}
          width={NODE_WIDTH}
          height={NODE_HEIGHT}
          style={{ overflow: 'visible', pointerEvents: 'auto' }}
        >
          <div
            className={`workflow-node ${isSelected ? 'selected' : ''} ${isHighlighted ? 'highlighted' : ''}`}
            style={{
              borderColor: config.color,
              backgroundColor: isHighlighted ? `${config.color}20` : 'white',
            }}
            onMouseDown={(e) => handleNodeMouseDown(e, node)}
            onClick={(e) => handleNodeClick(e, node)}
          >
            <div className="node-header" style={{ backgroundColor: config.color }}>
              <span className="node-icon">{config.icon}</span>
              <span className="node-type-name">{config.name}</span>
            </div>
            <div className="node-body">
              <span className="node-title">{node.name}</span>
            </div>
            {!readOnly && node.type !== 'end' && (
              <div
                className="node-connector"
                style={{ backgroundColor: config.color }}
                onMouseDown={(e) => handleConnectorMouseDown(e, node)}
                title="拖拽创建连线"
              />
            )}
          </div>
        </foreignObject>
      </g>
    );
  };

  const getBranchTypeBadge = (branchType) => {
    if (branchType === 'approve') {
      return { text: '批准', color: '#52c41a', bg: '#f6ffed' };
    } else if (branchType === 'reject') {
      return { text: '拒绝', color: '#ff4d4f', bg: '#fff1f0' };
    }
    return null;
  };

  const renderEdge = (edge) => {
    const sourceNode = nodes.find((n) => n.id === edge.source);
    const targetNode = nodes.find((n) => n.id === edge.target);

    if (!sourceNode || !targetNode) return null;

    const isSelected = selectedEdgeId === edge.id;
    const labelPos = getEdgeLabelPosition(sourceNode, targetNode);
    const badge = getBranchTypeBadge(edge.branchType);

    return (
      <g key={edge.id}>
        <path
          d={getEdgePath(sourceNode, targetNode)}
          fill="none"
          stroke={isSelected ? '#1890ff' : (badge ? badge.color : '#999')}
          strokeWidth={isSelected ? 3 : 2}
          className="workflow-edge"
          onClick={(e) => handleEdgeClick(e, edge.id)}
          markerEnd="url(#arrowhead)"
          style={{ cursor: readOnly ? 'default' : 'pointer' }}
        />
        {(edge.label || badge) && (
          <g>
            <rect
              x={labelPos.x - 35}
              y={labelPos.y - 12}
              width="70"
              height="20"
              fill={badge ? badge.bg : 'white'}
              stroke={badge ? badge.color : '#ddd'}
              rx="4"
            />
            <text
              x={labelPos.x}
              y={labelPos.y + 2}
              textAnchor="middle"
              fontSize="12"
              fill={badge ? badge.color : '#666'}
            >
              {edge.label || (badge ? badge.text : '')}
            </text>
          </g>
        )}
      </g>
    );
  };

  return (
    <div
      ref={canvasRef}
      className="workflow-canvas"
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={onCanvasClick}
    >
      <svg width="100%" height="100%" style={{ minHeight: '600px' }}>
        <defs>
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="7"
            refX="9"
            refY="3.5"
            orient="auto"
          >
            <polygon points="0 0, 10 3.5, 0 7" fill="#999" />
          </marker>
          <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
            <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#f0f0f0" strokeWidth="1" />
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />

        {edges.map(renderEdge)}

        {tempLine && (
          <path
            d={`M ${tempLine.x1} ${tempLine.y1} C ${(tempLine.x1 + tempLine.x2) / 2} ${tempLine.y1}, ${(tempLine.x1 + tempLine.x2) / 2} ${tempLine.y2}, ${tempLine.x2} ${tempLine.y2}`}
            fill="none"
            stroke="#1890ff"
            strokeWidth="2"
            strokeDasharray="5,5"
          />
        )}

        {nodes.map(renderNode)}
      </svg>
    </div>
  );
}

export default WorkflowCanvas;
