package sit.int221.integratedproject.kanbanborad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.Board;

public interface BoardRepository extends JpaRepository<Board, Integer> {
}
