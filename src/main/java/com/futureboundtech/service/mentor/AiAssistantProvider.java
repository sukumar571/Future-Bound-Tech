package com.futureboundtech.service.mentor;

import java.util.List;

/**
 * Contract for the optional "Future Mentor" answer generator.
 *
 * <p><strong>This is an optional enhancement, never a core dependency.</strong>
 * The LMS works identically whether or not a real AI provider is configured. The
 * application always ships with a {@link LocalKnowledgeMentorProvider fallback}
 * that answers offline; a network provider
 * ({@link OpenAiMentorProvider}) is only used when explicitly enabled and given an
 * API key through the environment. Any provider failure degrades back to the local
 * assistant so a request path can never be broken by the AI being unreachable.</p>
 *
 * <p><strong>Security posture:</strong> implementations receive only a
 * student-scoped, non-sensitive learning digest ({@link MentorPrompt#learnerContext()}).
 * They must never be handed other students' data or private admin information, and the
 * composed answer must carry a "verify important details" disclaimer because the
 * assistant is <em>not</em> guaranteed to be correct.</p>
 */
public interface AiAssistantProvider {

    /** Stable identifier for the provider, e.g. {@code "openai"} or {@code "local"}. */
    String id();

    /** True only when this provider is configured and safe to invoke right now. */
    boolean isAvailable();

    /** Produce an assistant reply for the given prompt. May throw on transient errors. */
    MentorReply answer(MentorPrompt prompt);

    /** Immutable reply returned by a provider. */
    record MentorReply(String content, boolean aiGenerated, String providerId) { }

    /** A single prior conversation turn replayed to the model for context. */
    record Turn(String role, String content) { }

    /**
     * The full instruction set handed to a provider.
     *
     * @param systemInstructions the fixed, safety-guarded persona/instructions
     * @param learnerContext     a student-scoped, non-sensitive digest of the
     *                           learner's own enrolled courses/progress (may be empty)
     * @param history            the most recent prior turns (already trimmed), oldest first
     * @param question           the learner's latest free-text question
     */
    record MentorPrompt(String systemInstructions, String learnerContext,
                        List<Turn> history, String question) { }
}
