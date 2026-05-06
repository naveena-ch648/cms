-- V8: Seed default storage quota for existing organizations
INSERT INTO storage_quotas (organization_id, max_storage_bytes, used_storage_bytes, max_file_size_bytes, trash_retention_days)
SELECT id, 10737418240, 0, 10737418240, 30 FROM organizations
WHERE id NOT IN (SELECT organization_id FROM storage_quotas);
