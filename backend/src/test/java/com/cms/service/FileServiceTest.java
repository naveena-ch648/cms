package com.cms.service;

import com.cms.entity.*;
import com.cms.event.FileIndexEventPublisher;
import com.cms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock FileRepository fileRepository;
    @Mock FileVersionRepository fileVersionRepository;
    @Mock FolderRepository folderRepository;
    @Mock StorageService storageService;
    @Mock StorageQuotaService storageQuotaService;
    @Mock FileProcessingQueueService fileProcessingQueueService;
    @Mock AuditService auditService;
    @Mock PreviewService previewService;
    @Mock FileIndexEventPublisher fileIndexEventPublisher;
    @Mock EmbeddingJobService embeddingJobService;
    @Mock ActivityEventService activityEventService;
    @Mock AIAutomationService aiAutomationService;

    @InjectMocks FileService fileService;

    private Organization org;
    private Workspace workspace;
    private Folder folder;
    private User uploader;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setId(1L);
        org.setName("Org");
        org.setSlug("org");
        org.setBillingContactEmail("a@b.com");
        org.setStatus(Organization.OrganizationStatus.ACTIVE);

        workspace = new Workspace();
        workspace.setId(1L);
        workspace.setOrganization(org);

        folder = new Folder();
        folder.setId(1L);
        folder.setWorkspace(workspace);

        uploader = new User();
        uploader.setId(5L);
        uploader.setEmail("user@test.com");
        uploader.setFirstName("U");
        uploader.setLastName("P");
        uploader.setStatus(User.UserStatus.ACTIVE);
        uploader.setOrganization(org);
    }

    @Test
    void createFileRecord_savesFileAndVersion() {
        FileEntity savedFile = new FileEntity();
        savedFile.setId(100L);
        savedFile.setUuid("file-uuid");
        savedFile.setName("doc.pdf");
        savedFile.setWorkspace(workspace);
        savedFile.setOrganization(org);
        savedFile.setStatus(FileEntity.FileStatus.ACTIVE);
        savedFile.setFolder(folder);

        FileVersion savedVersion = new FileVersion();
        savedVersion.setId(1L);
        savedVersion.setVersionNumber(1);

        when(fileRepository.save(any())).thenReturn(savedFile);
        when(fileVersionRepository.save(any())).thenReturn(savedVersion);

        FileEntity result = fileService.createFileRecord(
                folder, uploader, "doc.pdf", "doc.pdf",
                1024L, "application/pdf",
                "storage/key/doc.pdf", "cms-files",
                null, null, "RENAME");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("doc.pdf");

        verify(fileRepository, atLeast(2)).save(any());
        verify(fileVersionRepository).save(any());
        verify(storageQuotaService).updateUsedStorage(eq(1L), eq(1024L));
        verify(fileProcessingQueueService).publishJob(anyLong(), anyLong(), eq("process"));
    }

    @Test
    void createFileRecord_withDuplicateRename_savesWithSuffix() {
        when(fileRepository.existsByFolderIdAndNameAndStatus(1L, "doc.pdf", FileEntity.FileStatus.ACTIVE)).thenReturn(true);
        when(fileRepository.existsByFolderIdAndNameAndStatus(1L, "doc (1).pdf", FileEntity.FileStatus.ACTIVE)).thenReturn(false);

        FileEntity savedFile = new FileEntity();
        savedFile.setId(200L);
        savedFile.setName("doc (1).pdf");
        savedFile.setWorkspace(workspace);
        savedFile.setOrganization(org);
        savedFile.setStatus(FileEntity.FileStatus.ACTIVE);
        savedFile.setFolder(folder);

        FileVersion savedVersion = new FileVersion();
        savedVersion.setId(1L);
        savedVersion.setVersionNumber(1);

        when(fileRepository.save(any())).thenReturn(savedFile);
        when(fileVersionRepository.save(any())).thenReturn(savedVersion);

        FileEntity result = fileService.createFileRecord(
                folder, uploader, "doc.pdf", "doc.pdf",
                512L, "application/pdf",
                "storage/key/doc2.pdf", "cms-files",
                null, null, "RENAME");

        assertThat(result.getName()).isEqualTo("doc (1).pdf");
    }

    @Test
    void getByUuid_withValidUuid_returnsFile() {
        FileEntity file = new FileEntity();
        file.setId(1L);
        file.setUuid("some-uuid");

        when(fileRepository.findByUuid("some-uuid")).thenReturn(Optional.of(file));

        FileEntity result = fileService.getByUuid("some-uuid");

        assertThat(result.getUuid()).isEqualTo("some-uuid");
    }

    @Test
    void getByUuid_withUnknownUuid_throwsException() {
        when(fileRepository.findByUuid("bad-uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.getByUuid("bad-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }
}
