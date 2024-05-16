package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByStatusId(Integer id);
    List<Task> findByStatusNameIn(List<String> statusNames);
    List<Task> findByStatusNameIn(List<String> statusNames, Sort sort);
}
