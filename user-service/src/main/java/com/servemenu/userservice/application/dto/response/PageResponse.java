package com.servemenu.userservice.application.dto.response;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

/**
 * Generic Page Response DTO
 * 
 * Wraps Spring Data Page to provide a stable JSON structure for serialization.
 * This is especially important for Redis caching and API responses.
 * 
 * Spring Data recommends using DTOs instead of serializing Page directly:
 * https://docs.spring.io/spring-data/commons/reference/repositories/core-extensions.html#core.web.pageables
 */
public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean empty
) implements Serializable {
    
    /**
     * Create PageResponse from Spring Data Page
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }
}
