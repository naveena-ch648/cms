import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { autocomplete } from '../../api/search';
import type { AutocompleteResponse } from '../../types/search';

interface SearchBarProps {
  workspaceId: string;
  value: string;
  onChange: (value: string) => void;
  onSearch: (query: string) => void;
}

export default function SearchBar({ workspaceId, value, onChange, onSearch }: SearchBarProps) {
  const navigate = useNavigate();
  const [suggestions, setSuggestions] = useState<AutocompleteResponse | null>(null);
  const [showDropdown, setShowDropdown] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchSuggestions = useCallback(async (prefix: string) => {
    if (!workspaceId || prefix.length < 1) {
      setSuggestions(null);
      return;
    }
    try {
      const result = await autocomplete(prefix, workspaceId, 5);
      setSuggestions(result);
      setShowDropdown(true);
    } catch {
      setSuggestions(null);
    }
  }, [workspaceId]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (value.length >= 1) {
      debounceRef.current = setTimeout(() => fetchSuggestions(value), 300);
    } else {
      setSuggestions(null);
      setShowDropdown(false);
    }

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [value, fetchSuggestions]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node) &&
          inputRef.current && !inputRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const allItems = [
    ...(suggestions?.files?.map((f) => ({ type: 'file' as const, ...f })) ?? []),
    ...(suggestions?.recentSearches?.map((s) => ({ type: 'recent' as const, query: s })) ?? []),
  ];

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showDropdown || allItems.length === 0) {
      if (e.key === 'Enter') {
        e.preventDefault();
        onSearch(value);
        setShowDropdown(false);
      }
      return;
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < allItems.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : allItems.length - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (selectedIndex >= 0 && selectedIndex < allItems.length) {
        const item = allItems[selectedIndex];
        if (item && item.type === 'file') {
          navigate(`/workspaces/${workspaceId}?file=${item.fileUuid}`);
        } else if (item && item.type === 'recent') {
          onChange(item.query);
          onSearch(item.query);
        }
      } else {
        onSearch(value);
      }
      setShowDropdown(false);
    } else if (e.key === 'Escape') {
      setShowDropdown(false);
    }
  };

  const handleItemClick = (item: typeof allItems[number]) => {
    if (!item) return;
    if (item.type === 'file') {
      navigate(`/workspaces/${workspaceId}?file=${item.fileUuid}`);
    } else {
      onChange(item.query);
      onSearch(item.query);
    }
    setShowDropdown(false);
  };

  return (
    <div style={{ position: 'relative', flex: 1 }}>
      <input
        ref={inputRef}
        type="text"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setSelectedIndex(-1);
        }}
        onFocus={() => { if (suggestions) setShowDropdown(true); }}
        onKeyDown={handleKeyDown}
        placeholder="Search files..."
        style={{ width: '100%', padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '14px', outline: 'none' }}
      />

      {showDropdown && allItems.length > 0 && (
        <div
          ref={dropdownRef}
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            marginTop: '4px',
            background: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '6px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            zIndex: 100,
            maxHeight: '300px',
            overflowY: 'auto',
          }}
        >
          {suggestions?.files && suggestions.files.length > 0 && (
            <div>
              <div style={{ padding: '6px 12px', fontSize: '11px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>
                Files
              </div>
              {suggestions.files.map((file, i) => (
                <div
                  key={file.fileUuid}
                  onClick={() => handleItemClick({ type: 'file', ...file })}
                  style={{
                    padding: '8px 12px',
                    cursor: 'pointer',
                    background: selectedIndex === i ? '#f3f4f6' : 'transparent',
                    fontSize: '13px',
                  }}
                >
                  <div style={{ fontWeight: 500 }}>{file.fileName}</div>
                  <div style={{ fontSize: '11px', color: '#6b7280' }}>{file.folderPath}</div>
                </div>
              ))}
            </div>
          )}

          {suggestions?.recentSearches && suggestions.recentSearches.length > 0 && (
            <div>
              <div style={{ padding: '6px 12px', fontSize: '11px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', borderTop: '1px solid #e5e7eb' }}>
                Recent Searches
              </div>
              {suggestions.recentSearches.map((q, i) => {
                const idx = (suggestions.files?.length ?? 0) + i;
                return (
                  <div
                    key={q}
                    onClick={() => handleItemClick({ type: 'recent', query: q })}
                    style={{
                      padding: '8px 12px',
                      cursor: 'pointer',
                      background: selectedIndex === idx ? '#f3f4f6' : 'transparent',
                      fontSize: '13px',
                    }}
                  >
                    🕐 {q}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
