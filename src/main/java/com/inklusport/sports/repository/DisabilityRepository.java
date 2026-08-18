package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Disability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisabilityRepository extends JpaRepository<Disability, Integer> {
    List<Disability> findByIsActiveTrue();
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}