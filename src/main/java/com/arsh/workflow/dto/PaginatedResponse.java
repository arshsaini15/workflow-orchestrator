package com.arsh.workflow.dto;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}