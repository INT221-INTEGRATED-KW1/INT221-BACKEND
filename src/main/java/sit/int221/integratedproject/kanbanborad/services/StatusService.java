
package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;
import sit.int221.integratedproject.kanbanborad.enumeration.CollabStatus;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.exceptions.StatusUniqueException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.CollaboratorRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StatusService {
    private final TaskRepository taskRepository;
    private final StatusRepository statusRepository;
    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public StatusService(TaskRepository taskRepository, StatusRepository statusRepository
            , ModelMapper modelMapper, BoardRepository boardRepository, CollaboratorRepository collaboratorRepository, JwtTokenUtil jwtTokenUtil) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.boardRepository = boardRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public List<StatusResponseDetailDTO> findAllStatus(Claims claims, String id) {
        Board board = findBoardByIdAndValidateAccess(claims, id);

        List<Status> statuses = board.getStatuses();
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setNoOfTasks(status.getTasks().size());
            statusResponseDTOs.add(statusResponseDTO);
        }
        return statusResponseDTOs;
    }

    public StatusResponseDTO findStatusById(Claims claims, String id, Integer statusId) {
        Board board = findBoardByIdAndValidateAccess(claims, id);

        Status status = statusRepository.findStatusByIdAndBoardId(statusId, id);
        if (status == null) {
            throw new ItemNotFoundException("Status Id " + statusId + " does not belong to Board Id " + id);
        }
        return modelMapper.map(status, StatusResponseDTO.class);
    }

    private Board findBoardByIdAndValidateAccess(Claims claims, String id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));

        if (!board.getVisibility().equalsIgnoreCase("PUBLIC")) {
            if (claims == null) {
                throw new ForbiddenException("Authentication required to access this board.");
            }

            String oid = (String) claims.get("oid");
            if (!isOwner(oid, id) && !isCollaborator(oid, id)) {
                throw new ForbiddenException("You are not allowed to access this board.");
            }
        }
        return board;
    }

    @Transactional
    public StatusResponseDetailDTO createNewStatus(StatusRequestDTO statusDTO, String id) {
        Board board = findBoardByIdAndValidate(id);
        Status status = new Status();

        boolean statusExists = statusRepository.existsByNameAndBoardId(statusDTO.getName(), board.getId());
        if (statusExists) {
            throw new StatusUniqueException("Status name '" + statusDTO.getName() + "' already exists in board '" + board.getName() + "'");
        }

        return saveStatus(statusDTO, status, board);
    }

    @Transactional
    public StatusResponseDetailDTO updateStatus(String id, StatusRequestDTO statusDTO, Integer statusId) {
        Board board = findBoardByIdAndValidate(id);
        Status existingStatus = findStatusByIdAndValidate(statusId);

        if (!existingStatus.getBoard().getId().equals(id)) {
            throw new ItemNotFoundException("Status Id " + statusId + " does not belong to Board Id " + id);
        }

        validateStatusModification(existingStatus);

        if (!existingStatus.getName().equals(statusDTO.getName())) {
            boolean statusExists = statusRepository.existsByNameAndBoardId(statusDTO.getName(), board.getId());
            if (statusExists) {
                throw new StatusUniqueException("Status name '" + statusDTO.getName() + "' already exists in board '" + board.getName() + "'");
            }
        }

        return saveStatus(statusDTO, existingStatus, board);
    }

    private StatusResponseDetailDTO saveStatus(StatusRequestDTO statusDTO, Status status, Board board) {
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        status.setBoard(board);

        Status savedStatus = statusRepository.save(status);
        return convertToDetailDTO(savedStatus);
    }

    @Transactional
    public StatusResponseDTO deleteStatus(String id, Integer statusId) {
        Status statusToDelete = getStatusAndValidate(id, statusId);

        validateStatusDeletion(statusToDelete);
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Destination status for task transfer not specified.");
        }
        statusRepository.deleteById(statusId);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(String id, Integer oldId, Integer newId) {
        Status statusToDelete = getStatusAndValidate(id, oldId);
        Status transferStatus = getStatusAndValidate(id, newId);

        validateStatusDeletion(statusToDelete);
        transferTasks(statusToDelete, transferStatus);
        statusRepository.deleteById(statusToDelete.getId());
        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }

    private Status getStatusAndValidate(String boardId, Integer statusId) {
        if (!boardRepository.existsById(boardId)) {
            throw new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!");
        }
        Status status = statusRepository.findStatusByIdAndBoardId(statusId, boardId);
        if (status == null) {
            throw new ItemNotFoundException("Status Id " + statusId + " does not belong to Board Id " + boardId);
        }
        return status;
    }

    private Status findStatusByIdAndValidate(Integer id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
    }

    private void validateStatusModification(Status status) {
        if (status.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("No Status cannot be modified");
        }
        if (status.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Done cannot be modified");
        }
    }

    private void validateStatusDeletion(Status status) {
        if (status.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("No Status cannot be deleted");
        }
        if (status.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Done cannot be deleted");
        }
    }

    private void transferTasks(Status fromStatus, Status toStatus) {
        List<Task> tasks = taskRepository.findByStatusId(fromStatus.getId());
        if (tasks.isEmpty()) {
            throw new BadRequestException("Cannot transfer status because there are no tasks to transfer.");
        }
        checkStatusLimit(toStatus, tasks.size());
        tasks.forEach(task -> task.setStatus(toStatus));
        taskRepository.saveAll(tasks);
    }

    private void checkStatusLimit(Status status, int additionalTasks) {
        int totalTasksAfterTransfer = status.getTasks().size() + additionalTasks;

        boolean isSpecialStatus = status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE);
        if (totalTasksAfterTransfer > Utils.MAX_SIZE && !isSpecialStatus && status.getBoard().getLimitMaximumStatus()) {
            throw new BadRequestException("Cannot transfer status; limit exceeded.");
        }
    }

    private StatusResponseDetailDTO convertToDetailDTO(Status status) {
        StatusResponseDetailDTO dto = modelMapper.map(status, StatusResponseDetailDTO.class);
        dto.setNoOfTasks(status.getTasks().size());
        return dto;
    }

    private Board findBoardByIdAndValidate(String id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
    }

    private boolean isCollaborator(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        return collaborator.isPresent();
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    public Claims validateMicrosoftTokenAndAuthorization(String token, String boardId) {
        Claims claims = Utils.extractClaimsFromMicrosoftToken(token);

        String oid = JwtTokenUtil.getOidFromClaims(claims);

        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }

        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        if (collaborator.isPresent() && collaborator.get().getStatus() == CollabStatus.PENDING) {
            throw new ForbiddenException("You cannot access this board because your invitation is pending.");
        }

        return claims;
    }

    public Claims validateMicrosoftTokenForModify(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
        Claims claims;
        try {
            claims = Utils.extractClaimsFromMicrosoftToken(jwtToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Microsoft Token");
        }

        String oid = JwtTokenUtil.getOidFromClaims(claims);

        if (!isOwner(oid, boardId) && !hasWriteAccess(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to modify this board.");
        }

        return claims;
    }

    public boolean hasWriteAccess(String oid, String boardId) {
        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);

        if (collaborator.isPresent()) {
            return collaborator.get().getAccessRight().equalsIgnoreCase("WRITE");
        }

        return false;
    }

    public Claims validateReadTokenAndOwnership(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
        Claims claims;
        try {
            claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        } catch (SignatureException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has been tampered with");
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
        }  catch (MalformedJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token not well-formed");
        }

        String oid = JwtTokenUtil.getOidFromClaims(claims);

        if (!isOwner(oid, boardId) && !isCollaborator(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }

        Optional<Collaborator> collaborator = collaboratorRepository.findByOidAndBoardId(oid, boardId);
        if (collaborator.isPresent() && collaborator.get().getStatus() == CollabStatus.PENDING) {
            throw new ForbiddenException("You cannot access this board because your invitation is pending.");
        }

        return claims;
    }

    public Claims validateTokenAndOwnership(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
        Claims claims;
        try {
            claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        } catch (SignatureException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has been tampered with");
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
        }  catch (MalformedJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token not well-formed");
        }

        String oid = JwtTokenUtil.getOidFromClaims(claims);
        if (!isOwner(oid, boardId) && !hasWriteAccess(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to modify this board.");
        }

        return claims;
    }

    public boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }

    public Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    public Board validateBoardAndOwnership(String boardId, String token) {
        Board board = getBoardOrThrow(boardId);
        Claims claims;

        if (Utils.isMicrosoftToken(token)) {
            claims = validateMicrosoftTokenForModify(token, boardId);
        } else {
            claims = validateTokenAndOwnership(token, boardId);
        }
        return board;
    }


}
