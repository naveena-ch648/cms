import { useState, useRef } from 'react';
import FolderContextMenu from './FolderContextMenu';

interface TreeNode {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  status: string;
  childCount: number;
  createdAt: string;
  children: TreeNode[];
}

interface FolderTreeNodeProps {
  node: TreeNode;
  depth: number;
  selectedFolderId: string | null;
  expandedIds: Set<string>;
  dragSourceId: string | null;
  onSelect: (id: string) => void;
  onToggleExpand: (id: string) => void;
  onCreateFolder: (parentId: string | null) => void;
  onRenameFolder: (folderId: string, currentName: string) => void;
  onDeleteFolder: (folderId: string) => void;
  onDragStart: (folderId: string) => void;
  onDrop: (targetParentId: string | null) => void;
  onDragEnd: () => void;
  onToggleFavorite?: (folderId: string, isFavorite: boolean) => void;
  onDiscuss?: (folderId: string) => void;
  isFavorite: boolean;
  favoriteFolderIds?: Set<string>;
}

export default function FolderTreeNode({
  node,
  depth,
  selectedFolderId,
  expandedIds,
  dragSourceId,
  onSelect,
  onToggleExpand,
  onCreateFolder,
  onRenameFolder,
  onDeleteFolder,
  onDragStart,
  onDrop,
  onDragEnd,
  onToggleFavorite,
  onDiscuss,
  isFavorite,
  favoriteFolderIds,
}: FolderTreeNodeProps) {
  const [dropTarget, setDropTarget] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null);
  const nodeRef = useRef<HTMLDivElement>(null);

  const isExpanded = expandedIds.has(node.id);
  const isSelected = selectedFolderId === node.id;
  const isDragSource = dragSourceId === node.id;
  const hasChildren = node.children.length > 0 || node.childCount > 0;

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY });
  };

  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', node.id);
    onDragStart(node.id);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (dragSourceId && dragSourceId !== node.id) {
      setDropTarget(true);
    }
  };

  const handleDragLeave = () => {
    setDropTarget(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDropTarget(false);
    onDrop(node.id);
  };

  // Touch support for mobile drag-drop
  const touchStartPos = useRef<{ x: number; y: number } | null>(null);

  const handleTouchStart = (e: React.TouchEvent) => {
    const touch = e.touches[0];
    if (touch) {
      touchStartPos.current = { x: touch.clientX, y: touch.clientY };
    }
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    touchStartPos.current = null;
    if (dragSourceId) {
      const touch = e.changedTouches[0];
      if (touch) {
        const element = document.elementFromPoint(touch.clientX, touch.clientY);
        const targetId = element?.closest('[data-folder-id]')?.getAttribute('data-folder-id');
        if (targetId && targetId !== dragSourceId) {
          onDrop(targetId);
        }
      }
    }
  };

  return (
    <div>
      <div
        ref={nodeRef}
        data-folder-id={node.id}
        draggable
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onDragEnd={onDragEnd}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        onContextMenu={handleContextMenu}
        onClick={() => onSelect(node.id)}
        style={{
          display: 'flex',
          alignItems: 'center',
          padding: '4px 8px',
          paddingLeft: depth * 20 + 8,
          cursor: 'pointer',
          backgroundColor: isSelected ? '#e3f2fd' : dropTarget ? '#f0f4c3' : 'transparent',
          opacity: isDragSource ? 0.5 : 1,
          borderBottom: '1px solid #f0f0f0',
          userSelect: 'none',
        }}
      >
        {hasChildren ? (
          <span
            onClick={(e) => {
              e.stopPropagation();
              onToggleExpand(node.id);
            }}
            style={{ marginRight: 4, cursor: 'pointer', fontSize: 12, width: 16, textAlign: 'center' }}
          >
            {isExpanded ? '▼' : '▶'}
          </span>
        ) : (
          <span style={{ marginRight: 4, width: 16 }} />
        )}

        <span style={{ marginRight: 6 }}>📁</span>

        <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {node.name}
        </span>

        {onToggleFavorite && (
          <span
            onClick={(e) => {
              e.stopPropagation();
              onToggleFavorite(node.id, isFavorite);
            }}
            style={{ cursor: 'pointer', marginLeft: 4, fontSize: 14 }}
            title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
          >
            {isFavorite ? '⭐' : '☆'}
          </span>
        )}

        {node.childCount > 0 && !isExpanded && (
          <span style={{ marginLeft: 4, fontSize: 11, color: '#999' }}>
            ({node.childCount})
          </span>
        )}
      </div>

      {contextMenu && (
        <FolderContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          onClose={() => setContextMenu(null)}
          onNewFolder={() => {
            setContextMenu(null);
            onCreateFolder(node.id);
          }}
          onRename={() => {
            setContextMenu(null);
            onRenameFolder(node.id, node.name);
          }}
          onDelete={() => {
            setContextMenu(null);
            onDeleteFolder(node.id);
          }}
          onDiscuss={onDiscuss ? () => {
            setContextMenu(null);
            onDiscuss(node.id);
          } : undefined}
        />
      )}

      {isExpanded && node.children.map(child => (
        <FolderTreeNode
          key={child.id}
          node={child}
          depth={depth + 1}
          selectedFolderId={selectedFolderId}
          expandedIds={expandedIds}
          dragSourceId={dragSourceId}
          onSelect={onSelect}
          onToggleExpand={onToggleExpand}
          onCreateFolder={onCreateFolder}
          onRenameFolder={onRenameFolder}
          onDeleteFolder={onDeleteFolder}
          onDragStart={onDragStart}
          onDrop={onDrop}
          onDragEnd={onDragEnd}
          onToggleFavorite={onToggleFavorite}
          onDiscuss={onDiscuss}
          isFavorite={favoriteFolderIds?.has(child.id) ?? false}
          favoriteFolderIds={favoriteFolderIds}
        />
      ))}
    </div>
  );
}
