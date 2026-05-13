/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class PaginationSupport {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationSupport() {
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    public static int totalPages(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) {
            return 1;
        }
        return (totalItems + pageSize - 1) / pageSize;
    }

    public static <T> List<T> sortAndPaginate(List<T> items, String sort, String dir,
            Map<String, SortColumn<T>> sortColumns, Integer page, Integer pageSize) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<T> sorted = new ArrayList<>(items);
        if (sort != null && !sort.isBlank() && sortColumns.containsKey(sort)) {
            SortColumn<T> sortColumn = sortColumns.get(sort);
            sorted.sort(sortColumn.comparator(dir));
        }
        int normalizedPageSize = normalizePageSize(pageSize);
        int normalizedPage = normalizePageForTotal(page, sorted.size(), normalizedPageSize);
        int fromIndex = (normalizedPage - 1) * normalizedPageSize;
        int toIndex = Math.min(fromIndex + normalizedPageSize, sorted.size());
        return sorted.subList(fromIndex, toIndex);
    }

    public record PaginationMeta(int page, int pageSize, int totalItems, int totalPages) {
    }

    public static PaginationMeta meta(Integer page, Integer pageSize, int totalItems) {
        int normalizedPageSize = normalizePageSize(pageSize);
        int normalizedPage = normalizePageForTotal(page, totalItems, normalizedPageSize);
        return new PaginationMeta(normalizedPage, normalizedPageSize, totalItems,
                totalPages(totalItems, normalizedPageSize));
    }

    private static int normalizePageForTotal(Integer page, int totalItems, int pageSize) {
        int totalPages = totalPages(totalItems, pageSize);
        return Math.min(normalizePage(page), totalPages);
    }

    public record SortColumn<T>(Comparator<T> ascending, Comparator<T> descending) {
        Comparator<T> comparator(String dir) {
            return "desc".equalsIgnoreCase(dir) ? descending : ascending;
        }
    }

    public static <T, U> SortColumn<T> sortColumn(Function<T, U> keyExtractor, Comparator<U> valueComparator) {
        return new SortColumn<>(compareByValue(keyExtractor, valueComparator, false),
                compareByValue(keyExtractor, valueComparator, true));
    }

    private static <T, U> Comparator<T> compareByValue(Function<T, U> keyExtractor, Comparator<U> valueComparator,
            boolean descending) {
        return (left, right) -> {
            U leftValue = keyExtractor.apply(left);
            U rightValue = keyExtractor.apply(right);
            if (leftValue == null && rightValue == null) {
                return 0;
            }
            if (leftValue == null) {
                return 1;
            }
            if (rightValue == null) {
                return -1;
            }
            int result = valueComparator.compare(leftValue, rightValue);
            return descending ? -result : result;
        };
    }
}
