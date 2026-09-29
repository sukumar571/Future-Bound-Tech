package com.futureboundtech.config;

import com.futureboundtech.enums.NotificationRelatedType;
import com.futureboundtech.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repairs the {@code notifications} table on databases created before Phase 19.
 *
 * <p>Hibernate's {@code ddl-auto=update} adds missing columns but never revises
 * an existing column definition or CHECK constraint, and the schema it writes
 * guards each {@code @Enumerated} column with {@code CHECK (col IN (...)} listing
 * only the constants that existed when the table was first built. On an existing
 * installation that guard still allows just the four legacy
 * {@code INFO/WARNING/ALERT/MESSAGE} kinds, so every one of the nine Phase 19
 * notification types is rejected on insert, and {@code message} is left at its
 * original 255 characters.</p>
 *
 * <p>The repair drops any enum guard on {@code notifications} that no longer
 * covers the whole enum and widens {@code message}. It is idempotent — a database
 * built from the current entities has nothing to fix — and it never fails
 * startup: a dialect or metadata layout it does not recognise is logged and
 * skipped.</p>
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class NotificationSchemaRepair implements ApplicationRunner {

    private static final String TABLE = "notifications";
    private static final int MESSAGE_LENGTH = 1000;

    /** Quoted enum literals inside a stored CHECK clause, e.g. {@code 'warning'}. */
    private static final Pattern ALLOWED_VALUES = Pattern.compile("'([a-z0-9_]+)'");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean mySql = isMySql();
            dropStaleEnumGuards(mySql);
            widenMessageColumn(mySql);
        } catch (Exception ex) {
            // A schema we cannot read is not a reason to refuse to boot.
            log.warn("Notification schema repair skipped: {}", ex.getMessage());
        }
    }

    private boolean isMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("mysql");
        }
    }

    /**
     * Drops the CHECK constraints guarding {@code notifications.type} and
     * {@code notifications.related_type} when they no longer accept every value
     * the enum can now produce. H2 and MySQL name the check-constraint columns
     * differently, so the lookup is dialect specific.
     */
    private void dropStaleEnumGuards(boolean mySql) {
        String sql = mySql
                ? "SELECT tc.constraint_name, cc.check_clause "
                  + "FROM information_schema.check_constraints cc "
                  + "JOIN information_schema.table_constraints tc "
                  + "  ON tc.constraint_name = cc.check_constraint_name "
                  + " AND tc.constraint_schema = cc.check_constraint_schema "
                  + " AND tc.table_schema = DATABASE() "
                  + "WHERE tc.table_name = ? AND tc.constraint_type = 'CHECK'"
                : "SELECT tc.constraint_name, cc.check_clause "
                  + "FROM information_schema.table_constraints tc "
                  + "JOIN information_schema.check_constraints cc "
                  + "  ON cc.constraint_name = tc.constraint_name "
                  + "WHERE tc.table_name = ? AND tc.constraint_type = 'CHECK'";

        List<Object[]> guards;
        try {
            guards = jdbcTemplate.query(sql,
                    (rs, rowNum) -> new Object[]{rs.getString(1), rs.getString(2)}, TABLE);
        } catch (Exception ex) {
            log.warn("Could not read CHECK constraints on {}: {}", TABLE, ex.getMessage());
            return;
        }

        for (Object[] guard : guards) {
            String name = (String) guard[0];
            String clause = normalise(guard[1] == null ? "" : (String) guard[1]);
            boolean related = clause.contains("related_type");
            if (!related && !clause.contains("type")) {
                continue;
            }
            if (coversAll(clause, requiredValues(related))) {
                continue;
            }
            // H2 stores these auto-named guards in upper case, so the identifier
            // has to keep its quoting; MySQL uses backticks instead of doubles.
            String drop = "ALTER TABLE " + TABLE + (mySql ? " DROP CHECK `" : " DROP CONSTRAINT \"")
                    + name + (mySql ? "`" : "\"");
            try {
                jdbcTemplate.execute(drop);
                log.info("Notification schema repair: dropped stale enum guard {} on {}.{}",
                        name, TABLE, related ? "related_type" : "type");
            } catch (Exception ex) {
                log.warn("Could not drop guard {} on {}: {}", name, TABLE, ex.getMessage());
            }
        }
    }

    private Set<String> requiredValues(boolean related) {
        Enum<?>[] values = related ? NotificationRelatedType.values() : NotificationType.values();
        Set<String> names = new LinkedHashSet<>();
        for (Enum<?> value : values) {
            names.add(value.name().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /** Strips identifier quoting and case so a clause can be compared loosely. */
    private String normalise(String clause) {
        return clause.toLowerCase(Locale.ROOT).replace("`", "").replace("\"", "");
    }

    private boolean coversAll(String clause, Set<String> required) {
        Set<String> allowed = new LinkedHashSet<>();
        Matcher matcher = ALLOWED_VALUES.matcher(clause);
        while (matcher.find()) {
            allowed.add(matcher.group(1));
        }
        return !allowed.isEmpty() && allowed.containsAll(required);
    }

    /**
     * {@code ddl-auto=update} also leaves {@code message} at its original width,
     * which silently truncates long announcements. The current width is read
     * through JDBC metadata rather than {@code information_schema}, whose layout
     * differs between H2 and MySQL.
     */
    private void widenMessageColumn(boolean mySql) {
        int current = currentMessageLength();
        if (current >= MESSAGE_LENGTH) {
            return;
        }
        String alter = mySql
                ? "ALTER TABLE " + TABLE + " MODIFY COLUMN `message` VARCHAR(" + MESSAGE_LENGTH + ") NOT NULL"
                : "ALTER TABLE " + TABLE + " ALTER COLUMN \"message\" SET DATA TYPE VARCHAR(" + MESSAGE_LENGTH + ")";
        try {
            jdbcTemplate.execute(alter);
            log.info("Notification schema repair: widened {}.message from {} to {} characters",
                    TABLE, current, MESSAGE_LENGTH);
        } catch (Exception ex) {
            log.warn("Could not widen {}.message: {}", TABLE, ex.getMessage());
        }
    }

    private int currentMessageLength() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // getColumns is case sensitive on some drivers, so try both spellings.
            for (String table : new String[]{TABLE, TABLE.toUpperCase(Locale.ROOT)}) {
                try (ResultSet rs = metaData.getColumns(null, null, table, "message")) {
                    if (rs.next()) {
                        return rs.getInt("COLUMN_SIZE");
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not read the width of {}.message: {}", TABLE, ex.getMessage());
        }
        // Unknown: let the ALTER decide, it is harmless when already wide enough.
        return 0;
    }
}
