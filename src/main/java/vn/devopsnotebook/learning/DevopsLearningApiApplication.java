package vn.devopsnotebook.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import vn.devopsnotebook.learning.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DevopsLearningApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevopsLearningApiApplication.class, args);
	}

}
