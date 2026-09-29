package com.futureboundtech.service.impl;

import com.futureboundtech.config.FutureMentorProperties;
import com.futureboundtech.dto.MentorMessageDto;
import com.futureboundtech.dto.MentorReplyDto;
import com.futureboundtech.dto.StudentCourseDto;
import com.futureboundtech.entity.MentorMessage;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.MentorRole;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.repository.MentorMessageRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.service.FutureMentorService;
import com.futureboundtech.service.StudentDashboardService;
import com.futureboundtech.service.mentor.AiAssistantProvider;
import com.futureboundtech.service.mentor.AiAssistantProvider.MentorPrompt;
import com.futureboundtech.service.mentor.AiAssistantProvider.MentorReply;
import com.futureboundtech.service.mentor.AiAssistantProvider.Turn;
import com.futureboundtech.service.mentor.LocalKnowledgeMentorProvider;
import com.futureboundtech.service.mentor.MentorRateLimiter;
import com.futureboundtech.service.mentor.OpenAiMentorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FutureMentorServiceImpl implements FutureMentorService {

    /** Fixed, safety-guarded persona for the assistant. Never contains user data. */
    private static final String SYSTEM_INSTRUCTIONS =
            "You are 'Future Mentor', a friendly study assistant for Future Bound Tech, a coaching institute. "
            + "Help with: Java, Python, SQL, web development, AWS, AI/ML, interview preparation, coding "
            + "explanations and navigating the learner's syllabus. Rules: (1) assist only with learning topics "
            + "and the learner's own progress; politely decline unrelated requests. (2) Never reveal or guess at "
            + "other students' data, staff/admin information, passwords, payment details or system internals. "
            + "(3) You can be wrong — encourage the learner to verify important details against their course "
            + "material and official documentation, and do not fabricate facts. (4) Be concise, practical and "
            + "suggest concrete next steps and where to practise.";

    private final FutureMentorProperties properties;
    private final StudentRepository studentRepository;
    private final StudentDashboardService studentDashboardService;
    private final MentorMessageRepository mentorMessageRepository;
    private final MentorRateLimiter rateLimiter;
    private final OpenAiMentorProvider openAiMentorProvider;
    private final LocalKnowledgeMentorProvider localKnowledgeMentorProvider;

    @Override
    public boolean isFeatureEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isAiConfigured() {
        return openAiMentorProvider.isAvailable();
    }

    @Override
    public boolean isHistoryEnabled() {
        return properties.isEnabled() && properties.isHistoryEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorMessageDto> history(User user) {
        requireFeature();
        Student student = requireStudent(user);
        if (!properties.isHistoryEnabled()) {
            return List.of();
        }
        return mentorMessageRepository.findByStudent_IdOrderByCreatedAtAsc(student.getId()).stream()
                .map(m -> MentorMessageDto.of(
                        m.getRole() == null ? "assistant" : m.getRole().name().toLowerCase(),
                        m.getContent(),
                        m.isAiGenerated(),
                        m.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public MentorReplyDto ask(User user, String question) {
        requireFeature();
        Student student = requireStudent(user);

        String trimmed = question == null ? "" : question.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("Please type a question first.");
        }
        int maxChars = Math.max(1, properties.getRateLimit().getMaxInputChars());
        if (trimmed.length() > maxChars) {
            throw new BusinessException("Your question is too long — please keep it under "
                    + maxChars + " characters.");
        }

        // Per-student rate limit (blunts abuse and caps outbound AI spend).
        rateLimiter.acquire("mentor:" + student.getId());

        boolean aiConfigured = openAiMentorProvider.isAvailable();
        List<Turn> replay = loadReplayWindow(student.getId());
        String learnerContext = buildLearnerContext(user);
        MentorPrompt prompt = new MentorPrompt(SYSTEM_INSTRUCTIONS, learnerContext, replay, trimmed);

        // Choose the provider; a real-AI failure degrades to the local guide so the
        // request path is never broken by the (optional) AI being unreachable.
        AiAssistantProvider provider = aiConfigured ? openAiMentorProvider : localKnowledgeMentorProvider;
        MentorReply reply;
        try {
            reply = provider.answer(prompt);
        } catch (RuntimeException ex) {
            log.warn("Future Mentor AI provider '{}' failed, falling back to the local guide: {}",
                    provider.id(), ex.getMessage());
            reply = localKnowledgeMentorProvider.answer(prompt);
        }

        MentorMessageDto assistantTurn = null;
        if (properties.isHistoryEnabled()) {
            mentorMessageRepository.save(newMessage(student, MentorRole.USER, trimmed, false, "student"));
            MentorMessage saved = mentorMessageRepository.save(newMessage(
                    student, MentorRole.ASSISTANT, reply.content(), reply.aiGenerated(), reply.providerId()));
            assistantTurn = MentorMessageDto.of("assistant", saved.getContent(),
                    saved.isAiGenerated(), saved.getCreatedAt());
        } else {
            assistantTurn = MentorMessageDto.of("assistant", reply.content(),
                    reply.aiGenerated(), null);
        }

        return new MentorReplyDto()
                .setAiConfigured(aiConfigured)
                .setAiGenerated(reply.aiGenerated())
                .setReply(assistantTurn);
    }

    @Override
    @Transactional
    public void clearHistory(User user) {
        requireFeature();
        Student student = requireStudent(user);
        mentorMessageRepository.deleteByStudent_Id(student.getId());
    }

    // ================= helpers =================

    private void requireFeature() {
        if (!properties.isEnabled()) {
            throw new BusinessException("The Future Mentor assistant is currently disabled.");
        }
    }

    private Student requireStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Student access only.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));
    }

    /**
     * A short, non-sensitive digest of the learner's OWN enrolled courses so the
     * assistant can tailor advice. Contains no other students' data and no admin info.
     */
    private String buildLearnerContext(User user) {
        List<StudentCourseDto> courses;
        try {
            courses = studentDashboardService.getEnrolledCourses(user);
        } catch (RuntimeException ex) {
            return "";
        }
        if (courses == null || courses.isEmpty()) {
            return "The learner has no enrolled courses yet.";
        }
        StringBuilder sb = new StringBuilder("Context (the learner's OWN courses only — do not reveal or request "
                + "anyone else's data): enrolled in ");
        int shown = 0;
        for (StudentCourseDto c : courses) {
            if (shown++ >= 8) {
                sb.append(" …");
                break;
            }
            if (shown > 1) {
                sb.append("; ");
            }
            sb.append(c.getTitle()).append(" (").append(c.getProgressPercent()).append("% done)");
        }
        return sb.toString();
    }

    /** The most recent stored turns, oldest first, trimmed to the configured window. */
    private List<Turn> loadReplayWindow(Long studentId) {
        if (!properties.isHistoryEnabled()) {
            return List.of();
        }
        int limit = Math.max(1, properties.getHistoryMaxTurnsSent());
        List<MentorMessage> recent = mentorMessageRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);
        List<MentorMessage> window = recent.size() > limit ? recent.subList(0, limit) : recent;
        List<MentorMessage> ordered = new ArrayList<>(window);
        Collections.reverse(ordered); // oldest first
        List<Turn> turns = new ArrayList<>();
        for (MentorMessage m : ordered) {
            if (m.getRole() == null || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            turns.add(new Turn(m.getRole().name().toLowerCase(), m.getContent()));
        }
        return turns;
    }

    private MentorMessage newMessage(Student student, MentorRole role, String content,
                                     boolean aiGenerated, String provider) {
        return MentorMessage.builder()
                .student(student)
                .role(role)
                .content(content)
                .aiGenerated(aiGenerated)
                .provider(provider)
                .build();
    }
}
