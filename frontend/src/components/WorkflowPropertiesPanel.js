import { useState, useEffect, useRef } from 'react';
import { getNodeTypeConfig } from '../services/workflowApi';
import { formApi } from '../services/api';

const BRANCH_TYPES = [
  { value: 'approve', label: '批准路径' },
  { value: 'reject', label: '拒绝路径' },
];

function WorkflowPropertiesPanel({ selectedNode, selectedEdge, nodes, edges, onUpdateNode, onUpdateEdge, onDeleteNode, onDeleteEdge, formId, setFormId, formName, setFormName }) {
  const workflowBoundFormId = formId;
  const [nodeName, setNodeName] = useState('');
  const [edgeLabel, setEdgeLabel] = useState('');
  const [properties, setProperties] = useState({});
  const [formsList, setFormsList] = useState([]);
  const [selectedFormForFields, setSelectedFormForFields] = useState(null);
  const [loadingForms, setLoadingForms] = useState(false);
  const [showExpressionHint, setShowExpressionHint] = useState(false);
  const [expressionError, setExpressionError] = useState('');
  const expressionInputRef = useRef(null);

  useEffect(() => {
    if (selectedNode) {
      setNodeName(selectedNode.name || '');
      setProperties(selectedNode.properties || {});
    }
  }, [selectedNode]);

  const [edgeBranchType, setEdgeBranchType] = useState('');

  useEffect(() => {
    if (selectedEdge) {
      setEdgeLabel(selectedEdge.label || '');
      setEdgeBranchType(selectedEdge.branchType || '');
    }
  }, [selectedEdge]);

  useEffect(() => {
    loadFormsList();
  }, []);

  useEffect(() => {
    if (selectedNode?.type === 'condition') {
      if (workflowBoundFormId) {
        handleFormForFieldsSelect(workflowBoundFormId);
      }
    } else {
      setSelectedFormForFields(null);
    }
  }, [selectedNode, workflowBoundFormId]);

  const loadFormsList = async () => {
    setLoadingForms(true);
    try {
      const response = await formApi.getFormsList();
      setFormsList(response.data || []);
    } catch (error) {
      console.error('加载表单列表失败:', error);
    } finally {
      setLoadingForms(false);
    }
  };

  const handleFormForFieldsSelect = async (formId) => {
    setSelectedFormForFields(null);
    if (formId) {
      try {
        const response = await formApi.getFormById(formId);
        setSelectedFormForFields(response.data);
      } catch (error) {
        console.error('加载表单详情失败:', error);
      }
    }
  };

  const insertFieldToExpression = (fieldId) => {
    const currentExpr = properties.expression || '';
    const newExpr = currentExpr + fieldId;
    handlePropertyChange('expression', newExpr);
    if (expressionInputRef.current) {
      expressionInputRef.current.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      setTimeout(() => {
        if (expressionInputRef.current) {
          expressionInputRef.current.focus();
          const len = newExpr.length;
          expressionInputRef.current.setSelectionRange(len, len);
        }
      }, 100);
    }
  };

  const insertOperatorToExpression = (operator) => {
    const currentExpr = properties.expression || '';
    const newExpr = currentExpr + ' ' + operator + ' ';
    handlePropertyChange('expression', newExpr);
    if (expressionInputRef.current) {
      expressionInputRef.current.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      setTimeout(() => {
        if (expressionInputRef.current) {
          expressionInputRef.current.focus();
          const len = newExpr.length;
          expressionInputRef.current.setSelectionRange(len, len);
        }
      }, 100);
    }
  };

  const validateExpression = (expr) => {
    if (!expr || !expr.trim()) {
      setExpressionError('');
      return;
    }
    try {
      let i = 0;
      const len = expr.length;
      let parenCount = 0;
      let inString = false;
      let stringChar = '';
      let lastToken = '';
      
      while (i < len) {
        const c = expr.charAt(i);
        
        if (inString) {
          if (c === '\\' && i + 1 < len) {
            i += 2;
            continue;
          }
          if (c === stringChar) {
            inString = false;
            stringChar = '';
          }
          i++;
          continue;
        }
        
        if (c === '"' || c === "'") {
          inString = true;
          stringChar = c;
          i++;
          continue;
        }
        
        if (c === '(') {
          parenCount++;
          i++;
          continue;
        }
        if (c === ')') {
          parenCount--;
          if (parenCount < 0) {
            throw new Error('多余的右括号');
          }
          i++;
          continue;
        }
        
        if (c === '&' && i + 1 < len && expr.charAt(i + 1) === '&') {
          i += 2;
          continue;
        }
        if (c === '|' && i + 1 < len && expr.charAt(i + 1) === '|') {
          i += 2;
          continue;
        }
        
        if (c === '>' || c === '<' || c === '=' || c === '!') {
          if (c === '=' && i + 1 < len && expr.charAt(i + 1) === '=') {
            i += 2;
            continue;
          }
          if (c === '!' && i + 1 < len && expr.charAt(i + 1) === '=') {
            i += 2;
            continue;
          }
          if (c === '>' && i + 1 < len && expr.charAt(i + 1) === '=') {
            i += 2;
            continue;
          }
          if (c === '<' && i + 1 < len && expr.charAt(i + 1) === '=') {
            i += 2;
            continue;
          }
          if (c === '>' || c === '<') {
            i++;
            continue;
          }
        }
        
        if (/\s/.test(c)) {
          i++;
          continue;
        }
        
        if (/[a-zA-Z0-9_\-.$]/.test(c)) {
          while (i < len && /[a-zA-Z0-9_\-.$]/.test(expr.charAt(i))) {
            i++;
          }
          continue;
        }
        
        if (c === '-' && i + 1 < len && /[0-9]/.test(expr.charAt(i + 1))) {
          i++;
          continue;
        }
        
        throw new Error(`无法识别的字符: ${c}`);
      }
      
      if (inString) {
        throw new Error('字符串未闭合');
      }
      if (parenCount > 0) {
        throw new Error('括号未闭合');
      }
      
      setExpressionError('');
    } catch (e) {
      setExpressionError(e.message || '语法错误');
    }
  };

  useEffect(() => {
    if (selectedNode?.type === 'condition') {
      validateExpression(properties.expression || '');
    } else {
      setExpressionError('');
    }
  }, [properties.expression, selectedNode]);

  const handleWorkflowFormSelect = (e) => {
    const value = e.target.value;
    if (value === '') {
      setFormId(null);
      setFormName('');
    } else {
      const form = formsList.find(f => f.id === Number(value));
      setFormId(Number(value));
      setFormName(form ? form.name : '');
    }
  };

  if (!selectedNode && !selectedEdge) {
    return (
      <div className="properties-panel">
        <h3>流程属性</h3>
        <div className="properties-section">
          <label>绑定表单</label>
          <select
            value={formId || ''}
            onChange={handleWorkflowFormSelect}
            style={{ width: '100%' }}
          >
            <option value="">不绑定表单</option>
            {formsList.map(form => (
              <option key={form.id} value={form.id}>{form.name}</option>
            ))}
          </select>
          {formId && (
            <div className="properties-hint" style={{ marginTop: '8px', color: '#1890ff', fontSize: '13px' }}>
              ✅ 已绑定：{formName}
            </div>
          )}
          <div className="properties-hint" style={{ marginTop: '8px', fontSize: '12px', color: '#8c8c8c' }}>
            绑定表单后，发起该流程时会自动显示此表单供填写
          </div>
        </div>
        <div className="properties-section" style={{ marginTop: '20px', borderTop: '1px solid #e8e8e8', paddingTop: '20px' }}>
          <p style={{ color: '#8c8c8c', fontSize: '13px' }}>💡 点击画布上的节点或连线可编辑其属性</p>
        </div>
      </div>
    );
  }

  if (selectedEdge) {
    const sourceNode = nodes.find(n => n.id === selectedEdge.source);
    const targetNode = nodes.find(n => n.id === selectedEdge.target);
    const sourceNodeType = sourceNode?.type;
    const needsBranchType = ['condition', 'approval', 'countersign'].includes(sourceNodeType);
    const outgoingEdges = edges.filter(e => e.source === selectedEdge.source);
    const hasMultipleOutgoing = outgoingEdges.length > 1;
    const showBranchType = needsBranchType && hasMultipleOutgoing;

    const handleBranchTypeChange = (e) => {
      const value = e.target.value;
      setEdgeBranchType(value);
      onUpdateEdge({ ...selectedEdge, branchType: value });
    };

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
        {showBranchType && (
          <div className="properties-section">
            <label style={{ color: '#ff4d4f' }}>分支类型 *</label>
            <select
              value={edgeBranchType}
              onChange={handleBranchTypeChange}
              style={{
                borderColor: !edgeBranchType ? '#ff4d4f' : undefined,
                backgroundColor: !edgeBranchType ? '#fff2f0' : undefined
              }}
            >
              <option value="">请选择分支类型</option>
              {BRANCH_TYPES.map(bt => (
                <option key={bt.value} value={bt.value}>{bt.label}</option>
              ))}
            </select>
            {!edgeBranchType && (
              <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
                ⚠️ 必须选择分支类型才能保存
              </div>
            )}
          </div>
        )}
        <div className="properties-section">
          <label>标签（可选，仅显示用）</label>
          <input
            type="text"
            value={edgeLabel}
            onChange={(e) => setEdgeLabel(e.target.value)}
            onBlur={() => onUpdateEdge({ ...selectedEdge, label: edgeLabel })}
            placeholder="如：经理通过/驳回给申请人"
          />
          <div style={{ color: '#8c8c8c', fontSize: '12px', marginTop: '4px' }}>
            标签仅供显示，不影响流程路由
          </div>
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
            <label style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>条件表达式</span>
              <button
                type="button"
                className="btn btn-default"
                style={{ 
                  fontSize: '12px', 
                  padding: '2px 8px', 
                  height: 'auto',
                  lineHeight: '1.5'
                }}
                onClick={() => setShowExpressionHint(!showExpressionHint)}
              >
                {showExpressionHint ? '收起说明' : '📖 语法说明'}
              </button>
            </label>
            <input
              ref={expressionInputRef}
              type="text"
              value={properties.expression || ''}
              onChange={(e) => handlePropertyChange('expression', e.target.value)}
              placeholder="如：days > 3"
              style={{ 
                fontFamily: 'monospace',
                borderColor: expressionError ? '#ff4d4f' : undefined,
                backgroundColor: expressionError ? '#fff2f0' : undefined
              }}
            />
            {expressionError && (
              <div style={{
                color: '#ff4d4f',
                fontSize: '12px',
                marginTop: '4px',
                lineHeight: '1.5'
              }}>
                ⚠️ {expressionError}
              </div>
            )}
          </div>

          {showExpressionHint && (
            <div className="properties-section" style={{
              padding: '10px 12px',
              backgroundColor: '#fafafa',
              borderRadius: '6px',
              border: '1px solid #e8e8e8',
              marginBottom: '12px'
            }}>
              <div style={{ 
                fontSize: '12px', 
                color: '#666', 
                lineHeight: '1.8'
              }}>
                <div><strong>数字字段：</strong>直接写，如 <code>days &gt; 3</code></div>
                <div><strong>字符串字段：</strong>值加引号，如 <code>leaveType == "annual"</code></div>
                <div><strong>逻辑运算：</strong><code>&amp;&amp;</code> 且、<code>||</code> 或</div>
                <div><strong>运算符：</strong>&gt; &lt; &gt;= &lt;= == !=</div>
                <div style={{ marginTop: '4px', color: '#8c8c8c' }}>
                  💡 点击下方字段快速插入表达式
                </div>
              </div>
            </div>
          )}

          <div className="properties-section">
            <label>快捷运算符</label>
            <div className="operator-buttons" style={{ 
              display: 'flex', 
              gap: '6px', 
              flexWrap: 'wrap',
              marginBottom: '8px'
            }}>
              {['>', '<', '>=', '<=', '==', '!=', '&&', '||'].map(op => (
                <button
                  key={op}
                  type="button"
                  className="btn btn-default"
                  style={{ 
                    padding: '4px 10px', 
                    fontSize: '13px',
                    fontFamily: 'monospace',
                    minWidth: '40px'
                  }}
                  onClick={() => insertOperatorToExpression(op)}
                >
                  {op}
                </button>
              ))}
            </div>
          </div>

          <div className="properties-section">
            <label>可选字段</label>
            {workflowBoundFormId ? (
              <div style={{ 
                padding: '6px 10px', 
                backgroundColor: '#e6f7ff', 
                border: '1px solid #91d5ff', 
                borderRadius: '4px',
                marginBottom: '8px',
                fontSize: '12px',
                color: '#1890ff'
              }}>
                ✅ 已绑定：{formName || selectedFormForFields?.name}
              </div>
            ) : (
              <select
                className="select-input"
                value={selectedFormForFields?.id || ''}
                onChange={(e) => handleFormForFieldsSelect(e.target.value || null)}
                disabled={loadingForms}
                style={{ marginBottom: '8px', fontSize: '13px' }}
              >
                <option value="">{loadingForms ? '加载中...' : '选择表单查看字段'}</option>
                {formsList.map(form => (
                  <option key={form.id} value={form.id}>{form.name}</option>
                ))}
              </select>
            )}

            {selectedFormForFields && (
              <div className="form-fields-list" style={{
                maxHeight: '220px',
                overflowY: 'auto',
                padding: '8px',
                backgroundColor: '#fafafa',
                borderRadius: '6px',
                border: '1px solid #e8e8e8'
              }}>
                {selectedFormForFields.fields?.map(field => (
                  <div key={field.id} style={{ marginBottom: '6px' }}>
                    <div
                      className="field-chip"
                      onClick={() => insertFieldToExpression(field.id)}
                      style={{
                        display: 'inline-block',
                        padding: '3px 8px',
                        backgroundColor: '#e6f7ff',
                        border: '1px solid #91d5ff',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '12px',
                        transition: 'all 0.2s',
                      }}
                      onMouseEnter={(e) => {
                        e.target.style.backgroundColor = '#bae7ff';
                      }}
                      onMouseLeave={(e) => {
                        e.target.style.backgroundColor = '#e6f7ff';
                      }}
                      title={`${field.label} (${field.type}) - 点击插入`}
                    >
                      <span style={{ fontWeight: '500', color: '#1890ff' }}>{field.label}</span>
                      <span style={{ color: '#8c8c8c', marginLeft: '4px', fontFamily: 'monospace' }}>
                        {field.id}
                      </span>
                    </div>
                    {(field.type === 'select' || field.type === 'radio') && field.options && (
                      <div style={{ 
                        marginTop: '2px', 
                        marginLeft: '4px', 
                        fontSize: '11px', 
                        color: '#8c8c8c' 
                      }}>
                        值：{field.options.map(opt => 
                          <code key={opt.value} style={{
                            backgroundColor: '#f0f0f0',
                            padding: '1px 4px',
                            borderRadius: '2px',
                            marginRight: '3px',
                            fontFamily: 'monospace'
                          }}>"{opt.value}"</code>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="properties-section">
            <label>条件说明</label>
            <textarea
              value={properties.description || ''}
              onChange={(e) => handlePropertyChange('description', e.target.value)}
              placeholder="请输入条件说明（可选）"
              rows={2}
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
