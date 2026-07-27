package com.inklusport.sports.service;

import com.inklusport.sports.dto.WaitlistRequest;
import com.inklusport.sports.dto.WaitlistResponse;
import com.inklusport.sports.entity.Waitlist;
import com.inklusport.sports.enums.WaitlistStatus;
import com.inklusport.sports.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getWaitlistByEvent(String eventId) {
        return waitlistRepository.findByEventIdOrderByPositionAsc(eventId).stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional
    public WaitlistResponse addToWaitlist(WaitlistRequest request) {
        List<Waitlist> currentList = waitlistRepository.findByEventIdOrderByPositionAsc(request.getEventId());
        int nextPosition = currentList.size() + 1;

        Waitlist w = Waitlist.builder()
                .userId(request.getUserId()).eventId(request.getEventId())
                .position(nextPosition).status(WaitlistStatus.waiting).build();
        return convertToResponse(waitlistRepository.save(w));
    }

    private WaitlistResponse convertToResponse(Waitlist w) {
        return WaitlistResponse.builder()
                .id(w.getId()).userId(w.getUserId()).eventId(w.getEventId())
                .requestedAt(w.getRequestedAt()).position(w.getPosition()).status(w.getStatus().name()).build();
    }
}