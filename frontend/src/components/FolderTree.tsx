import { useState, useCallback } from 'react';
import type { FolderTreeNode as FolderTreeNodeType } from '../types/folder';
import FolderTreeNodeComponent from './FolderTreeNode';

interface FolderTreeProps {
  folders: FolderTreeNodeType[];
  selectedFolderId: string | null;
  onSelectFolder: (folderId: string) => void;
  onCreateFolder: (parentId: string | null) => void;
  onRenameFolder: (folderId: string, currentName: string) => void;
  onDeleteFolder: (folderId: string) => void;
  onMoveFolder: (folderId: string, targetParentId: string | null) => void;
  onToggleFavorite?: (folderId: string, isFavorite: boolean) => void;
  onDiscuss?: (folderId: string) => void;
  favoriteFolderIds?: Set<string>;
  loading?: boolean;
  error?: string | null;
}

interface TreeNode extends FolderTreeNodeType {
  children: TreeNode[];
  expanded: boolean;
}

function buildTree(folders: FolderTreeNodeType[]): TreeNode[] {
  const map = new Map<string, TreeNode>();
  const roots: TreeNode[] = [];

  // Create nodes
  for (const f of folders) {
    map.set(f.id, { ...f, children: [], expanded: false });
  }

  // Build tree
  for (const f of folders) {
    const node = map.get(f.id)!;
    if (f.parentId && map.has(f.parentId)) {
      map.get(f.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  }

  // Sort children by sortOrder
  const sortChildren = (nodes: TreeNode[]) => {
    nodes.sort((a, b) => a.sortOrder - b.sortOrder);
    nodes.forEach(n => sortChildren(n.children));
  };
  sortChildren(roots);

  return roots;
}

function isDescendant(folders: FolderTreeNodeType[], folderId: string, potentialAncestorId: string): boolean {
  let current = folders.find(f => f.id === folderId);
  while (current && current.parentId) {
    if (current.parentId === potentialAncestorId) return true;
    current = folders.find(f => f.id === current!.parentId);
  }
  return false;
}

export default function FolderTree({
  folders,
  selectedFolderId,
  onSelectFolder,
  onCreateFolder,
  onRenameFolder,
  onDeleteFolder,
  onMoveFolder,
  onToggleFavorite,
  onDiscuss,
  favoriteFolderIds,
  loading,
  error,
}: FolderTreeProps) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [dragSourceId, setDragSourceId] = useState<string | null>(null);

  const tree = buildTree(folders);

  const toggleExpand = useCallback((id: string) => {
    setExpandedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const handleDragStart = useCallback((folderId: string) => {
    setDragSourceId(folderId);
  }, []);

  const handleDrop = useCallback((targetParentId: string | null) => {
    if (!dragSourceId || dragSourceId === targetParentId) {
      setDragSourceId(null);
      return;
    }
    // Client-side circular validation
    if (targetParentId && (dragSourceId === targetParentId || isDescendant(folders, targetParentId, dragSourceId))) {
      setDragSourceId(null);
      return;
    }
    onMoveFolder(dragSourceId, targetParentId);
    setDragSourceId(null);
  }, [dragSourceId, folders, onMoveFolder]);

  const handleDragEnd = useCallback(() => {
    setDragSourceId(null);
  }, []);

  if (loading) {
    return <div style={{ padding: 16, color: '#888' }}>Loading folders...</div>;
  }

  if (error) {
    return <div style={{ padding: 16, color: '#d32f2f' }}>{error}</div>;
  }

  if (tree.length === 0) {
    return (
      <div style={{ padding: 16, color: '#888', textAlign: 'center' }}>
        <p>No folders yet</p>
        <button
          onClick={() => onCreateFolder(null)}
          style={{ padding: '6px 12px', cursor: 'pointer' }}
        >
          Create First Folder
        </button>
      </div>
    );
  }

  return (
    <div
      style={{ padding: '8px 0' }}
      onDragOver={(e) => {
        e.preventDefault();
        e.stopPropagation();
      }}
      onDrop={(e) => {
        e.preventDefault();
        handleDrop(null);
      }}
    >
      {tree.map(node => (
        <FolderTreeNodeComponent
          key={node.id}
          node={node}
          depth={0}
          selectedFolderId={selectedFolderId}
          expandedIds={expandedIds}
          dragSourceId={dragSourceId}
          onSelect={onSelectFolder}
          onToggleExpand={toggleExpand}
          onCreateFolder={onCreateFolder}
          onRenameFolder={onRenameFolder}
          onDeleteFolder={onDeleteFolder}
          onDragStart={handleDragStart}
          onDrop={handleDrop}
          onDragEnd={handleDragEnd}
          onToggleFavorite={onToggleFavorite}
          onDiscuss={onDiscuss}
          isFavorite={favoriteFolderIds?.has(node.id) ?? false}
          favoriteFolderIds={favoriteFolderIds}
        />
      ))}
    </div>
  );
}
