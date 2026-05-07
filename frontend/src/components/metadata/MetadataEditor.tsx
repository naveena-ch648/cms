import React, { useState, useEffect } from 'react';
import { metadataApi, MetadataFieldDto, MetadataValueDto, MetadataValueUpdate } from '../../api/metadata';

interface MetadataEditorProps {
  fileId: string;
  workspaceId: string;
}

const MetadataEditor: React.FC<MetadataEditorProps> = ({ fileId, workspaceId }) => {
  const [fields, setFields] = useState<MetadataFieldDto[]>([]);
  const [_values, setValues] = useState<MetadataValueDto[]>([]);
  const [editValues, setEditValues] = useState<Record<string, string | number | null>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
  }, [fileId, workspaceId]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [fieldsRes, valuesRes] = await Promise.all([
        metadataApi.listFields(workspaceId),
        metadataApi.getFileMetadata(fileId),
      ]);
      setFields(fieldsRes.data.data);
      setValues(valuesRes.data.data);

      const initialValues: Record<string, string | number | null> = {};
      valuesRes.data.data.forEach(v => {
        initialValues[v.fieldId] = v.value;
      });
      setEditValues(initialValues);
      setDirty(false);
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to load metadata');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (fieldId: string, value: string | number | null) => {
    setEditValues(prev => ({ ...prev, [fieldId]: value }));
    setDirty(true);
  };

  const handleSave = async () => {
    setError('');
    setSaving(true);
    try {
      const updates: MetadataValueUpdate[] = fields
        .filter(f => editValues[f.id] !== undefined)
        .map(f => ({
          fieldId: f.id,
          value: editValues[f.id] === '' ? null : (editValues[f.id] ?? null),
        }));

      await metadataApi.updateFileMetadata(fileId, updates);
      setDirty(false);
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to save metadata');
    } finally {
      setSaving(false);
    }
  };

  const renderFieldInput = (field: MetadataFieldDto) => {
    const value = editValues[field.id] ?? '';

    switch (field.fieldType) {
      case 'TEXT':
        return (
          <input
            type="text"
            value={value as string}
            onChange={e => handleChange(field.id, e.target.value)}
            className="w-full px-2 py-1 border rounded text-sm"
            maxLength={1000}
          />
        );
      case 'NUMBER':
        return (
          <input
            type="number"
            value={value as number}
            onChange={e => handleChange(field.id, e.target.value ? parseFloat(e.target.value) : null)}
            className="w-full px-2 py-1 border rounded text-sm"
            step="any"
          />
        );
      case 'DATE':
        return (
          <input
            type="date"
            value={value as string}
            onChange={e => handleChange(field.id, e.target.value || null)}
            className="w-full px-2 py-1 border rounded text-sm"
          />
        );
      case 'DROPDOWN':
        return (
          <select
            value={value as string}
            onChange={e => handleChange(field.id, e.target.value || null)}
            className="w-full px-2 py-1 border rounded text-sm"
          >
            <option value="">— Select —</option>
            {(field.options || []).map(opt => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        );
      default:
        return null;
    }
  };

  if (loading) return <div className="text-xs text-gray-500 p-2">Loading metadata...</div>;
  if (fields.length === 0) return null;

  return (
    <div className="border-t mt-3 pt-3">
      <div className="flex justify-between items-center mb-2">
        <h4 className="text-sm font-medium">Metadata</h4>
        {dirty && (
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-2 py-0.5 bg-blue-600 text-white rounded text-xs hover:bg-blue-700 disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        )}
      </div>

      {error && (
        <div className="mb-2 p-1.5 bg-red-100 text-red-700 rounded text-xs">{error}</div>
      )}

      <div className="space-y-2">
        {fields.map(field => (
          <div key={field.id}>
            <label className="block text-xs text-gray-600 mb-0.5">
              {field.name}
              {field.required && <span className="text-red-500 ml-0.5">*</span>}
            </label>
            {renderFieldInput(field)}
          </div>
        ))}
      </div>
    </div>
  );
};

export default MetadataEditor;
