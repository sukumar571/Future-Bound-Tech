package com.futureboundtech.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single label/value pair used to feed dashboard charts. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValueDto {
    private String name;
    private long value;
}
