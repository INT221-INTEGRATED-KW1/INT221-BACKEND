package sit.int221.integratedproject.kanbanborad.services;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Collaborator;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.exceptions.StatusUniqueException;
import sit.int221.integratedproject.kanbanborad.repositories.itbkkshared.UserRepository;
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
    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;

    public StatusService(TaskRepository taskRepository, StatusRepository statusRepository
            , ModelMapper modelMapper, BoardRepository boardRepository, UserRepository userRepository, CollaboratorRepository collaboratorRepository) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.collaboratorRepository = collaboratorRepository;
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
}