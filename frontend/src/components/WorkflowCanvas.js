import { useRef, useState, useEffect, useMemo } from 'react';
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
  showControls = true,
}) {
  const canvasRef = useRef(null);
  const svgContainerRef = useRef(null);
  const [draggingNode, setDraggingNode] = useState(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [connecting, setConnecting] = useState(null);
  const [tempLine, setTempLine] = useState(null);
  const [scale, setScale] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [isPanning, setIsPanning] = useState(false);
  const [panStart, setPanStart] = useState({ x: 0, y: 0 });

  const NODE_WIDTH = 140;
  const NODE_HEIGHT = 60;
  const PADDING = 100;

  const canvasBounds = useMemo(() => {
    if (nodes.length === 0) {
      return { minX: 0, minY: 0, maxX: 800, maxY: 600, width: 800, height: 600 };
    }
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    nodes.forEach(node => {
      minX = Math.min(minX, node.x);
      minY = Math.min(minY, node.y);
      maxX = Math.max(maxX, node.x + NODE_WIDTH);
      maxY = Math.max(maxY, node.y + NODE_HEIGHT);
    });
    minX = Math.min(0, minX - PADDING);
    minY = Math.min(0, minY - PADDING);
    maxX = maxX + PADDING;
    maxY = maxY + PADDING;
    return {
      minX,
      minY,
      maxX,
      maxY,
      width: maxX - minX,
      height: maxY - minY,
    };
  }, [nodes]);

  useEffect(() => {
    if (svgContainerRef.current && scale === 1) {
      svgContainerRef.current.scrollLeft = -canvasBounds.minX;
      svgContainerRef.current.scrollTop = -canvasBounds.minY;
    }
  }, [canvasBounds.minX, canvasBounds.minY]);

  const handleZoomIn = () => {
    setScale(prev => Math.min(prev + 0.1, 2));
  };

  const handleZoomOut = () => {
    setScale(prev => Math.max(prev - 0.1, 0.3));
  };

  const handleFitToScreen = () => {
    if (!svgContainerRef.current) return;
    const containerWidth = svgContainerRef.current.clientWidth - 40;
    const containerHeight = svgContainerRef.current.clientHeight - 40;
    const scaleX = containerWidth / canvasBounds.width;
    const scaleY = containerHeight / canvasBounds.height;
    const newScale = Math.min(scaleX, scaleY, 1);
    setScale(newScale);
  };

  const handleResetZoom = () => {
    setScale(1);
    setOffset({ x: 0, y: 0 });
  };

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (draggingNode) {
        const point = getSvgPoint(e.clientX, e.clientY);
        const x = point.x - dragOffset.x;
        const y = point.y - dragOffset.y;
        onUpdateNode({ ...draggingNode, x: Math.max(0, x), y: Math.max(0, y) });
      }

      if (connecting && canvasRef.current) {
        const point = getSvgPoint(e.clientX, e.clientY);
        setTempLine({
          x1: connecting.x,
          y1: connecting.y,
          x2: point.x,
          y2: point.y,
        });
      }

      if (isPanning && svgContainerRef.current) {
        svgContainerRef.current.scrollLeft = panStart.scrollLeft - (e.clientX - panStart.clientX);
        svgContainerRef.current.scrollTop = panStart.scrollTop - (e.clientY - panStart.clientY);
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

      if (isPanning) {
        setIsPanning(false);
      }
    };

    const handleWheel = (e) => {
      if (!svgContainerRef.current) return;
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        setScale(prev => {
          const newScale = Math.max(0.3, Math.min(2, prev + delta));
          return newScale;
        });
      }
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    const container = svgContainerRef.current;
    if (container) {
      container.addEventListener('wheel', handleWheel, { passive: false });
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      if (container) {
        container.removeEventListener('wheel', handleWheel);
      }
    };
  }, [draggingNode, dragOffset, connecting, edges, onUpdateNode, onAddEdge, isPanning, panStart, scale]);

  const getSvgPoint = (clientX, clientY) => {
    if (!svgContainerRef.current || !canvasRef.current) {
      return { x: clientX, y: clientY };
    }
    const containerRect = svgContainerRef.current.getBoundingClientRect();
    const scrollLeft = svgContainerRef.current.scrollLeft;
    const scrollTop = svgContainerRef.current.scrollTop;
    const x = (clientX - containerRect.left + scrollLeft) / scale;
    const y = (clientY - containerRect.top + scrollTop) / scale;
    return { x, y };
  };

  const findNodeAtPosition = (clientX, clientY) => {
    const point = getSvgPoint(clientX, clientY);

    return nodes.find((node) => {
      return (
        point.x >= node.x &&
        point.x <= node.x + NODE_WIDTH &&
        point.y >= node.y &&
        point.y <= node.y + NODE_HEIGHT
      );
    });
  };

  const handleCanvasMouseDown = (e) => {
    if (readOnly && e.button === 0) {
      setIsPanning(true);
      setPanStart({
        clientX: e.clientX,
        clientY: e.clientY,
        scrollLeft: svgContainerRef.current.scrollLeft,
        scrollTop: svgContainerRef.current.scrollTop,
      });
    }
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
      const point = getSvgPoint(e.clientX, e.clientY);
      const x = point.x - NODE_WIDTH / 2;
      const y = point.y - NODE_HEIGHT / 2;
      onAddNode(nodeType, nodeName, Math.max(0, x), Math.max(0, y));
    }
  };

  const handleNodeMouseDown = (e, node) => {
    if (readOnly) return;
    e.preventDefault();
    e.stopPropagation();
    onSelectNode(node.id);

    const point = getSvgPoint(e.clientX, e.clientY);
    setDragOffset({
      x: point.x - node.x,
      y: point.y - node.y,
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
    e.preventDefault();
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
    <div className="workflow-canvas-wrapper">
      {showControls && (
        <div className="canvas-controls">
          <button
            className="canvas-control-btn"
            onClick={handleZoomIn}
            title="放大"
            disabled={scale >= 2}
          >
            +
          </button>
          <div className="canvas-scale-info">
            {Math.round(scale * 100)}%
          </div>
          <button
            className="canvas-control-btn"
            onClick={handleZoomOut}
            title="缩小"
            disabled={scale <= 0.3}
          >
            −
          </button>
          <button
            className="canvas-control-btn"
            onClick={handleFitToScreen}
            title="适应窗口"
          >
            ⛶
          </button>
          <button
            className="canvas-control-btn"
            onClick={handleResetZoom}
            title="重置缩放"
          >
            ↺
          </button>
        </div>
      )}
      <div
        ref={svgContainerRef}
        className="workflow-canvas-scroll"
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onMouseDown={handleCanvasMouseDown}
        style={{ cursor: isPanning ? 'grabbing' : (readOnly ? 'grab' : 'default') }}
      >
        <div
          ref={canvasRef}
          className="workflow-canvas"
          onClick={onCanvasClick}
          style={{
            width: canvasBounds.width,
            height: canvasBounds.height,
            transform: `scale(${scale})`,
            transformOrigin: 'top left',
          }}
        >
          <svg width={canvasBounds.width} height={canvasBounds.height}>
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

            <g transform={`translate(${-canvasBounds.minX}, ${-canvasBounds.minY})`}>
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
            </g>
          </svg>
        </div>
      </div>
    </div>
  );
}

export default WorkflowCanvas;
