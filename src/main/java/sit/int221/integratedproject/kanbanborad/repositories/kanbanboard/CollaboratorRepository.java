package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;

import java.util.List;
import java.util.Optional;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {
    List<Collaborator> findByBoardId(String boardId);
    Optional<Collaborator> findByBoardIdAndOid(String boardId, String oid);
    Optional<Collaborator> findByBoardAndEmail(Board board, String email);
    Optional<Collaborator> findByOidAndBoardId(String oid, String boardId);
    Collaborator findByBoardAndOid(Board board, String oid);
    List<Collaborator> findByBoardIdAndStatus(String boardId, CollabStatus status);
    List<Collaborator> findByOidAndStatus(String oid, CollabStatus status);
    Optional<Collaborator> findByOidAndBoardIdAndStatus(String oid, String boardId, CollabStatus status);
}