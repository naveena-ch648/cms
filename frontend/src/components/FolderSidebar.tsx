import type {
  FolderFavorite,
  FolderRecent,
  FolderTreeNode,
} from "../types/folder";
import FolderTree from "./FolderTree";

interface FolderSidebarProps {
  folders: FolderTreeNode[];
  favorites: FolderFavorite[];
  recents: FolderRecent[];
  selectedFolderId: string | null;
  favoriteFolderIds: Set<string>;
  onSelectFolder: (folderId: string) => void;
  onCreateFolder: (parentId: string | null) => void;
  onRenameFolder: (folderId: string, currentName: string) => void;
  onDeleteFolder: (folderId: string) => void;
  onMoveFolder: (folderId: string, targetParentId: string | null) => void;
  onToggleFavorite: (folderId: string, isFavorite: boolean) => void;
  onDiscuss?: (folderId: string) => void;
  loading?: boolean;
  error?: string | null;
  /** When true, hides all create/rename/delete controls (viewer-only mode) */
  readOnly?: boolean;
}

export default function FolderSidebar({
  folders,
  favorites,
  recents,
  selectedFolderId,
  favoriteFolderIds,
  onSelectFolder,
  onCreateFolder,
  onRenameFolder,
  onDeleteFolder,
  onMoveFolder,
  onToggleFavorite,
  onDiscuss,
  loading,
  error,
  readOnly = false,
}: FolderSidebarProps) {
  return (
    <div
      style={{
        width: 280,
        borderRight: "1px solid #e0e0e0",
        height: "100%",
        overflowY: "auto",
      }}
    >
      {/* Favorites Section */}
      {favorites.length > 0 && (
        <div style={{ borderBottom: "1px solid #e0e0e0" }}>
          <div
            style={{
              padding: "8px 12px",
              fontWeight: 600,
              fontSize: 12,
              color: "#666",
              textTransform: "uppercase",
            }}
          >
            ⭐ Favorites
          </div>
          {favorites.map((fav) => (
            <div
              key={fav.id}
              onClick={() => onSelectFolder(fav.id)}
              style={{
                padding: "6px 12px 6px 24px",
                cursor: "pointer",
                backgroundColor:
                  selectedFolderId === fav.id ? "#e3f2fd" : "transparent",
                fontSize: 14,
                display: "flex",
                alignItems: "center",
              }}
            >
              <span style={{ marginRight: 6 }}>📁</span>
              <span
                style={{
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {fav.name}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Recents Section */}
      {recents.length > 0 && (
        <div style={{ borderBottom: "1px solid #e0e0e0" }}>
          <div
            style={{
              padding: "8px 12px",
              fontWeight: 600,
              fontSize: 12,
              color: "#666",
              textTransform: "uppercase",
            }}
          >
            🕐 Recent
          </div>
          {recents.map((recent) => (
            <div
              key={recent.id}
              onClick={() => onSelectFolder(recent.id)}
              style={{
                padding: "6px 12px 6px 24px",
                cursor: "pointer",
                backgroundColor:
                  selectedFolderId === recent.id ? "#e3f2fd" : "transparent",
                fontSize: 14,
                display: "flex",
                alignItems: "center",
              }}
            >
              <span style={{ marginRight: 6 }}>📁</span>
              <span
                style={{
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {recent.name}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Folder Tree Section */}
      <div>
        <div
          style={{
            padding: "8px 12px",
            fontWeight: 600,
            fontSize: 12,
            color: "#666",
            textTransform: "uppercase",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <span>Folders</span>
          {!readOnly && (
            <button
              onClick={() => onCreateFolder(null)}
              style={{
                border: "none",
                background: "none",
                cursor: "pointer",
                fontSize: 16,
                color: "#1976d2",
                padding: "0 4px",
              }}
              title="New root folder"
            >
              +
            </button>
          )}
        </div>
        <FolderTree
          folders={folders}
          selectedFolderId={selectedFolderId}
          onSelectFolder={onSelectFolder}
          onCreateFolder={onCreateFolder}
          onRenameFolder={onRenameFolder}
          onDeleteFolder={onDeleteFolder}
          onMoveFolder={onMoveFolder}
          onToggleFavorite={onToggleFavorite}
          onDiscuss={onDiscuss}
          favoriteFolderIds={favoriteFolderIds}
          loading={loading}
          error={error}
          readOnly={readOnly}
        />
      </div>
    </div>
  );
}
