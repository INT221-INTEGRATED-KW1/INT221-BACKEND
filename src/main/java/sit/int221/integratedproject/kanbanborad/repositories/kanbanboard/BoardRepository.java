package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, String> {
    List<Board> findByOid(String oid);
}
