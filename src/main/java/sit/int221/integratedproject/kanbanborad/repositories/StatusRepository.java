package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.Status;

public interface StatusRepository extends JpaRepository<Status, Integer> {
    Status findStatusByIdAndBoardId(Integer id, Integer boardId);
}
