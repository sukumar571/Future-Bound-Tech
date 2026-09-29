package com.futureboundtech.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

/**
 * Backing form for a student's own profile edit: an optional profile photo upload
 * plus a couple of editable text fields. All other profile fields are read-only.
 */
@Data
@Accessors(chain = true)
public class StudentProfileFormDto {

    /** Optional new profile photo. When empty, the existing photo is kept. */
    private MultipartFile photoFile;

    @Size(max = 200, message = "Education must be 200 characters or fewer")
    private String education;
}
