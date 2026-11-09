package vn.devopsnotebook.learning.study;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devopsnotebook.learning.web.ApiExceptionHandler.ApiException;
import vn.devopsnotebook.learning.catalog.CatalogService;

@Service
public class StudyService {
  private static final long USER_ID = 1L;
  private final JdbcTemplate jdbc;
  private final CatalogService catalog;
  public StudyService(JdbcTemplate jdbc, CatalogService catalog) { this.jdbc = jdbc; this.catalog = catalog; }

  public Map<String, Object> state() {
    var meta = jdbc.queryForMap("select revision, last_lesson_id, reading_size, updated_at from learner_state where user_id = ?", USER_ID);
    var result = new LinkedHashMap<String, Object>();
    result.put("revision", ((Number) meta.get("revision")).longValue());
    result.put("lastLessonId", meta.get("last_lesson_id")); result.put("readingSize", meta.get("reading_size")); result.put("updatedAt", meta.get("updated_at"));
    result.put("completedLessonIds", jdbc.queryForList("select lesson_id from lesson_progress where user_id = ? order by lesson_id", String.class, USER_ID));
    result.put("bookmarkedLessonIds", jdbc.queryForList("select lesson_id from bookmark where user_id = ? order by lesson_id", String.class, USER_ID));
    result.put("notes", jdbc.queryForList("select id, lesson_id as lessonId, body, created_at as createdAt, updated_at as updatedAt from note where user_id = ? order by updated_at desc", USER_ID));
    result.put("labChecks", jdbc.queryForList("select lab_id as labId, definition_version as definitionVersion, check_id as checkId, checked_at as checkedAt from lab_check_state where user_id = ?", USER_ID));
    return result;
  }

  @Transactional public void setCompletion(String lessonId, boolean completed, String match) {
    if (!catalog.lessonExists(lessonId)) throw new ApiException(HttpStatus.BAD_REQUEST, "Bài học không tồn tại trong nội dung Git hiện tại.");
    requireRevision(match); if (completed) jdbc.update("insert ignore into lesson_progress (user_id, lesson_id) values (?, ?)", USER_ID, lessonId);
    else jdbc.update("delete from lesson_progress where user_id = ? and lesson_id = ?", USER_ID, lessonId);
    jdbc.update("update learner_state set last_lesson_id = ?, revision = revision + 1 where user_id = ?", lessonId, USER_ID);
  }
  @Transactional public void setBookmark(String lessonId, boolean bookmarked, String match) {
    if (!catalog.lessonExists(lessonId)) throw new ApiException(HttpStatus.BAD_REQUEST, "Bài học không tồn tại trong nội dung Git hiện tại.");
    requireRevision(match); if (bookmarked) jdbc.update("insert ignore into bookmark (user_id, lesson_id) values (?, ?)", USER_ID, lessonId);
    else jdbc.update("delete from bookmark where user_id = ? and lesson_id = ?", USER_ID, lessonId);
    bump();
  }
  @Transactional public Map<String, Object> saveNote(String id, String lessonId, String body, String match) {
    requireRevision(match); if (body == null || body.isBlank() || body.length() > 20_000) throw new ApiException(HttpStatus.BAD_REQUEST, "Ghi chú cần có từ 1 đến 20.000 ký tự.");
    if (id == null || id.isBlank()) { id = UUID.randomUUID().toString(); jdbc.update("insert into note (user_id, id, lesson_id, body) values (?, ?, ?, ?)", USER_ID, id, lessonId, body); }
    else { int changed = jdbc.update("update note set lesson_id = ?, body = ? where user_id = ? and id = ?", lessonId, body, USER_ID, id); if (changed == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ghi chú."); }
    bump(); return jdbc.queryForMap("select id, lesson_id as lessonId, body, created_at as createdAt, updated_at as updatedAt from note where user_id = ? and id = ?", USER_ID, id);
  }
  @Transactional public void deleteNote(String id, String match) { requireRevision(match); if (jdbc.update("delete from note where user_id = ? and id = ?", USER_ID, id) == 0) throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy ghi chú."); bump(); }
  @Transactional public void setLabCheck(String labId, String version, String checkId, boolean checked, String match) {
    if (!catalog.labExists(labId, version, checkId)) throw new ApiException(HttpStatus.BAD_REQUEST, "Mục kiểm tra lab không còn hợp lệ.");
    requireRevision(match); if (checked) jdbc.update("insert ignore into lab_check_state (user_id, lab_id, definition_version, check_id) values (?, ?, ?, ?)", USER_ID, labId, version, checkId);
    else jdbc.update("delete from lab_check_state where user_id = ? and lab_id = ? and definition_version = ? and check_id = ?", USER_ID, labId, version, checkId); bump();
  }
  @Transactional public Map<String, Object> recordQuiz(String quizId, String version, String selected, String match) {
    requireRevision(match); String id = UUID.randomUUID().toString();
    boolean correct = catalog.isCorrect(quizId, version, selected);
    jdbc.update("insert into quiz_attempt (user_id, id, quiz_id, quiz_version, selected_option_id, origin, grading_status, is_correct) values (?, ?, ?, ?, ?, 'WEB', 'GRADED', ?)", USER_ID, id, quizId, version, selected, correct);
    bump(); return Map.of("id", id, "quizId", quizId, "isCorrect", correct, "attemptedAt", Instant.now());
  }
  @Transactional public void updatePreferences(String readingSize, String match) { if (!Set.of("normal", "large").contains(readingSize)) throw new ApiException(HttpStatus.BAD_REQUEST, "Cỡ chữ không hợp lệ."); requireRevision(match); jdbc.update("update learner_state set reading_size = ?, revision = revision + 1 where user_id = ?", readingSize, USER_ID); }
  @Transactional public void reset(String match) { requireRevision(match); for (String table : List.of("lesson_progress", "bookmark", "note", "lab_check_state", "quiz_attempt")) jdbc.update("delete from " + table + " where user_id = ?", USER_ID); jdbc.update("update learner_state set last_lesson_id = null, reading_size = 'normal', revision = revision + 1 where user_id = ?", USER_ID); }
  private void bump() { jdbc.update("update learner_state set revision = revision + 1 where user_id = ?", USER_ID); }
  private void requireRevision(String header) {
    Long current = jdbc.queryForObject("select revision from learner_state where user_id = ? for update", Long.class, USER_ID);
    String expected = "\"study-" + current + "\"";
    if (!expected.equals(header)) throw new ApiException(HttpStatus.CONFLICT, "Trạng thái đã thay đổi ở một phiên khác. Hãy tải lại dữ liệu trước khi lưu.");
  }
}
