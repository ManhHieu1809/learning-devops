package vn.devopsnotebook.learning.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.devopsnotebook.learning.config.AppProperties;
import vn.devopsnotebook.learning.web.ApiExceptionHandler.ApiException;

@Service
public class CatalogService {
  private final ObjectMapper mapper; private final AppProperties properties; private JsonNode catalog;
  public CatalogService(ObjectMapper mapper, AppProperties properties) { this.mapper = mapper; this.properties = properties; }
  @PostConstruct public void load() {
    try { catalog = mapper.readTree(Files.readString(Path.of(properties.contentManifest()))); }
    catch (IOException ex) { throw new IllegalStateException("Không đọc được content manifest: " + properties.contentManifest(), ex); }
  }
  public JsonNode publicCatalog() {
    JsonNode copy = catalog.deepCopy(); stripAnswers(copy); return copy;
  }
  public boolean lessonExists(String id) { return contains(catalog.path("lessons"), id); }
  public boolean labExists(String id, String version, String checkId) {
    for (JsonNode lab : catalog.path("labs")) if (id.equals(lab.path("id").asText()) && version.equals(lab.path("definitionVersion").asText())) return contains(lab.path("checks"), checkId);
    return false;
  }
  public boolean isCorrect(String quizId, String version, String selected) {
    for (JsonNode quiz : catalog.path("quizzes")) if (quizId.equals(quiz.path("id").asText()) && version.equals(quiz.path("version").asText())) return selected.equals(quiz.path("correctOptionId").asText());
    throw new ApiException(HttpStatus.BAD_REQUEST, "Quiz hoặc phiên bản quiz không còn hợp lệ.");
  }
  private boolean contains(JsonNode nodes, String id) { for (JsonNode node : nodes) if (id.equals(node.path("id").asText())) return true; return false; }
  private void stripAnswers(JsonNode node) {
    if (node.isObject()) { ((ObjectNode) node).remove("correctOptionId"); Iterator<JsonNode> values = node.elements(); while (values.hasNext()) stripAnswers(values.next()); }
    else if (node.isArray()) for (JsonNode child : node) stripAnswers(child);
  }
}
