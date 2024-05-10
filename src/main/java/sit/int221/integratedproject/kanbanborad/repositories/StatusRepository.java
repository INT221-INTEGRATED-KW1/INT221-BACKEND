package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.Status;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Integer> {
    Optional<Status> findByName(String statusName);
}
