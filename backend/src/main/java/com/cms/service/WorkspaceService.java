package com.cms.service;

import com.cms.entity.*;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserWorkspaceRoleRepository userWorkspaceRoleRepository;
    private final GroupWorkspaceRoleRepository groupWorkspaceRoleRepository;
    private final RoleRepository roleRepository;
    private final OrganizationService organizationService;
    private final PolicyService policyService;

    @Transactional
    public Workspace create(Long organizationId, String name, String description, Long creatorUserId) {
        Organization org = organizationService.getByIdInternal(organizationId);

        // Check workspace limit from policies
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());
        int maxWorkspaces = policyService.getMaxWorkspaces(policy);
        long currentCount = workspaceRepository.countByOrganizationIdAndStatusNot(
                organizationId, Workspace.WorkspaceStatus.DELETED);
        if (currentCount >= maxWorkspaces) {
            throw new BusinessRuleException("WORKSPACE_LIMIT",
                    "Maximum workspace limit of " + maxWorkspaces + " reached");
        }

        Workspace workspace = Workspace.builder()
                .organization(org)
                .name(name)
                .description(description)
                .build();
        workspace = workspaceRepository.save(workspace);

        // Assign creator as Admin on workspace
        if (creatorUserId != null) {
            Role adminRole = roleRepository.findByNameAndOrganizationId("Admin", organizationId)
                    .orElse(null);
            if (adminRole != null) {
                UserWorkspaceRole uwRole = UserWorkspaceRole.builder()
                        .userId(creatorUserId)
                        .workspaceId(workspace.getId())
                        .role(adminRole)
                        .build();
                userWorkspaceRoleRepository.save(uwRole);
            }
        }

        return workspace;
    }

    public Workspace getById(String uuid) {
        return workspaceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    public Page<Workspace> list(Long organizationId, Pageable pageable) {
        return workspaceRepository.findByOrganizationIdAndStatusNot(
                organizationId, Workspace.WorkspaceStatus.DELETED, pageable);
    }

    @Transactional
    public Workspace update(String uuid, String name, String description) {
        Workspace workspace = getById(uuid);
        if (name != null) workspace.setName(name);
        if (description != null) workspace.setDescription(description);
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public Workspace archive(String uuid) {
        Workspace workspace = getById(uuid);
        workspace.setStatus(Workspace.WorkspaceStatus.ARCHIVED);
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void delete(String uuid) {
        Workspace workspace = getById(uuid);
        workspace.setStatus(Workspace.WorkspaceStatus.DELETED);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void assignUserRole(String workspaceUuid, Long userId, String roleUuid) {
        Workspace workspace = getById(workspaceUuid);
        Role role = roleRepository.findByUuid(roleUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        userWorkspaceRoleRepository.findByUserIdAndWorkspaceId(userId, workspace.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setRole(role);
                            userWorkspaceRoleRepository.save(existing);
                        },
                        () -> {
                            UserWorkspaceRole uwRole = UserWorkspaceRole.builder()
                                    .userId(userId)
                                    .workspaceId(workspace.getId())
                                    .role(role)
                                    .build();
                            userWorkspaceRoleRepository.save(uwRole);
                        }
                );
    }

    @Transactional
    public void removeUserRole(String workspaceUuid, Long userId) {
        Workspace workspace = getById(workspaceUuid);
        userWorkspaceRoleRepository.findByUserIdAndWorkspaceId(userId, workspace.getId())
                .ifPresent(userWorkspaceRoleRepository::delete);
    }

    @Transactional
    public void assignGroupRole(String workspaceUuid, Long groupId, String roleUuid) {
        Workspace workspace = getById(workspaceUuid);
        Role role = roleRepository.findByUuid(roleUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        groupWorkspaceRoleRepository.findByGroupIdAndWorkspaceId(groupId, workspace.getId())
                .ifPresentOrElse(
                        existing -> {
                            existing.setRole(role);
                            groupWorkspaceRoleRepository.save(existing);
                        },
                        () -> {
                            GroupWorkspaceRole gwRole = GroupWorkspaceRole.builder()
                                    .groupId(groupId)
                                    .workspaceId(workspace.getId())
                                    .role(role)
                                    .build();
                            groupWorkspaceRoleRepository.save(gwRole);
                        }
                );
    }

    public List<UserWorkspaceRole> getWorkspaceMembers(Long workspaceId) {
        return userWorkspaceRoleRepository.findByWorkspaceId(workspaceId);
    }

    public List<UserWorkspaceRole> getWorkspaceMembersByUuid(String workspaceUuid) {
        Workspace workspace = getById(workspaceUuid);
        return userWorkspaceRoleRepository.findByWorkspaceId(workspace.getId());
    }

    public Workspace getByUuid(String uuid) {
        return workspaceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    public boolean isWorkspaceAdmin(Long workspaceId, Long userId) {
        return userWorkspaceRoleRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .map(uwr -> uwr.getRole().getName().equalsIgnoreCase("Admin"))
                .orElse(false);
    }

    public boolean isWorkspaceMember(Long workspaceId, Long userId) {
        return userWorkspaceRoleRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .isPresent();
    }
}
