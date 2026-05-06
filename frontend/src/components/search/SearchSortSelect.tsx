import { SORT_OPTIONS } from '../../types/search';
import type { SortOption } from '../../types/search';

interface SearchSortSelectProps {
  sortBy: SortOption;
  sortOrder: 'asc' | 'desc';
  onChange: (sortBy: SortOption, sortOrder: 'asc' | 'desc') => void;
}

export default function SearchSortSelect({ sortBy, sortOrder, onChange }: SearchSortSelectProps) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
      <label style={{ fontSize: '13px', color: '#6b7280' }}>Sort by:</label>
      <select
        value={sortBy}
        onChange={(e) => onChange(e.target.value as SortOption, sortOrder)}
        style={{ padding: '4px 8px', border: '1px solid #d1d5db', borderRadius: '4px', fontSize: '13px' }}
      >
        {SORT_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      <button
        onClick={() => onChange(sortBy, sortOrder === 'asc' ? 'desc' : 'asc')}
        style={{ padding: '4px 8px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', fontSize: '13px', background: '#fff' }}
        title={sortOrder === 'asc' ? 'Ascending' : 'Descending'}
      >
        {sortOrder === 'asc' ? '↑' : '↓'}
      </button>
    </div>
  );
}
