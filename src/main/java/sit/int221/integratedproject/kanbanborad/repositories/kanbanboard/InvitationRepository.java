package sit.int221.integratedproject.kanbanborad.repositories.kanbanboard;

import org.springframework.data.jpa.repository.JpaRepository;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Invitation;
import sit.int221.integratedproject.kanbanborad.services.InvitationService;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Integer> {
    boolean existsByBoardAndInviteeOid(Board board, String inviteeOid);
    List<Invitation> findByBoard(Board board);
}
