package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;


public interface StatusRepository extends JpaRepository<Status, Integer> {
    Status findStatusByIdAndBoardId(Integer statusId, String id);
    Status findByNameAndBoardId(String name, String id);
    Boolean existsByNameAndBoardId(String name, String id);
}
