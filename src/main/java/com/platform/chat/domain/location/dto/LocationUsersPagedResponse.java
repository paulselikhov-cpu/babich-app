package com.platform.chat.domain.location.dto;
import java.util.List;
public record LocationUsersPagedResponse<T>(List<T> items, boolean hasMore) {}