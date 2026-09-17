package com.platform.chat.dto.response;
import java.util.List;
public record LocationUsersPagedResponse<T>(List<T> items, boolean hasMore) {}