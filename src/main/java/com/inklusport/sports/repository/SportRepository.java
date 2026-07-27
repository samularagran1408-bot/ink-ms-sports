package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportRepository extends JpaRepository<Sport, Integer> {
    List<Sport> findByIsActiveTrue();
    boolean existsByName(String name);
}