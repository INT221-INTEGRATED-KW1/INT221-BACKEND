package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findByStatusId(Integer id);
    List<Task> findByBoardId(String boardId, Sort sort);
    List<Task> findByBoardIdAndStatusNameIn(String boardId, List<String> statusNames);
    List<Task> findByBoardIdAndStatusNameIn(String boardId, List<String> statusNames, Sort sort);
    Task findTaskByIdAndBoardId(Integer id, String boardId);
}