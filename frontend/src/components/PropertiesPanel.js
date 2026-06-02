function PropertiesPanel({ field, onUpdate }) {
  if (!field) {
    return (
      <div className="editor-properties">
        <h2>属性设置</h2>
        <div className="properties-panel" style={{ color: '#999', textAlign: 'center', padding: '40px 0' }}>
          请选择一个控件
        </div>
      </div>
    );
  }

  const handleChange = (key, value) => {
    onUpdate({ ...field, [key]: value });
  };

  const handleOptionChange = (index, key, value) => {
    const newOptions = [...field.options];
    newOptions[index] = { ...newOptions[index], [key]: value };
    onUpdate({ ...field, options: newOptions });
  };

  const addOption = () => {
    const newOptions = [...(field.options || []), { label: `选项${field.options.length + 1}`, value: `option${field.options.length + 1}` }];
    onUpdate({ ...field, options: newOptions });
  };

  const removeOption = (index) => {
    const newOptions = field.options.filter((_, i) => i !== index);
    onUpdate({ ...field, options: newOptions });
  };

  const showOptions = ['select', 'radio', 'checkbox'].includes(field.type);
  const showLength = ['text', 'textarea', 'email'].includes(field.type);
  const showPattern = ['text', 'email'].includes(field.type);

  return (
    <div className="editor-properties">
      <h2>属性设置</h2>
      <div className="properties-panel">
        <div className="property-group">
          <label>字段ID</label>
          <input type="text" value={field.id} disabled />
        </div>

        <div className="property-group">
          <label>字段类型</label>
          <input type="text" value={field.type} disabled />
        </div>

        <div className="property-group">
          <label>列宽度（共24栏）</label>
          <select value={field.span || 24} onChange={(e) => handleChange('span', parseInt(e.target.value))}>
            <option value={24}>整行 (24/24)</option>
            <option value={12}>半行 (12/24) — 两列</option>
            <option value={8}>三分之一 (8/24) — 三列</option>
            <option value={6}>四分之一 (6/24) — 四列</option>
            <option value={16}>三分之二 (16/24)</option>
            <option value={18}>四分之三 (18/24)</option>
          </select>
        </div>

        <div className="property-group">
          <label>标签文字</label>
          <input
            type="text"
            value={field.label || ''}
            onChange={(e) => handleChange('label', e.target.value)}
          />
        </div>

        {field.placeholder !== undefined && (
          <div className="property-group">
            <label>占位符</label>
            <input
              type="text"
              value={field.placeholder || ''}
              onChange={(e) => handleChange('placeholder', e.target.value)}
            />
          </div>
        )}

        <div className="property-group">
          <label className="checkbox-inline">
            <input
              type="checkbox"
              checked={field.required || false}
              onChange={(e) => handleChange('required', e.target.checked)}
            />
            必填字段
          </label>
        </div>

        <div className="property-group">
          <label className="checkbox-inline">
            <input
              type="checkbox"
              checked={field.disabled || false}
              onChange={(e) => handleChange('disabled', e.target.checked)}
            />
            禁用字段
          </label>
        </div>

        {showLength && (
          <>
            <div className="property-group">
              <label>最小长度</label>
              <input
                type="number"
                value={field.minLength || ''}
                onChange={(e) => handleChange('minLength', e.target.value ? parseInt(e.target.value) : null)}
                placeholder="不限制"
              />
            </div>
            <div className="property-group">
              <label>最大长度</label>
              <input
                type="number"
                value={field.maxLength || ''}
                onChange={(e) => handleChange('maxLength', e.target.value ? parseInt(e.target.value) : null)}
                placeholder="不限制"
              />
            </div>
          </>
        )}

        {showPattern && (
          <>
            <div className="property-group">
              <label>正则验证</label>
              <input
                type="text"
                value={field.pattern || ''}
                onChange={(e) => handleChange('pattern', e.target.value)}
                placeholder="如: ^1[3-9]\d{9}$"
              />
            </div>
            <div className="property-group">
              <label>错误提示</label>
              <input
                type="text"
                value={field.patternMessage || ''}
                onChange={(e) => handleChange('patternMessage', e.target.value)}
                placeholder="格式不正确"
              />
            </div>
          </>
        )}

        {showOptions && (
          <div className="property-group">
            <label>选项配置</label>
            <div className="options-editor">
              {field.options?.map((opt, idx) => (
                <div key={idx} className="option-item">
                  <input
                    type="text"
                    placeholder="显示文字"
                    value={opt.label}
                    onChange={(e) => handleOptionChange(idx, 'label', e.target.value)}
                  />
                  <input
                    type="text"
                    placeholder="值"
                    value={opt.value}
                    onChange={(e) => handleOptionChange(idx, 'value', e.target.value)}
                  />
                  <button onClick={() => removeOption(idx)}>×</button>
                </div>
              ))}
              <button className="btn-add-option" onClick={addOption}>
                + 添加选项
              </button>
            </div>
          </div>
        )}

        <div className="property-group">
          <label>默认值</label>
          <input
            type="text"
            value={field.defaultValue || ''}
            onChange={(e) => handleChange('defaultValue', e.target.value)}
          />
        </div>
      </div>
    </div>
  );
}

export default PropertiesPanel;
