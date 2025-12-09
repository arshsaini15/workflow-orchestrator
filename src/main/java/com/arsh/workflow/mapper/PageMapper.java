package com.arsh.workflow.mapper;

import com.arsh.workflow.dto.response.PaginatedResponse;
import org.springframework.data.domain.Page;

public class PageMapper {

    public static <T> PaginatedResponse<T> toResponse(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
