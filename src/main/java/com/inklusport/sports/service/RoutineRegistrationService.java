package com.inklusport.sports.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutineRegistrationService {

    private final RoutineRegistrationRepository registrationRepository;
    private final TrainingRoutineRepository routineRepository;
    private final StaffNotificationService staffNotificationService;
    private final UserIdentityService userIdentityService;

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
            return toResponse(saved, routine, "Inscripción reactivada.");
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

        return toResponse(saved, routine, "Inscripción confirmada a la rutina.");
    }

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
                "El usuario " + reg.getUserId() + " canceló su inscripción a \"" + routine.getName() + "\".",
                null
        );
    }

    @Transactional(readOnly = true)
    public List<RoutineRegistrationResponse> byUser(String userId) {
        return userIdentityService.identityAliases(userId).stream()
                .flatMap(alias -> registrationRepository.findByUserId(alias).stream())
                .collect(Collectors.toMap(RoutineRegistration::getId, r -> r, (a, b) -> a))
                .values()
                .stream()
                .map(reg -> {
                    TrainingRoutine routine = routineRepository.findById(reg.getRoutineId()).orElse(null);
                    return toResponse(reg, routine, null);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoutineRegistrationResponse> byRoutine(String routineId) {
        TrainingRoutine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));
        return registrationRepository.findByRoutineId(routineId).stream()
                .map(reg -> toResponse(reg, routine, null))
                .collect(Collectors.toList());
    }

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
                "El usuario " + userId + " se inscribió a \"" + routine.getName() + "\". Cupos restantes: "
                        + routine.getAvailableCapacity() + ".",
                null
        );
    }

    private RoutineRegistrationResponse toResponse(RoutineRegistration reg, TrainingRoutine routine, String message) {
        return RoutineRegistrationResponse.builder()
                .id(reg.getId())
                .userId(reg.getUserId())
                .routineId(reg.getRoutineId())
                .routineName(routine != null ? routine.getName() : null)
                .status(reg.getStatus() != null ? reg.getStatus().name() : null)
                .registrationDate(reg.getRegistrationDate())
                .trainerId(routine != null ? routine.getTrainerId() : null)
                .message(message)
                .build();
    }
}
