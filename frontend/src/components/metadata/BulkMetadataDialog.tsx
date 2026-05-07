import { useState, useEffect } from 'react';
import { metadataApi } from '../../api/metadata';
import { tagsApi } from '../../api/tags';

interface MetadataField {
  id: string;
  name: string;
  fieldType: string;
  options?: string[];
}

interface BulkMetadataDialogProps {
  fileIds: string[];
  workspaceId: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function BulkMetadataDialog({ fileIds, workspaceId, onClose, onSuccess }: BulkMetadataDialogProps) {
  const [fields, setFields] = useState<MetadataField[]>([]);
  const [metadataValues, setMetadataValues] = useState<Record<string, string>>({});
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'metadata' | 'tags'>('metadata');

  useEffect(() => {
    metadataApi.listFields(workspaceId).then(res => {
      setFields(res.data?.data || []);
    }).catch(() => {});
  }, [workspaceId]);

  const handleMetadataChange = (fieldId: string, value: string) => {
    if (value) {
      setMetadataValues({ ...metadataValues, [fieldId]: value });
    } else {
      const updated = { ...metadataValues };
      delete updated[fieldId];
      setMetadataValues(updated);
    }
  };

  const addTag = () => {
    const tag = tagInput.trim();
    if (tag && !tags.includes(tag) && tags.length < 20) {
      setTags([...tags, tag]);
      setTagInput('');
    }
  };

  const removeTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag));
  };

  const handleApply = async () => {
    setLoading(true);
    setError(null);
    try {
      if (activeTab === 'metadata' && Object.keys(metadataValues).length > 0) {
        const values = Object.entries(metadataValues).map(([fieldId, value]) => ({
          fieldId,
          value,
        }));
        await metadataApi.bulkUpdateMetadata({ fileIds, values });
      }
      if (activeTab === 'tags' && tags.length > 0) {
        await tagsApi.bulkAddTags({ fileIds, tags });
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Bulk operation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: '#fff', borderRadius: 12, padding: 24, width: 480, maxHeight: '80vh', overflowY: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={{ margin: 0 }}>Bulk Edit ({fileIds.length} files)</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer' }}>✕</button>
        </div>

        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <button
            onClick={() => setActiveTab('metadata')}
            style={{ padding: '6px 16px', cursor: 'pointer', borderRadius: 4, border: '1px solid #ddd', background: activeTab === 'metadata' ? '#1976d2' : '#fff', color: activeTab === 'metadata' ? '#fff' : '#333' }}
          >
            Metadata
          </button>
          <button
            onClick={() => setActiveTab('tags')}
            style={{ padding: '6px 16px', cursor: 'pointer', borderRadius: 4, border: '1px solid #ddd', background: activeTab === 'tags' ? '#1976d2' : '#fff', color: activeTab === 'tags' ? '#fff' : '#333' }}
          >
            Tags
          </button>
        </div>

        {activeTab === 'metadata' && (
          <div>
            {fields.length === 0 ? (
              <p style={{ color: '#888' }}>No metadata fields defined for this workspace.</p>
            ) : (
              fields.map(field => (
                <div key={field.id} style={{ marginBottom: 12 }}>
                  <label style={{ fontSize: 13, fontWeight: 500, display: 'block', marginBottom: 4 }}>{field.name}</label>
                  {field.fieldType === 'DROPDOWN' && field.options ? (
                    <select
                      value={metadataValues[field.id] || ''}
                      onChange={e => handleMetadataChange(field.id, e.target.value)}
                      style={{ width: '100%', padding: '6px 8px', border: '1px solid #ddd', borderRadius: 4 }}
                    >
                      <option value="">-- Leave unchanged --</option>
                      {field.options.map(opt => (
                        <option key={opt} value={opt}>{opt}</option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type={field.fieldType === 'NUMBER' ? 'number' : field.fieldType === 'DATE' ? 'date' : 'text'}
                      value={metadataValues[field.id] || ''}
                      onChange={e => handleMetadataChange(field.id, e.target.value)}
                      placeholder={`Set ${field.name} for all selected files`}
                      style={{ width: '100%', padding: '6px 8px', border: '1px solid #ddd', borderRadius: 4 }}
                    />
                  )}
                </div>
              ))
            )}
          </div>
        )}

        {activeTab === 'tags' && (
          <div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginBottom: 8 }}>
              {tags.map(tag => (
                <span key={tag} style={{ background: '#e3f2fd', borderRadius: 12, padding: '2px 8px', fontSize: 13, display: 'flex', alignItems: 'center', gap: 4 }}>
                  {tag}
                  <button onClick={() => removeTag(tag)} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: 10, padding: 0 }}>✕</button>
                </span>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                type="text"
                value={tagInput}
                onChange={e => setTagInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { addTag(); e.preventDefault(); } }}
                placeholder="Add tag..."
                style={{ flex: 1, padding: '6px 8px', border: '1px solid #ddd', borderRadius: 4 }}
              />
              <button onClick={addTag} style={{ padding: '6px 12px', cursor: 'pointer' }}>Add</button>
            </div>
            <p style={{ fontSize: 12, color: '#888', marginTop: 4 }}>Tags will be added to all {fileIds.length} selected files.</p>
          </div>
        )}

        {error && <p style={{ color: '#d32f2f', fontSize: 13, marginTop: 8 }}>{error}</p>}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
          <button onClick={onClose} style={{ padding: '8px 16px', cursor: 'pointer', border: '1px solid #ddd', borderRadius: 4 }}>Cancel</button>
          <button
            onClick={handleApply}
            disabled={loading || (activeTab === 'metadata' && Object.keys(metadataValues).length === 0) || (activeTab === 'tags' && tags.length === 0)}
            style={{ padding: '8px 16px', cursor: 'pointer', background: '#1976d2', color: '#fff', border: 'none', borderRadius: 4, opacity: loading ? 0.6 : 1 }}
          >
            {loading ? 'Applying...' : 'Apply'}
          </button>
        </div>
      </div>
    </div>
  );
}
