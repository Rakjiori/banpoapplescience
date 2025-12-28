package com.example.sbb.domain.user;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RootAccountInitializer {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.root.username:}")
    private String rootUsername;

    @Value("${app.root.password:}")
    private String rootPassword;

    @Value("${app.root.email:}")
    private String rootEmail;

    @PostConstruct
    @Transactional
    public void ensureRootAccount() {
        if (!StringUtils.hasText(rootUsername) || !StringUtils.hasText(rootPassword)) {
            return;
        }
        List<SiteUser> roots = userRepository.findByRoleOrderByIdAsc("ROLE_ROOT");
        SiteUser root = roots.stream()
                .filter(u -> rootUsername.equals(u.getUsername()))
                .findFirst()
                .orElse(null);
        for (SiteUser stray : roots) {
            if (!rootUsername.equals(stray.getUsername())) {
                stray.setRole("ROLE_USER");
                userRepository.save(stray);
            }
        }
        if (root == null) {
            root = userRepository.findByUsername(rootUsername).orElseGet(SiteUser::new);
            if (root.getId() == null) root.setUsername(rootUsername);
        }
        root.setRole("ROLE_ROOT");
        root.setEmail(StringUtils.hasText(rootEmail) ? rootEmail : root.getEmail());
        if (root.getPassword() == null || !passwordEncoder.matches(rootPassword, root.getPassword())) {
            root.setPassword(passwordEncoder.encode(rootPassword));
        }
        userRepository.save(root);
    }
}
