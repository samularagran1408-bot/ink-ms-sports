package com.inklusport.sports.service;

import com.inklusport.sports.client.UserServiceClient;
import com.inklusport.sports.dto.RoutineRequest;
import com.inklusport.sports.dto.RoutineResponse;
import com.inklusport.sports.entity.Sport;
import com.inklusport.sports.entity.TrainingRoutine;
import com.inklusport.sports.enums.RoutineLevel;
import com.inklusport.sports.enums.RoutineStatus;
import com.inklusport.sports.repository.SportRepository;
import com.inklusport.sports.repository.TrainingRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingRoutineService {

    private final TrainingRoutineRepository routineRepository;
    private final SportRepository sportRepository;
    private final UserServiceClient userServiceClient;
    private final UserIdentityService userIdentityService;
    private final QuizEligibilityService quizEligibilityService;

    @Transactional(readOnly = true)
    public List<RoutineResponse> listPublished() {
        return routineRepository.findByStatus(RoutineStatus.published).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoutineResponse getById(String id) {
        TrainingRoutine routine = routineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));
        return toResponse(routine);
    }

    @Transactional(readOnly = true)
    public List<RoutineResponse> listByTrainer(String trainerId) {
        return userIdentityService.identityAliases(trainerId).stream()
                .flatMap(alias -> routineRepository.findByTrainerId(alias).stream())
                .collect(Collectors.toMap(TrainingRoutine::getId, r -> r, (a, b) -> a))
                .values()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crea una rutina en borrador; exige quiz de entrenador aprobado (salvo admin).
     */
    @Transactional
    public RoutineResponse create(RoutineRequest request) {
        String trainerId = userIdentityService.resolveTrainerId(request.getTrainerId());
        quizEligibilityService.assertTrainerQuizPassed(trainerId);
        Integer sportId = null;
        if (request.getSportId() != null) {
            Sport sport = sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new IllegalArgumentException("Deporte no encontrado"));
            sportId = sport.getId();
        }

        int maxCapacity = request.getMaxCapacity() != null ? request.getMaxCapacity() : 20;

        TrainingRoutine routine = TrainingRoutine.builder()
                .trainerId(trainerId)
                .sportId(sportId)
                .name(request.getName())
                .description(request.getDescription())
                .disabilityFocus(request.getDisabilityFocus())
                .level(parseLevel(request.getLevel()))
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 35)
                .exercisesJson(normalizeExercisesJson(request.getExercisesJson()))
                .status(RoutineStatus.draft)
                .maxCapacity(maxCapacity)
                .availableCapacity(maxCapacity)
                .build();

        return toResponse(routineRepository.save(routine));
    }

    /**
     * Actualiza una rutina del entrenador dueño; exige quiz de entrenador aprobado.
     */
    @Transactional
    public RoutineResponse update(String id, RoutineRequest request) {
        TrainingRoutine routine = routineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));

        assertOwner(routine);
        quizEligibilityService.assertTrainerQuizPassed(routine.getTrainerId());

        if (request.getName() != null && !request.getName().isBlank()) {
            routine.setName(request.getName());
        }
        if (request.getDescription() != null) {
            routine.setDescription(request.getDescription());
        }
        if (request.getDisabilityFocus() != null) {
            routine.setDisabilityFocus(request.getDisabilityFocus());
        }
        if (request.getLevel() != null) {
            routine.setLevel(parseLevel(request.getLevel()));
        }
        if (request.getDurationMinutes() != null) {
            routine.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getExercisesJson() != null) {
            routine.setExercisesJson(normalizeExercisesJson(request.getExercisesJson()));
        }
        if (request.getSportId() != null) {
            Sport sport = sportRepository.findById(request.getSportId())
                    .orElseThrow(() -> new IllegalArgumentException("Deporte no encontrado"));
            routine.setSportId(sport.getId());
        }
        if (request.getMaxCapacity() != null) {
            int used = routine.getMaxCapacity() - routine.getAvailableCapacity();
            if (request.getMaxCapacity() < used) {
                throw new IllegalStateException("El cupo no puede ser menor que las inscripciones activas.");
            }
            routine.setMaxCapacity(request.getMaxCapacity());
            routine.setAvailableCapacity(request.getMaxCapacity() - used);
        }

        return toResponse(routineRepository.save(routine));
    }

    @Transactional
    /**
     * Publica una rutina; exige ownership y quiz de entrenador aprobado.
     */
    public RoutineResponse publish(String id) {
        TrainingRoutine routine = routineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rutina no encontrada"));

        assertOwner(routine);
        quizEligibilityService.assertTrainerQuizPassed(routine.getTrainerId());

        if (routine.getStatus() == RoutineStatus.archived) {
            throw new IllegalStateException("No se puede publicar una rutina archivada.");
        }
        routine.setStatus(RoutineStatus.published);
        return toResponse(routineRepository.save(routine));
    }

    private void assertOwner(TrainingRoutine routine) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return; // perfil docker permitAll
        }
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        String principal = String.valueOf(auth.getPrincipal());
        if (!routine.getTrainerId().equals(principal)
                && !routine.getTrainerId().equalsIgnoreCase(principal)) {
            /**
             * También aceptar si el principal es email y trainerId es UUID resuelto
             */
            try {
                Map<String, String> resolved = userServiceClient.getUserIdByEmail(principal);
                if (resolved != null && routine.getTrainerId().equals(resolved.get("id"))) {
                    return;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Sólo el entrenador dueño puede modificar esta rutina.");
        }
    }

    private String normalizeExercisesJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        String trimmed = raw.trim();
        if ("null".equalsIgnoreCase(trimmed)) {
            return "[]";
        }
        /**
         * MySQL JSON rechaza cadenas vacías; exigir JSON mínimo válido.
         */
        if (!(trimmed.startsWith("[") || trimmed.startsWith("{"))) {
            return "[]";
        }
        return trimmed;
    }

    private RoutineLevel parseLevel(String level) {
        if (level == null || level.isBlank()) {
            return RoutineLevel.principiante;
        }
        String normalized = level.trim().toLowerCase();
        return switch (normalized) {
            case "beginner", "principiante" -> RoutineLevel.principiante;
            case "intermediate", "intermedio" -> RoutineLevel.intermedio;
            case "advanced", "avanzado" -> RoutineLevel.avanzado;
            default -> {
                try {
                    yield RoutineLevel.valueOf(normalized);
                } catch (IllegalArgumentException e) {
                    yield RoutineLevel.principiante;
                }
            }
        };
    }

    private RoutineResponse toResponse(TrainingRoutine r) {
        String sportName = null;
        if (r.getSport() != null) {
            sportName = r.getSport().getName();
        } else if (r.getSportId() != null) {
            sportName = sportRepository.findById(r.getSportId()).map(Sport::getName).orElse(null);
        }
        return RoutineResponse.builder()
                .id(r.getId())
                .trainerId(r.getTrainerId())
                .sportId(r.getSportId())
                .sportName(sportName)
                .name(r.getName())
                .description(r.getDescription())
                .disabilityFocus(r.getDisabilityFocus())
                .level(r.getLevel() != null ? r.getLevel().name() : null)
                .durationMinutes(r.getDurationMinutes())
                .exercisesJson(r.getExercisesJson())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .maxCapacity(r.getMaxCapacity())
                .availableCapacity(r.getAvailableCapacity())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
