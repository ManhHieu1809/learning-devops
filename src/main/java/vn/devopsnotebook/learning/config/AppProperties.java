package vn.devopsnotebook.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String corsOrigin, String contentManifest, Bootstrap bootstrap) {
  public record Bootstrap(String username, String password) {}
}
