import React, { useState, useEffect, useRef } from 'react';
import { tagsApi, TagDto } from '../../api/tags';

interface TagInputProps {
  fileId: string;
  workspaceId: string;
}

const TagInput: React.FC<TagInputProps> = ({ fileId, workspaceId }) => {
  const [tags, setTags] = useState<TagDto[]>([]);
  const [input, setInput] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadTags();
  }, [fileId]);

  const loadTags = async () => {
    try {
      setLoading(true);
      const res = await tagsApi.getFileTags(fileId);
      setTags(res.data.data);
    } catch (err: any) {
      setError('Failed to load tags');
    } finally {
      setLoading(false);
    }
  };

  const fetchSuggestions = async (prefix: string) => {
    if (prefix.length < 1) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    try {
      const res = await tagsApi.autocomplete(workspaceId, prefix);
      const existingNames = tags.map(t => t.name);
      const filtered = res.data.data.filter(s => !existingNames.includes(s));
      setSuggestions(filtered);
      setShowSuggestions(filtered.length > 0);
    } catch {
      setSuggestions([]);
    }
  };

  const handleInputChange = (value: string) => {
    setInput(value);
    fetchSuggestions(value);
  };

  const addTag = async (tagName: string) => {
    const normalized = tagName.trim().toLowerCase();
    if (!normalized) return;
    setError('');

    try {
      await tagsApi.addTags(fileId, [normalized]);
      setInput('');
      setSuggestions([]);
      setShowSuggestions(false);
      loadTags();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to add tag');
    }
  };

  const removeTag = async (tagName: string) => {
    setError('');
    try {
      await tagsApi.removeTag(fileId, tagName);
      loadTags();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to remove tag');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag(input);
    }
  };

  if (loading) return <div className="text-xs text-gray-500 p-2">Loading tags...</div>;

  return (
    <div className="border-t mt-3 pt-3">
      <h4 className="text-sm font-medium mb-2">Tags</h4>

      {error && <div className="text-xs text-red-600 mb-1">{error}</div>}

      <div className="flex flex-wrap gap-1 mb-2">
        {tags.map(tag => (
          <span key={tag.name} className="px-2 py-0.5 bg-blue-100 text-blue-800 rounded text-xs flex items-center gap-1">
            {tag.name}
            <button onClick={() => removeTag(tag.name)} className="text-blue-600 hover:text-blue-900 font-bold">×</button>
          </span>
        ))}
      </div>

      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          value={input}
          onChange={e => handleInputChange(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => input && fetchSuggestions(input)}
          onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
          placeholder="Add tag..."
          className="w-full px-2 py-1 border rounded text-sm"
          maxLength={50}
        />
        {showSuggestions && suggestions.length > 0 && (
          <div className="absolute z-10 w-full mt-1 bg-white border rounded shadow-lg max-h-32 overflow-y-auto">
            {suggestions.map(s => (
              <button
                key={s}
                onMouseDown={(e) => { e.preventDefault(); addTag(s); }}
                className="w-full text-left px-2 py-1 text-sm hover:bg-blue-50"
              >
                {s}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TagInput;
