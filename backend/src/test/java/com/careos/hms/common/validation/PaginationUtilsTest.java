package com.careos.hms.common.validation;

import com.careos.hms.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationUtilsTest {

    @Test
    void createsDefaultPageableWhenParametersAreNull() {
        Pageable pageable = PaginationUtils.createPageable(null, null, null, null, Set.of("id", "createdAt"));

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 10, "passwordHash", "ASC", Set.of("id", "createdAt")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    @Test
    void rejectsOutOfRangePageSize() {
        assertThatThrownBy(() -> PaginationUtils.createPageable(0, 500, "id", "DESC", Set.of("id")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Page size must be between 1 and 100");
    }
}
