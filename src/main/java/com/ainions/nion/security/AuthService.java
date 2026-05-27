package com.ainions.nion.security;

import com.ainions.nion.api.dto.AuthRequest;
import com.ainions.nion.api.dto.AuthResponse;
import com.ainions.nion.api.dto.RegisterRequest;
import com.ainions.nion.config.NionProperties;
import com.ainions.nion.domain.Role;
import com.ainions.nion.domain.UserAccount;
import com.ainions.nion.domain.enums.RoleName;
import com.ainions.nion.repository.RoleRepository;
import com.ainions.nion.repository.UserAccountRepository;
import com.ainions.nion.tenant.TenantContext;
import com.ainions.nion.util.AuditLogService;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final NionProperties properties;
    private final AuditLogService auditLogService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            NionProperties properties,
            AuditLogService auditLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.auditLogService = auditLogService;
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        auditLogService.logEvent(principal.username(), "LOGIN", "auth", "token issued");
        return new AuthResponse(
                token,
                "Bearer",
                properties.jwt().accessTokenMinutes() * 60L,
                principal.tenantId()
        );
    }

    public AuthResponse register(RegisterRequest request) {
        Role analystRole = roleRepository.findByName(RoleName.ROLE_ANALYST)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ANALYST)));

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setDisplayName(request.displayName());
        user.setTenantId(resolveTenant(request.tenantId()));
        user.setRoles(Set.of(analystRole));
        userAccountRepository.save(user);

        UserPrincipal principal = new UserPrincipal(
                user.getUsername(),
                user.getPasswordHash(),
                user.getTenantId(),
                Set.of(() -> analystRole.getName().name()),
                user.isEnabled()
        );
        String token = jwtService.generateToken(principal);
        auditLogService.logEvent(user.getUsername(), "REGISTER", "auth", "user created");
        return new AuthResponse(
                token,
                "Bearer",
                properties.jwt().accessTokenMinutes() * 60L,
                user.getTenantId()
        );
    }

    private String resolveTenant(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        String context = TenantContext.getTenantId();
        return context == null || context.isBlank() ? "default" : context;
    }
}
