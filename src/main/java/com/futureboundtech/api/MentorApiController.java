package com.futureboundtech.api;

import com.futureboundtech.dto.MentorChatRequest;
import com.futureboundtech.dto.MentorMessageDto;
import com.futureboundtech.dto.MentorReplyDto;
import com.futureboundtech.dto.api.ApiResponse;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.security.CustomUserDetails;
import com.futureboundtech.service.FutureMentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * JSON endpoints behind the Future Mentor chat page.
 *
 * <p>Access: {@code /api/student/**} already requires the STUDENT role, and every
 * call is scoped to the authenticated principal — a student can only ever read or
 * clear their own history and only ever receives answers built from their own
 * learning context. CSRF is exempted for {@code /api/**} (session-cookie auth).</p>
 */
@RestController
@RequestMapping("/api/student/mentor")
@RequiredArgsConstructor
public class MentorApiController {

    private final FutureMentorService futureMentorService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<MentorReplyDto>> chat(@Valid @RequestBody MentorChatRequest request,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        requirePrincipal(principal);
        MentorReplyDto reply = futureMentorService.ask(principal.getUser(), request.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(reply));
    }

    @GetMapping("/history")
    public ApiResponse<List<MentorMessageDto>> history(@AuthenticationPrincipal CustomUserDetails principal) {
        requirePrincipal(principal);
        return ApiResponse.ok(futureMentorService.history(principal.getUser()));
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal CustomUserDetails principal) {
        requirePrincipal(principal);
        futureMentorService.clearHistory(principal.getUser());
        return ApiResponse.ok("Your Future Mentor history has been cleared.", null);
    }

    private void requirePrincipal(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException("You must be signed in to use Future Mentor.");
        }
    }
}
