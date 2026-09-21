package com.inklusport.sports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

/**
 * Ejecuta efectos secundarios (notificaciones, contadores, etc.) tras confirmar
 * la transacción y fuera del hilo HTTP, para que la respuesta no espere a Feign.
 */
@Component
@Slf4j
public class AfterCommitRunner {

    private final Executor notificationExecutor;

    public AfterCommitRunner(@Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    /**
     * Encola {@code action} al commit. Si no hay sincronización activa, la lanza
     * ya en el pool de notificaciones.
     */
    public void run(Runnable action) {
        Runnable aislada = () -> {
            try {
                action.run();
            } catch (Exception e) {
                log.error("Fallo en efecto secundario tras commit: {}", e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.execute(aislada);
                }
            });
            return;
        }
        notificationExecutor.execute(aislada);
    }
}
