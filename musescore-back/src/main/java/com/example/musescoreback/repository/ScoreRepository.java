package com.example.musescoreback.repository;

import com.example.musescoreback.dto.ScoreUpsertRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class ScoreRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final List<String> TABLE_NAME_HINTS = List.of("score", "sheet", "music");
    private static final List<String> ID_CANDIDATES = List.of("id", "score_id");
    private static final List<String> USER_ID_CANDIDATES = List.of("user_id", "uid", "author_id", "creator_id", "owner_id");
    private static final List<String> TITLE_CANDIDATES = List.of("title", "score_title", "name");
    private static final List<String> COMPOSER_CANDIDATES = List.of("composer", "author", "artist");
    private static final List<String> TEMPO_CANDIDATES = List.of("tempo", "bpm");
    private static final List<String> TIME_SIGNATURE_CANDIDATES = List.of("time_signature", "timeSignature", "time_sign");
    private static final List<String> KEY_SIGNATURE_CANDIDATES = List.of("key_signature", "keySignature", "key_sign");
    private static final List<String> NOTES_CANDIDATES = List.of("notes", "note_data", "content", "score_content", "music_data");
    private static final List<String> MEASURES_CANDIDATES = List.of("measures", "measure_data");
    private static final List<String> CREATED_AT_CANDIDATES = List.of("created_at", "create_time", "createdAt");
    private static final List<String> UPDATED_AT_CANDIDATES = List.of("updated_at", "update_time", "modified_at", "updatedAt");
    private static final List<String> DRAFT_CANDIDATES = List.of("is_draft", "draft", "isDraft");
    private static final List<String> TAGS_CANDIDATES = List.of("tags", "tag_list");
    private static final List<String> DESCRIPTION_CANDIDATES = List.of("description", "desc");
    private static final List<String> VISIBILITY_CANDIDATES = List.of("visibility", "access_level");

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    private ScoreTableMeta meta;

    public ScoreRepository(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        this.meta = detectScoreTable();
        if (this.meta == null) {
            throw new IllegalStateException("未识别到乐谱表，请确认存在 user_id + title 等字段");
        }
    }

    public List<ScoreRow> findByUserId(String userId) {
        String orderClause = meta.updatedAtColumn != null
                ? " ORDER BY " + q(meta.updatedAtColumn) + " DESC"
                : (meta.idColumn != null ? " ORDER BY " + q(meta.idColumn) + " DESC" : "");
        String sql = "SELECT * FROM " + q(meta.tableName)
                + " WHERE " + q(meta.userIdColumn) + " = ?" + orderClause;
        return jdbcTemplate.query(sql, rowMapper(), userId);
    }

    public Optional<ScoreRow> findByIdAndUserId(String id, String userId) {
        if (meta.idColumn == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM " + q(meta.tableName)
                + " WHERE " + q(meta.idColumn) + " = ? AND " + q(meta.userIdColumn) + " = ? LIMIT 1";
        List<ScoreRow> rows = jdbcTemplate.query(sql, rowMapper(), id, userId);
        return rows.stream().findFirst();
    }

    public ScoreRow insert(String userId, ScoreUpsertRequest request) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();

        String generatedId = null;
        if (meta.idColumn != null && !meta.idAutoIncrement && meta.requiredColumns.contains(meta.idColumn.toLowerCase(Locale.ROOT))) {
            generatedId = UUID.randomUUID().toString().replace("-", "");
            values.put(meta.idColumn, generatedId);
        }

        values.put(meta.userIdColumn, userId);
        values.put(meta.titleColumn, safeTitle(request.title()));

        putIfColumnPresent(values, meta.composerColumn, safeString(request.composer(), ""));
        putIfColumnPresent(values, meta.tempoColumn, request.tempo() != null ? request.tempo() : 120);
        putIfColumnPresent(values, meta.timeSignatureColumn, toJsonString(request.timeSignature(), "{\"numerator\":4,\"denominator\":4}"));
        putIfColumnPresent(values, meta.keySignatureColumn, safeString(request.keySignature(), "C"));
        putIfColumnPresent(values, meta.notesColumn, toJsonString(request.notes(), "[]"));
        putIfColumnPresent(values, meta.measuresColumn, toJsonString(request.measures(), "[]"));
        putIfColumnPresent(values, meta.isDraftColumn, request.isDraft() != null && request.isDraft() ? 1 : 0);
        putIfColumnPresent(values, meta.tagsColumn, toJsonString(request.tags(), "[]"));
        putIfColumnPresent(values, meta.descriptionColumn, safeString(request.description(), ""));
        putIfColumnPresent(values, meta.visibilityColumn, safeString(request.visibility(), "private"));

        LocalDateTime now = LocalDateTime.now();
        putIfColumnPresent(values, meta.createdAtColumn, now);
        putIfColumnPresent(values, meta.updatedAtColumn, now);

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
            return findByIdAndUserId(generatedId, userId)
                    .orElseThrow(() -> new IllegalStateException("乐谱创建成功但读取失败"));
        }

        String fallbackSql = "SELECT * FROM " + q(meta.tableName)
                + " WHERE " + q(meta.userIdColumn) + " = ?"
                + (meta.updatedAtColumn != null
                ? " ORDER BY " + q(meta.updatedAtColumn) + " DESC LIMIT 1"
                : (meta.idColumn != null ? " ORDER BY " + q(meta.idColumn) + " DESC LIMIT 1" : " LIMIT 1"));
        List<ScoreRow> rows = jdbcTemplate.query(fallbackSql, rowMapper(), userId);
        return rows.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("乐谱创建成功但读取失败"));
    }

    public Optional<ScoreRow> update(String id, String userId, ScoreUpsertRequest request) {
        if (meta.idColumn == null) {
            return Optional.empty();
        }
        if (findByIdAndUserId(id, userId).isEmpty()) {
            return Optional.empty();
        }

        LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
        updates.put(meta.titleColumn, safeTitle(request.title()));
        putIfColumnPresent(updates, meta.composerColumn, safeString(request.composer(), ""));
        putIfColumnPresent(updates, meta.tempoColumn, request.tempo() != null ? request.tempo() : 120);
        putIfColumnPresent(updates, meta.timeSignatureColumn, toJsonString(request.timeSignature(), "{\"numerator\":4,\"denominator\":4}"));
        putIfColumnPresent(updates, meta.keySignatureColumn, safeString(request.keySignature(), "C"));
        putIfColumnPresent(updates, meta.notesColumn, toJsonString(request.notes(), "[]"));
        putIfColumnPresent(updates, meta.measuresColumn, toJsonString(request.measures(), "[]"));
        putIfColumnPresent(updates, meta.isDraftColumn, request.isDraft() != null && request.isDraft() ? 1 : 0);
        putIfColumnPresent(updates, meta.tagsColumn, toJsonString(request.tags(), "[]"));
        putIfColumnPresent(updates, meta.descriptionColumn, safeString(request.description(), ""));
        putIfColumnPresent(updates, meta.visibilityColumn, safeString(request.visibility(), "private"));
        putIfColumnPresent(updates, meta.updatedAtColumn, LocalDateTime.now());

        String sql = buildUpdateSql(updates);
        List<Object> params = new ArrayList<>(updates.values());
        params.add(id);
        params.add(userId);
        jdbcTemplate.update(sql, params.toArray());
        return findByIdAndUserId(id, userId);
    }

    public boolean delete(String id, String userId) {
        if (meta.idColumn == null) {
            return false;
        }
        String sql = "DELETE FROM " + q(meta.tableName)
                + " WHERE " + q(meta.idColumn) + " = ? AND " + q(meta.userIdColumn) + " = ?";
        return jdbcTemplate.update(sql, id, userId) > 0;
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

    private String buildUpdateSql(LinkedHashMap<String, Object> updates) {
        StringBuilder setClause = new StringBuilder();
        int i = 0;
        for (String col : updates.keySet()) {
            if (i > 0) {
                setClause.append(", ");
            }
            setClause.append(q(col)).append(" = ?");
            i++;
        }
        return "UPDATE " + q(meta.tableName) + " SET " + setClause
                + " WHERE " + q(meta.idColumn) + " = ? AND " + q(meta.userIdColumn) + " = ?";
    }

    private RowMapper<ScoreRow> rowMapper() {
        return (rs, rowNum) -> new ScoreRow(
                readColumnAsString(rs, meta.idColumn, ""),
                readColumnAsString(rs, meta.titleColumn, "未命名乐谱"),
                readColumnAsString(rs, meta.composerColumn, ""),
                readColumnAsInteger(rs, meta.tempoColumn, 120),
                readColumnAsJson(rs, meta.timeSignatureColumn, "{\"numerator\":4,\"denominator\":4}"),
                readColumnAsString(rs, meta.keySignatureColumn, "C"),
                readColumnAsJson(rs, meta.notesColumn, "[]"),
                readColumnAsJson(rs, meta.measuresColumn, "[]"),
                readColumnAsDateTime(rs, meta.createdAtColumn),
                readColumnAsDateTime(rs, meta.updatedAtColumn),
                readColumnAsBoolean(rs, meta.isDraftColumn, true),
                readColumnAsJson(rs, meta.tagsColumn, "[]"),
                readColumnAsString(rs, meta.descriptionColumn, ""),
                readColumnAsString(rs, meta.visibilityColumn, "private")
        );
    }

    private ScoreTableMeta detectScoreTable() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData dbmd = connection.getMetaData();
            String catalog = connection.getCatalog();
            try (ResultSet tables = dbmd.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                ScoreTableMeta best = null;
                int bestScore = -1;
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    ScoreTableMeta candidate = buildTableMeta(dbmd, catalog, tableName);
                    if (candidate == null) continue;
                    int score = candidate.score();
                    if (score > bestScore) {
                        best = candidate;
                        bestScore = score;
                    }
                }
                return bestScore >= 8 ? best : null;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("读取乐谱表结构失败: " + ex.getMessage(), ex);
        }
    }

    private ScoreTableMeta buildTableMeta(DatabaseMetaData dbmd, String catalog, String tableName) throws Exception {
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

        String userIdCol = findFirstColumn(originalColumns, USER_ID_CANDIDATES);
        String titleCol = findFirstColumn(originalColumns, TITLE_CANDIDATES);
        if (userIdCol == null || titleCol == null) {
            return null;
        }

        return new ScoreTableMeta(
                tableName,
                idColumn,
                idAutoIncrement,
                userIdCol,
                titleCol,
                findFirstColumn(originalColumns, COMPOSER_CANDIDATES),
                findFirstColumn(originalColumns, TEMPO_CANDIDATES),
                findFirstColumn(originalColumns, TIME_SIGNATURE_CANDIDATES),
                findFirstColumn(originalColumns, KEY_SIGNATURE_CANDIDATES),
                findFirstColumn(originalColumns, NOTES_CANDIDATES),
                findFirstColumn(originalColumns, MEASURES_CANDIDATES),
                findFirstColumn(originalColumns, CREATED_AT_CANDIDATES),
                findFirstColumn(originalColumns, UPDATED_AT_CANDIDATES),
                findFirstColumn(originalColumns, DRAFT_CANDIDATES),
                findFirstColumn(originalColumns, TAGS_CANDIDATES),
                findFirstColumn(originalColumns, DESCRIPTION_CANDIDATES),
                findFirstColumn(originalColumns, VISIBILITY_CANDIDATES),
                requiredColumns,
                originalColumns,
                columnSqlTypes
        );
    }

    private String safeTitle(String title) {
        String normalized = safeString(title, "未命名乐谱");
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private String safeString(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) return defaultValue;
        return value.trim();
    }

    private void putIfColumnPresent(LinkedHashMap<String, Object> values, String column, Object value) {
        if (column != null) {
            values.put(column, value);
        }
    }

    private String toJsonString(JsonNode value, String fallback) {
        try {
            if (value == null || value.isNull()) {
                return fallback;
            }
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String readColumnAsString(ResultSet rs, String column, String fallback) throws java.sql.SQLException {
        if (column == null) return fallback;
        Object value = rs.getObject(column);
        if (value == null) return fallback;
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private Integer readColumnAsInteger(ResultSet rs, String column, int fallback) throws java.sql.SQLException {
        if (column == null) return fallback;
        Object value = rs.getObject(column);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean readColumnAsBoolean(ResultSet rs, String column, boolean fallback) throws java.sql.SQLException {
        if (column == null) return fallback;
        Object value = rs.getObject(column);
        if (value == null) return fallback;
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return fallback;
        return "1".equals(text) || "true".equals(text) || "yes".equals(text) || "y".equals(text);
    }

    private JsonNode readColumnAsJson(ResultSet rs, String column, String fallbackJson) throws java.sql.SQLException {
        String text = readColumnAsString(rs, column, fallbackJson);
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (Exception ex) {
            try {
                return OBJECT_MAPPER.readTree(fallbackJson);
            } catch (Exception ignore) {
                return OBJECT_MAPPER.nullNode();
            }
        }
    }

    private String readColumnAsDateTime(ResultSet rs, String column) throws java.sql.SQLException {
        if (column == null) {
            return LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
        }
        Object value = rs.getObject(column);
        if (value == null) {
            return LocalDateTime.now().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
        }
        Instant instant;
        if (value instanceof Timestamp ts) {
            instant = ts.toInstant();
        } else if (value instanceof java.util.Date date) {
            instant = date.toInstant();
        } else {
            try {
                instant = Instant.parse(String.valueOf(value));
            } catch (Exception ex) {
                return String.valueOf(value);
            }
        }
        return instant.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
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

    public record ScoreRow(
            String id,
            String title,
            String composer,
            Integer tempo,
            JsonNode timeSignature,
            String keySignature,
            JsonNode notes,
            JsonNode measures,
            String createdAt,
            String updatedAt,
            Boolean isDraft,
            JsonNode tags,
            String description,
            String visibility
    ) {
    }

    private record ScoreTableMeta(
            String tableName,
            String idColumn,
            boolean idAutoIncrement,
            String userIdColumn,
            String titleColumn,
            String composerColumn,
            String tempoColumn,
            String timeSignatureColumn,
            String keySignatureColumn,
            String notesColumn,
            String measuresColumn,
            String createdAtColumn,
            String updatedAtColumn,
            String isDraftColumn,
            String tagsColumn,
            String descriptionColumn,
            String visibilityColumn,
            Set<String> requiredColumns,
            Map<String, String> originalCaseColumns,
            Map<String, Integer> columnSqlTypes
    ) {
        int score() {
            int score = 0;
            String lowerTableName = tableName.toLowerCase(Locale.ROOT);
            for (String hint : TABLE_NAME_HINTS) {
                if (lowerTableName.contains(hint)) {
                    score += 3;
                    break;
                }
            }
            if (idColumn != null) score += 1;
            if (userIdColumn != null) score += 4;
            if (titleColumn != null) score += 4;
            if (notesColumn != null) score += 2;
            if (updatedAtColumn != null) score += 1;
            return score;
        }
    }
}
