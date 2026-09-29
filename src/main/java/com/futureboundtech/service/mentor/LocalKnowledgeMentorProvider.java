package com.futureboundtech.service.mentor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Always-available, offline fallback for the Future Mentor. It answers with
 * curated, topic-aware <em>study guidance</em> and links to the right part of the
 * LMS — never fabricated facts — so it is safe to use without any AI provider.
 *
 * <p>Because it is purely local it is deterministic, has no secrets, makes no
 * network calls and cannot leak data. It deliberately does not pretend to be a
 * human tutor and always reminds the learner to verify important details.</p>
 */
@Component
public class LocalKnowledgeMentorProvider implements AiAssistantProvider {

    public static final String ID = "local";

    private static final String DISCLAIMER =
            "\n\nI'm Future Mentor's offline study guide — my tips may be incomplete, so "
            + "always verify important details against your course material.";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean isAvailable() {
        return true; // never depends on external configuration
    }

    @Override
    public MentorReply answer(MentorPrompt prompt) {
        String q = prompt == null || prompt.question() == null
                ? "" : prompt.question().toLowerCase(Locale.ROOT);

        String body;
        if (containsAny(q, "java", "jvm", "spring", "servlet", "jpa", "hibernate")) {
            body = "Java pointers:\n"
                    + "• Solidify OOP first — classes, inheritance, polymorphism, interfaces, generics.\n"
                    + "• Master collections (List/Set/Map), streams, lambdas and Optional.\n"
                    + "• For backend roles, learn Spring Boot: dependency injection, @RestController, JPA repositories.\n"
                    + "• Practise exception handling and the equals/hashCode/toString contracts.\n"
                    + "Open Practice → MCQ/Coding and filter the Java category to drill these.";
        } else if (containsAny(q, "python", "pandas", "numpy", "django", "flask")) {
            body = "Python pointers:\n"
                    + "• Be fluent with lists, dicts, sets, comprehensions and slicing.\n"
                    + "• Understand functions, *args/**kwargs, and context managers (with).\n"
                    + "• For data roles: NumPy, Pandas and basic visualisation. For web: Flask or Django.\n"
                    + "• Learn virtual environments and how to read tracebacks.\n"
                    + "Use the Practice section (Python category) to build muscle memory.";
        } else if (containsAny(q, "sql", "query", "join", "database", "normal", "index")) {
            body = "SQL pointers:\n"
                    + "• Order of execution: FROM/JOIN → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT.\n"
                    + "• Know JOIN types (INNER vs LEFT) and when to aggregate.\n"
                    + "• Understand primary/foreign keys, normalisation and how indexes speed reads.\n"
                    + "• Practice EXPLAIN on real queries.\n"
                    + "The Placement Prep and Practice areas have SQL drills.";
        } else if (containsAny(q, "web", "html", "css", "javascript", "react", "frontend", "backend", "api", "rest")) {
            body = "Web development pointers:\n"
                    + "• Fundamentals: semantic HTML, responsive CSS (flexbox/grid), core JavaScript (async, fetch, events).\n"
                    + "• Learn how HTTP + REST work: methods, status codes, JSON payloads.\n"
                    + "• Then pick one framework (e.g. React) and one backend path (e.g. Spring Boot or Node).\n"
                    + "• Build one small full project end-to-end — it teaches more than tutorials.";
        } else if (containsAny(q, "aws", "cloud", "ec2", "s3", "lambda", "devops")) {
            body = "AWS pointers:\n"
                    + "• Start with IAM, EC2, S3 and VPC basics — the core building blocks.\n"
                    + "• Then add a managed service relevant to your goal (RDS, Lambda, CloudWatch).\n"
                    + "• Learn the shared-responsibility model and cost basics.\n"
                    + "The AWS Free Tier + a solution-architect study track is a good path. Check Placements → Prep tracks.";
        } else if (containsAny(q, "ai", "ml", "machine learning", "deep learning", "llm", "model train")) {
            body = "AI / ML pointers:\n"
                    + "• Prereqs: Python, basic statistics and linear algebra intuition.\n"
                    + "• Learn the workflow: data prep → features → train → evaluate → iterate.\n"
                    + "• Start with classic ML (scikit-learn) before deep learning.\n"
                    + "• For LLMs, understand embeddings, prompts and evaluation rather than just APIs.";
        } else if (containsAny(q, "interview", "dsa", "data structure", "algorithm", "resume", "hr round", "prep")) {
            body = "Interview-prep pointers:\n"
                    + "• Rotate topics: arrays/strings → hashing → trees/graphs → DP → your stack.\n"
                    + "• Do timed practice and talk through your reasoning out loud.\n"
                    + "• Keep a short project you can explain deeply.\n"
                    + "• Mock interviews matter — see Placements → Placement Prep for tracks and job opportunities.";
        } else if (containsAny(q, "explain", "code", "example", "how does", "what is", "difference")) {
            body = "Happy to help you work through it. A good study loop is: read the concept → write a tiny "
                    + "snippet → break it on purpose → fix it → explain it back in one sentence. Open a Coding "
                    + "question in Practice for worked, browsable examples you can reason through step by step.";
        } else if (containsAny(q, "syllabus", "curriculum", "module", "lesson", "progress", "navigat", "where do i")) {
            body = "To navigate your learning: Student Portal → Syllabus lists your enrolled modules; the course "
                    + "page tracks lesson completion and progress; Live Classes shows scheduled sessions; "
                    + "Assignments/Quizzes are under their own tabs. Your dashboard's 'Continue Learning' jumps "
                    + "straight to your next lesson.";
        } else {
            body = "I can help with Java, Python, SQL, web development, AWS, AI/ML, interview preparation, "
                    + "coding explanations and navigating your syllabus. Ask a specific question — for example "
                    + "\"how do I prepare for Java interviews?\" or \"explain SQL joins\" — and I'll point you "
                    + "to the right practice and course material.";
        }
        return new MentorReply(body + DISCLAIMER, false, ID);
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String haystack, String... needles) {
        return containsAny(haystack, List.of(needles));
    }
}
