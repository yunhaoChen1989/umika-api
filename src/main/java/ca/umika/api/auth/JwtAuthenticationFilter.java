package ca.umika.api.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ca.umika.api.user.UserRepository userRepository;
    private final AccountRoleService accountRoleService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            ca.umika.api.user.UserRepository userRepository,
            AccountRoleService accountRoleService
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                email = jwtUtil.getSubjectFromToken(token);
            }
        }
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user (optional for authorities, but we just set email as principal)
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                var authorities = accountRoleService.resolveRoleNames(userOpt.get().getId()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
