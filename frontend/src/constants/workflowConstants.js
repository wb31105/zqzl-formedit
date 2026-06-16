export const NODE_TYPES = [
  { type: 'start', name: '开始节点', icon: '▶', color: '#52c41a', canDelete: false },
  { type: 'approval', name: '审批节点', icon: '✓', color: '#1890ff', canDelete: true },
  { type: 'countersign', name: '会签节点', icon: '👥', color: '#13c2c2', canDelete: true },
  { type: 'condition', name: '条件分支', icon: '◆', color: '#fa8c16', canDelete: true },
  { type: 'auto', name: '自动任务', icon: '⚙', color: '#722ed1', canDelete: true },
  { type: 'end', name: '结束节点', icon: '■', color: '#f5222d', canDelete: false },
];

export const getNodeTypeConfig = (type) => {
  return NODE_TYPES.find(t => t.type === type) || NODE_TYPES[1];
};

export const NODE_TYPE_CODES = {
  START: 'start',
  APPROVAL: 'approval',
  COUNTERSIGN: 'countersign',
  CONDITION: 'condition',
  AUTO: 'auto',
  END: 'end',
};

export const TASK_STATUS = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED',
};

export const INSTANCE_STATUS = {
  RUNNING: 'RUNNING',
  COMPLETED: 'COMPLETED',
  ERROR: 'ERROR',
};

export const APPROVAL_ACTION = {
  APPROVE: 'approve',
  REJECT: 'reject',
};

export const BRANCH_TYPE = {
  APPROVE: 'approve',
  REJECT: 'reject',
};

export const COUNTERSIGN_TYPE = {
  VETO: 'veto',
  MAJORITY: 'majority',
  ALL: 'all',
};

export const COUNTERSIGN_TYPE_LABEL = {
  veto: '一票否决',
  majority: '过半通过',
  all: '全部同意才通过',
};

export const APPROVAL_ACTION_LABEL = {
  approve: '批准',
  reject: '拒绝',
};

export const STATUS_LABEL = {
  PENDING: '等待中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  RUNNING: '运行中',
  ERROR: '错误',
};

export const NODE_PROPS = {
  COUNTERSIGN_TYPE: 'countersignType',
  ACTION_TYPE: 'actionType',
  APPROVER: 'approver',
  APPROVERS: 'approvers',
  APPROVE_TEXT: 'approveText',
  REJECT_TEXT: 'rejectText',
  COMMENT_LABEL: 'commentLabel',
  DESCRIPTION: 'description',
  EXPRESSION: 'expression',
};
