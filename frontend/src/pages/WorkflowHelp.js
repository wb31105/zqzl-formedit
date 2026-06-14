import { useNavigate } from 'react-router-dom';

function WorkflowHelp() {
  const navigate = useNavigate();

  const nodeTypes = [
    {
      type: 'start',
      name: '开始节点',
      icon: '▶',
      color: '#52c41a',
      description: '每个流程必须有且只有一个开始节点，是流程的起点。',
      canDelete: false,
      properties: '无特殊属性',
      usage: '新建流程时自动创建，不能删除。流程从这里开始执行。',
    },
    {
      type: 'approval',
      name: '审批节点',
      icon: '✓',
      color: '#1890ff',
      description: '需要人工处理的审批环节，流程到达后会暂停等待审批。',
      canDelete: true,
      properties: [
        { name: '审批人', desc: '指定该节点的审批人姓名或角色' },
        { name: '审批说明', desc: '对该审批环节的说明或要求' },
      ],
      usage: '用于需要人工审核的环节，如请假审批、报销审批等。流程执行到该节点时会创建待办任务，需要人工批准或拒绝后才能继续。',
    },
    {
        type: 'condition',
        name: '条件分支节点',
        icon: '◆',
        color: '#fa8c16',
        description: '根据条件判断流程走向，支持多条分支路径。无需人工干预，自动判断并流转。',
        canDelete: true,
        properties: [
          { name: '条件表达式', desc: '用于自动判断的条件表达式，如：amount > 1000、days <= 3' },
          { name: '条件说明', desc: '对该条件的描述说明，方便其他人员理解' },
        ],
        usage: '用于需要根据审批结果或业务数据自动选择不同路径的场景。流程流转到条件分支节点时会自动判断，无需人工操作。\n\n**两种判断方式（优先级从高到低）：**\n\n1. **条件表达式判断**：如果配置了条件表达式，系统会自动解析表达式并判断结果。支持的运算符：>、<、>=、<=、==、!=。支持变量替换，如 `${node_xxx_action}` 可以获取某个节点的操作结果。\n\n2. **上一节点操作判断**：如果没有配置条件表达式，系统会自动使用上一个审批节点的操作结果（批准/拒绝）来判断路径。\n\n**连线分支类型配置：**\n每条出边必须设置分支类型，用于决定流程走向：\n- 批准路径（approve）：判断结果为"是/批准"时走此路径\n- 拒绝路径（reject）：判断结果为"否/拒绝"时走此路径\n\n连线标签仅供显示，不参与路由判断。条件分支节点必须有恰好两条出边，分别设置为"批准路径"和"拒绝路径"。',
      },
    {
      type: 'countersign',
      name: '会签节点',
      icon: '👥',
      color: '#13c2c2',
      description: '多人会签审批节点，需多个审批人同时审批，支持全部同意、一票否决、过半通过等规则。',
      canDelete: true,
      properties: [
        { name: '审批人（多个）', desc: '多个审批人，用逗号分隔，如：张三,李四,王五' },
        { name: '会签方式', desc: '全部同意才通过 / 一票否决 / 过半通过' },
        { name: '操作类型', desc: '审批 / 审核 / 自定义按钮文本' },
        { name: '意见框标题', desc: '自定义意见输入框的标题' },
        { name: '会签说明', desc: '会签要求或说明文字' },
      ],
      usage: '用于需要多人共同审批的场景，如项目评审、合同会签等。流程执行到会签节点时会为每个审批人生成待办任务，根据会签规则决定最终结果。',
    },
    {
      type: 'auto',
      name: '自动任务节点',
      icon: '⚙',
      color: '#722ed1',
      description: '自动执行的任务节点，无需人工干预，执行完成后自动继续。',
      canDelete: true,
      properties: [
        { name: '任务类型', desc: '选择任务类型：发送通知、调用Webhook、执行脚本' },
        { name: '任务配置', desc: '具体的任务配置内容' },
      ],
      usage: '用于自动执行的操作，如发送邮件通知、调用外部接口、更新数据等。流程执行到该节点时会自动执行任务，完成后自动进入下一个节点。',
    },
    {
      type: 'end',
      name: '结束节点',
      icon: '■',
      color: '#f5222d',
      description: '每个流程必须有且只有一个结束节点，是流程的终点。',
      canDelete: false,
      properties: '无特殊属性',
      usage: '新建流程时自动创建，不能删除。流程执行到这里表示完成。',
    },
  ];

  const operationGuide = [
    {
      title: '添加节点',
      steps: [
        '在左侧节点库中找到需要的节点类型',
        '按住鼠标左键拖拽节点到中间画布上',
        '松开鼠标，节点即添加成功',
      ],
    },
    {
      title: '移动节点',
      steps: [
        '点击选中需要移动的节点',
        '按住鼠标左键拖动节点到目标位置',
        '松开鼠标，节点位置更新',
      ],
    },
    {
      title: '创建连线',
      steps: [
        '将鼠标移到节点右侧的圆形连接点上',
        '按住鼠标左键拖动，会出现一条虚线',
        '拖动到目标节点上松开鼠标',
        '连线创建成功',
        '如连线来自审批/会签/条件节点，创建后需点击连线设置分支类型',
      ],
    },
    {
      title: '编辑节点属性',
      steps: [
        '点击选中需要编辑的节点',
        '右侧属性面板会显示该节点的属性',
        '修改节点名称或其他属性',
        '输入框失去焦点时自动保存',
      ],
    },
    {
      title: '编辑连线属性',
      steps: [
        '点击需要编辑的连线',
        '右侧属性面板会显示连线信息',
        '如果连线来自审批/会签/条件节点且有两条以上出边，必须选择"分支类型"：批准路径 或 拒绝路径',
        '可选设置连线标签，标签仅用于画布显示，不影响流程路由',
      ],
    },
    {
      title: '删除节点或连线',
      steps: [
        '点击选中要删除的节点或连线',
        '按 Delete 或 Backspace 键删除',
        '或者在右侧属性面板点击"删除节点/连线"按钮',
        '注意：开始节点和结束节点不能删除',
      ],
    },
    {
      title: '保存流程',
      steps: [
        '在顶部名称输入框中输入流程名称',
        '点击右上角的"保存"按钮',
        '系统会自动验证流程配置',
        '验证通过后保存成功',
      ],
    },
    {
      title: '启动流程实例',
      steps: [
        '保存流程后，点击"启动流程"按钮',
        '进入流程实例运行页面',
        '在待办任务卡片中点击"批准"或"拒绝"处理任务',
        '观察流程自动流转和执行历史',
      ],
    },
  ];

  const validationRules = [
    { rule: '必须有开始节点和结束节点', desc: '每个流程必须包含一个开始节点和一个结束节点' },
    { rule: '不能有孤立节点', desc: '所有节点（除开始节点外）必须至少有一条入边；所有节点（除结束节点外）必须至少有一条出边' },
    { rule: '节点名称不能重复', desc: '流程中所有节点的名称必须唯一' },
    { rule: '分支节点必须设置分支类型', desc: '审批、会签、条件节点有两条出边时，每条出边必须设置分支类型（批准路径/拒绝路径），且恰好一条批准、一条拒绝' },
    { rule: '审批节点完整性', desc: '审批节点必须有入边和出边' },
  ];

  const bestPractices = [
    '设计流程前先理清业务逻辑，确定需要哪些环节',
    '节点命名要清晰明确，反映该节点的实际功能',
    '双出边的连线必须选择分支类型（批准路径/拒绝路径），这是流程路由的唯一依据',
    '连线标签仅供显示，可以自由命名（如"经理通过"、"驳回给申请人"），不影响流程走向',
    '复杂流程可以分步设计，先搭骨架再完善细节',
    '保存前点击"验证"按钮检查流程配置',
    '使用"预览"功能查看整体流程图',
    '测试流程时可以先运行简单场景，再测试复杂分支',
  ];

  return (
    <div className="workflow-help">
      <div className="help-header">
        <button className="btn btn-default" onClick={() => navigate('/workflows')}>
          ← 返回列表
        </button>
        <h1>工作流设计器使用说明</h1>
      </div>

      <div className="help-content">
        <section className="help-section">
          <h2>一、节点类型说明</h2>
          <div className="node-types-grid">
            {nodeTypes.map((node) => (
              <div key={node.type} className="node-type-card">
                <div className="node-type-header" style={{ backgroundColor: node.color }}>
                  <span className="node-type-icon">{node.icon}</span>
                  <span className="node-type-title">{node.name}</span>
                </div>
                <div className="node-type-body">
                  <p className="node-desc">{node.description}</p>
                  <div className="node-property">
                    <strong>可删除：</strong>
                    <span>{node.canDelete ? '是' : '否'}</span>
                  </div>
                  <div className="node-property">
                    <strong>属性配置：</strong>
                    {Array.isArray(node.properties) ? (
                      <ul>
                        {node.properties.map((prop, idx) => (
                          <li key={idx}>
                            <span className="prop-name">{prop.name}</span>
                            <span className="prop-desc">：{prop.desc}</span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p>{node.properties}</p>
                    )}
                  </div>
                  <div className="node-usage">
                    <strong>使用场景：</strong>
                    <p>{node.usage}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="help-section">
          <h2>二、操作指南</h2>
          <div className="operation-list">
            {operationGuide.map((item, idx) => (
              <div key={idx} className="operation-item">
                <h3>{idx + 1}. {item.title}</h3>
                <ol>
                  {item.steps.map((step, stepIdx) => (
                    <li key={stepIdx}>{step}</li>
                  ))}
                </ol>
              </div>
            ))}
          </div>
        </section>

        <section className="help-section">
          <h2>三、流程验证规则</h2>
          <table className="rules-table">
            <thead>
              <tr>
                <th>验证规则</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              {validationRules.map((item, idx) => (
                <tr key={idx}>
                  <td>{item.rule}</td>
                  <td>{item.desc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="help-section">
          <h2>四、最佳实践</h2>
          <ul className="best-practices-list">
            {bestPractices.map((item, idx) => (
              <li key={idx}>{item}</li>
            ))}
          </ul>
        </section>

        <section className="help-section">
          <h2>五、条件分支节点详解</h2>
          <div className="condition-detail">
            <h3>5.1 底层工作原理</h3>
            <div className="principle-diagram">
              <p><strong>条件分支节点的执行流程：</strong></p>
              <ol>
                <li>流程流转到条件分支节点</li>
                <li>检查是否配置了<strong>条件表达式</strong>
                  <ul>
                    <li>如果有表达式 → 解析表达式，计算结果（approve/reject）</li>
                    <li>如果没有表达式 → 查找上一个审批节点的操作结果（批准/拒绝）</li>
                  </ul>
                </li>
                <li>根据判断结果查找对应<strong>分支类型</strong>的出边
                  <ul>
                    <li>结果为批准 → 沿分支类型为"批准路径"的连线流转</li>
                    <li>结果为拒绝 → 沿分支类型为"拒绝路径"的连线流转</li>
                  </ul>
                </li>
                <li>自动流转到对应连线的目标节点</li>
              </ol>
              <p><strong>关键概念：分支类型（branchType）</strong></p>
              <p>每条出边都携带一个分支类型字段，只有两个可选值：</p>
              <ul>
                <li><strong>批准路径（approve）</strong>：判断结果为"是/批准"时走此边</li>
                <li><strong>拒绝路径（reject）</strong>：判断结果为"否/拒绝"时走此边</li>
              </ul>
              <p>连线标签（label）仅用于画布显示，<strong>不参与</strong>路由判断。无论标签写什么，引擎只看分支类型。</p>
            </div>

            <h3>5.2 条件表达式配置</h3>
            <div className="expression-config">
              <p><strong>支持的运算符：</strong></p>
              <table className="config-table">
                <thead>
                  <tr>
                    <th>运算符</th>
                    <th>说明</th>
                    <th>示例</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>&gt;</td>
                    <td>大于</td>
                    <td><code>amount &gt; 1000</code></td>
                  </tr>
                  <tr>
                    <td>&lt;</td>
                    <td>小于</td>
                    <td><code>days &lt; 3</code></td>
                  </tr>
                  <tr>
                    <td>&gt;=</td>
                    <td>大于等于</td>
                    <td><code>amount &gt;= 5000</code></td>
                  </tr>
                  <tr>
                    <td>&lt;=</td>
                    <td>小于等于</td>
                    <td><code>days &lt;= 5</code></td>
                  </tr>
                  <tr>
                    <td>==</td>
                    <td>等于</td>
                    <td><code>type == "leave"</code></td>
                  </tr>
                  <tr>
                    <td>!=</td>
                    <td>不等于</td>
                    <td><code>status != "rejected"</code></td>
                  </tr>
                </tbody>
              </table>

              <p><strong>支持的变量替换：</strong></p>
              <p>使用 {'${变量名}'} 或 {'{变量名}'} 的格式引用上下文变量。</p>
              <table className="config-table">
                <thead>
                  <tr>
                    <th>变量名</th>
                    <th>说明</th>
                    <th>示例值</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td><code>instanceId</code></td>
                    <td>流程实例ID</td>
                    <td>123</td>
                  </tr>
                  <tr>
                    <td><code>definitionId</code></td>
                    <td>流程定义ID</td>
                    <td>456</td>
                  </tr>
                  <tr>
                    <td><code>definitionName</code></td>
                    <td>流程名称</td>
                    <td>员工请假审批</td>
                  </tr>
                  <tr>
                    <td><code>status</code></td>
                    <td>实例状态</td>
                    <td>RUNNING</td>
                  </tr>
                  <tr>
                    <td><code>node_节点ID_action</code></td>
                    <td>指定节点的操作结果</td>
                    <td>批准、拒绝</td>
                  </tr>
                  <tr>
                    <td><code>node_节点ID_comment</code></td>
                    <td>指定节点的处理意见</td>
                    <td>同意请假</td>
                  </tr>
                </tbody>
              </table>

              <p><strong>表达式示例：</strong></p>
              <ul>
                <li><code>amount &gt; 1000</code> - 金额大于1000走特殊审批</li>
                <li><code>days &lt;= 3</code> - 请假天数小于等于3天走快速审批</li>
                <li><code>leaveType == &quot;annual&quot;</code> - 年假走特殊流程</li>
                <li><code>days &gt; 3 &amp;&amp; leaveType == &quot;sick&quot;</code> - 病假且超过3天</li>
              </ul>

              <p style={{ marginTop: '16px', padding: '12px', backgroundColor: '#fffbe6', border: '1px solid #ffe58f', borderRadius: '4px', fontSize: '13px', lineHeight: '1.8' }}>
                <strong>💡 注意：</strong><br/>
                1. 表单字段使用<strong>字段ID</strong>（不是字段标签，如用 <code>days</code> 不用"请假天数"），可在属性面板点击字段ID快速插入<br/>
                2. 字符串比较值必须加<strong>引号</strong>，如 <code>leaveType == &quot;personal&quot;</code>（引号中的值是 select/radio 的 option value，不是显示文字）<br/>
                3. 支持 <code>&amp;&amp;</code>（且）、<code>||</code>（或）、括号和优先级，如 <code>(days &gt; 3 &amp;&amp; leaveType == &quot;sick&quot;) || amount &gt; 5000</code><br/>
                4. 表达式<strong>可以为空</strong>，空表达式时系统会使用上一审批节点的批准/拒绝结果判断走向<br/>
                5. 写错表达式（语法错误、未闭合字符串等）在保存时会被校验拦截，运行时解析失败会停止流程并标记错误，不会静默当通过
              </p>
            </div>

            <h3>5.3 分支类型配置规则</h3>
            <div className="label-rules">
              <p>当审批节点、会签节点或条件节点有两条出边时，每条出边<strong>必须</strong>设置分支类型。分支类型是引擎路由的唯一依据。</p>
              <div className="label-groups">
                <div className="label-group">
                  <h4>✅ 批准路径（approve）</h4>
                  <p>判断结果为"是/批准/通过"时走此路径</p>
                  <p>适用场景：审批通过后继续下一环节、条件满足时走主流程</p>
                </div>
                <div className="label-group">
                  <h4>❌ 拒绝路径（reject）</h4>
                  <p>判断结果为"否/拒绝/退回"时走此路径</p>
                  <p>适用场景：审批拒绝后退回修改、条件不满足时走备选流程</p>
                </div>
              </div>
              <p><strong>配置方式：</strong></p>
              <ol>
                <li>点击选中从审批/会签/条件节点拉出的连线</li>
                <li>在右侧属性面板的"分支类型"下拉框中选择"批准路径"或"拒绝路径"</li>
                <li>同一起始节点的两条出边，必须恰好一条选"批准路径"、一条选"拒绝路径"</li>
              </ol>
              <p><strong>关于连线标签：</strong></p>
              <ul>
                <li>标签仅供画布上显示，可以随意命名（如"经理通过"、"驳回给申请人"、"金额&gt;1000"等）</li>
                <li>标签<strong>不参与</strong>流程路由判断，引擎只认分支类型</li>
                <li>如果不设标签，画布上会自动显示分支类型名称（批准/拒绝）</li>
              </ul>
              <p><strong>保存校验规则：</strong></p>
              <ul>
                <li>有两条出边的审批/会签/条件节点，出边必须全部设置分支类型</li>
                <li>必须恰好一条"批准路径"和一条"拒绝路径"，否则无法保存</li>
                <li>未设置分支类型的连线将导致验证失败</li>
              </ul>
            </div>

            <h3>5.4 实际应用场景</h3>
            <div className="use-cases">
              <h4>场景1：根据审批结果自动流转（无需配置表达式）</h4>
              <pre className="code-block">
{`流程：员工提交 → 部门经理审批 → 条件判断 → [批准→经理审批 / 退回→员工修改]

配置方式：
1. 条件分支节点不配置表达式（留空）
2. 选中指向"经理审批"的连线，分支类型选"批准路径"，标签可写"批准"
3. 选中指向"员工修改"的连线，分支类型选"拒绝路径"，标签可写"退回"
4. 系统根据部门经理的批准/拒绝操作，沿对应分支类型自动流转`}
              </pre>

              <h4>场景2：根据请假天数自动判断审批级别</h4>
              <pre className="code-block">
{`流程：员工提交 → 条件判断 → [≤3天→部门经理审批 / >3天→总监审批]

配置方式：
1. 在条件表达式中填写：days <= 3
2. 选中指向"部门经理"的连线，分支类型选"批准路径"，标签可写"≤3天"
3. 选中指向"总监"的连线，分支类型选"拒绝路径"，标签可写">3天"
4. 系统自动计算请假天数，沿对应分支类型选择路径`}
              </pre>

              <h4>场景3：多级审批支持退回</h4>
              <pre className="code-block">
{`流程：员工 → 部门经理 → 条件1 → [批准→经理 / 退回→员工]
                      → 经理 → 条件2 → [批准→通知 / 退回→部门经理]

配置方式：
1. 每个条件分支都不配置表达式
2. 每个条件节点的出边：指向下一级的选"批准路径"，指回上级的选"拒绝路径"
3. 标签可以自由命名，如"经理通过"、"驳回给申请人"
4. 退回的连线指回到上一级节点，支持循环审批直到最终批准`}
              </pre>
            </div>
          </div>
        </section>

        <section className="help-section">
          <h2>六、审批节点操作配置</h2>
          <div className="approval-config-detail">
            <h3>6.1 操作类型说明</h3>
            <table className="config-table">
              <thead>
                <tr>
                  <th>操作类型</th>
                  <th>按钮1</th>
                  <th>按钮2</th>
                  <th>默认意见标题</th>
                  <th>适用场景</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><strong>审批</strong></td>
                  <td>批准（绿色）</td>
                  <td>拒绝（红色）</td>
                  <td>处理意见</td>
                  <td>正常审批环节，需要批准或拒绝</td>
                </tr>
                <tr>
                  <td><strong>提交申请</strong></td>
                  <td>提交（绿色）</td>
                  <td>- 不显示 -</td>
                  <td>申请理由</td>
                  <td>申请人发起环节，只需提交，不需要批准/拒绝</td>
                </tr>
                <tr>
                  <td><strong>审核</strong></td>
                  <td>同意（绿色）</td>
                  <td>退回（红色）</td>
                  <td>审核意见</td>
                  <td>审核环节，可以同意或退回到上一步</td>
                </tr>
                <tr>
                  <td><strong>自定义</strong></td>
                  <td>自定义文本</td>
                  <td>自定义文本</td>
                  <td>自定义文本</td>
                  <td>需要自定义按钮文字的特殊场景</td>
                </tr>
              </tbody>
            </table>

            <h3>6.2 配置示例</h3>
            <p><strong>员工请假流程的节点配置：</strong></p>
            <ul>
              <li><strong>员工提交申请</strong>节点：操作类型=提交申请，按钮=提交，意见标题=请假理由</li>
              <li><strong>部门经理审批</strong>节点：操作类型=审核，按钮=批准/退回，意见标题=审批意见</li>
              <li><strong>经理审批</strong>节点：操作类型=审核，按钮=批准/退回，意见标题=审批意见</li>
            </ul>
          </div>
        </section>

        <section className="help-section">
          <h2>七、示例流程</h2>
          <div className="examples-list">
            <div className="example-item">
              <h3>1. 简单审批流程</h3>
              <p><strong>适用场景：</strong>单级审批，如日常请假申请</p>
              <p><strong>流程：</strong>开始 → 主管审批 → 结束</p>
              <p><strong>特点：</strong>线性流程，只有一条路径</p>
            </div>
            <div className="example-item">
              <h3>2. 条件分支流程</h3>
              <p><strong>适用场景：</strong>根据审批结果走不同路径</p>
              <p><strong>流程：</strong>开始 → 申请审批 → 条件判断 → [批准通知 / 拒绝通知] → 结束</p>
              <p><strong>特点：</strong>使用条件分支节点，批准走一条路，拒绝走另一条路</p>
            </div>
            <div className="example-item">
              <h3>3. 多级审批流程</h3>
              <p><strong>适用场景：</strong>需要多级领导审批，如大额报销</p>
              <p><strong>流程：</strong>开始 → 部门经理审批 → 总监审批 → 发送通知 → 结束</p>
              <p><strong>特点：</strong>串联多个审批节点，逐级审批</p>
            </div>
            <div className="example-item">
              <h3>4. 退回审批流程</h3>
              <p><strong>适用场景：</strong>审批不通过时退回到上一级或申请人</p>
              <p><strong>流程：</strong>员工提交 → 部门经理审批 → [批准→经理审批 / 退回→员工重新提交] → 经理审批 → [批准→通知 / 退回→部门经理] → 结束</p>
              <p><strong>特点：</strong>支持循环退回，审批不通过时可以返回修改后重新提交</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export default WorkflowHelp;
