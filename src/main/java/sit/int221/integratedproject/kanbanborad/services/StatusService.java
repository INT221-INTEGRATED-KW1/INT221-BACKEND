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
import sit.int221.integratedproject.kanbanborad.repositories.StatusLimitRepository;
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
    private StatusLimitRepository statusLimitRepository;
    @Autowired
    private ModelMapper modelMapper;

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
        return getStatusResponseDTO(statusDTO, status);
    }

    @Transactional
    public StatusResponseDetailDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = findStatusByIdAndValidate(id);
        validateStatusForOperation(existingStatus);
        return getStatusResponseDTO(statusDTO, existingStatus);
    }

    private StatusResponseDetailDTO getStatusResponseDTO(StatusRequestDTO statusDTO, Status existingStatus) {
        existingStatus.setName(Utils.trimString(statusDTO.getName()));
        existingStatus.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        existingStatus.setColor(Utils.trimString(statusDTO.getColor()));
        Status updatedStatus = statusRepository.save(existingStatus);
        StatusResponseDetailDTO statusResponseDetailDTO = modelMapper.map(updatedStatus, StatusResponseDetailDTO.class);
        statusResponseDetailDTO.setNoOfTasks(updatedStatus.getTasks().size());
        return statusResponseDetailDTO;
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id) {
        Status statusToDelete = findStatusByIdAndValidate(id);
        validateStatusForOperation(statusToDelete);
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Cannot delete status because it has associated tasks.");
        }
        statusRepository.deleteById(id);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(Integer id, Integer newId) {
        Status statusToDelete = findStatusByIdAndValidate(id);
        Status transferStatus = findStatusByIdAndValidate(newId);
        validateStatusForOperation(statusToDelete);
        List<Task> tasks = taskRepository.findByStatusId(id);
        if (tasks.isEmpty()) {
            throw new BadRequestException("Cannot transfer status because there are no tasks to transfer.");
        }
        checkStatusLimit(transferStatus, tasks.size());
        tasks.forEach(task -> task.setStatus(transferStatus));
        taskRepository.saveAll(tasks);
        statusRepository.deleteById(statusToDelete.getId());
        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }

    private Status findStatusByIdAndValidate(Integer id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
    }

    private void validateStatusForOperation(Status status) {
        if (status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Cannot edit/delete 'No Status' or 'Done' status.");
        }
    }

    private void checkStatusLimit(Status status, int additionalTasks) {
        int totalTasksAfterTransfer = status.getTasks().size() + additionalTasks;
        StatusLimit statusLimit = statusLimitRepository.findById(Utils.STATUS_LIMIT)
                .orElseThrow(() -> new ItemNotFoundException("StatusLimit Id " + Utils.STATUS_LIMIT + " DOES NOT EXIST !!!"));

        boolean isSpecialStatus = status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE);
        if (totalTasksAfterTransfer > Utils.MAX_SIZE && !isSpecialStatus && statusLimit.getStatusLimit()) {
            throw new BadRequestException("Cannot transfer status; limit exceeded.");
        }
    }
}