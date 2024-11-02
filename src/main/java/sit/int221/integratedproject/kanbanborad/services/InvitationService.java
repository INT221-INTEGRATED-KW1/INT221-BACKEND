package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.InvitationRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.CollaboratorResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.InvitationAcceptanceResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.InvitationResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Invitation;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.enumeration.InvitationStatus;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.InvitationRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final BoardRepository boardRepository;

    public InvitationService(InvitationRepository invitationRepository, CollaboratorRepository collaboratorRepository, BoardRepository boardRepository) {
        this.invitationRepository = invitationRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.boardRepository = boardRepository;
    }

    @Transactional
    public String sendInvitation(String boardId, Claims claims, InvitationRequestDTO invitationRequestDTO) {
        String inviterOid = (String) claims.get("oid");
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        // Check if the invitee is already a collaborator or has a pending invitation
        if (collaboratorRepository.existsByBoardAndOid(board, invitationRequestDTO.getInviteeOid()) ||
                invitationRepository.existsByBoardAndInviteeOid(board, invitationRequestDTO.getInviteeOid())) {
            throw new RuntimeException("The user is already the collaborator or pending collaborator of this board");
        }

        Invitation invitation = new Invitation();
        invitation.setBoard(board);
        invitation.setInviterOid(inviterOid);
        invitation.setInviteeOid(invitationRequestDTO.getInviteeOid());
        invitation.setAccessRight(invitationRequestDTO.getAccessRight());
        invitation.setStatus(InvitationStatus.PENDING);

        // Send email logic here (not implemented in this example)

        invitationRepository.save(invitation);
        return "Invitation sent successfully.";
    }

    @Transactional
    public InvitationAcceptanceResponseDTO acceptInvitation(String invitationId, Claims claims) {
        String inviteeOid = (String) claims.get("oid");
        Invitation invitation = invitationRepository.findById(Integer.valueOf(invitationId))
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING || !invitation.getInviteeOid().equals(inviteeOid)) {
            throw new RuntimeException("Invitation is not pending or does not belong to the invitee");
        }

        Collaborator collaborator = new Collaborator();
        collaborator.setBoard(invitation.getBoard());
        collaborator.setOid(invitation.getInviteeOid());
        collaborator.setAccessRight(invitation.getAccessRight());
        collaborator.setStatus(CollabStatus.ACCEPTED);
        collaboratorRepository.save(collaborator);

        // Update invitation status to accepted
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return new InvitationAcceptanceResponseDTO(
                collaborator.getOid(),
                collaborator.getAccessRight(),
                collaborator.getStatus().name() // Convert Enum to String if necessary
        );
    }

    @Transactional
    public void declineInvitation(String invitationId, Claims claims) {
        String inviteeOid = (String) claims.get("oid");
        Invitation invitation = invitationRepository.findById(Integer.valueOf(invitationId))
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getInviteeOid().equals(inviteeOid)) {
            throw new RuntimeException("Invitation does not belong to the invitee");
        }

        invitationRepository.delete(invitation);
    }

    public List<InvitationResponseDTO> getPendingInvitations(String boardId, Claims claims) {
        String inviteeOid = (String) claims.get("oid");
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));

        List<Invitation> invitations = invitationRepository.findByBoard(board);

        return invitations.stream()
                .filter(invitation -> invitation.getInviteeOid().equals(inviteeOid) && invitation.getStatus() == InvitationStatus.PENDING)
                .map(invitation -> new InvitationResponseDTO(invitation.getId(), invitation.getInviterOid(), invitation.getAccessRight(), invitation.getBoard().getId(), invitation.getStatus()))
                .collect(Collectors.toList());
    }

}
