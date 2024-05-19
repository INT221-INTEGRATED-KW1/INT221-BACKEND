package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Board;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatusService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private ModelMapper modelMapper;

    public List<StatusResponseDetailDTO> findAllStatus(Integer boardId) {
        Board board = getBoardById(boardId);
        List<Status> statuses = board.getStatuses();
        return mapStatusToDetailDTO(statuses);
    }

    public StatusResponseDTO findStatusById(Integer id, Integer boardId) {
        validateStatusAndBoardExistence(id, boardId);
        Status status = statusRepository.findStatusByIdAndBoardId(id, boardId);
        return modelMapper.map(status, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO createNewStatus(StatusRequestDTO statusDTO, Integer boardId) {
        Board board = getBoardById(boardId);
        Status status = new Status();
        populateStatusFromDTO(status, statusDTO, board);
        Status savedStatus = statusRepository.save(status);
        return modelMapper.map(savedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO updateStatus(Integer id, StatusRequestDTO statusDTO, Integer boardId) {
        Status existingStatus = getStatusById(id);
        Board board = getBoardById(boardId);
        validateStatusForOperation(existingStatus);
        populateStatusFromDTO(existingStatus, statusDTO, board);
        Status updatedStatus = statusRepository.save(existingStatus);
        return modelMapper.map(updatedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id, Integer boardId) {
        Status statusToDelete = validateStatusAndBoardExistence(id, boardId);
        validateStatusForOperation(statusToDelete);
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Cannot delete status because it has associated tasks.");
        }
        statusRepository.deleteById(id);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(Integer id, Integer newId, Integer boardId) {
        validateStatusAndBoardExistence(id, boardId);
        validateStatusAndBoardExistence(newId, boardId);
        Status statusToDelete = statusRepository.findStatusByIdAndBoardId(id, boardId);
        Status transferStatus = statusRepository.findStatusByIdAndBoardId(newId, boardId);
        validateStatusForOperation(statusToDelete);
        transferTasks(statusToDelete, transferStatus);
        statusRepository.deleteById(statusToDelete.getId());
        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }

    private List<StatusResponseDetailDTO> mapStatusToDetailDTO(List<Status> statuses) {
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setNoOfTasks(status.getTasks().size());
            statusResponseDTOs.add(statusResponseDTO);
        }
        return statusResponseDTOs;
    }

    private void populateStatusFromDTO(Status status, StatusRequestDTO statusDTO, Board board) {
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        status.setBoard(board);
    }

    private void transferTasks(Status fromStatus, Status toStatus) {
        List<Task> tasks = taskRepository.findByStatusId(fromStatus.getId());
        if (tasks.isEmpty()) {
            throw new BadRequestException("Cannot transfer status because there are no tasks to transfer.");
        }
        int totalTasksAfterTransfer = toStatus.getTasks().size() + tasks.size();
        boolean isTransferStatusSpecial = isSpecialStatus(toStatus);
        if (totalTasksAfterTransfer > Utils.MAX_SIZE && !isTransferStatusSpecial && toStatus.getBoard().getLimitMaximumStatus()) {
            throw new BadRequestException("Cannot transfer status will exceed the limit");
        }
        tasks.forEach(task -> task.setStatus(toStatus));
        taskRepository.saveAll(tasks);
    }

    private Status getStatusById(Integer id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
    }

    private Board getBoardById(Integer id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
    }

    private Status validateStatusAndBoardExistence(Integer statusId, Integer boardId) {
        if (!statusRepository.existsById(statusId)) {
            throw new BadRequestException("Status Id " + statusId + " DOES NOT EXIST !!!");
        }
        if (!boardRepository.existsById(boardId)) {
            throw new BadRequestException("Board Id " + boardId + " DOES NOT EXIST !!!");
        }
        return statusRepository.findStatusByIdAndBoardId(statusId, boardId);
    }

    private void validateStatusForOperation(Status status) {
        if (isSpecialStatus(status)) {
            throw new BadRequestException("Cannot edit/delete 'No Status' or 'Done' status.");
        }
    }

    private boolean isSpecialStatus(Status status) {
        return status.getName().equalsIgnoreCase(Utils.NO_STATUS) || status.getName().equalsIgnoreCase(Utils.DONE);
    }

}
