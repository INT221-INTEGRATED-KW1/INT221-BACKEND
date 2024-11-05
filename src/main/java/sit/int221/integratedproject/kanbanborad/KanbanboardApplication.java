package sit.int221.integratedproject.kanbanborad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import sit.int221.integratedproject.kanbanborad.properties.FileStorageProperties;

@SpringBootApplication
@EnableConfigurationProperties({FileStorageProperties.class})
public class KanbanboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(KanbanboardApplication.class, args);
	}

}
