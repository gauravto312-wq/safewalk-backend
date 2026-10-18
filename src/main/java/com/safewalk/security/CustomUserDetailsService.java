package com.safewalk.security;

import com.safewalk.entity.User;
import com.safewalk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String clean = identifier.trim();
        User user = clean.contains("@")
                ? userRepository.findByEmail(clean.toLowerCase())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + clean))
                : userRepository.findByMobileNumber(clean)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with mobile number: " + clean));

        return UserPrincipal.create(user);
    }
}
