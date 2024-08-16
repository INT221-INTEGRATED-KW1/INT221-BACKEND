package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.StatusLimit;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusLimitRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatusService {
    private final TaskRepository taskRepository;
    private final StatusRepository statusRepository;
    private final StatusLimitRepository statusLimitRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public StatusService(TaskRepository taskRepository, StatusRepository statusRepository,
                         StatusLimitRepository statusLimitRepository, ModelMapper modelMapper) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.statusLimitRepository = statusLimitRepository;
        this.modelMapper = modelMapper;
    }

    public List<StatusResponseDetailDTO> findAllStatus() {
        List<Status> statuses = statusRepository.findAll();
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setNoOfTasks(status.getTasks().size());
            statusResponseDTOs.add(statusResponseDTO);
        }
        return statusResponseDTOs;
    }

    public StatusResponseDTO findStatusById(Integer id) {
        Status status = findStatusByIdAndValidate(id);
        return modelMapper.map(status, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDetailDTO createNewStatus(StatusRequestDTO statusDTO) {
        Status status = new Status();
        return saveStatus(statusDTO, status);
    }

    @Transactional
    public StatusResponseDetailDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = findStatusByIdAndValidate(id);
        validateStatusModification(existingStatus);
        return saveStatus(statusDTO, existingStatus);
    }

    private StatusResponseDetailDTO saveStatus(StatusRequestDTO statusDTO, Status status) {
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        Status savedStatus = statusRepository.save(status);
        return convertToDetailDTO(savedStatus);
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id) {
        Status statusToDelete = findStatusByIdAndValidate(id);
        validateStatusDeletion(statusToDelete);
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Destination status for task transfer not specified.");
        }
        statusRepository.deleteById(id);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(Integer id, Integer newId) {
        Status statusToDelete = findStatusByIdAndValidate(id);
        Status transferStatus = statusRepository.findById(newId)
                .orElseThrow(() -> new BadRequestException("the specified status for task transfer does not exist"));
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
        StatusLimit statusLimit = statusLimitRepository.findById(Utils.STATUS_LIMIT)
                .orElseThrow(() -> new BadRequestException("StatusLimit Id " + Utils.STATUS_LIMIT + " DOES NOT EXIST !!!"));

        boolean isSpecialStatus = status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE);
        if (totalTasksAfterTransfer > Utils.MAX_SIZE && !isSpecialStatus && statusLimit.getStatusLimit()) {
            throw new BadRequestException("Cannot transfer status; limit exceeded.");
        }
    }   

    private StatusResponseDetailDTO convertToDetailDTO(Status status) {
        StatusResponseDetailDTO dto = modelMapper.map(status, StatusResponseDetailDTO.class);
        dto.setNoOfTasks(status.getTasks().size());
        return dto;
    }
}
