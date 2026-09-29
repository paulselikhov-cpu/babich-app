package com.platform.chat.base.location.dto;
import java.util.List;
public record LocationUsersPagedResponse<T>(List<T> items, boolean hasMore) {}