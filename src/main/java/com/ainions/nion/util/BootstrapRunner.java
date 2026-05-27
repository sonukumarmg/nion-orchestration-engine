package com.ainions.nion.util;

import com.ainions.nion.domain.Role;
import com.ainions.nion.domain.UserAccount;
import com.ainions.nion.domain.enums.RoleName;
import com.ainions.nion.repository.RoleRepository;
import com.ainions.nion.repository.UserAccountRepository;
import com.ainions.nion.tenant.TenantContext;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRunner implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapRunner(
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role adminRole = ensureRole(RoleName.ROLE_ADMIN);
        Role analystRole = ensureRole(RoleName.ROLE_ANALYST);
        ensureRole(RoleName.ROLE_VIEWER);

        String adminUsername = System.getenv("NION_ADMIN_USERNAME");
        String adminPassword = System.getenv("NION_ADMIN_PASSWORD");
        if (adminUsername == null || adminPassword == null) {
            return;
        }
        userAccountRepository.findByUsername(adminUsername).orElseGet(() -> {
            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setEmail(System.getenv().getOrDefault("NION_ADMIN_EMAIL", "admin@nion.local"));
            admin.setDisplayName(System.getenv().getOrDefault("NION_ADMIN_NAME", "NION Admin"));
            admin.setTenantId(System.getenv().getOrDefault("NION_ADMIN_TENANT", "default"));
            admin.setRoles(Set.of(adminRole, analystRole));
            TenantContext.setTenantId(admin.getTenantId());
            try {
                return userAccountRepository.save(admin);
            } finally {
                TenantContext.clear();
            }
        });
    }

    private Role ensureRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
    }
}
