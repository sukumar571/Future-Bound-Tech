package com.futureboundtech.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/** One row of the certificate eligibility evaluation (e.g. "Quiz score ≥ 75% — actual 82%"). */
@Data
@Accessors(chain = true)
public class CertificateCriterionDto {

    private String label;
    private String requirement;
    private String actual;
    private boolean met;

    /** True when the criterion is informational (not configured / nothing to check). */
    private boolean skipped;
}
