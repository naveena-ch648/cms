import { FILE_TYPE_OPTIONS } from '../../types/search';

interface SearchFiltersProps {
  fileType: string[];
  ownerUuid: string;
  dateFrom: string;
  dateTo: string;
  onFileTypeChange: (types: string[]) => void;
  onOwnerChange: (owner: string) => void;
  onDateFromChange: (date: string) => void;
  onDateToChange: (date: string) => void;
  onApply: () => void;
}

export default function SearchFilters({
  fileType,
  ownerUuid,
  dateFrom,
  dateTo,
  onFileTypeChange,
  onOwnerChange,
  onDateFromChange,
  onDateToChange,
  onApply,
}: SearchFiltersProps) {
  const handleTypeToggle = (type: string) => {
    const updated = fileType.includes(type)
      ? fileType.filter((t) => t !== type)
      : [...fileType, type];
    onFileTypeChange(updated);
  };

  const handleClearAll = () => {
    onFileTypeChange([]);
    onOwnerChange('');
    onDateFromChange('');
    onDateToChange('');
    onApply();
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>Filters</h3>
        <button
          onClick={handleClearAll}
          style={{ fontSize: '12px', color: '#2563eb', background: 'none', border: 'none', cursor: 'pointer' }}
        >
          Clear all
        </button>
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ fontSize: '12px', fontWeight: 500, color: '#374151', display: 'block', marginBottom: '6px' }}>
          File Type
        </label>
        {FILE_TYPE_OPTIONS.map((type) => (
          <label key={type} style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px', fontSize: '13px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={fileType.includes(type)}
              onChange={() => handleTypeToggle(type)}
            />
            {type.charAt(0).toUpperCase() + type.slice(1)}
          </label>
        ))}
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ fontSize: '12px', fontWeight: 500, color: '#374151', display: 'block', marginBottom: '6px' }}>
          Owner
        </label>
        <input
          type="text"
          value={ownerUuid}
          onChange={(e) => onOwnerChange(e.target.value)}
          placeholder="Owner UUID"
          style={{ width: '100%', padding: '6px 8px', border: '1px solid #d1d5db', borderRadius: '4px', fontSize: '13px' }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ fontSize: '12px', fontWeight: 500, color: '#374151', display: 'block', marginBottom: '6px' }}>
          Date From
        </label>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => onDateFromChange(e.target.value)}
          style={{ width: '100%', padding: '6px 8px', border: '1px solid #d1d5db', borderRadius: '4px', fontSize: '13px' }}
        />
      </div>

      <div style={{ marginBottom: '16px' }}>
        <label style={{ fontSize: '12px', fontWeight: 500, color: '#374151', display: 'block', marginBottom: '6px' }}>
          Date To
        </label>
        <input
          type="date"
          value={dateTo}
          onChange={(e) => onDateToChange(e.target.value)}
          style={{ width: '100%', padding: '6px 8px', border: '1px solid #d1d5db', borderRadius: '4px', fontSize: '13px' }}
        />
      </div>

      <button
        onClick={onApply}
        style={{ width: '100%', padding: '8px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: 500 }}
      >
        Apply Filters
      </button>
    </div>
  );
}
