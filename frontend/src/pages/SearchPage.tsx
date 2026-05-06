import { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { search as searchApi } from '../api/search';
import SearchResults from '../components/search/SearchResults';
import SearchFilters from '../components/search/SearchFilters';
import SearchSortSelect from '../components/search/SearchSortSelect';
import SearchBar from '../components/search/SearchBar';
import type { SearchResponseData, SearchResult, SortOption } from '../types/search';

export default function SearchPage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [searchData, setSearchData] = useState<SearchResponseData | null>(null);
  const [fileType, setFileType] = useState<string[]>([]);
  const [ownerUuid, setOwnerUuid] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [sortBy, setSortBy] = useState<SortOption>('relevance');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);

  const executeSearch = useCallback(async (overrides?: { page?: number; sortBy?: SortOption; sortOrder?: 'asc' | 'desc' }) => {
    if (!workspaceId) return;

    setLoading(true);
    try {
      const result = await searchApi({
        workspaceId,
        q: query || undefined,
        fileType: fileType.length > 0 ? fileType : undefined,
        ownerUuid: ownerUuid || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        sortBy: overrides?.sortBy ?? sortBy,
        sortOrder: overrides?.sortOrder ?? sortOrder,
        page: overrides?.page ?? page,
        size: 20,
      });
      setSearchData(result);
      if (overrides?.page !== undefined) setPage(overrides.page);
    } catch (err) {
      console.error('Search failed:', err);
      setSearchData(null);
    } finally {
      setLoading(false);
    }
  }, [workspaceId, query, fileType, ownerUuid, dateFrom, dateTo, sortBy, sortOrder, page]);

  const handleSearch = (searchQuery: string) => {
    setQuery(searchQuery);
    setPage(0);
    setTimeout(() => executeSearch({ page: 0 }), 0);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    executeSearch({ page: 0 });
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    executeSearch({ page: newPage });
  };

  const handleSortChange = (newSortBy: SortOption, newSortOrder: 'asc' | 'desc') => {
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    setPage(0);
    executeSearch({ page: 0, sortBy: newSortBy, sortOrder: newSortOrder });
  };

  const handleFilterChange = () => {
    setPage(0);
    executeSearch({ page: 0 });
  };

  const handleResultClick = (result: SearchResult) => {
    navigate(`/workspaces/${workspaceId}?folder=${result.folderUuid}&file=${result.fileUuid}`);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <header style={{ padding: '16px 24px', borderBottom: '1px solid #e5e7eb', background: '#fff' }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '12px', alignItems: 'center', maxWidth: '800px' }}>
          <SearchBar
            workspaceId={workspaceId || ''}
            value={query}
            onChange={setQuery}
            onSearch={handleSearch}
          />
          <button
            type="submit"
            style={{ padding: '8px 16px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 500, whiteSpace: 'nowrap' }}
          >
            Search
          </button>
        </form>
      </header>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <aside style={{ width: '260px', borderRight: '1px solid #e5e7eb', overflowY: 'auto', padding: '16px', background: '#f9fafb' }}>
          <SearchFilters
            fileType={fileType}
            ownerUuid={ownerUuid}
            dateFrom={dateFrom}
            dateTo={dateTo}
            onFileTypeChange={setFileType}
            onOwnerChange={setOwnerUuid}
            onDateFromChange={setDateFrom}
            onDateToChange={setDateTo}
            onApply={handleFilterChange}
          />
        </aside>

        <main style={{ flex: 1, overflowY: 'auto' }}>
          <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '8px 16px', borderBottom: '1px solid #e5e7eb' }}>
            <SearchSortSelect
              sortBy={sortBy}
              sortOrder={sortOrder}
              onChange={handleSortChange}
            />
          </div>
          <SearchResults
            results={searchData?.results ?? []}
            pagination={searchData?.pagination ?? null}
            loading={loading}
            query={query}
            onPageChange={handlePageChange}
            onResultClick={handleResultClick}
          />
        </main>
      </div>
    </div>
  );
}
