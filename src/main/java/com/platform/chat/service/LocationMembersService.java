package com.platform.chat.service;

import com.platform.chat.dto.LocationUserDto;
import com.platform.chat.dto.LocationUserProjection;
import com.platform.chat.dto.PagedResponse;
import com.platform.chat.repository.LocationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationMembersService {

    private final LocationMemberRepository locationMemberRepository;

    public PagedResponse<LocationUserDto> getLocationUsers(
            Long locationId, String query, String filter, int page, int size
    ) {
        Boolean onlineFilter = switch (filter == null ? "all" : filter) {
            case "online" -> true;
            case "offline" -> false;
            default -> null;
        };

        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Pageable pageable = PageRequest.of(page, size);

        Page<LocationUserProjection> result = locationMemberRepository.search(
                locationId, normalizedQuery, onlineFilter, pageable
        );

        List<LocationUserDto> items = result.getContent().stream()
                .map(LocationUserDto::fromProjection)
                .toList();

        return new PagedResponse<>(items, result.hasNext());
    }
}