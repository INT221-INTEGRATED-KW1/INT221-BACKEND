package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;

import java.util.List;
import java.util.Optional;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {
    List<Collaborator> findByBoardId(String boardId);
    Optional<Collaborator> findByBoardIdAndOid(String boardId, String oid);
    Optional<Collaborator> findByBoardAndEmail(Board board, String email);
    Optional<Collaborator> findByOidAndBoardId(String oid, String boardId);
    Collaborator findByBoardAndOid(Board board, String oid);
    boolean existsByBoardIdAndOid(String boardId, String oid);
}