package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sit.int221.integratedproject.kanbanborad.dtos.request.CollaboratorRequestDTO;
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
        // Handle missing request body (400 error)
        if (collaboratorRequestDTO == null) {
            throw new BadRequestException("Request body is required.");
        }

        // Validate access_right field (400 error)
        if (!List.of("READ", "WRITE").contains(collaboratorRequestDTO.getAccess_right().toUpperCase())) {
            throw new BadRequestException("Invalid access right. Must be 'READ' or 'WRITE'.");
        }

        // Validate the token and fetch claims
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board ID " + id + " not found."));

        // Check if the token owner is the board owner (403 error)
        validateOwnership(claims, id);

        // Check if the email exists (404 error)
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

        Optional<UserOwn> existingUserOwn = userOwnRepository.findByOid(newCollaborator.getOid());

        // If the user has never logged in, create an entry in users_own
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

}
