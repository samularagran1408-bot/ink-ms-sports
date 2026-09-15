package com.inklusport.sports.client;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Configuración sólo del cliente de IA: reenvía el JWT de quien hace la
 * petición y corta pronto para no retrasar la respuesta al entrenador.
 */
public class AiAssistantClientConfig {

    @Bean
    public RequestInterceptor aiAuthorizationInterceptor() {
        return template -> {
            if (template.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return;
            }
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
                return;
            }
            String authorization = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, authorization);
            }
        };
    }

    @Bean
    public Request.Options aiRequestOptions() {
        return new Request.Options(1500, TimeUnit.MILLISECONDS, 2500, TimeUnit.MILLISECONDS, true);
    }
}
