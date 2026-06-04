function FieldRenderer({ field, value, onChange, errors = {}, disabled = false, readOnly = false }) {
  const hasError = !!errors[field.id];

  const handleChange = (e) => {
    if (field.type === 'checkbox') {
      const checked = e.target.checked;
      const currentValues = Array.isArray(value) ? [...value] : [];
      if (checked) {
        currentValues.push(e.target.value);
      } else {
        const index = currentValues.indexOf(e.target.value);
        if (index > -1) currentValues.splice(index, 1);
      }
      onChange(field.id, currentValues);
    } else {
      onChange(field.id, e.target.value);
    }
  };

  const getDisplayValue = () => {
    if (value === null || value === undefined || value === '') {
      return '-';
    }
    if (field.type === 'select' || field.type === 'radio') {
      const option = field.options?.find(opt => opt.value === value);
      return option ? option.label : value;
    }
    if (field.type === 'checkbox') {
      const values = Array.isArray(value) ? value : [];
      const labels = values.map(v => {
        const option = field.options?.find(opt => opt.value === v);
        return option ? option.label : v;
      });
      return labels.length > 0 ? labels.join('、') : '-';
    }
    return String(value);
  };

  if (readOnly) {
    return (
      <div className="field-wrapper field-readonly">
        <div className="field-readonly-value">{getDisplayValue()}</div>
      </div>
    );
  }

  const renderField = () => {
    switch (field.type) {
      case 'text':
      case 'email':
      case 'number':
        return (
          <input
            type={field.type}
            className={`field-input ${hasError ? 'error' : ''}`}
            placeholder={field.placeholder}
            value={value || ''}
            onChange={handleChange}
            disabled={disabled || field.disabled}
          />
        );

      case 'textarea':
        return (
          <textarea
            className={`field-input ${hasError ? 'error' : ''}`}
            placeholder={field.placeholder}
            value={value || ''}
            onChange={handleChange}
            disabled={disabled || field.disabled}
            rows={4}
            style={{ resize: 'vertical' }}
          />
        );

      case 'select':
        return (
          <select
            className={`select-input ${hasError ? 'error' : ''}`}
            value={value || ''}
            onChange={handleChange}
            disabled={disabled || field.disabled}
          >
            <option value="">{field.placeholder || '请选择'}</option>
            {field.options?.map((opt, idx) => (
              <option key={idx} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        );

      case 'radio':
        return (
          <div className="radio-group">
            {field.options?.map((opt, idx) => (
              <label key={idx} className="radio-item">
                <input
                  type="radio"
                  name={field.id}
                  value={opt.value}
                  checked={value === opt.value}
                  onChange={handleChange}
                  disabled={disabled || field.disabled}
                />
                {opt.label}
              </label>
            ))}
          </div>
        );

      case 'checkbox':
        const checkboxValues = Array.isArray(value) ? value : [];
        return (
          <div className="checkbox-group">
            {field.options?.map((opt, idx) => (
              <label key={idx} className="checkbox-item">
                <input
                  type="checkbox"
                  value={opt.value}
                  checked={checkboxValues.includes(opt.value)}
                  onChange={handleChange}
                  disabled={disabled || field.disabled}
                />
                {opt.label}
              </label>
            ))}
          </div>
        );

      case 'date':
        return (
          <input
            type="date"
            className={`field-input ${hasError ? 'error' : ''}`}
            value={value || ''}
            onChange={handleChange}
            disabled={disabled || field.disabled}
          />
        );

      case 'time':
        return (
          <input
            type="time"
            className={`field-input ${hasError ? 'error' : ''}`}
            value={value || ''}
            onChange={handleChange}
            disabled={disabled || field.disabled}
          />
        );

      default:
        return <div>未知字段类型: {field.type}</div>;
    }
  };

  return (
    <div className="field-wrapper">
      {renderField()}
      {hasError && <div className="field-error">{errors[field.id]}</div>}
    </div>
  );
}

export default FieldRenderer;
