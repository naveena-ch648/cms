package com.cms.security;

import com.cms.entity.User;
import com.cms.entity.UserOrganizationRole;
import com.cms.repository.UserOrganizationRoleRepository;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Use loadUserByEmailAndOrgId instead");
    }

    public UserDetails loadUserByEmailAndOrgId(String email, Long organizationId) {
        User user = userRepository.findByEmailAndOrganizationId(email, organizationId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return buildUserPrincipal(user);
    }

    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return buildUserPrincipal(user);
    }

    private UserPrincipal buildUserPrincipal(User user) {
        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(user.getId(), user.getOrganization().getId())
                .orElse(null);

        List<SimpleGrantedAuthority> authorities = orgRole != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + orgRole.getRole().getName().toUpperCase()))
                : List.of();

        return new UserPrincipal(
                user.getId(),
                user.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFirstName(),
                user.getLastName(),
                authorities
        );
    }
}
