import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { v4 as uuidv4 } from 'uuid';
import NodeLibrary from '../components/NodeLibrary';
import WorkflowCanvas from '../components/WorkflowCanvas';
import WorkflowPropertiesPanel from '../components/WorkflowPropertiesPanel';
import FieldRenderer from '../components/FieldRenderer';
import { workflowDefinitionApi, workflowInstanceApi, getNodeTypeConfig } from '../services/workflowApi';
import { formApi, getErrorMessage } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import PageError from '../components/PageError';
import { validateFormFields } from '../utils/formValidation';

function WorkflowEditor() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNew = id === 'new';
  const hasPersistedId = !isNew && id && !isNaN(Number(id));
  const [currentDefinitionId, setCurrentDefinitionId] = useState(hasPersistedId ? id : null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [formId, setFormId] = useState(null);
  const [formName, setFormName] = useState('');
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNodeId, setSelectedNodeId] = useState(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);
  const [validationErrors, setValidationErrors] = useState([]);
  const [isNewFlag, setIsNewFlag] = useState(id === 'new');
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedEdge, setSelectedEdge] = useState(null);
  const { showConfirm, setAlert, clearAlert } = useNotification();

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const [showStartModal, setShowStartModal] = useState(false);
  const [selectedStartForm, setSelectedStartForm] = useState(null);
  const [startFormData, setStartFormData] = useState({});
  const [startFormErrors, setStartFormErrors] = useState({});
  const [startingInstance, setStartingInstance] = useState(false);

  useEffect(() => {
    if (id && id !== 'new') {
      loadWorkflow(id);
    } else {
      initEmptyWorkflow();
    }
  }, [id]);

  const canPreviewOrStart = !!currentDefinitionId;

  useEffect(() => {
    if (selectedNodeId) {
      const node = nodes.find((n) => n.id === selectedNodeId);
      setSelectedNode(node || null);
    } else {
      setSelectedNode(null);
    }
  }, [selectedNodeId, nodes]);

  useEffect(() => {
    if (selectedEdgeId) {
      const edge = edges.find((e) => e.id === selectedEdgeId);
      setSelectedEdge(edge || null);
    } else {
      setSelectedEdge(null);
    }
  }, [selectedEdgeId, edges]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Delete' || e.key === 'Backspace') {
        if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') {
          return;
        }
        if (selectedNodeId) {
          handleDeleteNode(selectedNodeId);
        } else if (selectedEdgeId) {
          handleDeleteEdge(selectedEdgeId);
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [selectedNodeId, selectedEdgeId, nodes]);

  const initEmptyWorkflow = () => {
    setName('');
    setDescription('');
    const startNode = {
      id: 'node-start',
      type: 'start',
      name: '开始',
      x: 100,
      y: 200,
      properties: {},
    };
    const endNode = {
      id: 'node-end',
      type: 'end',
      name: '结束',
      x: 500,
      y: 200,
      properties: {},
    };
    setNodes([startNode, endNode]);
    setEdges([]);
  };

  const loadWorkflow = async (workflowId) => {
    setLoading(true);
    setLoadError('');
    try {
      const response = await workflowDefinitionApi.getDefinitionById(workflowId);
      const data = response.data;
      setName(data.name || '');
      setDescription(data.description || '');
      setFormId(data.formId || null);
      setFormName(data.formName || '');
      setNodes(data.nodes || []);
      setEdges(data.edges || []);
      setCurrentDefinitionId(data.id);
      setIsNewFlag(false);
    } catch (error) {
      console.error('加载流程失败:', error);
      let msg = '加载流程失败: ';
      if (error.response?.status === 404) {
        msg = '流程不存在或已被删除';
      } else {
        msg += getErrorMessage(error, '网络错误');
      }
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleAddNode = (nodeType, nodeName, x, y) => {
    const config = getNodeTypeConfig(nodeType);
    const newNode = {
      id: `node-${uuidv4().substring(0, 8)}`,
      type: nodeType,
      name: nodeName || config.name,
      x,
      y,
      properties: {},
    };
    setNodes([...nodes, newNode]);
    setSelectedNodeId(newNode.id);
    setSelectedEdgeId(null);
  };

  const handleUpdateNode = (updatedNode) => {
    setNodes(nodes.map((n) => (n.id === updatedNode.id ? updatedNode : n)));
  };

  const handleDeleteNode = (nodeId) => {
    const node = nodes.find((n) => n.id === nodeId);
    if (!node) return;

    const config = getNodeTypeConfig(node.type);
    if (!config.canDelete) {
      setAlert('warning', '开始节点和结束节点不能删除', 3000);
      return;
    }

    setNodes(nodes.filter((n) => n.id !== nodeId));
    setEdges(edges.filter((e) => e.source !== nodeId && e.target !== nodeId));
    setSelectedNodeId(null);
  };

  const handleAddEdge = (sourceId, targetId) => {
    const newEdge = {
      id: `edge-${uuidv4().substring(0, 8)}`,
      source: sourceId,
      target: targetId,
      label: '',
    };
    setEdges([...edges, newEdge]);
  };

  const handleUpdateEdge = (updatedEdge) => {
    setEdges(edges.map((e) => (e.id === updatedEdge.id ? updatedEdge : e)));
  };

  const handleDeleteEdge = (edgeId) => {
    setEdges(edges.filter((e) => e.id !== edgeId));
    setSelectedEdgeId(null);
  };

  const handleSelectNode = (nodeId) => {
    setSelectedNodeId(nodeId);
    setSelectedEdgeId(null);
  };

  const handleSelectEdge = (edgeId) => {
    setSelectedEdgeId(edgeId);
    setSelectedNodeId(null);
  };

  const handleCanvasClick = () => {
    setSelectedNodeId(null);
    setSelectedEdgeId(null);
  };

  const handleValidate = async () => {
    setValidating(true);
    setValidationErrors([]);
    clearAlert();
    try {
      const response = await workflowDefinitionApi.validateDefinition({
        name,
        description,
        formId,
        nodes,
        edges,
      });
      const result = response.data;
      if (result.valid) {
        setAlert('success', '流程验证通过！', 3000);
      } else {
        setValidationErrors(result.errors || []);
        setAlert('warning', '流程验证未通过，请检查下方错误列表');
      }
    } catch (error) {
      console.error('验证失败:', error);
      setAlert('error', '验证失败: ' + getErrorMessage(error));
    } finally {
      setValidating(false);
    }
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setAlert('warning', '请输入流程名称');
      return;
    }

    setSaving(true);
    clearAlert();
    try {
      const data = { name, description, formId, nodes, edges };

      if (isNewFlag && !currentDefinitionId) {
        const response = await workflowDefinitionApi.createDefinition(data);
        const newId = response.data.id;
        setCurrentDefinitionId(newId);
        setIsNewFlag(false);
        setAlert('success', '保存成功！', 3000);
        navigate(`/workflow/editor/${newId}`, { replace: true });
      } else {
        const defIdToSave = currentDefinitionId || id;
        await workflowDefinitionApi.updateDefinition(defIdToSave, data);
        setAlert('success', '保存成功！', 3000);
      }
    } catch (error) {
      console.error('保存失败:', error);
      setAlert('error', '保存失败: ' + getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const handlePreview = () => {
    if (canPreviewOrStart) {
      navigate(`/workflow/preview/${currentDefinitionId}`);
    }
  };

  const handleStartInstance = async () => {
    if (canPreviewOrStart) {
      if (formId) {
        try {
          const response = await formApi.getFormById(formId);
          setSelectedStartForm(response.data);
          setStartFormData({});
          setStartFormErrors({});
          setShowStartModal(true);
        } catch (error) {
          console.error('加载表单失败:', error);
          setAlert('error', '加载表单失败: ' + getErrorMessage(error));
        }
      } else {
        const confirmed = await showConfirm('此流程未绑定表单，是否直接启动？', '启动确认');
        if (confirmed) {
          navigate(`/workflow/instance/new/${currentDefinitionId}`);
        }
      }
    }
  };

  const handleStartFormFieldChange = (fieldId, value) => {
    setStartFormData(prev => ({
      ...prev,
      [fieldId]: value
    }));
    if (startFormErrors[fieldId]) {
      setStartFormErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[fieldId];
        return newErrors;
      });
    }
  };

  const validateStartForm = () => {
    if (!selectedStartForm) return true;
    const result = validateFormFields(selectedStartForm.fields, startFormData);
    setStartFormErrors(result.errors || {});
    return result.valid;
  };

  const doStartInstance = async () => {
    setStartingInstance(true);
    try {
      let response;
      if (formId) {
        response = await workflowInstanceApi.startInstanceWithForm(currentDefinitionId, {
          formId,
          formData: startFormData
        });
      } else {
        response = await workflowInstanceApi.startInstance(currentDefinitionId);
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
    if (formId && selectedStartForm) {
      if (!validateStartForm()) {
        return;
      }
    }
    doStartInstance();
  };

  const handleCloseStartModal = () => {
    if (!startingInstance) {
      setShowStartModal(false);
    }
  };

  if (loading) {
    return <div className="workflow-editor">加载中...</div>;
  }

  if (loadError) {
    return (
      <div className="workflow-editor">
        <PageError
          title="加载失败"
          message={loadError}
          onRetry={!isNewFlag && currentDefinitionId ? () => loadWorkflow(currentDefinitionId) : null}
          backTo="/workflows"
          backText="返回流程列表"
        />
      </div>
    );
  }

  return (
    <div className="workflow-editor">
      <div className="editor-header">
        <div className="editor-title">
          <button className="btn btn-default" onClick={() => navigate('/workflows')}>
            ← 返回列表
          </button>
          <input
            type="text"
            className="workflow-name-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="请输入流程名称"
          />
          <span className="workflow-description-input">
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="请输入流程描述（可选）"
            />
          </span>
        </div>
        <div className="editor-actions">
          <button className="btn btn-default" onClick={() => navigate('/workflow/help')}>
            ❓ 帮助
          </button>
          <button className="btn btn-default" onClick={handleValidate} disabled={validating}>
            {validating ? '验证中...' : '✓ 验证'}
          </button>
          <button
            className="btn btn-default"
            onClick={handlePreview}
            disabled={!canPreviewOrStart}
            title={!canPreviewOrStart ? '请先保存流程后再预览' : '预览流程图'}
          >
            👁 预览
          </button>
          <button
            className="btn btn-success"
            onClick={handleStartInstance}
            disabled={!canPreviewOrStart}
            title={!canPreviewOrStart ? '请先保存流程后再启动' : '启动流程实例'}
          >
            ▶ 启动流程
          </button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '💾 保存'}
          </button>
        </div>
      </div>

      {validationErrors.length > 0 && (
        <div className="validation-errors">
          <h4>❌ 流程验证失败：</h4>
          <ul>
            {validationErrors.map((error, index) => (
              <li key={index}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="editor-body">
        <NodeLibrary />

        <div className="canvas-container">
          <WorkflowCanvas
            nodes={nodes}
            edges={edges}
            selectedNodeId={selectedNodeId}
            selectedEdgeId={selectedEdgeId}
            onSelectNode={handleSelectNode}
            onSelectEdge={handleSelectEdge}
            onAddNode={handleAddNode}
            onUpdateNode={handleUpdateNode}
            onAddEdge={handleAddEdge}
            onCanvasClick={handleCanvasClick}
          />
        </div>

        <WorkflowPropertiesPanel
          selectedNode={selectedNode}
          selectedEdge={selectedEdge}
          nodes={nodes}
          edges={edges}
          onUpdateNode={handleUpdateNode}
          onUpdateEdge={handleUpdateEdge}
          onDeleteNode={handleDeleteNode}
          onDeleteEdge={handleDeleteEdge}
          formId={formId}
          setFormId={setFormId}
          formName={formName}
          setFormName={setFormName}
        />
      </div>

      {showStartModal && (
        <div className="modal-overlay" onClick={handleCloseStartModal}>
          <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>启动流程 - 填写表单</h3>
              <button className="modal-close" onClick={handleCloseStartModal} disabled={startingInstance}>×</button>
            </div>
            <div className="modal-body">
              {selectedStartForm && (
                <div className="form-preview-section">
                  <h4 style={{ marginBottom: '16px', color: '#1890ff' }}>
                    📝 {selectedStartForm.name}
                  </h4>
                  {selectedStartForm.description && (
                    <p style={{ color: '#666', marginBottom: '16px' }}>
                      {selectedStartForm.description}
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
                      {selectedStartForm.fields?.map(field => (
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
                            value={startFormData[field.id]}
                            onChange={handleStartFormFieldChange}
                            errors={startFormErrors}
                            disabled={startingInstance}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {!selectedStartForm && (
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
                onClick={handleCloseStartModal} 
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

export default WorkflowEditor;
