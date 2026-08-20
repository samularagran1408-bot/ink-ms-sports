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

    @Query("SELECT DISTINCT sd FROM SportDisability sd JOIN FETCH sd.sport JOIN FETCH sd.disability d WHERE d.isActive = true")
    List<SportDisability> findAllWithActiveDisabilities();

    @Query("SELECT DISTINCT sd FROM SportDisability sd JOIN FETCH sd.sport s JOIN FETCH sd.disability d " +
           "WHERE d.isActive = true AND (" +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(COALESCE(sd.adaptations, '')) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<SportDisability> searchActive(@Param("q") String q);
}