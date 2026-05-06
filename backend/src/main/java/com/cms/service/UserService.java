package com.cms.service;

import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.entity.Role;
import com.cms.entity.UserOrganizationRole;
import com.cms.exception.BusinessRuleException;
import com.cms.exception.DuplicateResourceException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.middleware.TenantContext;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final OrganizationService organizationService;
    private final PolicyService policyService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(Long organizationId, String email, String firstName, String lastName,
                         String password, String roleUuid) {
        Organization org = organizationService.getByIdInternal(organizationId);

        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new DuplicateResourceException("USER_EMAIL_EXISTS", "Email already registered in this organization");
        }

        // Validate password against org policy
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());
        validatePassword(password, policy);

        Role role = roleRepository.findByUuid(roleUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = User.builder()
                .organization(org)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .passwordHash(passwordEncoder.encode(password))
                .build();
        user = userRepository.save(user);

        // Assign org-level role
        UserOrganizationRole orgRole = UserOrganizationRole.builder()
                .userId(user.getId())
                .organizationId(organizationId)
                .role(role)
                .build();
        userOrgRoleRepository.save(orgRole);

        return user;
    }

    public User getById(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getByIdInternal(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Page<User> list(Long organizationId, String search, User.UserStatus status, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return userRepository.searchByOrganizationId(organizationId, search, pageable);
        }
        if (status != null) {
            return userRepository.findByOrganizationIdAndStatus(organizationId, status, pageable);
        }
        return userRepository.findByOrganizationId(organizationId, pageable);
    }

    @Transactional
    public User update(String uuid, String firstName, String lastName, User.UserStatus status) {
        User user = getById(uuid);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (status != null) user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void changeRole(String userUuid, String roleUuid) {
        User user = getById(userUuid);
        Role newRole = roleRepository.findByUuid(roleUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Long orgId = user.getOrganization().getId();
        UserOrganizationRole orgRole = userOrgRoleRepository.findByUserIdAndOrganizationId(user.getId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("User organization role not found"));

        // Check last admin protection
        checkLastAdmin(orgRole, orgId);

        orgRole.setRole(newRole);
        userOrgRoleRepository.save(orgRole);
    }

    @Transactional
    public void changePassword(String userUuid, String newPassword, Long organizationId) {
        User user = getById(userUuid);
        Organization org = organizationService.getByIdInternal(organizationId);
        Map<String, Object> policy = policyService.getEffectivePolicy(org.getPolicies());
        validatePassword(newPassword, policy);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User deactivate(String uuid) {
        User user = getById(uuid);

        // Check last admin protection
        Long orgId = user.getOrganization().getId();
        UserOrganizationRole orgRole = userOrgRoleRepository.findByUserIdAndOrganizationId(user.getId(), orgId)
                .orElse(null);
        if (orgRole != null) {
            checkLastAdmin(orgRole, orgId);
        }

        user.setStatus(User.UserStatus.INACTIVE);
        return userRepository.save(user);
    }

    private void checkLastAdmin(UserOrganizationRole currentRole, Long orgId) {
        if (currentRole.getRole().getName().equals("Admin")) {
            long adminCount = userOrgRoleRepository.countByOrganizationIdAndRoleId(orgId, currentRole.getRole().getId());
            if (adminCount <= 1) {
                throw new BusinessRuleException("LAST_ADMIN",
                        "Cannot remove the last admin from the organization");
            }
        }
    }

    public void validatePassword(String password, Map<String, Object> policy) {
        int minLength = policyService.getPasswordMinLength(policy);
        if (password.length() < minLength) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "Password must be at least " + minLength + " characters");
        }
        if (policyService.getPasswordRequireUppercase(policy) && !password.matches(".*[A-Z].*")) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "Password must contain at least one uppercase letter");
        }
        if (policyService.getPasswordRequireNumber(policy) && !password.matches(".*[0-9].*")) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "Password must contain at least one number");
        }
        if (policyService.getPasswordRequireSpecialChar(policy) && !password.matches(".*[^a-zA-Z0-9].*")) {
            throw new BusinessRuleException("VALIDATION_ERROR",
                    "Password must contain at least one special character");
        }
    }

    public User findByEmailAndOrgId(String email, Long orgId) {
        return userRepository.findByEmailAndOrganizationId(email, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
