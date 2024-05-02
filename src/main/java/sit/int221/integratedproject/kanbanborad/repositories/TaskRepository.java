package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.Task;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Integer> {
}
