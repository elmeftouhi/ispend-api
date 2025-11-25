package org.example.expenseapi.util;

import org.example.expenseapi.dto.PaginatedResponse;
import org.example.expenseapi.dto.PaginationMetadata;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PaginationUtils {
    private PaginationUtils() {}

    public static <T, R> PaginatedResponse<R> toPaginatedResponse(Page<T> page, Function<? super T, ? extends R> mapper) {
        Page<R> mapped = page.map(mapper);
        List<R> content = mapped.getContent();

        PaginationMetadata meta = new PaginationMetadata();
        // Use 1-based page numbering in the response (show first page as 1)
        meta.setPage(mapped.getNumber() + 1);
        meta.setSize(mapped.getSize());
        meta.setTotalItems(mapped.getTotalElements());
        meta.setTotalPages(mapped.getTotalPages());
        meta.setHasNext(mapped.hasNext());
        meta.setHasPrevious(mapped.hasPrevious());
        // next/previous pages should be 1-based as well
        meta.setNextPage(mapped.hasNext() ? mapped.getNumber() + 2 : null);
        meta.setPreviousPage(mapped.hasPrevious() ? mapped.getNumber() : null);

        return new PaginatedResponse<>(content, meta);
    }
}
