package com.cms.service;

import com.cms.dto.dashboard.RecentFileDto;
import com.cms.entity.FileEntity;
import com.cms.entity.User;
import com.cms.entity.UserRecentFile;
import com.cms.repository.FileRepository;
import com.cms.repository.UserRecentFileRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecentFileService {

    private static final int MAX_RECENT = 10;

    private final UserRecentFileRepository userRecentFileRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    /**
     * Record or refresh a file access for a user.
     * If the file is already in the user's recent list, its timestamp is updated in-place.
     * If it is new, it is inserted and the list is trimmed to MAX_RECENT entries.
     */
    @Transactional
    public void recordAccess(Long userId, Long fileId) {
        try {
            Optional<UserRecentFile> existing =
                    userRecentFileRepository.findByUserIdAndFileId(userId, fileId);

            if (existing.isPresent()) {
                // Move to top — just update timestamp
                UserRecentFile urf = existing.get();
                urf.setLastAccessedAt(Instant.now());
                userRecentFileRepository.save(urf);
            } else {
                // New entry — use lazy references to avoid extra SELECTs
                User userRef = userRepository.getReferenceById(userId);
                FileEntity fileRef = fileRepository.getReferenceById(fileId);

                UserRecentFile urf = UserRecentFile.builder()
                        .user(userRef)
                        .file(fileRef)
                        .lastAccessedAt(Instant.now())
                        .build();
                userRecentFileRepository.save(urf);

                // Prune: keep only the MAX_RECENT newest entries
                userRecentFileRepository.trimToLimit(userId, MAX_RECENT);
            }
        } catch (Exception ex) {
            // Best-effort: do not fail the main request if tracking fails
            log.warn("Failed to record recent file access for user={} file={}: {}", userId, fileId, ex.getMessage());
        }
    }

    /**
     * Fetch the user's recent files, newest first, up to limit entries.
     */
    @Transactional(readOnly = true)
    public List<RecentFileDto> getRecentFiles(Long userId, int limit) {
        int safeLimit = Math.min(limit, MAX_RECENT);
        return userRecentFileRepository
                .findRecentByUserId(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(RecentFileDto::fromUserRecent)
                .collect(Collectors.toList());
    }
}
