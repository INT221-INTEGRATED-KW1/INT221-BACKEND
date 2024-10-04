package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, String> {
    List<Board> findByOid(String oid);
    @Query("SELECT b FROM Board b JOIN Collaborator c ON b.id = c.board.id WHERE c.oid = :oid")
    List<Board> findCollaboratorBoardsByUserOid(@Param("oid") String oid);
}
