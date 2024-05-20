package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.StatusLimit;

public interface StatusLimitRepository extends JpaRepository<StatusLimit, Integer> {
}
