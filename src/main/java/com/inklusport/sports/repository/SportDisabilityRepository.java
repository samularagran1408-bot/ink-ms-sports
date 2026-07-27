package com.inklusport.sports.repository;

import com.inklusport.sports.entity.SportDisability;
import com.inklusport.sports.entity.Disability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SportDisabilityRepository extends JpaRepository<SportDisability, SportDisability.SportDisabilityId> {
    List<SportDisability> findBySportId(Integer sportId);

    boolean existsBySportIdAndDisabilityId(Integer sportId, Integer disabilityId);
    
    @Query("SELECT sd.disability FROM SportDisability sd WHERE sd.sport.id = :sportId")
    List<Disability> findDisabilitiesBySportId(@Param("sportId") Integer sportId);
}