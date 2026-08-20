package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Integer> {
    List<Sport> findByIsActiveTrue();
    List<Sport> findByNameContainingIgnoreCase(String name);
    List<Sport> findByIsActiveTrueAndNameContainingIgnoreCase(String name);
    boolean existsByName(String name);
}