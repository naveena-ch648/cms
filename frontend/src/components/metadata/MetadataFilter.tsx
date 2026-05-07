import { useState, useEffect, useCallback } from 'react';
import { metadataApi } from '../../api/metadata';
import { tagsApi } from '../../api/tags';

interface MetadataField {
  id: string;
  name: string;
  fieldType: string;
  options?: string[];
}

interface FilterState {
  tags: string[];
  metadataFilters: Record<string, string>;
}

interface MetadataFilterProps {
  workspaceId: string;
  onFilterChange: (filters: FilterState) => void;
}

export default function MetadataFilter({ workspaceId, onFilterChange }: MetadataFilterProps) {
  const [fields, setFields] = useState<MetadataField[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [metadataValues, setMetadataValues] = useState<Record<string, string>>({});
  const [tagInput, setTagInput] = useState('');
  const [tagSuggestions, setTagSuggestions] = useState<string[]>([]);

  useEffect(() => {
    metadataApi.listFields(workspaceId).then(res => {
      setFields(res.data?.data || []);
    }).catch(() => {});
  }, [workspaceId]);

  useEffect(() => {
    onFilterChange({ tags: selectedTags, metadataFilters: metadataValues });
  }, [selectedTags, metadataValues, onFilterChange]);

  const handleTagInputChange = useCallback(async (value: string) => {
    setTagInput(value);
    if (value.length >= 2) {
      try {
        const res = await tagsApi.autocomplete(workspaceId, value);
        setTagSuggestions(res.data?.data || []);
      } catch {
        setTagSuggestions([]);
      }
    } else {
      setTagSuggestions([]);
    }
  }, [workspaceId]);

  const addTag = (tag: string) => {
    if (!selectedTags.includes(tag)) {
      setSelectedTags([...selectedTags, tag]);
    }
    setTagInput('');
    setTagSuggestions([]);
  };

  const removeTag = (tag: string) => {
    setSelectedTags(selectedTags.filter(t => t !== tag));
  };

  const setMetadataFilter = (fieldName: string, value: string) => {
    if (value) {
      setMetadataValues({ ...metadataValues, [fieldName]: value });
    } else {
      const updated = { ...metadataValues };
      delete updated[fieldName];
      setMetadataValues(updated);
    }
  };

  const clearAll = () => {
    setSelectedTags([]);
    setMetadataValues({});
    setTagInput('');
  };

  const hasFilters = selectedTags.length > 0 || Object.keys(metadataValues).length > 0;

  return (
    <div style={{ padding: 12, border: '1px solid #e0e0e0', borderRadius: 8, marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <strong style={{ fontSize: 14 }}>Filters</strong>
        {hasFilters && (
          <button onClick={clearAll} style={{ fontSize: 12, color: '#1976d2', background: 'none', border: 'none', cursor: 'pointer' }}>
            Clear all
          </button>
        )}
      </div>

      {/* Tag filter */}
      <div style={{ marginBottom: 8 }}>
        <label style={{ fontSize: 12, color: '#666' }}>Tags</label>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 4 }}>
          {selectedTags.map(tag => (
            <span key={tag} style={{ background: '#e3f2fd', borderRadius: 12, padding: '2px 8px', fontSize: 12, display: 'flex', alignItems: 'center', gap: 4 }}>
              {tag}
              <button onClick={() => removeTag(tag)} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 10, padding: 0 }}>✕</button>
            </span>
          ))}
        </div>
        <div style={{ position: 'relative' }}>
          <input
            type="text"
            value={tagInput}
            onChange={e => handleTagInputChange(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && tagInput.trim()) { addTag(tagInput.trim()); e.preventDefault(); } }}
            placeholder="Filter by tag..."
            style={{ width: '100%', padding: '4px 8px', fontSize: 12, border: '1px solid #ddd', borderRadius: 4 }}
          />
          {tagSuggestions.length > 0 && (
            <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, background: '#fff', border: '1px solid #ddd', borderRadius: 4, zIndex: 10, maxHeight: 120, overflowY: 'auto' }}>
              {tagSuggestions.map(s => (
                <div key={s} onClick={() => addTag(s)} style={{ padding: '4px 8px', fontSize: 12, cursor: 'pointer' }}
                     onMouseOver={e => (e.currentTarget.style.background = '#f5f5f5')}
                     onMouseOut={e => (e.currentTarget.style.background = '#fff')}>
                  {s}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Metadata field filters */}
      {fields.map(field => (
        <div key={field.id} style={{ marginBottom: 8 }}>
          <label style={{ fontSize: 12, color: '#666' }}>{field.name}</label>
          {field.fieldType === 'DROPDOWN' && field.options ? (
            <select
              value={metadataValues[field.name] || ''}
              onChange={e => setMetadataFilter(field.name, e.target.value)}
              style={{ width: '100%', padding: '4px 8px', fontSize: 12, border: '1px solid #ddd', borderRadius: 4 }}
            >
              <option value="">Any</option>
              {field.options.map(opt => (
                <option key={opt} value={opt}>{opt}</option>
              ))}
            </select>
          ) : (
            <input
              type={field.fieldType === 'NUMBER' ? 'number' : field.fieldType === 'DATE' ? 'date' : 'text'}
              value={metadataValues[field.name] || ''}
              onChange={e => setMetadataFilter(field.name, e.target.value)}
              placeholder={`Filter by ${field.name}...`}
              style={{ width: '100%', padding: '4px 8px', fontSize: 12, border: '1px solid #ddd', borderRadius: 4 }}
            />
          )}
        </div>
      ))}
    </div>
  );
}
