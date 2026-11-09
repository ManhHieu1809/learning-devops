package vn.devopsnotebook.learning.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
  private final CatalogService service;
  public CatalogController(CatalogService service) { this.service = service; }
  @GetMapping JsonNode catalog() { return service.publicCatalog(); }
}
