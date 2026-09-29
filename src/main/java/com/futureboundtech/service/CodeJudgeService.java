package com.futureboundtech.service;

/**
 * Contract for an <em>optional</em> online-judge capability for coding questions.
 *
 * <p><strong>Security posture (mandatory):</strong> candidate source code must
 * NEVER be compiled or executed inside the main Spring Boot server. Running
 * untrusted code in-process risks arbitrary code execution, resource exhaustion
 * and container/host escape. This application therefore ships <em>no</em>
 * implementation that executes code — coding questions are a browsable,
 * study-only library.</p>
 *
 * <p>If judged execution is ever required, implement this interface as a client
 * to a <strong>separate, hardened sandbox service</strong> deployed outside this
 * application's process and network blast radius. That service should provide, at
 * minimum:</p>
 * <ul>
 *   <li>Per-run isolation in throwaway containers/micro-VMs (e.g. gVisor/Firecracker),
 *       destroyed after every submission.</li>
 *   <li>Default-deny egress networking (no internet, no access to internal services).</li>
 *   <li>Strict CPU / wall-clock / memory / file-descriptor / output-size rlimits.</li>
 *   <li>Read-only, non-root filesystem with a tmpfs work dir and no host mounts.</li>
 *   <li>A fixed allow-list of compilers/interpreters and pinned versions.</li>
 *   <li>Submission rate limiting and payload size caps to blunt abuse/DoS.</li>
 *   <li>No secrets, credentials or app configuration exposed to the sandbox.</li>
 *   <li>A narrow, authenticated, versioned API between this app and the judge
 *       (submit {language, source, stdin, limits} -&gt; poll/get {status, stdout,
 *       stderr, exitCode, timeMs, memKb}), never a shared DB or shell.</li>
 * </ul>
 *
 * <p>Nothing in this repository calls this interface from a request path, so the
 * feature is inert until a compliant external judge is wired in.</p>
 */
public interface CodeJudgeService {

    /** True only when a real, external sandbox judge is configured and reachable. */
    boolean isAvailable();

    /**
     * Submits code to the external sandbox for evaluation. Implementations MUST
     * delegate to an isolated service and MUST NOT run code in this JVM.
     */
    JudgeResult submit(JudgeRequest request);

    /** Immutable-ish request DTO for the judge. */
    record JudgeRequest(String language, String source, String stdin,
                        int timeLimitSeconds, int memoryLimitMb) { }

    /** Result returned by the sandbox judge. */
    record JudgeResult(String status, Integer exitCode, String stdout,
                       String stderr, long timeMs, long memoryKb) { }
}
