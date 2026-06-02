export const fieldTypes = [
  { type: 'text', label: '文本框', icon: 'T' },
  { type: 'textarea', label: '多行文本', icon: '¶' },
  { type: 'number', label: '数字', icon: '#' },
  { type: 'email', label: '邮箱', icon: '@' },
  { type: 'select', label: '下拉选择', icon: '▼' },
  { type: 'radio', label: '单选按钮', icon: '○' },
  { type: 'checkbox', label: '多选框', icon: '☐' },
  { type: 'date', label: '日期', icon: '📅' },
  { type: 'time', label: '时间', icon: '⏰' },
];

export const createField = (type) => {
  const id = `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  const fieldConfig = {
    text: { label: '文本框', placeholder: '请输入文本' },
    textarea: { label: '多行文本', placeholder: '请输入内容' },
    number: { label: '数字', placeholder: '请输入数字' },
    email: { label: '邮箱', placeholder: '请输入邮箱地址' },
    select: { label: '下拉选择', placeholder: '请选择', options: [{ label: '选项1', value: 'option1' }, { label: '选项2', value: 'option2' }] },
    radio: { label: '单选', options: [{ label: '选项1', value: 'option1' }, { label: '选项2', value: 'option2' }] },
    checkbox: { label: '多选', options: [{ label: '选项1', value: 'option1' }, { label: '选项2', value: 'option2' }] },
    date: { label: '日期' },
    time: { label: '时间' },
  };

  return {
    id,
    type,
    required: false,
    disabled: false,
    span: 24,
    minLength: null,
    maxLength: null,
    pattern: '',
    patternMessage: '',
    defaultValue: '',
    ...fieldConfig[type],
  };
};
