package com.careos.hms.common.validation;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static Pageable createPageable(
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection,
            Set<String> allowedSortFields
    ) {
        int resolvedPage = page != null ? page : ApiConstants.DEFAULT_PAGE_NUMBER;
        int resolvedSize = size != null ? size : ApiConstants.DEFAULT_PAGE_SIZE;

        if (resolvedPage < 0) {
            throw new InvalidRequestException("Page index must be zero or greater");
        }
        if (resolvedSize < 1 || resolvedSize > ApiConstants.MAX_PAGE_SIZE) {
            throw new InvalidRequestException(
                    String.format("Page size must be between 1 and %d", ApiConstants.MAX_PAGE_SIZE)
            );
        }

        String resolvedSortField = (sortBy != null && !sortBy.isBlank())
                ? sortBy.trim()
                : ApiConstants.DEFAULT_SORT_FIELD;

        if (allowedSortFields != null && !allowedSortFields.isEmpty()
                && !allowedSortFields.contains(resolvedSortField)) {
            throw new InvalidRequestException("Unsupported sort field: " + resolvedSortField);
        }

        Sort.Direction direction = ApiConstants.DEFAULT_SORT_DIRECTION.equalsIgnoreCase(sortDirection)
                || sortDirection == null
                || sortDirection.isBlank()
                ? Sort.Direction.ASC
                : parseDirection(sortDirection);

        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortField));
    }

    private static Sort.Direction parseDirection(String sortDirection) {
        try {
            return Sort.Direction.fromString(sortDirection.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Sort direction must be ASC or DESC");
        }
    }
}
