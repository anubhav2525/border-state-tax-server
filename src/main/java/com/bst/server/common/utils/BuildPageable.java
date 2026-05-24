package com.bst.server.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BuildPageable {
    public Pageable build(
            Integer page, Integer size, String sortBy,
            String sortDir, Set<String> allowedSortFields, String defaultSortedField) {
        String safeSortField = allowedSortFields.contains(sortBy) ? sortBy : defaultSortedField;

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortField).ascending()
                : Sort.by(safeSortField).descending();

        return PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                sort);
    }
}
