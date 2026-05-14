import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { foldersApi } from "../api/folders";
import { filesApi } from "../api/files";
import { workspacesApi } from "../api/workspaces";
import FolderSidebar from "../components/FolderSidebar";
import Breadcrumbs from "../components/Breadcrumbs";
import FileUploadZone from "../components/FileUploadZone";
import FileList from "../components/FileList";
import UploadProgressPanel from "../components/UploadProgressPanel";
import FileDetailPanel from "../components/FileDetailPanel";
import PreviewModal from "../components/preview/PreviewModal";
import CollaborationSidebar from "../components/collaboration/CollaborationSidebar";
import MetadataFieldManager from "../components/metadata/MetadataFieldManager";
import MetadataFilter from "../components/metadata/MetadataFilter";
import BulkMetadataDialog from "../components/metadata/BulkMetadataDialog";
import InternalShareDialog from "../components/InternalShareDialog";
import { useAuth } from "../contexts/AuthContext";
import type {
  FolderTreeNode,
  Folder,
  FolderFavorite,
  FolderRecent,
  BreadcrumbItem,
} from "../types/folder";
import type { FileInfo, UploadProgress } from "../types/file";

export default function WorkspacePage() {
  const { workspaceId } = useParams<{ workspaceId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [workspaceName, setWorkspaceName] = useState("");
  const [folders, setFolders] = useState<FolderTreeNode[]>([]);
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [currentFolder, setCurrentFolder] = useState<Folder | null>(null);
  const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItem[]>([]);
  const [favorites, setFavorites] = useState<FolderFavorite[]>([]);
  const [recents, setRecents] = useState<FolderRecent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [files, setFiles] = useState<FileInfo[]>([]);
  const [filesLoading, setFilesLoading] = useState(false);
  const [uploads, setUploads] = useState<UploadProgress[]>([]);
  const [selectedFile, setSelectedFile] = useState<FileInfo | null>(null);
  const [previewFile, setPreviewFile] = useState<FileInfo | null>(null);
  const [collaborationFileId, setCollaborationFileId] = useState<string | null>(
    null,
  );
  const [collaborationFolderId, setCollaborationFolderId] = useState<
    string | null
  >(null);
  const [showMetadataSettings, setShowMetadataSettings] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [_activeFilters, setActiveFilters] = useState<{
    tags: string[];
    metadataFilters: Record<string, string>;
  }>({ tags: [], metadataFilters: {} });
  const [bulkEditFileIds, setBulkEditFileIds] = useState<string[] | null>(null);
  const [shareFile, setShareFile] = useState<FileInfo | null>(null);

  const favoriteFolderIds = new Set(favorites.map((f) => f.id));

  // Only org-level Admin and Editor have manage-folders + FILE_UPLOAD permissions (V5/V7 migrations).
  // Workspace-level myRole is informational only — the backend checks org role via PermissionService.
  const orgRole = (user?.organizationRole ?? "").toLowerCase();
  const canWrite = orgRole === "admin" || orgRole === "editor";

  const loadFolders = useCallback(async () => {
    if (!workspaceId) return;
    try {
      setLoading(true);
      const res = await foldersApi.list(workspaceId);
      setFolders(res.data.data);
      setError(null);
    } catch {
      setError("Failed to load folders");
    } finally {
      setLoading(false);
    }
  }, [workspaceId]);

  const loadFavorites = useCallback(async () => {
    if (!workspaceId) return;
    try {
      const res = await foldersApi.listFavorites(workspaceId);
      setFavorites(res.data.data);
    } catch {
      // silent fail for favorites
    }
  }, [workspaceId]);

  const loadRecents = useCallback(async () => {
    if (!workspaceId) return;
    try {
      const res = await foldersApi.listRecents(workspaceId);
      setRecents(res.data.data);
    } catch {
      // silent fail for recents
    }
  }, [workspaceId]);

  const loadFiles = useCallback(async (folderId: string) => {
    try {
      setFilesLoading(true);
      const result = await filesApi.listFiles(folderId);
      setFiles(result.content);
    } catch {
      setFiles([]);
    } finally {
      setFilesLoading(false);
    }
  }, []);

  const handleFilesSelected = useCallback(
    async (selectedFiles: File[]) => {
      if (!selectedFolderId) return;
      for (const file of selectedFiles) {
        const uploadId = crypto.randomUUID();
        const progress: UploadProgress = {
          fileId: uploadId,
          fileName: file.name,
          totalBytes: file.size,
          uploadedBytes: 0,
          percentage: 0,
          status: "uploading",
          startedAt: Date.now(),
        };
        setUploads((prev) => [...prev, progress]);

        filesApi
          .uploadFile(file, selectedFolderId, {}, (pct) => {
            setUploads((prev) =>
              prev.map((u) =>
                u.fileId === uploadId
                  ? {
                      ...u,
                      percentage: pct,
                      uploadedBytes: Math.round((file.size * pct) / 100),
                    }
                  : u,
              ),
            );
          })
          .then(() => {
            setUploads((prev) =>
              prev.map((u) =>
                u.fileId === uploadId
                  ? { ...u, status: "completed", percentage: 100 }
                  : u,
              ),
            );
            if (selectedFolderId) loadFiles(selectedFolderId);
          })
          .catch((err) => {
            setUploads((prev) =>
              prev.map((u) =>
                u.fileId === uploadId
                  ? {
                      ...u,
                      status: "failed",
                      error: err.message || "Upload failed",
                    }
                  : u,
              ),
            );
          });
      }
    },
    [selectedFolderId, loadFiles],
  );

  const handleDismissUpload = useCallback((fileId: string) => {
    setUploads((prev) => prev.filter((u) => u.fileId !== fileId));
  }, []);

  useEffect(() => {
    if (!workspaceId) return;
    workspacesApi
      .get(workspaceId)
      .then((res) => {
        setWorkspaceName(res.data.data.name);
      })
      .catch(() => {});
    loadFolders();
    loadFavorites();
    loadRecents();
  }, [workspaceId, loadFolders, loadFavorites, loadRecents]);

  const handleSelectFolder = useCallback(
    async (folderId: string) => {
      if (!workspaceId) return;
      setSelectedFolderId(folderId);
      setSelectedFile(null);
      try {
        const res = await foldersApi.get(workspaceId, folderId);
        setCurrentFolder(res.data.data);
        setBreadcrumbs(res.data.data.breadcrumbs ?? []);
        loadFiles(folderId);
        // Record visit
        foldersApi
          .recordVisit(workspaceId, folderId)
          .then(() => loadRecents())
          .catch(() => {});
      } catch {
        setError("Failed to load folder");
      }
    },
    [workspaceId, loadRecents, loadFiles],
  );

  const handleNavigateBreadcrumb = useCallback(
    (folderId: string | null) => {
      if (folderId) {
        handleSelectFolder(folderId);
      } else {
        setSelectedFolderId(null);
        setCurrentFolder(null);
        setBreadcrumbs([]);
      }
    },
    [handleSelectFolder],
  );

  const handleCreateFolder = useCallback(
    async (parentId: string | null) => {
      if (!workspaceId) return;
      const name = prompt("Enter folder name:");
      if (!name?.trim()) return;
      try {
        await foldersApi.create(workspaceId, { name: name.trim(), parentId });
        await loadFolders();
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : "Failed to create folder";
        alert(message);
      }
    },
    [workspaceId, loadFolders],
  );

  const handleRenameFolder = useCallback(
    async (folderId: string, currentName: string) => {
      if (!workspaceId) return;
      const newName = prompt("Enter new name:", currentName);
      if (!newName?.trim() || newName === currentName) return;
      try {
        await foldersApi.update(workspaceId, folderId, {
          name: newName.trim(),
        });
        await loadFolders();
        if (selectedFolderId === folderId) {
          handleSelectFolder(folderId);
        }
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : "Failed to rename folder";
        alert(message);
      }
    },
    [workspaceId, loadFolders, selectedFolderId, handleSelectFolder],
  );

  const handleDeleteFolder = useCallback(
    async (folderId: string) => {
      if (!workspaceId) return;
      if (
        !confirm(
          "Are you sure you want to delete this folder and all its contents?",
        )
      )
        return;
      try {
        await foldersApi.delete(workspaceId, folderId);
        await loadFolders();
        await loadFavorites();
        if (selectedFolderId === folderId) {
          setSelectedFolderId(null);
          setCurrentFolder(null);
          setBreadcrumbs([]);
        }
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : "Failed to delete folder";
        alert(message);
      }
    },
    [workspaceId, loadFolders, loadFavorites, selectedFolderId],
  );

  const handleMoveFolder = useCallback(
    async (folderId: string, targetParentId: string | null) => {
      if (!workspaceId) return;
      try {
        await foldersApi.move(workspaceId, folderId, { targetParentId });
        await loadFolders();
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : "Failed to move folder";
        alert(message);
      }
    },
    [workspaceId, loadFolders],
  );

  const handleToggleFavorite = useCallback(
    async (folderId: string, isFavorite: boolean) => {
      if (!workspaceId) return;
      try {
        if (isFavorite) {
          await foldersApi.removeFavorite(workspaceId, folderId);
        } else {
          await foldersApi.addFavorite(workspaceId, folderId);
        }
        await loadFavorites();
      } catch {
        // silent fail
      }
    },
    [workspaceId, loadFavorites],
  );

  // Children of current folder or root
  const currentChildren = currentFolder
    ? folders.filter((f) => f.parentId === currentFolder.id)
    : folders.filter((f) => f.parentId === null);

  return (
    <div style={{ display: "flex", height: "100vh", fontFamily: "sans-serif" }}>
      <FolderSidebar
        folders={folders}
        favorites={favorites}
        recents={recents}
        selectedFolderId={selectedFolderId}
        favoriteFolderIds={favoriteFolderIds}
        onSelectFolder={handleSelectFolder}
        onCreateFolder={handleCreateFolder}
        onRenameFolder={handleRenameFolder}
        onDeleteFolder={handleDeleteFolder}
        onMoveFolder={handleMoveFolder}
        onToggleFavorite={handleToggleFavorite}
        onDiscuss={(folderId) => {
          setCollaborationFolderId(folderId);
          setCollaborationFileId(null);
        }}
        loading={loading}
        error={error}
        readOnly={!canWrite}
      />

      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <div
          style={{
            borderBottom: "1px solid #e0e0e0",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Breadcrumbs
            items={breadcrumbs}
            workspaceName={workspaceName || "Workspace"}
            onNavigate={handleNavigateBreadcrumb}
          />
          <button
            onClick={() => navigate("/")}
            style={{ marginRight: 16, padding: "4px 12px", cursor: "pointer" }}
          >
            ← Back
          </button>
        </div>

        <div style={{ flex: 1, padding: 16, overflowY: "auto" }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: 16,
            }}
          >
            <h2 style={{ margin: 0 }}>
              {currentFolder
                ? currentFolder.name
                : workspaceName || "Workspace"}
            </h2>
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                onClick={() => setShowMetadataSettings(!showMetadataSettings)}
                style={{ padding: "6px 16px", cursor: "pointer" }}
                title="Metadata settings"
              >
                ⚙️ Metadata
              </button>
              <button
                onClick={() => setShowFilters(!showFilters)}
                style={{ padding: "6px 16px", cursor: "pointer" }}
                title="Filter by metadata and tags"
              >
                🏷️ Filter
              </button>
              <button
                onClick={() => navigate(`/workspaces/${workspaceId}/search`)}
                style={{ padding: "6px 16px", cursor: "pointer" }}
                title="Search files"
              >
                🔍 Search
              </button>
              {canWrite && (
                <button
                  onClick={() => handleCreateFolder(selectedFolderId)}
                  style={{ padding: "6px 16px", cursor: "pointer" }}
                >
                  + New Folder
                </button>
              )}
            </div>
          </div>

          {selectedFolderId && canWrite && (
            <FileUploadZone onFilesSelected={handleFilesSelected} />
          )}

          {showFilters && workspaceId && (
            <MetadataFilter
              workspaceId={workspaceId}
              onFilterChange={setActiveFilters}
            />
          )}

          {showMetadataSettings && workspaceId && (
            <div
              style={{
                marginBottom: 16,
                border: "1px solid #e0e0e0",
                borderRadius: 8,
              }}
            >
              <MetadataFieldManager workspaceId={workspaceId} />
            </div>
          )}

          {selectedFolderId && files.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <FileList
                files={files}
                onFileClick={(file) => {
                  // Record this view in the user's personal recent-files history (fire-and-forget)
                  filesApi.recordView(file.id);
                  if (file.previewable) {
                    setPreviewFile(file);
                  } else {
                    setSelectedFile(file);
                  }
                  setCollaborationFileId(file.id);
                }}
                onShare={canWrite ? (file) => setShareFile(file) : undefined}
                onBulkEdit={(fileIds) => setBulkEditFileIds(fileIds)}
                loading={filesLoading}
              />
            </div>
          )}

          {currentChildren.length === 0 &&
          files.length === 0 &&
          !selectedFolderId ? (
            <div style={{ textAlign: "center", padding: 40, color: "#888" }}>
              <p>This folder is empty</p>
            </div>
          ) : currentChildren.length > 0 ? (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
                gap: 12,
                marginTop: 16,
              }}
            >
              {currentChildren.map((child) => (
                <div
                  key={child.id}
                  onClick={() => handleSelectFolder(child.id)}
                  style={{
                    padding: 16,
                    border: "1px solid #e0e0e0",
                    borderRadius: 8,
                    cursor: "pointer",
                    textAlign: "center",
                    transition: "background-color 0.15s",
                  }}
                  onMouseEnter={(e) =>
                    (e.currentTarget.style.backgroundColor = "#f5f5f5")
                  }
                  onMouseLeave={(e) =>
                    (e.currentTarget.style.backgroundColor = "transparent")
                  }
                >
                  <div style={{ fontSize: 32, marginBottom: 8 }}>📁</div>
                  <div
                    style={{
                      fontSize: 14,
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {child.name}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      </div>
      {selectedFile && (
        <FileDetailPanel
          file={selectedFile}
          onClose={() => setSelectedFile(null)}
          onDownload={(f) => filesApi.downloadFile(f.id)}
          onPreview={(f) => filesApi.getPreviewUrl(f.id)}
        />
      )}
      {collaborationFileId && user && (
        <CollaborationSidebar
          fileId={collaborationFileId}
          workspaceId={workspaceId}
          currentUserId={user.id}
          onClose={() => setCollaborationFileId(null)}
        />
      )}
      {collaborationFolderId && user && (
        <CollaborationSidebar
          folderId={collaborationFolderId}
          workspaceId={workspaceId}
          currentUserId={user.id}
          onClose={() => setCollaborationFolderId(null)}
        />
      )}
      <UploadProgressPanel uploads={uploads} onDismiss={handleDismissUpload} />
      {previewFile && (
        <PreviewModal
          fileId={previewFile.id}
          fileName={previewFile.name}
          mimeType={previewFile.mimeType}
          onClose={() => setPreviewFile(null)}
        />
      )}
      {bulkEditFileIds && workspaceId && (
        <BulkMetadataDialog
          fileIds={bulkEditFileIds}
          workspaceId={workspaceId}
          onClose={() => setBulkEditFileIds(null)}
          onSuccess={() => {
            if (selectedFolderId) loadFiles(selectedFolderId);
          }}
        />
      )}
      {shareFile && workspaceId && (
        <InternalShareDialog
          open={!!shareFile}
          fileUuid={shareFile.id}
          fileName={shareFile.name}
          onClose={() => setShareFile(null)}
        />
      )}
    </div>
  );
}
