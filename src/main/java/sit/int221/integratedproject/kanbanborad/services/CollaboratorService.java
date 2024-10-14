package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardAccessRightRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.CollaboratorRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.BoardAccessRightResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.CollabAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.CollaboratorResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.itbkkshared.User;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.UserOwn;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ConflictException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.UserOwnRepository;
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

    public CollaboratorService(BoardRepository boardRepository, UserRepository userRepository,
                               CollaboratorRepository collaboratorRepository, JwtTokenUtil jwtTokenUtil, UserOwnRepository userOwnRepository, EntityManager entityManager) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userOwnRepository = userOwnRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public CollaboratorResponseDTO addNewCollaborator(String id, String token, CollaboratorRequestDTO collaboratorRequestDTO) {
        // Validate the token and fetch claims (do this first)
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        // Check if the board exists (404 error)
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        // Check if the token owner is the board owner (403 error)
        validateOwnership(claims, id);

        // Now handle missing request body (400 error)
        if (collaboratorRequestDTO == null) {
            throw new BadRequestException("Request body is required.");
        }

        // Manually validate 'email' and 'access_right' (400 error if empty)
        if (collaboratorRequestDTO.getEmail() == null || collaboratorRequestDTO.getEmail().isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        if (collaboratorRequestDTO.getAccess_right() == null || collaboratorRequestDTO.getAccess_right().isBlank()) {
            throw new BadRequestException("Access right is required.");
        }

        // Validate access_right field value (400 error)
        if (!List.of("READ", "WRITE").contains(collaboratorRequestDTO.getAccess_right().toUpperCase())) {
            throw new BadRequestException("Invalid access right. Must be 'READ' or 'WRITE'.");
        }

        // Check if the email exists in the user repository (404 error)
        User newCollaborator = userRepository.findByEmail(collaboratorRequestDTO.getEmail())
                .orElseThrow(() -> new ItemNotFoundException("User with email " + collaboratorRequestDTO.getEmail() + " not found in shared database."));

        // Check if the email belongs to the board owner (409 Conflict)
        if (board.getOid().equals(newCollaborator.getOid())) {
            throw new ConflictException("The email belongs to the board owner.");
        }

        // Check if the email already belongs to an existing collaborator (409 Conflict)
        Optional<Collaborator> existingCollaborator = collaboratorRepository.findByBoardAndEmail(board, newCollaborator.getEmail());
        if (existingCollaborator.isPresent()) {
            throw new ConflictException("This email already belongs to an existing collaborator.");
        }

        // If the user has never logged in, create an entry in users_own
        Optional<UserOwn> existingUserOwn = userOwnRepository.findByOid(newCollaborator.getOid());
        if (existingUserOwn.isEmpty()) {
            UserOwn userOwn = new UserOwn();
            userOwn.setOid(newCollaborator.getOid());
            userOwn.setName(newCollaborator.getName());
            userOwn.setUsername(newCollaborator.getUsername());
            userOwn.setEmail(newCollaborator.getEmail());
            userOwnRepository.save(userOwn);  // Save to users_own
        }

        // Proceed with adding collaborator logic
        Collaborator collaborator = new Collaborator();
        collaborator.setBoard(board);
        collaborator.setOid(newCollaborator.getOid());
        collaborator.setName(newCollaborator.getName());
        collaborator.setEmail(newCollaborator.getEmail());
        collaborator.setAccessRight(collaboratorRequestDTO.getAccess_right());

        Collaborator savedCollaborator = collaboratorRepository.save(collaborator);
        entityManager.refresh(savedCollaborator);

        return convertToCollaboratorDTO(savedCollaborator);
    }

    @Transactional
    public CollaboratorResponseDTO  updateBoardAccessRight(String id, String collabOid, String token, BoardAccessRightRequestDTO boardAccessRightRequestDTO) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        // Check if the token owner is the board owner (403 error)
        validateOwnership(claims, id);

        if (boardAccessRightRequestDTO == null) {
            throw new BadRequestException("Request body is required.");
        }

        if (boardAccessRightRequestDTO.getAccess_right() == null || boardAccessRightRequestDTO.getAccess_right().isBlank()) {
            throw new BadRequestException("Access right is required.");
        }

        // Validate access_right field (400 error)
        if (!List.of("READ", "WRITE").contains(boardAccessRightRequestDTO.getAccess_right().toUpperCase())) {
            throw new BadRequestException("Invalid access right. Must be 'READ' or 'WRITE'.");
        }

        // Check if the email exists (404 error)
        User newCollaborator = userRepository.findByOid(collabOid)
                .orElseThrow(() -> new ItemNotFoundException("User with oid " + collabOid + " not found in shared database."));

        Collaborator existingCollaborator = collaboratorRepository.findByBoardAndOid(board, collabOid);
        if (existingCollaborator == null) {
            throw new ItemNotFoundException("Collaborator with OID " + collabOid + " is not a collaborator on this board.");
        }

        existingCollaborator.setAccessRight(boardAccessRightRequestDTO.getAccess_right());

        Collaborator updatedCollaborator = collaboratorRepository.save(existingCollaborator);

        return convertToCollaboratorDTO(updatedCollaborator);
    }

    @Transactional
    public CollaboratorResponseDTO deleteBoardCollaborator(String id, String collabOid, String token) {
        // Validate the token and fetch claims
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        // Check if the board exists (404 error)
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        // Check if the token owner is either the board owner or a collaborator (403 error)
        String oid = claims.get("oid", String.class);
        boolean isBoardOwner = oid.equals(board.getOid());
        Collaborator tokenCollaborator = collaboratorRepository.findByBoardAndOid(board, oid);

        if (!isBoardOwner && tokenCollaborator == null) {
            throw new ForbiddenException("You do not have permission to delete collaborators on this board.");
        }

        // Check if the collaborator exists (404 error)
        Collaborator existingCollaborator = collaboratorRepository.findByBoardAndOid(board, collabOid);
        if (existingCollaborator == null) {
            throw new ItemNotFoundException("Collaborator with OID " + collabOid + " is not a collaborator on this board.");
        }

        // Remove collaborator from the board
        collaboratorRepository.delete(existingCollaborator);
        return convertToCollaboratorDTO(existingCollaborator);
    } 

    private void validateOwnership(Claims claims, String boardId) {
        String oid = (String) claims.get("oid");
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
                collaborator.getAccessRight()
        );
    }

    private BoardAccessRightResponseDTO convertToBoardAccessRightResponseDTO(Collaborator collaborator) {
        BoardAccessRightResponseDTO responseDTO = new BoardAccessRightResponseDTO();
        responseDTO.setAccessRight(collaborator.getAccessRight());
        return responseDTO;
    }

}
