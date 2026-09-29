package com.futureboundtech.service;

import com.futureboundtech.dto.MentorMessageDto;
import com.futureboundtech.dto.MentorReplyDto;
import com.futureboundtech.entity.User;

import java.util.List;

/**
 * The optional Future Mentor learning assistant. Every method is scoped to the
 * given authenticated student, so a student can only ever read or clear their own
 * conversation history and the assistant only ever sees that student's own
 * (non-sensitive) learning context.
 */
public interface FutureMentorService {

    /** Whether the assistant feature is switched on at all. */
    boolean isFeatureEnabled();

    /** Whether a real AI provider is configured (vs. running as the offline guide). */
    boolean isAiConfigured();

    /** Whether chat history is being persisted (drives the privacy note in the UI). */
    boolean isHistoryEnabled();

    /** This student's stored history, oldest first. Empty when history is disabled. */
    List<MentorMessageDto> history(User user);

    /** Answer one question for this student, applying rate limits and safe context. */
    MentorReplyDto ask(User user, String question);

    /** Permanently delete this student's own stored history (privacy / right to erase). */
    void clearHistory(User user);
}
