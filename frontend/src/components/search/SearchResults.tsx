import type { SearchResult, SearchPagination } from '../../types/search';
import SearchResultItem from './SearchResultItem';

interface SearchResultsProps {
  results: SearchResult[];
  pagination: SearchPagination | null;
  loading: boolean;
  query: string;
  onPageChange: (page: number) => void;
  onResultClick?: (result: SearchResult) => void;
}

export default function SearchResults({
  results,
  pagination,
  loading,
  query,
  onPageChange,
  onResultClick,
}: SearchResultsProps) {
  if (loading) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>
        <div>Searching...</div>
      </div>
    );
  }

  if (!pagination) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>
        <p>Enter a keyword or apply filters to search files.</p>
      </div>
    );
  }

  if (results.length === 0) {
    return (
      <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>
        <p style={{ fontSize: '16px', marginBottom: '8px' }}>No results found</p>
        {query && <p style={{ fontSize: '14px' }}>Try different keywords or adjust your filters.</p>}
      </div>
    );
  }

  return (
    <div>
      <div style={{ padding: '8px 16px', fontSize: '13px', color: '#6b7280', borderBottom: '1px solid #e5e7eb' }}>
        {pagination.totalResults.toLocaleString()} result{pagination.totalResults !== 1 ? 's' : ''} found
      </div>

      <div>
        {results.map((result) => (
          <SearchResultItem key={result.fileUuid} result={result} onClick={onResultClick} />
        ))}
      </div>

      {pagination.totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', padding: '16px' }}>
          <button
            onClick={() => onPageChange(pagination.page - 1)}
            disabled={pagination.page === 0}
            style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', cursor: pagination.page === 0 ? 'not-allowed' : 'pointer', opacity: pagination.page === 0 ? 0.5 : 1 }}
          >
            Previous
          </button>
          <span style={{ fontSize: '14px', color: '#374151' }}>
            Page {pagination.page + 1} of {pagination.totalPages}
          </span>
          <button
            onClick={() => onPageChange(pagination.page + 1)}
            disabled={pagination.page >= pagination.totalPages - 1}
            style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', cursor: pagination.page >= pagination.totalPages - 1 ? 'not-allowed' : 'pointer', opacity: pagination.page >= pagination.totalPages - 1 ? 0.5 : 1 }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
