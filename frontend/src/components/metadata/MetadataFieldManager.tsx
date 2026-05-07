import React, { useState, useEffect } from 'react';
import { metadataApi, MetadataFieldDto, MetadataFieldRequest } from '../../api/metadata';

interface MetadataFieldManagerProps {
  workspaceId: string;
}

const MetadataFieldManager: React.FC<MetadataFieldManagerProps> = ({ workspaceId }) => {
  const [fields, setFields] = useState<MetadataFieldDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingField, setEditingField] = useState<MetadataFieldDto | null>(null);
  const [formData, setFormData] = useState<MetadataFieldRequest>({
    name: '',
    fieldType: 'TEXT',
    description: '',
    options: [],
    required: false,
    displayOrder: 0,
  });
  const [optionInput, setOptionInput] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadFields();
  }, [workspaceId]);

  const loadFields = async () => {
    try {
      setLoading(true);
      const response = await metadataApi.listFields(workspaceId);
      setFields(response.data.data);
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to load fields');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      if (editingField) {
        await metadataApi.updateField(workspaceId, editingField.id, formData);
      } else {
        await metadataApi.createField(workspaceId, formData);
      }
      setShowForm(false);
      setEditingField(null);
      resetForm();
      loadFields();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to save field');
    }
  };

  const handleEdit = (field: MetadataFieldDto) => {
    setEditingField(field);
    setFormData({
      name: field.name,
      fieldType: field.fieldType,
      description: field.description || '',
      options: field.options || [],
      required: field.required,
      displayOrder: field.displayOrder,
    });
    setShowForm(true);
  };

  const handleDelete = async (field: MetadataFieldDto) => {
    if (!confirm(`Delete field "${field.name}"? Existing values will be hidden.`)) return;
    try {
      await metadataApi.deleteField(workspaceId, field.id);
      loadFields();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to delete field');
    }
  };

  const addOption = () => {
    if (optionInput.trim()) {
      setFormData(prev => ({
        ...prev,
        options: [...(prev.options || []), optionInput.trim()],
      }));
      setOptionInput('');
    }
  };

  const removeOption = (index: number) => {
    setFormData(prev => ({
      ...prev,
      options: (prev.options || []).filter((_, i) => i !== index),
    }));
  };

  const resetForm = () => {
    setFormData({
      name: '',
      fieldType: 'TEXT',
      description: '',
      options: [],
      required: false,
      displayOrder: 0,
    });
    setOptionInput('');
  };

  if (loading) return <div className="p-4">Loading metadata fields...</div>;

  return (
    <div className="p-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold">Metadata Fields</h2>
        <button
          onClick={() => { resetForm(); setEditingField(null); setShowForm(true); }}
          className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700"
        >
          Add Field
        </button>
      </div>

      {error && (
        <div className="mb-4 p-2 bg-red-100 text-red-700 rounded text-sm">{error}</div>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} className="mb-4 p-4 border rounded bg-gray-50">
          <h3 className="font-medium mb-3">{editingField ? 'Edit Field' : 'New Field'}</h3>

          <div className="grid gap-3">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <input
                type="text"
                value={formData.name}
                onChange={e => setFormData(prev => ({ ...prev, name: e.target.value }))}
                className="w-full px-3 py-1.5 border rounded text-sm"
                required
                maxLength={100}
              />
            </div>

            {!editingField && (
              <div>
                <label className="block text-sm font-medium mb-1">Type</label>
                <select
                  value={formData.fieldType}
                  onChange={e => setFormData(prev => ({ ...prev, fieldType: e.target.value }))}
                  className="w-full px-3 py-1.5 border rounded text-sm"
                >
                  <option value="TEXT">Text</option>
                  <option value="NUMBER">Number</option>
                  <option value="DATE">Date</option>
                  <option value="DROPDOWN">Dropdown</option>
                </select>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <input
                type="text"
                value={formData.description}
                onChange={e => setFormData(prev => ({ ...prev, description: e.target.value }))}
                className="w-full px-3 py-1.5 border rounded text-sm"
                maxLength={500}
              />
            </div>

            {formData.fieldType === 'DROPDOWN' && (
              <div>
                <label className="block text-sm font-medium mb-1">Options</label>
                <div className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={optionInput}
                    onChange={e => setOptionInput(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addOption(); } }}
                    className="flex-1 px-3 py-1.5 border rounded text-sm"
                    placeholder="Add option..."
                  />
                  <button type="button" onClick={addOption} className="px-3 py-1.5 bg-gray-200 rounded text-sm">
                    Add
                  </button>
                </div>
                <div className="flex flex-wrap gap-1">
                  {(formData.options || []).map((opt, i) => (
                    <span key={i} className="px-2 py-0.5 bg-blue-100 text-blue-800 rounded text-xs flex items-center gap-1">
                      {opt}
                      <button type="button" onClick={() => removeOption(i)} className="text-blue-600 hover:text-blue-800">×</button>
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={formData.required || false}
                onChange={e => setFormData(prev => ({ ...prev, required: e.target.checked }))}
                id="field-required"
              />
              <label htmlFor="field-required" className="text-sm">Required</label>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Display Order</label>
              <input
                type="number"
                value={formData.displayOrder || 0}
                onChange={e => setFormData(prev => ({ ...prev, displayOrder: parseInt(e.target.value) || 0 }))}
                className="w-20 px-3 py-1.5 border rounded text-sm"
              />
            </div>
          </div>

          <div className="flex gap-2 mt-4">
            <button type="submit" className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">
              {editingField ? 'Update' : 'Create'}
            </button>
            <button type="button" onClick={() => { setShowForm(false); setEditingField(null); }} className="px-3 py-1.5 bg-gray-200 rounded text-sm">
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="border rounded divide-y">
        {fields.length === 0 ? (
          <div className="p-4 text-center text-gray-500 text-sm">No metadata fields defined yet.</div>
        ) : (
          fields.map(field => (
            <div key={field.id} className="p-3 flex justify-between items-center">
              <div>
                <span className="font-medium text-sm">{field.name}</span>
                <span className="ml-2 px-1.5 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">{field.fieldType}</span>
                {field.required && <span className="ml-1 text-red-500 text-xs">*required</span>}
                {field.description && <p className="text-xs text-gray-500 mt-0.5">{field.description}</p>}
              </div>
              <div className="flex gap-1">
                <button onClick={() => handleEdit(field)} className="px-2 py-1 text-sm text-blue-600 hover:bg-blue-50 rounded">
                  Edit
                </button>
                <button onClick={() => handleDelete(field)} className="px-2 py-1 text-sm text-red-600 hover:bg-red-50 rounded">
                  Delete
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default MetadataFieldManager;
