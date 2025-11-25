package org.example.expenseapi.repository;

import org.example.expenseapi.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("select coalesce(sum(e.amount), 0) from Expense e " +
           "where e.expenseCategory.id = :categoryId and e.expenseDate >= :start and e.expenseDate <= :end")
    BigDecimal sumAmountByCategoryAndDateBetween(@Param("categoryId") Long categoryId,
                                                 @Param("start") LocalDate start,
                                                 @Param("end") LocalDate end);

    List<Expense> findAllByExpenseDateBetweenOrderByExpenseDateDesc(LocalDate start, LocalDate end);

    // Sum per category within a date range. Returns list of Object[] { categoryId (Long), sum (BigDecimal) }
    @Query("select e.expenseCategory.id as catId, coalesce(sum(e.amount),0) as s " +
           "from Expense e where e.expenseDate >= :start and e.expenseDate <= :end " +
           "and (:categoryIds is null or e.expenseCategory.id in :categoryIds) " +
           "group by e.expenseCategory.id")
    List<Object[]> sumAmountGroupedByCategoryBetween(@Param("start") LocalDate start,
                                                     @Param("end") LocalDate end,
                                                     @Param("categoryIds") List<Long> categoryIds);

    default Page<Expense> search(String keyword, List<Long> categoryIds, LocalDate start, LocalDate end, Pageable pageable) {
        // keyword normalization
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();

        // sanitize category ids: remove nulls/duplicates
        List<Long> cats = (categoryIds == null || categoryIds.isEmpty()) ? null : new java.util.ArrayList<>(categoryIds);
        if (cats != null) {
            cats.removeIf(java.util.Objects::isNull);
            if (cats.isEmpty()) cats = null;
            else cats = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(cats));
        }

        List<Expense> byDate = findAllByExpenseDateBetweenOrderByExpenseDateDesc(start, end);

        // apply in-memory filters
        final String keywordNormalized = k;
        final List<Long> catsNormalized = cats;
        List<Expense> filtered = byDate.stream()
                .filter(ex -> {
                    if (keywordNormalized != null) {
                        String d = ex.getDesignation() == null ? "" : ex.getDesignation().toLowerCase();
                        if (!d.contains(keywordNormalized)) return false;
                    }
                    if (catsNormalized != null) {
                        var c = ex.getExpenseCategory();
                        if (c == null || c.getId() == null) return false;
                        if (!catsNormalized.contains(c.getId())) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int fromIndex = Math.max(0, (int) pageable.getOffset());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Expense> pageContent = fromIndex >= toIndex ? java.util.Collections.emptyList() : filtered.subList(fromIndex, toIndex);

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, total);
    }
}
