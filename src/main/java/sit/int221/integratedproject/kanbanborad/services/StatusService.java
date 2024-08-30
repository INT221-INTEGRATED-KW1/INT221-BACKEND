package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatusService {
    private final TaskRepository taskRepository;
    private final StatusRepository statusRepository;
    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;

    @Autowired
    public StatusService(TaskRepository taskRepository, StatusRepository statusRepository
                        ,ModelMapper modelMapper, BoardRepository boardRepository) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.boardRepository = boardRepository;
    }

    public List<StatusResponseDetailDTO> findAllStatus(String id) {
        Board board = findBoardByIdAndValidate(id);
        List<Status> statuses = board.getStatuses();
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setNoOfTasks(status.getTasks().size());
            statusResponseDTOs.add(statusResponseDTO);
        }
        return statusResponseDTOs;
    }

    public StatusResponseDTO findStatusById(String id, Integer statusId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        Status status = statusRepository.findStatusByIdAndBoardId(statusId, id);
        if (status == null) {
            throw new ItemNotFoundException("Status Id " + statusId + " does not belong to Board Id " + id);
        }
        return modelMapper.map(status, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDetailDTO createNewStatus(StatusRequestDTO statusDTO, String id) {
        Board board = findBoardByIdAndValidate(id);
        Status status = new Status();
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
        if (!boardRepository.existsById(id)) {
            throw new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        if (!statusRepository.existsById(statusId)) {
            throw new ItemNotFoundException("Status Id " + statusId + " DOES NOT EXIST !!!");
        }
        Status statusToDelete = statusRepository.findStatusByIdAndBoardId(statusId, id);
        if (statusToDelete == null) {
            throw new ItemNotFoundException("Status Id " + statusId + " does not belong to Board Id " + id);
        }
        validateStatusDeletion(statusToDelete);
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Destination status for task transfer not specified.");
        }
        statusRepository.deleteById(statusId);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(String id, Integer oldId, Integer newId) {
        if (!boardRepository.existsById(id)) {
            throw new BadRequestException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        Status statusToDelete = statusRepository.findStatusByIdAndBoardId(oldId, id);
        Status transferStatus = statusRepository.findStatusByIdAndBoardId(newId, id);

        if (statusToDelete == null) {
            throw new ItemNotFoundException("Status Id " + oldId + " DOES NOT EXIST !!!");
        }
        if (transferStatus == null) {
            throw new ItemNotFoundException("Status Id " + newId + " DOES NOT EXIST !!!");
        }

        validateStatusDeletion(statusToDelete);
        transferTasks(statusToDelete, transferStatus);
        statusRepository.deleteById(statusToDelete.getId());
        return modelMapper.map(transferStatus, StatusResponseDTO.class);
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
}
