package com.inklusport.sports.repository;

import com.inklusport.sports.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitlistRepository extends JpaRepository<Waitlist, String> {
    List<Waitlist> findByEventIdOrderByPositionAsc(String eventId);
}