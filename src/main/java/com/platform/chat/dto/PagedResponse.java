package com.platform.chat.dto;
import java.util.List;
public record PagedResponse<T>(List<T> items, boolean hasMore) {}