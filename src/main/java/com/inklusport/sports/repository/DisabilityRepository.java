package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Disability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisabilityRepository extends JpaRepository<Disability, Integer> {
    List<Disability> findByIsActiveTrue();
    Optional<Disability> findByIdAndIsActiveTrue(Integer id);
    List<Disability> findByIsActiveTrueAndNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}