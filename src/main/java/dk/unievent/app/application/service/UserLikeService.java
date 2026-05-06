package dk.unievent.app.application.service;

import dk.unievent.app.db.model.EventEntity;
import dk.unievent.app.db.model.UserEntity;
import dk.unievent.app.db.model.UserEventLikeEntity;
import dk.unievent.app.db.model.UserEventLikeId;
import dk.unievent.app.db.repository.EventRepository;
import dk.unievent.app.db.repository.UserEventLikeRepository;
import dk.unievent.app.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// manages user event likes. like/unlike are idempotent; mergeLikedEventIds bulk-imports a list
// of event IDs used to transfer pre-login guest likes to the authenticated user after sign-in.
@Service
@RequiredArgsConstructor
public class UserLikeService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final UserEventLikeRepository userEventLikeRepository;

    @Transactional(readOnly = true)
    public List<String> getLikedEventIds(String email) {
        UserEntity user = getUserByEmail(email);
        return userEventLikeRepository.findEventIdsByUserId(user.getId());
    }

    @Transactional
    public List<String> likeEvent(String email, String eventId) {
        UserEntity user = getUserByEmail(email);
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));

        if (!userEventLikeRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
            userEventLikeRepository.save(UserEventLikeEntity.builder()
                    .id(new UserEventLikeId(user.getId(), eventId))
                    .user(user)
                    .event(event)
                    .build());
        }

        return userEventLikeRepository.findEventIdsByUserId(user.getId());
    }

    @Transactional
    public List<String> unlikeEvent(String email, String eventId) {
        UserEntity user = getUserByEmail(email);
        userEventLikeRepository.deleteByUserIdAndEventId(user.getId(), eventId);
        return userEventLikeRepository.findEventIdsByUserId(user.getId());
    }

    @Transactional
    public List<String> mergeLikedEventIds(String email, List<String> eventIds) {
        UserEntity user = getUserByEmail(email);
        Set<String> uniqueIds = eventIds.stream()
                .filter(Objects::nonNull)
                .filter(eventId -> !eventId.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (EventEntity event : eventRepository.findAllById(uniqueIds)) {
            String eventId = event.getId();
            if (!userEventLikeRepository.existsByUserIdAndEventId(user.getId(), eventId)) {
                userEventLikeRepository.save(UserEventLikeEntity.builder()
                        .id(new UserEventLikeId(user.getId(), eventId))
                        .user(user)
                        .event(event)
                        .build());
            }
        }

        return userEventLikeRepository.findEventIdsByUserId(user.getId());
    }

    private UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
