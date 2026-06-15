import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { formApi } from '../services/api';
import { useNotification } from '../context/NotificationContext';
import PageError from '../components/PageError';

function FormList() {
  const [forms, setForms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchName, setSearchName] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loadError, setLoadError] = useState('');
  const navigate = useNavigate();
  const { showConfirm, setAlert, clearAlert } = useNotification();

  useEffect(() => {
    loadForms();
  }, [page, searchName]);

  useEffect(() => {
    return () => clearAlert();
  }, []);

  const loadForms = async () => {
    setLoading(true);
    setLoadError('');
    clearAlert();
    try {
      const params = { page, size: pageSize };
      if (searchName) {
        params.name = searchName;
      }
      const response = await formApi.getAllForms(params);
      const data = response.data;
      setForms(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载表单列表失败:', error);
      const msg = '加载表单列表失败: ' + (error.response?.data?.error || error.message || '网络错误');
      setLoadError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    setSearchName(searchInput);
  };

  const handleSearchKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleResetSearch = () => {
    setSearchInput('');
    setSearchName('');
    setPage(0);
  };

  const handleDelete = async (id, e) => {
    e.stopPropagation();
    const confirmed = await showConfirm('确定要删除这个表单吗？删除后无法恢复。', '删除表单');
    if (confirmed) {
      try {
        await formApi.deleteForm(id);
        setAlert('success', '表单删除成功', 3000);
        loadForms();
      } catch (error) {
        console.error('删除表单失败:', error);
        setAlert('error', '删除失败: ' + (error.response?.data?.error || error.message));
      }
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, page - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    return (
      <div className="pagination">
        <button
          className="btn btn-default btn-page"
          disabled={page === 0}
          onClick={() => setPage(0)}
        >
          首页
        </button>
        <button
          className="btn btn-default btn-page"
          disabled={page === 0}
          onClick={() => setPage(page - 1)}
        >
          上一页
        </button>
        {pages.map((p) => (
          <button
            key={p}
            className={`btn btn-page ${p === page ? 'btn-primary' : 'btn-default'}`}
            onClick={() => setPage(p)}
          >
            {p + 1}
          </button>
        ))}
        <button
          className="btn btn-default btn-page"
          disabled={page >= totalPages - 1}
          onClick={() => setPage(page + 1)}
        >
          下一页
        </button>
        <button
          className="btn btn-default btn-page"
          disabled={page >= totalPages - 1}
          onClick={() => setPage(totalPages - 1)}
        >
          末页
        </button>
        <span className="pagination-info">
          共 {totalElements} 条，第 {page + 1}/{totalPages} 页
        </span>
      </div>
    );
  };

  if (loading && forms.length === 0) {
    return <div className="form-list-container">加载中...</div>;
  }

  if (loadError) {
    return (
      <div className="form-list-container">
        <PageError
          title="加载失败"
          message={loadError}
          onRetry={loadForms}
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
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={handleSearchKeyDown}
        />
        <button className="btn btn-primary" onClick={handleSearch}>
          搜索
        </button>
        {searchName && (
          <button className="btn btn-default" onClick={handleResetSearch}>
            重置
          </button>
        )}
      </div>

      <div className="form-list">
        {forms.map((form) => (
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
              <button className="btn btn-danger" onClick={(e) => handleDelete(form.id, e)}>
                删除
              </button>
            </div>
          </div>
        ))}
      </div>

      {forms.length === 0 && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          {searchName ? '未找到匹配的表单' : '暂无表单，点击"新建表单"创建'}
        </div>
      )}

      {renderPagination()}
    </div>
  );
}

export default FormList;
