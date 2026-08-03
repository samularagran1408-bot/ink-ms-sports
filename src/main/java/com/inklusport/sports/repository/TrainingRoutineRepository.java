package com.inklusport.sports.repository;

import com.inklusport.sports.entity.TrainingRoutine;
import com.inklusport.sports.enums.RoutineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRoutineRepository extends JpaRepository<TrainingRoutine, String> {

    List<TrainingRoutine> findByStatus(RoutineStatus status);

    List<TrainingRoutine> findByTrainerId(String trainerId);

    List<TrainingRoutine> findByTrainerIdAndStatus(String trainerId, RoutineStatus status);
}
