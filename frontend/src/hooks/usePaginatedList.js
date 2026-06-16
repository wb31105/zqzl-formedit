import { useState, useEffect, useCallback, useRef } from 'react';
import { useNotification } from '../context/NotificationContext';
import { getErrorMessage } from '../services/api';

export default function usePaginatedList({ fetchFunction, pageSize = 10, searchParamName = null }) {
  const { showConfirm, setAlert } = useNotification();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchValue, setSearchValue] = useState('');
  const [searchInputValue, setSearchInputValue] = useState('');
  const [loadError, setLoadError] = useState('');

  const fetchFnRef = useRef(fetchFunction);
  useEffect(() => {
    fetchFnRef.current = fetchFunction;
  }, [fetchFunction]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const params = { page, size: pageSize };
      if (searchParamName && searchValue) {
        params[searchParamName] = searchValue;
      }
      const response = await fetchFnRef.current(params);
      const data = response.data;
      setItems(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error('加载列表失败:', error);
      setLoadError(getErrorMessage(error, '加载列表失败'));
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, searchValue, searchParamName]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const reload = useCallback(() => {
    fetchData();
  }, [fetchData]);

  const handleSearch = useCallback(() => {
    setPage(0);
    setSearchValue(searchInputValue);
  }, [searchInputValue]);

  const resetSearch = useCallback(() => {
    setSearchInputValue('');
    setSearchValue('');
    setPage(0);
  }, []);

  const deleteItem = useCallback(async (deleteFn, id, confirmMsg, confirmTitle, successMsg) => {
    const title = successMsg ? confirmTitle : '确认删除';
    const msg = successMsg ? confirmMsg : confirmMsg;
    const successMessage = successMsg || confirmTitle;
    const confirmed = await showConfirm(msg, title);
    if (confirmed) {
      try {
        await deleteFn(id);
        setAlert('success', successMessage, 3000);
        reload();
      } catch (error) {
        console.error('删除失败:', error);
        setAlert('error', '删除失败: ' + getErrorMessage(error));
      }
    }
  }, [showConfirm, setAlert, reload]);

  const renderPagination = useCallback(() => {
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
  }, [page, totalPages, totalElements]);

  return {
    items,
    loading,
    page,
    setPage,
    pageSize,
    totalPages,
    totalElements,
    searchInput: searchInputValue,
    setSearchInput: setSearchInputValue,
    loadError,
    reload,
    handleSearch,
    resetSearch,
    renderPagination,
    deleteItem,
  };
}
