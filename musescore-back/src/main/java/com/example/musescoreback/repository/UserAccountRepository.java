package com.example.musescoreback.repository;

import com.example.musescoreback.model.UserAccount;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class UserAccountRepository {

    private static final List<String> USERNAME_CANDIDATES = List.of("username", "user_name", "account", "login_name");
    private static final List<String> EMAIL_CANDIDATES = List.of("email", "email_address", "mail");
    private static final List<String> PASSWORD_CANDIDATES = List.of("password", "password_hash", "pwd", "passwd", "user_password");
    private static final List<String> ID_CANDIDATES = List.of("id", "user_id", "uid");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    private UserTableMeta meta;

    public UserAccountRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        this.meta = detectUserTable();
        if (this.meta == null) {
            throw new IllegalStateException("未在数据库中识别到用户表，请确认存在用户名/邮箱/密码字段");
        }
    }

    public Optional<UserAccount> findByAccount(String account) {
        String sql = "SELECT * FROM " + q(meta.tableName)
                + " WHERE " + q(meta.usernameColumn) + " = ? OR " + q(meta.emailColumn) + " = ? LIMIT 1";
        List<UserAccount> users = jdbcTemplate.query(sql, rowMapper(), account, account);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findById(String id) {
        if (meta.idColumn == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM " + q(meta.tableName) + " WHERE " + q(meta.idColumn) + " = ? LIMIT 1";
        List<UserAccount> users = jdbcTemplate.query(sql, rowMapper(), id);
        return users.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM " + q(meta.tableName) + " WHERE " + q(meta.usernameColumn) + " = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM " + q(meta.tableName) + " WHERE " + q(meta.emailColumn) + " = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, email);
        return count != null && count > 0;
    }

    public UserAccount insert(String username, String email, String passwordHash) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(meta.usernameColumn, username);
        values.put(meta.emailColumn, email);
        values.put(meta.passwordColumn, passwordHash);

        String generatedId = null;
        if (meta.idColumn != null && !meta.idAutoIncrement && meta.requiredColumns.contains(meta.idColumn.toLowerCase(Locale.ROOT))) {
            generatedId = UUID.randomUUID().toString().replace("-", "");
            values.put(meta.idColumn, generatedId);
        }

        for (String required : meta.requiredColumns) {
            if (containsIgnoreCase(values.keySet(), required)) {
                continue;
            }
            String realName = meta.originalCaseColumns.get(required);
            Object defaultValue = defaultValueForColumn(required, meta.columnSqlTypes.getOrDefault(required, Types.VARCHAR));
            if (defaultValue != null) {
                values.put(realName, defaultValue);
            }
        }

        String sql = buildInsertSql(values);
        jdbcTemplate.update(sql, values.values().toArray());

        if (generatedId != null) {
            Optional<UserAccount> inserted = findById(generatedId);
            if (inserted.isPresent()) {
                return inserted.get();
            }
            return new UserAccount(generatedId, username, email, passwordHash);
        }

        Optional<UserAccount> inserted = findByAccount(email);
        if (inserted.isPresent()) {
            return inserted.get();
        }
        return new UserAccount(username, username, email, passwordHash);
    }

    private String buildInsertSql(LinkedHashMap<String, Object> values) {
        StringBuilder cols = new StringBuilder();
        StringBuilder params = new StringBuilder();
        int i = 0;
        for (String col : values.keySet()) {
            if (i > 0) {
                cols.append(", ");
                params.append(", ");
            }
            cols.append(q(col));
            params.append("?");
            i++;
        }
        return "INSERT INTO " + q(meta.tableName) + " (" + cols + ") VALUES (" + params + ")";
    }

    private RowMapper<UserAccount> rowMapper() {
        return (rs, rowNum) -> new UserAccount(
                readColumnAsString(rs, meta.idColumn, rs.getString(meta.usernameColumn)),
                rs.getString(meta.usernameColumn),
                rs.getString(meta.emailColumn),
                rs.getString(meta.passwordColumn)
        );
    }

    private String readColumnAsString(ResultSet rs, String column, String fallback) throws java.sql.SQLException {
        if (column == null) {
            return fallback;
        }
        Object value = rs.getObject(column);
        return value == null ? fallback : String.valueOf(value);
    }

    private UserTableMeta detectUserTable() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData dbmd = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet tables = dbmd.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                UserTableMeta best = null;
                int bestScore = -1;
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    UserTableMeta candidate = buildTableMeta(dbmd, catalog, tableName);
                    if (candidate == null) {
                        continue;
                    }
                    int score = candidate.score();
                    if (score > bestScore) {
                        best = candidate;
                        bestScore = score;
                    }
                }
                return bestScore >= 9 ? best : null;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取数据库表结构失败: " + ex.getMessage(), ex);
        }
    }

    private UserTableMeta buildTableMeta(DatabaseMetaData dbmd, String catalog, String tableName) throws Exception {
        Map<String, String> originalColumns = new LinkedHashMap<>();
        Map<String, Integer> columnSqlTypes = new LinkedHashMap<>();
        Set<String> requiredColumns = new HashSet<>();
        String idColumn = null;
        boolean idAutoIncrement = false;

        try (ResultSet rs = dbmd.getColumns(catalog, null, tableName, "%")) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                String lower = col.toLowerCase(Locale.ROOT);
                originalColumns.put(lower, col);
                columnSqlTypes.put(lower, rs.getInt("DATA_TYPE"));
                int nullable = rs.getInt("NULLABLE");
                String defaultValue = rs.getString("COLUMN_DEF");
                boolean autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                if (nullable == DatabaseMetaData.columnNoNulls && defaultValue == null && !autoIncrement) {
                    requiredColumns.add(lower);
                }
                if (idColumn == null && ID_CANDIDATES.contains(lower)) {
                    idColumn = col;
                    idAutoIncrement = autoIncrement;
                }
            }
        }

        String usernameCol = findFirstColumn(originalColumns, USERNAME_CANDIDATES);
        String emailCol = findFirstColumn(originalColumns, EMAIL_CANDIDATES);
        String passwordCol = findFirstColumn(originalColumns, PASSWORD_CANDIDATES);

        if (usernameCol == null || emailCol == null || passwordCol == null) {
            return null;
        }

        return new UserTableMeta(tableName, idColumn, idAutoIncrement, usernameCol, emailCol, passwordCol,
                requiredColumns, originalColumns, columnSqlTypes);
    }

    private String findFirstColumn(Map<String, String> columns, List<String> candidates) {
        for (String candidate : candidates) {
            if (columns.containsKey(candidate)) {
                return columns.get(candidate);
            }
        }
        return null;
    }

    private boolean containsIgnoreCase(Set<String> keys, String valueLower) {
        for (String key : keys) {
            if (key.equalsIgnoreCase(valueLower)) {
                return true;
            }
        }
        return false;
    }

    private Object defaultValueForColumn(String columnLower, int sqlType) {
        if (columnLower.contains("create") || columnLower.contains("update") || columnLower.endsWith("_at")
                || columnLower.endsWith("time")) {
            return LocalDateTime.now();
        }
        if (columnLower.contains("status")) {
            return 1;
        }
        if (columnLower.contains("deleted")) {
            return 0;
        }
        if (sqlType == Types.BOOLEAN || sqlType == Types.BIT) {
            return Boolean.FALSE;
        }
        if (sqlType == Types.INTEGER || sqlType == Types.TINYINT || sqlType == Types.SMALLINT || sqlType == Types.BIGINT) {
            return 0;
        }
        if (sqlType == Types.VARCHAR || sqlType == Types.CHAR || sqlType == Types.LONGVARCHAR) {
            return "";
        }
        return null;
    }

    private String q(String identifier) {
        return "`" + identifier.replace("`", "") + "`";
    }

    private record UserTableMeta(
            String tableName,
            String idColumn,
            boolean idAutoIncrement,
            String usernameColumn,
            String emailColumn,
            String passwordColumn,
            Set<String> requiredColumns,
            Map<String, String> originalCaseColumns,
            Map<String, Integer> columnSqlTypes
    ) {
        int score() {
            int score = 0;
            if (usernameColumn != null) {
                score += 3;
            }
            if (emailColumn != null) {
                score += 3;
            }
            if (passwordColumn != null) {
                score += 3;
            }
            if (idColumn != null) {
                score += 1;
            }
            return score;
        }
    }
}
