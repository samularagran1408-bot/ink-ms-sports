package com.inklusport.sports.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Se inyecta el JwtTokenProvider para validar y extraer información del token JWT. 
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Este método se ejecuta para cada solicitud entrante. 
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            List<String> roles = jwtTokenProvider.getRolesFromToken(token);
            
            if (roles == null) {
                roles = List.of();
            }

            /**
             * Mapeo y traducción estricta de roles para compatibilidad con Spring Security 
             */
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        String normalizedRole = role.toUpperCase().trim();
                        
                        /**
                         * Traducir del token (Español) al estándar de las anotaciones (Inglés)
                         */
                        if (normalizedRole.equals("USUARIO")) {
                            normalizedRole = "USER";
                        } else if (normalizedRole.equals("ADMINISTRADOR")) {
                            normalizedRole = "ADMIN";
                        } else if (normalizedRole.equals("ORGANIZADOR")) {
                            normalizedRole = "ORGANIZER";
                        } else if (normalizedRole.equals("ENTRENADOR")) {
                            normalizedRole = "COACH";   
                        }
                        
                        /**
                         * Asegurar el prefijo ROLE_ requerido por Spring Security de forma interna
                         */
                        String roleWithPrefix = normalizedRole.startsWith("ROLE_") ? normalizedRole : "ROLE_" + normalizedRole;
                        return new SimpleGrantedAuthority(roleWithPrefix);
                    })
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Usuario autenticado exitosamente en Microservicio Deportes: {} con roles mapeados: {}", email, authorities);
        } else if (token != null) {
            log.warn("Se envió un token pero falló la validación en JwtTokenProvider");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token de la cabecera HTTP Authorization, asegurándose de que tenga el formato correcto "Bearer <token>"
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}