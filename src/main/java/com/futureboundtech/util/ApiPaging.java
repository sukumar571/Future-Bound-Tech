package com.futureboundtech.util;

import com.futureboundtech.dto.api.PageDto;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies pagination, sorting and (already-filtered) slicing to an in-memory
 * list. The institute catalog queries use join-fetch aggregates that cannot be
 * safely {@code Page}-paged at the SQL level, so list endpoints page here.
 */
public final class ApiPaging {

    private ApiPaging() {
    }

    public static <T> PageDto<T> paginate(List<T> source, Pageable pageable) {
        List<T> list = new ArrayList<>(source == null ? List.of() : source);

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<T> comparator = null;
            for (Sort.Order order : sort) {
                Comparator<T> c = comparatorFor(order.getProperty());
                if (order.isDescending()) {
                    c = c.reversed();
                }
                comparator = (comparator == null) ? c : comparator.thenComparing(c);
            }
            if (comparator != null) {
                list.sort(comparator);
            }
        }

        int total = list.size();
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = pageable.getPageSize();

        List<T> content;
        int totalPages;
        if (size <= 0) {
            content = list;
            size = total;
            totalPages = total == 0 ? 0 : 1;
        } else {
            int from = Math.min(page * size, total);
            int to = Math.min(from + size, total);
            content = new ArrayList<>(list.subList(from, to));
            totalPages = (int) Math.ceil((double) total / size);
        }

        return new PageDto<T>()
                .setContent(content)
                .setPage(size <= 0 ? 0 : page)
                .setSize(size)
                .setTotalElements(total)
                .setTotalPages(totalPages)
                .setSorted(sort.isSorted());
    }

    /** Null-safe comparator over a bean property; unknown/unreadable props keep order. */
    private static <T> Comparator<T> comparatorFor(String property) {
        return (a, b) -> compare(readProperty(a, property), readProperty(b, property));
    }

    private static Object readProperty(Object target, String property) {
        if (target == null || property == null || property.isBlank()) {
            return null;
        }
        try {
            return new BeanWrapperImpl(target).getPropertyValue(property);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
        if (a instanceof Comparable && a.getClass().isInstance(b)) {
            return ((Comparable) a).compareTo(b);
        }
        return a.toString().compareToIgnoreCase(b.toString());
    }
}
