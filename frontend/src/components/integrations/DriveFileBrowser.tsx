import { useState, useEffect } from 'react';
import { integrationsApi, DriveItem } from '../../api/integrations';

interface DriveFileBrowserProps {
  onSelect: (items: DriveItem[]) => void;
  onCancel: () => void;
  multiSelect?: boolean;
}

export default function DriveFileBrowser({ onSelect, onCancel, multiSelect = true }: DriveFileBrowserProps) {
  const [items, setItems] = useState<DriveItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentFolderId, setCurrentFolderId] = useState<string | undefined>(undefined);
  const [folderStack, setFolderStack] = useState<{ id: string | undefined; name: string }[]>([
    { id: undefined, name: 'My Drive' }
  ]);
  const [selectedItems, setSelectedItems] = useState<DriveItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadFiles();
  }, [currentFolderId]);

  const loadFiles = async () => {
    setLoading(true);
    try {
      const data = await integrationsApi.browseDrive({
        folderId: currentFolderId,
        query: searchQuery || undefined,
      });
      setItems(data.items);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    loadFiles();
  };

  const handleFolderOpen = (folder: DriveItem) => {
    setFolderStack(prev => [...prev, { id: folder.id, name: folder.name }]);
    setCurrentFolderId(folder.id);
    setSelectedItems([]);
  };

  const handleNavigateBack = (index: number) => {
    const newStack = folderStack.slice(0, index + 1);
    setFolderStack(newStack);
    setCurrentFolderId(newStack.length > 0 ? newStack[newStack.length - 1]!.id : 'root');
    setSelectedItems([]);
  };

  const handleToggleSelect = (item: DriveItem) => {
    if (item.isFolder) {
      handleFolderOpen(item);
      return;
    }
    if (multiSelect) {
      setSelectedItems(prev =>
        prev.some(i => i.id === item.id)
          ? prev.filter(i => i.id !== item.id)
          : [...prev, item]
      );
    } else {
      setSelectedItems([item]);
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes === 0) return '—';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
    return `${size.toFixed(1)} ${units[i]}`;
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
    }}>
      <div style={{
        background: '#fff',
        borderRadius: '16px',
        width: '680px',
        maxHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)',
      }}>
        {/* Header */}
        <div style={{ padding: '20px 24px', borderBottom: '1px solid #e2e8f0' }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>
            Browse Google Drive
          </h3>
          {/* Breadcrumb */}
          <div style={{ display: 'flex', gap: '4px', marginTop: '8px', flexWrap: 'wrap' }}>
            {folderStack.map((folder, idx) => (
              <span key={idx}>
                {idx > 0 && <span style={{ color: '#94a3b8', margin: '0 4px' }}>/</span>}
                <span
                  onClick={() => handleNavigateBack(idx)}
                  style={{
                    color: idx === folderStack.length - 1 ? '#1e293b' : '#3b82f6',
                    cursor: idx === folderStack.length - 1 ? 'default' : 'pointer',
                    fontSize: '13px',
                    fontWeight: idx === folderStack.length - 1 ? 600 : 400,
                  }}
                >
                  {folder.name}
                </span>
              </span>
            ))}
          </div>
          {/* Search */}
          <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
            <input
              type="text"
              placeholder="Search files..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              style={{
                flex: 1,
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                fontSize: '13px',
                outline: 'none',
              }}
            />
            <button
              onClick={handleSearch}
              style={{
                padding: '8px 12px',
                background: '#f1f5f9',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Search
            </button>
          </div>
        </div>

        {/* File List */}
        <div style={{ flex: 1, overflow: 'auto', padding: '8px 0' }}>
          {loading ? (
            <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>Loading...</div>
          ) : items.length === 0 ? (
            <div style={{ padding: '40px', textAlign: 'center', color: '#64748b' }}>No files found</div>
          ) : (
            items.map(item => {
              const isSelected = selectedItems.some(i => i.id === item.id);
              return (
                <div
                  key={item.id}
                  onClick={() => handleToggleSelect(item)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    padding: '10px 24px',
                    cursor: 'pointer',
                    background: isSelected ? '#eff6ff' : 'transparent',
                    borderLeft: isSelected ? '3px solid #3b82f6' : '3px solid transparent',
                  }}
                >
                  <span style={{ fontSize: '18px', marginRight: '12px' }}>
                    {item.isFolder ? '📁' : '📄'}
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      fontSize: '14px',
                      color: '#1e293b',
                      fontWeight: 500,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}>
                      {item.name}
                    </div>
                    <div style={{ fontSize: '12px', color: '#94a3b8' }}>
                      {item.isFolder ? 'Folder' : formatSize(item.size)}
                    </div>
                  </div>
                  {!item.isFolder && (
                    <div style={{
                      width: '20px',
                      height: '20px',
                      borderRadius: '4px',
                      border: isSelected ? '2px solid #3b82f6' : '2px solid #cbd5e1',
                      background: isSelected ? '#3b82f6' : 'transparent',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: '#fff',
                      fontSize: '12px',
                    }}>
                      {isSelected && '✓'}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div style={{
          padding: '16px 24px',
          borderTop: '1px solid #e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            {selectedItems.length} file{selectedItems.length !== 1 ? 's' : ''} selected
          </span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={onCancel}
              style={{
                padding: '8px 16px',
                background: '#f1f5f9',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              onClick={() => onSelect(selectedItems)}
              disabled={selectedItems.length === 0}
              style={{
                padding: '8px 16px',
                background: selectedItems.length > 0 ? '#3b82f6' : '#94a3b8',
                color: '#fff',
                border: 'none',
                borderRadius: '8px',
                fontSize: '13px',
                fontWeight: 500,
                cursor: selectedItems.length > 0 ? 'pointer' : 'not-allowed',
              }}
            >
              Select
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
