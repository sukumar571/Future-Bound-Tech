package com.futureboundtech.dto.api;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination metadata paired with a page of content, used as the {@code data}
 * payload for every list endpoint. Mirrors the shape of Spring Data's
 * {@link Page} without leaking its (version-sensitive) JSON into the API.
 */
@Data
@Accessors(chain = true)
public class PageDto<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean sorted;

    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto<T>()
                .setContent(page.getContent())
                .setPage(page.getNumber())
                .setSize(page.getSize())
                .setTotalElements(page.getTotalElements())
                .setTotalPages(page.getTotalPages())
                .setSorted(page.getSort().isSorted());
    }
}
