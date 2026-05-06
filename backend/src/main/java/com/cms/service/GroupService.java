package com.cms.service;

import com.cms.entity.Group;
import com.cms.entity.UserGroup;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.GroupRepository;
import com.cms.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final OrganizationService organizationService;

    @Transactional
    public Group create(Long organizationId, String name, String description) {
        if (groupRepository.existsByNameAndOrganizationId(name, organizationId)) {
            throw new DuplicateResourceException("GROUP_NAME_EXISTS", "Group name already exists");
        }

        Group group = Group.builder()
                .organization(organizationService.getByIdInternal(organizationId))
                .name(name)
                .description(description)
                .build();
        return groupRepository.save(group);
    }

    public Group getById(String uuid) {
        return groupRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    public Page<Group> list(Long organizationId, Pageable pageable) {
        return groupRepository.findByOrganizationId(organizationId, pageable);
    }

    @Transactional
    public Group update(String uuid, String name, String description) {
        Group group = getById(uuid);
        if (name != null) group.setName(name);
        if (description != null) group.setDescription(description);
        return groupRepository.save(group);
    }

    @Transactional
    public void delete(String uuid) {
        Group group = getById(uuid);
        userGroupRepository.deleteByGroupId(group.getId());
        groupRepository.delete(group);
    }

    @Transactional
    public void addMember(String groupUuid, Long userId) {
        Group group = getById(groupUuid);
        if (userGroupRepository.existsByUserIdAndGroupId(userId, group.getId())) {
            throw new DuplicateResourceException("USER_IN_GROUP", "User is already a member of this group");
        }
        UserGroup userGroup = UserGroup.builder()
                .userId(userId)
                .groupId(group.getId())
                .build();
        userGroupRepository.save(userGroup);
    }

    @Transactional
    public void removeMember(String groupUuid, Long userId) {
        Group group = getById(groupUuid);
        userGroupRepository.deleteByUserIdAndGroupId(userId, group.getId());
    }

    public List<UserGroup> getMembers(Long groupId) {
        return userGroupRepository.findByGroupId(groupId);
    }
}
