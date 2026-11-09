package vn.devopsnotebook.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.devopsnotebook.learning.catalog.CatalogService;
import vn.devopsnotebook.learning.config.AppProperties;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevopsLearningApiApplicationTests {

	@Test
	void publicCatalogDoesNotExposeQuizAnswers() {
    var properties = new AppProperties("http://127.0.0.1:3000", "../docs/backend/content-manifest.snapshot.json", null);
    var catalog = new CatalogService(new ObjectMapper(), properties);
    catalog.load();
    var publicCatalog = catalog.publicCatalog();
    assertTrue(publicCatalog.path("modules").size() > 0);
    assertFalse(publicCatalog.toString().contains("correctOptionId"));
	}

}
