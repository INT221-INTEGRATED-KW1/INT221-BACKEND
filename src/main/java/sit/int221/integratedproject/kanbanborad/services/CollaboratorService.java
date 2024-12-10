package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardAccessRightRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.CollaboratorRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.CollaboratorStatusUpdateDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.enumeration.AccessRightStatus;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.enumeration.InviteStatus;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ConflictException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.UserOwnRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.List;
import java.util.Optional;

@Service
public class CollaboratorService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserOwnRepository userOwnRepository;
    private final EntityManager entityManager;
    private final ListMapper listMapper;
    private final EmailService emailService;

    public CollaboratorService(BoardRepository boardRepository, UserRepository userRepository,
                               CollaboratorRepository collaboratorRepository, JwtTokenUtil jwtTokenUtil, UserOwnRepository userOwnRepository, EntityManager entityManager, ListMapper listMapper, EmailService emailService) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userOwnRepository = userOwnRepository;
        this.entityManager = entityManager;
        this.listMapper = listMapper;
        this.emailService = emailService;
    }

    @Transactional
    public CollabAddEditResponseDTO addNewCollaborator(String id, String token, CollaboratorRequestDTO collaboratorRequestDTO) {
        Claims claims = extractClaims(token);

        String name = (String) claims.get("name");

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        validateOwnership(claims, id);

        if (collaboratorRequestDTO == null) {
            throw new BadRequestException("Request body is required.");
        }
        if (collaboratorRequestDTO.getEmail() == null || collaboratorRequestDTO.getEmail().isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        if (collaboratorRequestDTO.getAccessRight() == null || collaboratorRequestDTO.getAccessRight().isBlank()) {
            throw new BadRequestException("Access right is required.");
        }
        if (!List.of(AccessRightStatus.READ.name(), AccessRightStatus.WRITE.name()).contains(collaboratorRequestDTO.getAccessRight().toUpperCase())) {
            throw new BadRequestException("Invalid access right. Must be 'READ' or 'WRITE'.");
        }

        String email = collaboratorRequestDTO.getEmail();
        if (!email.endsWith(Utils.SIT_DOMAIN)) {
            throw new BadRequestException("Only emails from 'ad.sit.kmutt.ac.th' domain are supported.");
        }

        Optional<User> msEntraUser = Utils.findUserInMicrosoftEntra(email, token);
        User collaboratorUser = msEntraUser.orElseGet(() -> {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ItemNotFoundException("User with email " + email + " not found in MS Entra or shared database."));
        });

        if (board.getOid().equals(collaboratorUser.getOid())) {
            throw new ConflictException("The email belongs to the board owner.");
        }

        Optional<Collaborator> existingCollaborator = collaboratorRepository.findByBoardAndEmail(board, collaboratorUser.getEmail());
        if (existingCollaborator.isPresent()) {
            throw new ConflictException("This email already belongs to an existing collaborator or pending collaborator.");
        }

        Optional<UserOwn> existingUserOwn = userOwnRepository.findByOid(collaboratorUser.getOid());
        if (existingUserOwn.isEmpty()) {
            UserOwn userOwn = new UserOwn();
            userOwn.setOid(collaboratorUser.getOid());
            userOwn.setName(collaboratorUser.getName());
            userOwn.setUsername(collaboratorUser.getUsername());
            userOwn.setEmail(collaboratorUser.getEmail());
            userOwnRepository.save(userOwn);
        }

        Collaborator collaborator = new Collaborator();
        collaborator.setBoard(board);
        collaborator.setOid(collaboratorUser.getOid());
        collaborator.setName(collaboratorUser.getName());
        collaborator.setEmail(collaboratorUser.getEmail());
        collaborator.setAccessRight(collaboratorRequestDTO.getAccessRight());
        collaborator.setStatus(CollabStatus.PENDING);

        Collaborator savedCollaborator = collaboratorRepository.save(collaborator);
        entityManager.refresh(savedCollaborator);

        sendEmailToCollaborator(collaboratorUser, name, collaboratorRequestDTO.getAccessRight(), board);

        return convertToCollabAddEditResponseDTO(board, savedCollaborator);
    }

    @Transactional
    public BoardAccessRightResponseDTO updateBoardAccessRight(String id, String collabOid, String token, BoardAccessRightRequestDTO boardAccessRightRequestDTO) {
        Claims claims = extractClaims(token);
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        validateOwnership(claims, id);

        if (boardAccessRightRequestDTO == null) {
            throw new BadRequestException("Request body is required.");
        }

        if (boardAccessRightRequestDTO.getAccessRight() == null || boardAccessRightRequestDTO.getAccessRight().isBlank()) {
            throw new BadRequestException("Access right is required.");
        }

        if (!List.of(AccessRightStatus.READ.name(), AccessRightStatus.WRITE.name()).contains(boardAccessRightRequestDTO.getAccessRight().toUpperCase())) {
            throw new BadRequestException("Invalid access right. Must be 'READ' or 'WRITE'.");
        }

        UserOwn newCollaborator = userOwnRepository.findByOid(collabOid)
                .orElseThrow(() -> new ItemNotFoundException("User with oid " + collabOid + " not found in shared database."));

        Collaborator existingCollaborator = collaboratorRepository.findByBoardAndOid(board, collabOid);
        if (existingCollaborator == null) {
            throw new ItemNotFoundException("Collaborator with OID " + collabOid + " is not a collaborator on this board.");
        }

        existingCollaborator.setAccessRight(boardAccessRightRequestDTO.getAccessRight());

        Collaborator updatedCollaborator = collaboratorRepository.save(existingCollaborator);

        return convertToBoardAccessRightResponseDTO(updatedCollaborator);
    }

    @Transactional
    public CollaboratorResponseDTO deleteBoardCollaborator(String id, String collabOid, String token) {
        Claims claims = extractClaims(token);

        String oid = JwtTokenUtil.getOidFromClaims(claims);

        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        boolean isBoardOwner = oid.equals(board.getOid());

        Collaborator tokenCollaborator = collaboratorRepository.findByBoardAndOid(board, oid);
        String tokenCollaboratorRole = (tokenCollaborator != null) ? tokenCollaborator.getAccessRight() : null;

        boolean isSelfLeave = oid.equals(collabOid);

        if (isSelfLeave) {
            if (isBoardOwner) {
                throw new ItemNotFoundException("Collaborator with OID " + collabOid + " is not a collaborator on this board.");
            }

            Collaborator selfCollaborator = collaboratorRepository.findByBoardAndOid(board, collabOid);
            if (selfCollaborator == null) {
                throw new ForbiddenException("Board owner cannot leave the board.");
            }
            collaboratorRepository.delete(selfCollaborator);
            return convertToCollaboratorDTO(selfCollaborator);
        } else {
            if (!isBoardOwner && ! AccessRightStatus.WRITE.name().equalsIgnoreCase(tokenCollaboratorRole)) {
                throw new ForbiddenException("You do not have permission to delete this collaborator.");
            }
            if (AccessRightStatus.WRITE.name().equalsIgnoreCase(tokenCollaboratorRole) && !oid.equals(collabOid)) {
                throw new ForbiddenException("WRITE collaborators cannot remove other collaborators.");
            }

            Collaborator existingCollaborator = collaboratorRepository.findByBoardAndOid(board, collabOid);

            collaboratorRepository.delete(existingCollaborator);
            return convertToCollaboratorDTO(existingCollaborator);
        }
    }

    @Transactional
    public CollabAddEditResponseDTO updateCollaboratorStatus(String boardId, String collaboratorId, CollaboratorStatusUpdateDTO status, Claims claims) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        Collaborator collaborator = collaboratorRepository.findByOidAndBoardId(collaboratorId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found in this board."));

        if (isOwner(oid, boardId)) {
            throw new BadRequestException("Board owner cannot invite themselves as a collaborator.");
        }

        if (!status.getStatus().equals(InviteStatus.ACCEPTED.name()) &&
                !status.getStatus().equals(InviteStatus.DECLINED.name())) {
            throw new BadRequestException("Invalid status update. Must be 'ACCEPTED' or 'DECLINED'.");
        }

        if (status.getStatus().equals(InviteStatus.ACCEPTED.name())) {
            collaborator.setStatus(CollabStatus.ACCEPTED);
        } else {
            collaboratorRepository.delete(collaborator);
            return convertToCollabAddEditResponseDTO(boardId, collaborator);
        }

        Collaborator savedCollaborator = collaboratorRepository.save(collaborator);
        return convertToCollabAddEditResponseDTO(boardId, savedCollaborator);
    }

    public List<CollaboratorResponseDTO> getActiveInvitations(String boardId, String userId) {
        List<Collaborator> pendingInvitations = collaboratorRepository.findByBoardIdAndStatus(boardId, CollabStatus.PENDING);

        boolean isOwner = isOwner(userId, boardId);
        if (!isOwner && pendingInvitations.stream().noneMatch(collab -> collab.getOid().equals(userId))) {
            throw new ForbiddenException("You do not have permission to view these invitations.");
        }

        return listMapper.mapList(pendingInvitations, CollaboratorResponseDTO.class);
    }

    public List<CollaboratorResponseDTO> getPendingInvitationsForCollaborator(String userId) {
        List<Collaborator> pendingInvitations = collaboratorRepository.findByOidAndStatus(userId, CollabStatus.PENDING);
        return listMapper.mapList(pendingInvitations, CollaboratorResponseDTO.class);
    }

    @Transactional
    public CollabAddEditResponseDTO acceptCollaboratorInvitation(String boardId, String collaboratorId, Claims claims) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        Collaborator collaborator = collaboratorRepository.findByOidAndBoardId(collaboratorId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found in this board."));

        if (isOwner(oid, boardId)) {
            throw new ForbiddenException("Board owner cannot invite themselves as a collaborator.");
        }

        if (!collaborator.getStatus().equals(CollabStatus.PENDING)) {
            throw new BadRequestException("Invitation is not in a pending state.");
        }

        collaborator.setStatus(CollabStatus.ACCEPTED);
        Collaborator savedCollaborator = collaboratorRepository.save(collaborator);

        return convertToCollabAddEditResponseDTO(boardId, savedCollaborator);
    }

    @Transactional
    public CollabAddEditResponseDTO declineCollaboratorInvitation(String boardId, String collaboratorId, Claims claims) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        Collaborator collaborator = collaboratorRepository.findByOidAndBoardId(collaboratorId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found in this board."));

        if (isOwner(oid, boardId)) {
            throw new ForbiddenException("Board owner cannot decline themselves as a collaborator.");
        }

        if (!collaborator.getStatus().equals(CollabStatus.PENDING)) {
            throw new BadRequestException("Only pending invitations can be declined.");
        }

        collaboratorRepository.delete(collaborator);
        return convertToCollabAddEditResponseDTO(boardId, collaborator);
    }

    public CollaboratorInvitationResponseDTO getInvitationDetails(String boardId, String userId) {
        Collaborator collaborator = collaboratorRepository.findByOidAndBoardIdAndStatus(userId, boardId, CollabStatus.PENDING)
                .orElseThrow(() -> new ItemNotFoundException("No active invitation found for this user on the specified board."));

        UserOwn inviter = userOwnRepository.findById(collaborator.getBoard().getOid())
                .orElseThrow(() -> new ItemNotFoundException("Inviter not found."));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found."));

        return new CollaboratorInvitationResponseDTO(
                inviter.getName(),
                collaborator.getAccessRight(),
                board.getName(),
                collaborator.getStatus()
        );
    }

    public CollaboratorInvitationResponseDTO checkInvitation(String boardId, String userId) {
        Collaborator collaborator = collaboratorRepository.findByOidAndBoardId(userId, boardId)
                .orElseThrow(() -> new ItemNotFoundException("No collaboration record found for this user on the specified board."));

        UserOwn inviter = userOwnRepository.findById(collaborator.getBoard().getOid())
                .orElseThrow(() -> new ItemNotFoundException("Inviter not found."));

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found."));

        return new CollaboratorInvitationResponseDTO(
                inviter.getName(),
                collaborator.getAccessRight(),
                board.getName(),
                collaborator.getStatus()
        );
    }

    private void sendEmailToCollaborator(User newCollaborator, String inviterName, String accessRight, Board board) {
        String subject = String.format("%s has invited you to collaborate with %s access right on %s", inviterName, accessRight, board.getName());
        String body = String.format("You have been invited to collaborate on the board '%s'.\n\n" +
                "Please accept the invitation at the following link: https://intproj23.sit.kmutt.ac.th/kw1/board/%s/collab/invitations", board.getName(), board.getId());

        try {
            emailService.sendEmail(newCollaborator.getEmail(), subject, body, "ITBKK-KW1" , "DO NOT REPLY <noreply@intproj23.sit.kmutt.ac.th>");
        } catch (Exception e) {
            System.out.println("Error sending email to " + newCollaborator.getEmail() + ": " + e.getMessage());
        }
    }

    public void validateOwnership(Claims claims, String boardId) {
        String oid = JwtTokenUtil.getOidFromClaims(claims);
        if (!isOwner(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    private CollaboratorResponseDTO convertToCollaboratorDTO(Collaborator collaborator) {
        CollaboratorResponseDTO dto = new CollaboratorResponseDTO();
        dto.setOid(collaborator.getOid());
        dto.setName(collaborator.getName());
        dto.setEmail(collaborator.getEmail());
        dto.setAccessRight(collaborator.getAccessRight());
        dto.setAddedOn(collaborator.getAddedOn());
        return dto;
    }

    private CollabAddEditResponseDTO convertToCollabAddEditResponseDTO(Board board, Collaborator collaborator) {
        return new CollabAddEditResponseDTO(
                board.getId(),
                collaborator.getName(),
                collaborator.getEmail(),
                collaborator.getAccessRight(),
                collaborator.getStatus().name()
        );
    }

    private CollabAddEditResponseDTO convertToCollabAddEditResponseDTO(String boardId, Collaborator collaborator) {
        return new CollabAddEditResponseDTO(
                boardId,
                collaborator.getName(),
                collaborator.getEmail(),
                collaborator.getAccessRight(),
                collaborator.getStatus().name()
        );
    }

    private BoardAccessRightResponseDTO convertToBoardAccessRightResponseDTO(Collaborator collaborator) {
        BoardAccessRightResponseDTO responseDTO = new BoardAccessRightResponseDTO();
        responseDTO.setAccessRight(collaborator.getAccessRight());
        return responseDTO;
    }

    private Claims extractClaims(String token) {
        String jwtToken = token.substring(7);

        if (Utils.isMicrosoftToken(jwtToken)) {
            return Utils.extractClaimsFromMicrosoftToken(jwtToken);
        } else {
            return Utils.getClaims(jwtToken, jwtTokenUtil);
        }
    }



}