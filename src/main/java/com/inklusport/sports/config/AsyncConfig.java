package com.inklusport.sports.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Hilos para los efectos secundarios de una petición (avisos al asistente de IA
 * y envío de notificaciones). Se hacen aparte para que la respuesta salga al
 * instante, y los pools son pequeños y acotados para que una ráfaga de
 * inscripciones o de check-ins no dispare hilos sin control.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-progreso-");
        // Si la cola se llena, lo hace el hilo que llama en vez de perder el aviso.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Notificaciones posteriores al commit. Al inscribirse a un evento hay que
     * avisar al atleta, al organizador y a cada admin: hacerlo en el hilo de la
     * petición retrasaba la confirmación del propio usuario hasta después de que
     * el admin ya tuviera la suya.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notificacion-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // Un despliegue no debe tragarse los avisos que quedaron en la cola.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
