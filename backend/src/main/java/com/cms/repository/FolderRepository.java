package com.cms.repository;

import com.cms.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    Optional<Folder> findByUuid(String uuid);

    List<Folder> findByWorkspaceIdAndStatusOrderBySortOrder(Long workspaceId, Folder.FolderStatus status);

    List<Folder> findByParentIdAndStatusOrderBySortOrder(Long parentId, Folder.FolderStatus status);

    List<Folder> findByWorkspaceIdAndParentIsNullAndStatusOrderBySortOrder(Long workspaceId, Folder.FolderStatus status);

    long countByParentIdAndStatus(Long parentId, Folder.FolderStatus status);

    boolean existsByWorkspaceIdAndParentIdAndNameIgnoreCaseAndStatus(
            Long workspaceId, Long parentId, String name, Folder.FolderStatus status);

    boolean existsByWorkspaceIdAndParentIsNullAndNameIgnoreCaseAndStatus(
            Long workspaceId, String name, Folder.FolderStatus status);

    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT id, uuid, parent_id, name, 0 AS depth
                FROM folders WHERE id = :folderId
                UNION ALL
                SELECT f.id, f.uuid, f.parent_id, f.name, a.depth + 1
                FROM folders f INNER JOIN ancestors a ON f.id = a.parent_id
            )
            SELECT id FROM ancestors WHERE id = :ancestorId AND id != :folderId
            """, nativeQuery = true)
    List<Long> findAncestorMatch(@Param("folderId") Long folderId, @Param("ancestorId") Long ancestorId);

    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT id, uuid, parent_id, name
                FROM folders WHERE id = :folderId
                UNION ALL
                SELECT f.id, f.uuid, f.parent_id, f.name
                FROM folders f INNER JOIN ancestors a ON f.id = a.parent_id
            )
            SELECT * FROM ancestors ORDER BY id
            """, nativeQuery = true)
    List<Object[]> findAncestorPath(@Param("folderId") Long folderId);

    @Query("UPDATE Folder f SET f.status = 'DELETED' WHERE f.parent.id = :parentId AND f.status = 'ACTIVE'")
    @org.springframework.data.jpa.repository.Modifying
    void softDeleteChildren(@Param("parentId") Long parentId);

    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT id FROM folders WHERE parent_id = :parentId
                UNION ALL
                SELECT f.id FROM folders f INNER JOIN descendants d ON f.parent_id = d.id
            )
            UPDATE folders SET status = 'DELETED' WHERE id IN (SELECT id FROM descendants)
            """, nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    void softDeleteDescendants(@Param("parentId") Long parentId);
}
