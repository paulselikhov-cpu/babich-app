package com.platform.chat.dto.response;
import java.util.List;
public record LocationMembersPagedResponse<T>(List<T> items, boolean hasMore) {}