import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { formApi, getErrorMessage } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import PageError from '../components/PageError';
import usePaginatedList from '../hooks/usePaginatedList';

function FormList() {
  const navigate = useNavigate();
  const { clearAlert } = useNotification();
  const list = usePaginatedList({ fetchFunction: formApi.getAllForms, pageSize: 10, searchParamName: 'name' });

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const handleDelete = async (form, e) => {
    e.stopPropagation();
    await list.deleteItem(formApi.deleteForm, form.id, '确定要删除这个表单吗？删除后无法恢复。', '删除表单', '表单删除成功');
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const handleSearchKeyDown = (e) => {
    if (e.key === 'Enter') {
      list.handleSearch();
    }
  };

  if (list.loading && list.items.length === 0) {
    return <div className="form-list-container">加载中...</div>;
  }

  if (list.loadError) {
    return (
      <div className="form-list-container">
        <PageError
          title="加载失败"
          message={list.loadError}
          onRetry={list.reload}
          backTo="/"
          backText="返回首页"
        />
      </div>
    );
  }

  return (
    <div className="form-list-container">
      <div className="page-header">
        <h1>表单管理</h1>
        <button className="btn btn-primary" onClick={() => navigate('/editor/new')}>
          + 新建表单
        </button>
      </div>

      <div className="search-bar">
        <input
          type="text"
          className="search-input"
          placeholder="搜索表单名称..."
          value={list.searchInput}
          onChange={(e) => list.setSearchInput(e.target.value)}
          onKeyDown={handleSearchKeyDown}
        />
        <button className="btn btn-primary" onClick={list.handleSearch}>
          搜索
        </button>
        {list.searchInput && (
          <button className="btn btn-default" onClick={list.resetSearch}>
            重置
          </button>
        )}
      </div>

      <div className="form-list">
        {list.items.map((form) => (
          <div key={form.id} className="form-card">
            <h3>{form.name}</h3>
            <p>{form.description || '暂无描述'}</p>
            <div className="form-meta">
              <div>字段数量: {form.fields?.length || 0}</div>
              <div>更新时间: {formatDate(form.updatedAt)}</div>
            </div>
            <div className="form-actions">
              <button className="btn btn-default" onClick={() => navigate(`/editor/${form.id}`)}>
                编辑
              </button>
              <button className="btn btn-primary" onClick={() => navigate(`/preview/${form.id}`)}>
                预览
              </button>
              <button className="btn btn-danger" onClick={(e) => handleDelete(form, e)}>
                删除
              </button>
            </div>
          </div>
        ))}
      </div>

      {list.items.length === 0 && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          {list.searchInput ? '未找到匹配的表单' : '暂无表单，点击"新建表单"创建'}
        </div>
      )}

      {list.renderPagination()}
    </div>
  );
}

export default FormList;
