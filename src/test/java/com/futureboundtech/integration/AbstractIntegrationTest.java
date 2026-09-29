package com.futureboundtech.integration;

import com.futureboundtech.entity.Student;
import com.futureboundtech.entity.Trainer;
import com.futureboundtech.entity.User;
import com.futureboundtech.enums.Role;
import com.futureboundtech.repository.CourseRepository;
import com.futureboundtech.repository.StudentRepository;
import com.futureboundtech.repository.TrainerRepository;
import com.futureboundtech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Shared bootstrapping for the full-stack ({@code @SpringBootTest}) tests.
 *
 * <p>It pins these tests to a dedicated in-memory database (a different name to
 * the one the pure JPA slice tests use, and never the dev file DB), wipes every
 * table before each test for determinism, and exposes helpers to seed a user and
 * log in through the real Spring Security form-login filter.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:fbt-itest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected DataSource dataSource;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder passwordEncoder;

    @Autowired protected UserRepository userRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected TrainerRepository trainerRepository;
    @Autowired protected CourseRepository courseRepository;

    @BeforeEach
    void wipeDatabase() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            List<String> tables = new ArrayList<>();
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            try (var st = c.createStatement()) {
                st.execute("SET REFERENTIAL_INTEGRITY FALSE");
                for (String t : tables) {
                    try {
                        st.execute("TRUNCATE TABLE \"" + t + "\"");
                    } catch (Exception ignore) {
                        // System / non-truncatable tables are simply skipped.
                    }
                }
                st.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        }
    }

    protected User seedUser(String email, Role role, String rawPassword) {
        User u = User.builder()
                .firstName(email.substring(0, 1).toUpperCase() + "User")
                .lastName(role.name())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build();
        return userRepository.save(u);
    }

    /** Creates a STUDENT with the profile record the student dashboard requires. */
    protected User seedStudent(String email, String rawPassword) {
        User u = seedUser(email, Role.STUDENT, rawPassword);
        studentRepository.save(Student.builder().user(u).education("B.Tech").build());
        return u;
    }

    /** Creates a TRAINER with the profile record the trainer pages require. */
    protected User seedTrainer(String email, String rawPassword) {
        User u = seedUser(email, Role.TRAINER, rawPassword);
        trainerRepository.save(Trainer.builder().user(u).expertise("Java").build());
        return u;
    }

    /** Runs the real /login POST and returns the authenticated session. */
    protected MockHttpSession login(String email, String rawPassword) throws Exception {
        MvcResult r = mockMvc.perform(formLogin("/login")
                        .user("username", email).password("password", rawPassword))
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    protected static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withCsrf(String path) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post(path).with(csrf());
    }

    protected static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asAdmin(String path) {
        return get(path).with(user("admin@test.com").roles("ADMIN"));
    }
}
