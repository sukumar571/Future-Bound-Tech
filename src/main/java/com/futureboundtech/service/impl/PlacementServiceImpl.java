package com.futureboundtech.service.impl;

import com.futureboundtech.dto.PlacementDto;
import com.futureboundtech.dto.PlacementFormDto;
import com.futureboundtech.entity.Placement;
import com.futureboundtech.entity.PlacementSave;
import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.Role;
import com.futureboundtech.exception.BusinessException;
import com.futureboundtech.exception.ResourceNotFoundException;
import com.futureboundtech.repository.PlacementRepository;
import com.futureboundtech.repository.PlacementSaveRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Placement module implementation (Phase 17).
 * Safety rules enforced here: application links must be authorized http/https
 * URLs (never javascript:/data: pseudo-URLs), and the module stores no
 * placement claims — it only surfaces admin-curated posts with an explicit
 * "no guaranteed placement" disclaimer rendered in the UI.
 */
@Service
@RequiredArgsConstructor
public class PlacementServiceImpl implements PlacementService {

    private static final Set<String> JOB_TYPES = Set.of(
            "Full-time", "Internship", "Contract", "Part-time", "Remote");

    private final PlacementRepository placementRepository;
    private final PlacementSaveRepository placementSaveRepository;
    private final StudentRepository studentRepository;

    // =====================================================================
    // Admin
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<PlacementDto> listAll() {
        return placementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> toDto(p, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementDto getPlacement(Long id) {
        return toDto(requirePlacement(id), null);
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementFormDto getPlacementForm(Long id) {
        return toForm(requirePlacement(id));
    }

    @Override
    @Transactional
    public PlacementDto createPlacement(PlacementFormDto form) {
        Placement p = new Placement();
        applyFields(p, form);
        return toDto(placementRepository.save(p), null);
    }

    @Override
    @Transactional
    public PlacementDto updatePlacement(Long id, PlacementFormDto form) {
        Placement p = requirePlacement(id);
        applyFields(p, form);
        return toDto(placementRepository.save(p), null);
    }

    @Override
    @Transactional
    public void deletePlacement(Long id) {
        Placement p = requirePlacement(id);
        // Join rows have no cascade from Placement, so clear bookmarks first.
        placementSaveRepository.deleteByPlacementId(id);
        placementRepository.delete(p);
    }

    @Override
    @Transactional
    public void setPublished(Long id, boolean published) {
        Placement p = requirePlacement(id);
        p.setPublished(published);
        placementRepository.save(p);
    }

    // =====================================================================
    // Public / student
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public List<PlacementDto> listPublished(String skill) {
        List<Placement> list = (skill == null || skill.isBlank())
                ? placementRepository.findByPublishedTrueOrderByCreatedAtDesc()
                : placementRepository.findPublishedBySkill(skill.trim());
        return list.stream().map(p -> toDto(p, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlacementDto> listOpportunities(User user, String skill, boolean savedOnly) {
        Student student = requireStudent(user);
        if (savedOnly) {
            return placementRepository.findSavedByStudent(student.getId()).stream()
                    .filter(p -> p.isPublished())
                    .filter(p -> skill == null || skill.isBlank()
                            || nz(p.getSkills()).toLowerCase(Locale.ROOT).contains(skill.trim().toLowerCase(Locale.ROOT)))
                    .map(p -> toDto(p, student.getId()))
                    .toList();
        }
        List<Placement> base = (skill == null || skill.isBlank())
                ? placementRepository.findPublishedWithSavedFirst(student.getId())
                : placementRepository.findPublishedBySkill(skill.trim());
        return base.stream().map(p -> toDto(p, student.getId())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlacementDto getOpportunity(User user, Long id) {
        Student student = requireStudent(user);
        Placement p = requirePlacement(id);
        if (!p.isPublished()) {
            throw new ResourceNotFoundException("Opportunity not found.");
        }
        return toDto(p, student.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listDistinctSkills() {
        Set<String> skills = new LinkedHashSet<>();
        for (Placement p : placementRepository.findByPublishedTrueOrderByCreatedAtDesc()) {
            skills.addAll(parseSkills(p.getSkills()));
        }
        return skills.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    @Override
    @Transactional
    public boolean toggleSave(User user, Long placementId) {
        Student student = requireStudent(user);
        Placement p = requirePlacement(placementId);
        if (!p.isPublished()) {
            throw new BusinessException("Opportunity not found.");
        }
        if (placementSaveRepository.existsByStudentIdAndPlacementId(student.getId(), p.getId())) {
            placementSaveRepository.deleteByStudentIdAndPlacementId(student.getId(), p.getId());
            return false;
        }
        placementSaveRepository.save(PlacementSave.builder().student(student).placement(p).build());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public long publishedCount() {
        return placementRepository.findByPublishedTrueOrderByCreatedAtDesc().size();
    }

    // =====================================================================
    // Field mapping + validation
    // =====================================================================
    private void applyFields(Placement p, PlacementFormDto form) {
        p.setCompanyName(trimTo(form.getCompanyName(), 150));
        p.setJobTitle(trimTo(form.getJobTitle(), 150));
        p.setDescription(trimToNull(form.getDescription()));
        p.setEligibility(trimToNull(form.getEligibility()));
        p.setSkills(normalizeSkills(form.getSkills()));
        p.setLocation(trimTo(form.getLocation(), 120));
        p.setJobType(normalizeJobType(form.getJobType()));
        p.setDeadline(validateDeadline(form.getDeadline()));
        p.setApplicationUrl(validateApplicationUrl(form.getApplicationUrl()));
        p.setPrepResources(trimToNull(form.getPrepResources()));
        p.setPublished(form.isPublished());
    }

    /** Only http/https links (or blank) are accepted — blocks javascript:/data: injection. */
    private String validateApplicationUrl(String url) {
        String clean = trimToNull(url);
        if (clean == null) {
            return null;
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException("Application link must start with http:// or https://");
        }
        if (clean.length() > 500) {
            throw new BusinessException("Application link is too long (max 500 characters).");
        }
        return clean;
    }

    private LocalDate validateDeadline(LocalDate deadline) {
        if (deadline == null) {
            return null;
        }
        if (deadline.isAfter(LocalDate.now().plusYears(2))) {
            throw new BusinessException("Deadline looks too far in the future (max 2 years).");
        }
        return deadline;
    }

    /** Dedupe + tidy the comma-separated skill list; keep total within the column size. */
    private String normalizeSkills(String raw) {
        List<String> skills = parseSkills(raw);
        if (skills.isEmpty()) {
            return null;
        }
        String joined = String.join(", ", skills);
        if (joined.length() > 500) {
            throw new BusinessException("Too many skills listed (max 500 characters).");
        }
        return joined;
    }

    private List<String> parseSkills(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String s = part.trim();
            if (!s.isEmpty() && seen.add(s.toLowerCase(Locale.ROOT))) {
                out.add(s.length() > 40 ? s.substring(0, 40) : s);
            }
        }
        return out;
    }

    private String normalizeJobType(String jobType) {
        String raw = trimToNull(jobType);
        if (raw == null) {
            return null;
        }
        for (String allowed : JOB_TYPES) {
            if (allowed.equalsIgnoreCase(raw)) {
                return allowed;
            }
        }
        throw new BusinessException("Job type must be one of: " + String.join(", ", JOB_TYPES) + ".");
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    private Placement requirePlacement(Long id) {
        return placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement post not found."));
    }

    private Student requireStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException("Student access only.");
        }
        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new BusinessException("Student profile is missing. Please contact support."));
    }

    private PlacementDto toDto(Placement p, Long studentId) {
        PlacementDto dto = new PlacementDto()
                .setId(p.getId())
                .setCompanyName(p.getCompanyName())
                .setJobTitle(p.getJobTitle())
                .setDescription(p.getDescription())
                .setEligibility(p.getEligibility())
                .setSkills(p.getSkills())
                .setSkillList(parseSkills(p.getSkills()))
                .setLocation(p.getLocation())
                .setJobType(p.getJobType())
                .setDeadline(p.getDeadline())
                .setApplicationUrl(p.getApplicationUrl())
                .setPrepResources(p.getPrepResources())
                .setPublished(p.isPublished());
        if (studentId != null) {
            dto.setSaved(placementSaveRepository.existsByStudentIdAndPlacementId(studentId, p.getId()));
        }
        return dto;
    }

    private PlacementFormDto toForm(Placement p) {
        return new PlacementFormDto()
                .setId(p.getId())
                .setCompanyName(p.getCompanyName())
                .setJobTitle(p.getJobTitle())
                .setDescription(p.getDescription())
                .setEligibility(p.getEligibility())
                .setSkills(p.getSkills())
                .setLocation(p.getLocation())
                .setJobType(p.getJobType())
                .setDeadline(p.getDeadline())
                .setApplicationUrl(p.getApplicationUrl())
                .setPrepResources(p.getPrepResources())
                .setPublished(p.isPublished());
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trimTo(String s, int max) {
        String t = s == null ? "" : s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
