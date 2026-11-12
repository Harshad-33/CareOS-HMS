package com.careos.hms.security;

import com.careos.hms.user.entity.User;
import com.careos.hms.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareOsUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CareOsUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findActiveGraphByUsernameOrEmail(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User account not found"));
        return CareOsUserPrincipal.fromUser(user);
    }

    @Transactional(readOnly = true)
    public CareOsUserPrincipal loadPrincipalByPublicId(String publicId) {
        User user = userRepository.findActiveGraphByPublicId(publicId)
                .orElseThrow(() -> new UsernameNotFoundException("User account not found"));
        return CareOsUserPrincipal.fromUser(user);
    }
}
