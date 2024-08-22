package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByStatusId(Integer id);
    @Query("SELECT t FROM Task t WHERE t.status.name IN :statusNames ORDER BY t.id")
    List<Task> findByStatusNameIn(@Param("statusNames") List<String> statusNames);
    List<Task> findByStatusNameIn(List<String> statusNames, Sort sort);
}
