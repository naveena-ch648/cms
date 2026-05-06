import { useEffect, useRef } from 'react';

interface FolderContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
  onNewFolder: () => void;
  onRename: () => void;
  onDelete: () => void;
  onDiscuss?: () => void;
}

export default function FolderContextMenu({
  x,
  y,
  onClose,
  onNewFolder,
  onRename,
  onDelete,
  onDiscuss,
}: FolderContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onClose]);

  const menuItemStyle: React.CSSProperties = {
    padding: '8px 16px',
    cursor: 'pointer',
    fontSize: 14,
    whiteSpace: 'nowrap',
  };

  return (
    <div
      ref={menuRef}
      style={{
        position: 'fixed',
        top: y,
        left: x,
        backgroundColor: 'white',
        border: '1px solid #ddd',
        borderRadius: 4,
        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
        zIndex: 1000,
        minWidth: 150,
      }}
    >
      <div
        style={menuItemStyle}
        onClick={onNewFolder}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f5f5f5')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        📁 New Folder
      </div>
      <div
        style={menuItemStyle}
        onClick={onRename}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f5f5f5')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        ✏️ Rename
      </div>
      {onDiscuss && (
        <div
          style={menuItemStyle}
          onClick={onDiscuss}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#f5f5f5')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
        >
          💬 Discuss
        </div>
      )}
      <div style={{ borderTop: '1px solid #eee' }} />
      <div
        style={{ ...menuItemStyle, color: '#d32f2f' }}
        onClick={onDelete}
        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#fff5f5')}
        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
      >
        🗑️ Delete
      </div>
    </div>
  );
}
