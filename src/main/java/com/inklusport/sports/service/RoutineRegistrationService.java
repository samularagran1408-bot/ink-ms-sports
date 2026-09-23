package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.RoutineRegistrationRequest;
import com.inklusport.sports.dto.RoutineRegistrationResponse;
import com.inklusport.sports.entity.RoutineRegistration;
import com.inklusport.sports.entity.TrainingRoutine;
import com.inklusport.sports.enums.RoutineRegistrationStatus;
import com.inklusport.sports.enums.RoutineStatus;
import com.inklusport.sports.repository.RoutineRegistrationRepository;
import com.inklusport.sports.repository.TrainingRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Inscripciones de atletas a rutinas/sesiones de entrenamiento. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineRegistrationService {

    private final RoutineRegistrationRepository registrationRepository;
    private final TrainingRoutineRepository routineRepository;
    private final StaffNotificationService staffNotificationService;
    private final UserIdentityService userIdentityService;
    private final UserServiceClient userServiceClient;
    private final QuizEligibilityService quizEligibilityService;
    private final CompetitionModeService competitionModeService;

    /** Inscribe o reactiva al atleta; descuenta cupo y notifica (lleno/casi lleno). */
    @Transactional
    public RoutineRegistrationResponse register(RoutineRegistrationRequest request) {
        TrainingRoutine routine = routineRepository.findById(request.getRoutineId())
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));

        if (routine.getStatus() != RoutineStatus.published) {
            throw new IllegalStateException("Sólo puedes inscribirte en rutinas publicadas.");
        }

        String userId = userIdentityService.resolveCanonicalUserId(request.getUserId());

        var existing = userIdentityService.identityAliases(userId).stream()
                .map(alias -> registrationRepository.findByRoutineIdAndUserId(routine.getId(), alias))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .findFirst();
        if (existing.isPresent()) {
            RoutineRegistration reg = existing.get();
            if (reg.getStatus() == RoutineRegistrationStatus.active) {
                throw new IllegalStateException("Ya estás inscrito en esta rutina.");
            }
            if (routine.getAvailableCapacity() <= 0) {
                throw new IllegalStateException("La rutina no tiene cupos disponibles.");
            }
            reg.setStatus(RoutineRegistrationStatus.active);
            routine.setAvailableCapacity(routine.getAvailableCapacity() - 1);
            routineRepository.saveAndFlush(routine);
            RoutineRegistration saved = registrationRepository.saveAndFlush(reg);
            notifyRoutineJoined(routine, userId);
            return toResponse(saved, routine, "Inscripción reactivada.", null);
        }

        if (routine.getAvailableCapacity() <= 0) {
            throw new IllegalStateException("La rutina no tiene cupos disponibles.");
        }

        RoutineRegistration registration = RoutineRegistration.builder()
                .userId(userId)
                .routineId(routine.getId())
                .status(RoutineRegistrationStatus.active)
                .build();

        routine.setAvailableCapacity(routine.getAvailableCapacity() - 1);
        routineRepository.saveAndFlush(routine);

        RoutineRegistration saved = registrationRepository.saveAndFlush(registration);
        notifyRoutineJoined(routine, userId);

        if (routine.getAvailableCapacity() != null && routine.getAvailableCapacity() == 0) {
            staffNotificationService.notifyTrainer(
                    routine.getTrainerId(),
                    "trainer_routine_full",
                    "Sesión aforo completo",
                    "Tu sesión \"" + routine.getName() + "\" ya no tiene cupos disponibles.",
                    null
            );
        } else if (routine.getMaxCapacity() != null && routine.getAvailableCapacity() != null) {
            int used = routine.getMaxCapacity() - routine.getAvailableCapacity();
            if (routine.getMaxCapacity() > 0 && used * 100 / routine.getMaxCapacity() >= 90) {
                staffNotificationService.notifyTrainer(
                        routine.getTrainerId(),
                        "trainer_routine_almost_full",
                        "Sesión casi llena",
                        "Tu sesión \"" + routine.getName() + "\" está al " + (used * 100 / routine.getMaxCapacity())
                                + "% de su capacidad.",
                        null
                );
            }
        }

        return toResponse(saved, routine, "Inscripción confirmada a la rutina.", null);
    }

    /** Cancela la inscripción, libera cupo y notifica a atleta y entrenador. */
    @Transactional
    public void cancel(String registrationId) {
        RoutineRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));

        if (reg.getStatus() != RoutineRegistrationStatus.active) {
            throw new IllegalStateException("La inscripción ya no está activa.");
        }

        reg.setStatus(RoutineRegistrationStatus.cancelled);
        registrationRepository.save(reg);

        TrainingRoutine routine = routineRepository.findById(reg.getRoutineId())
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));
        routine.setAvailableCapacity(routine.getAvailableCapacity() + 1);
        routineRepository.saveAndFlush(routine);

        staffNotificationService.notifyUser(
                reg.getUserId(),
                "routine_registration_cancelled",
                "Inscripción a sesión cancelada",
                "Cancelaste tu inscripción a la sesión \"" + routine.getName() + "\".",
                null
        );
        staffNotificationService.notifyTrainer(
                routine.getTrainerId(),
                "trainer_routine_cancelled",
                "Cancelación en tu sesión",
                "El usuario " + staffNotificationService.displayLabel(reg.getUserId())
                        + " canceló su inscripción a \"" + routine.getName() + "\".",
                null
        );
    }

    /** Lista inscripciones del usuario resolviendo alias de identidad. */
    @Transactional(readOnly = true)
    public List<RoutineRegistrationResponse> byUser(String userId) {
        return userIdentityService.identityAliases(userId).stream()
                .flatMap(alias -> registrationRepository.findByUserId(alias).stream())
                .collect(Collectors.toMap(RoutineRegistration::getId, r -> r, (a, b) -> a))
                .values()
                .stream()
                .map(reg -> {
                    TrainingRoutine routine = routineRepository.findById(reg.getRoutineId()).orElse(null);
                    return toResponse(reg, routine, null, null);
                })
                .collect(Collectors.toList());
    }

    /** Lista inscripciones de una rutina; lanza si no existe. */
    @Transactional(readOnly = true)
    public List<RoutineRegistrationResponse> byRoutine(String routineId) {
        TrainingRoutine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));
        Map<String, UserSnapshot> cache = new HashMap<>();
        return registrationRepository.findByRoutineId(routineId).stream()
                .map(reg -> toResponse(reg, routine, null, cache))
                .collect(Collectors.toList());
    }

    /**
     * Marca o desmarca asistencia a la sesión ({@code completed} / {@code active}).
     * No cambia el cupo: el atleta sigue inscrito.
     *
     * <p>Registrarla exige que el atleta tenga el modo competencia activo (RF53);
     * quitarla siempre se puede, para poder corregir un error.
     */
    @Transactional
    public RoutineRegistrationResponse markAttendance(String registrationId, boolean attended) {
        quizEligibilityService.assertCurrentStaffQuizPassed();
        RoutineRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada"));
        TrainingRoutine routine = routineRepository.findById(reg.getRoutineId())
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));
        assertCanManageRoutine(routine);

        if (reg.getStatus() == RoutineRegistrationStatus.cancelled) {
            throw new IllegalStateException("No puedes marcar asistencia de una inscripción cancelada.");
        }

        // El perfil se resuelve una sola vez y se reutiliza en la respuesta.
        Map<String, UserSnapshot> perfiles = new HashMap<>();
        if (attended) {
            UserSnapshot atleta = resolveUser(reg.getUserId(), perfiles);
            competitionModeService.assertActive(reg.getUserId(), atleta.email(), atleta.fullName());
        }

        RoutineRegistrationStatus next = attended
                ? RoutineRegistrationStatus.completed
                : RoutineRegistrationStatus.active;
        if (reg.getStatus() == next) {
            String yaEstaba = attended ? "Asistencia ya registrada." : "Asistencia ya desmarcada.";
            return toResponse(reg, routine, yaEstaba, perfiles);
        }
        reg.setStatus(next);
        RoutineRegistration saved = registrationRepository.saveAndFlush(reg);
        String message = attended
                ? "Asistencia registrada en la sesión."
                : "Asistencia desmarcada; el atleta sigue inscrito.";
        return toResponse(saved, routine, message, perfiles);
    }

    /** Notifica al atleta y al entrenador la nueva inscripción. */
    private void notifyRoutineJoined(TrainingRoutine routine, String userId) {
        staffNotificationService.notifyUser(
                userId,
                "routine_registration",
                "Inscripción a sesión confirmada",
                "Te inscribiste a la sesión \"" + routine.getName() + "\".",
                null
        );
        staffNotificationService.notifyTrainer(
                routine.getTrainerId(),
                "trainer_new_registration",
                "Nueva inscripción en tu sesión",
                "El usuario " + staffNotificationService.displayLabel(userId)
                        + " se inscribió a \"" + routine.getName() + "\". Cupos restantes: "
                        + routine.getAvailableCapacity() + ".",
                null
        );
    }

    /** Convierte la inscripción a DTO de respuesta, enriquecida con el perfil del atleta. */
    private RoutineRegistrationResponse toResponse(
            RoutineRegistration reg,
            TrainingRoutine routine,
            String message,
            Map<String, UserSnapshot> cache
    ) {
        UserSnapshot user = resolveUser(reg.getUserId(), cache);
        return RoutineRegistrationResponse.builder()
                .id(reg.getId())
                .userId(reg.getUserId())
                .userFullName(user.fullName)
                .userEmail(user.email)
                .userProfilePicture(user.profilePicture)
                .userDisability(user.disability)
                .routineId(reg.getRoutineId())
                .routineName(routine != null ? routine.getName() : null)
                .status(reg.getStatus() != null ? reg.getStatus().name() : null)
                .registrationDate(reg.getRegistrationDate())
                .trainerId(routine != null ? routine.getTrainerId() : null)
                .message(message)
                .build();
    }

    /** Sólo el entrenador dueño (o un admin) puede marcar asistencia de la sesión. */
    private void assertCanManageRoutine(TrainingRoutine routine) {
        if (isAdmin()) {
            return;
        }
        String principal = userIdentityService.currentPrincipal();
        if (principal == null) {
            throw new IllegalStateException("Debes autenticarte para gestionar la sesión.");
        }
        var aliases = userIdentityService.identityAliases(principal);
        if (routine.getTrainerId() == null || !aliases.contains(routine.getTrainerId())) {
            throw new IllegalStateException("Sólo puedes marcar asistencia en tus propias sesiones.");
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private UserSnapshot resolveUser(String userId, Map<String, UserSnapshot> cache) {
        if (userId == null || userId.isBlank()) {
            return UserSnapshot.empty();
        }
        if (cache != null && cache.containsKey(userId)) {
            return cache.get(userId);
        }
        UserSnapshot snapshot = UserSnapshot.empty();
        try {
            Map<String, Object> user = userServiceClient.getUserByIdInternal(userId);
            if (user == null || user.isEmpty()) {
                user = userServiceClient.getUserById(userId);
            }
            snapshot = new UserSnapshot(
                    stringField(user, "fullName", "full_name", "name"),
                    stringField(user, "email"),
                    stringField(user, "profilePicture", "profile_picture"),
                    stringField(user, "disability", "disabilityType")
            );
        } catch (Exception e) {
            log.warn("No se pudo enriquecer al inscrito {}: {}", userId, e.getMessage());
        }
        if (cache != null) {
            cache.put(userId, snapshot);
        }
        return snapshot;
    }

    private String stringField(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private record UserSnapshot(String fullName, String email, String profilePicture, String disability) {
        static UserSnapshot empty() {
            return new UserSnapshot(null, null, null, null);
        }
    }
}
