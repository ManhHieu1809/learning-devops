package vn.devopsnotebook.learning.study;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class StudyController {
  private final StudyService service;
  public StudyController(StudyService service) { this.service = service; }
  @GetMapping("/study-state") ResponseEntity<Map<String, Object>> state() { var state = service.state(); return ResponseEntity.ok().eTag("\"study-" + state.get("revision") + "\"").body(state); }
  @GetMapping("/last-lesson") Map<String, Object> lastLesson() { var state = service.state(); return Map.of("lessonId", state.get("lastLessonId") == null ? "" : state.get("lastLessonId")); }
  @GetMapping("/export") Map<String, Object> export() { return Map.of("format", "devops-learning-backup", "version", 1, "state", service.state()); }
  @PutMapping("/preferences") @ResponseStatus(HttpStatus.NO_CONTENT) void preferences(@RequestBody @Valid Preferences body, @RequestHeader("If-Match") String match) { service.updatePreferences(body.readingSize(), match); }
  @PutMapping("/lessons/{lessonId}/completion") @ResponseStatus(HttpStatus.NO_CONTENT) void completion(@PathVariable String lessonId, @RequestBody @Valid Toggle body, @RequestHeader("If-Match") String match) { service.setCompletion(lessonId, body.value(), match); }
  @PutMapping("/bookmarks/{lessonId}") @ResponseStatus(HttpStatus.NO_CONTENT) void bookmark(@PathVariable String lessonId, @RequestBody @Valid Toggle body, @RequestHeader("If-Match") String match) { service.setBookmark(lessonId, body.value(), match); }
  @PostMapping("/notes") Map<String, Object> note(@RequestBody @Valid NoteInput body, @RequestHeader("If-Match") String match) { return service.saveNote(body.id(), body.lessonId(), body.body(), match); }
  @PutMapping("/notes/{id}") Map<String, Object> editNote(@PathVariable String id, @RequestBody @Valid NoteInput body, @RequestHeader("If-Match") String match) { return service.saveNote(id, body.lessonId(), body.body(), match); }
  @DeleteMapping("/notes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteNote(@PathVariable String id, @RequestHeader("If-Match") String match) { service.deleteNote(id, match); }
  @PutMapping("/labs/{labId}/checks/{checkId}") @ResponseStatus(HttpStatus.NO_CONTENT) void check(@PathVariable String labId, @PathVariable String checkId, @RequestBody @Valid Check body, @RequestHeader("If-Match") String match) { service.setLabCheck(labId, body.definitionVersion(), checkId, body.value(), match); }
  @PostMapping("/quiz-attempts") Map<String, Object> quiz(@RequestBody @Valid QuizAttempt body, @RequestHeader("If-Match") String match) { return service.recordQuiz(body.quizId(), body.quizVersion(), body.selectedOptionId(), match); }
  @PostMapping("/reset") @ResponseStatus(HttpStatus.NO_CONTENT) void reset(@RequestHeader("If-Match") String match) { service.reset(match); }
  public record Toggle(boolean value) {}
  public record Preferences(@NotBlank String readingSize) {}
  public record NoteInput(String id, String lessonId, @NotBlank String body) {}
  public record Check(@NotBlank String definitionVersion, boolean value) {}
  public record QuizAttempt(@NotBlank String quizId, @NotBlank String quizVersion, @NotBlank String selectedOptionId) {}
}
