package com.futureboundtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body of a Future Mentor chat request. */
@Data
public class MentorChatRequest {

    @NotBlank(message = "Please type a question first.")
    // A coarse transport ceiling; the tighter, configurable limit is enforced in the service.
    @Size(max = 5000, message = "Your question is too long — please shorten it.")
    private String message;
}
