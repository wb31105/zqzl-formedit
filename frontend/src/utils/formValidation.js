export function isEmptyValue(value) {
  if (value === null || value === undefined) return true;
  if (value === '') return true;
  if (Array.isArray(value) && value.length === 0) return true;
  if (typeof value === 'string' && value.trim() === '') return true;
  return false;
}

export function validateFormField(field, value, errorsMap) {
  const stringValue = value ? String(value).trim() : '';
  const isEmpty = isEmptyValue(value);

  if (field.required && isEmpty) {
    errorsMap[field.id] = `${field.label}不能为空`;
    return;
  }

  if (isEmpty) return;

  const isTextLike = ['text', 'textarea', 'email', 'number'].includes(field.type);

  if (isTextLike && field.minLength && stringValue.length < field.minLength) {
    errorsMap[field.id] = `${field.label}最少需要${field.minLength}个字符`;
  }

  if (isTextLike && field.maxLength && stringValue.length > field.maxLength) {
    errorsMap[field.id] = `${field.label}最多允许${field.maxLength}个字符`;
  }

  if (field.pattern && field.pattern.trim() && stringValue) {
    try {
      const regex = new RegExp(field.pattern);
      if (!regex.test(stringValue)) {
        errorsMap[field.id] = field.patternMessage || `${field.label}格式不正确`;
      }
    } catch (e) {
      console.error('正则表达式错误:', e);
    }
  }
}

export function validateFormFields(fields, formData) {
  const errors = {};

  fields?.forEach((field) => {
    const value = formData[field.id];
    validateFormField(field, value, errors);
  });

  return {
    valid: Object.keys(errors).length === 0,
    errors,
  };
}
