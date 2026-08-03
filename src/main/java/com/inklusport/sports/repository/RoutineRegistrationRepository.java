package com.inklusport.sports.repository;

import com.inklusport.sports.entity.RoutineRegistration;
import com.inklusport.sports.enums.RoutineRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutineRegistrationRepository extends JpaRepository<RoutineRegistration, String> {

    boolean existsByRoutineIdAndUserIdAndStatus(String routineId, String userId, RoutineRegistrationStatus status);

    Optional<RoutineRegistration> findByRoutineIdAndUserId(String routineId, String userId);

    List<RoutineRegistration> findByUserId(String userId);

    List<RoutineRegistration> findByRoutineId(String routineId);

    List<RoutineRegistration> findByRoutineIdAndStatus(String routineId, RoutineRegistrationStatus status);
}
