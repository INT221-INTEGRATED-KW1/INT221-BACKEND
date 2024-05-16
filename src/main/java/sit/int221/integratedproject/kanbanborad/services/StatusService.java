package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
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
    public StatusResponseDTO createNewStatus(StatusRequestDTO statusDTO) {
        Status status = new Status();
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        status.setLimitMaximumTask(Utils.DEFAULT_LIMIT);
        Status savedStatus = statusRepository.save(status);
        return modelMapper.map(savedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = findStatusByIdAndValidate(id);
        validateStatusForOperation(existingStatus);
        existingStatus.setName(Utils.trimString(statusDTO.getName()));
        existingStatus.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        existingStatus.setColor(Utils.trimString(statusDTO.getColor()));
        Status updatedStatus = statusRepository.save(existingStatus);
        return modelMapper.map(updatedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusLimitResponseDTO updateStatusLimit(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = findStatusByIdAndValidate(id);
        validateStatusForOperation(existingStatus);
        int noOfTasks = existingStatus.getTasks().size();
        if (statusDTO.getLimitMaximumTask()) {
            existingStatus.setLimitMaximumTask(noOfTasks <= Utils.MAX_SIZE);
        }
        Status updatedStatus = statusRepository.save(existingStatus);
        StatusLimitResponseDTO responseDTO = new StatusLimitResponseDTO();
        responseDTO.setId(updatedStatus.getId());
        responseDTO.setName(updatedStatus.getName());
        responseDTO.setLimitMaximumTask(updatedStatus.getLimitMaximumTask());
        responseDTO.setNoOfTasks(noOfTasks);
        if (noOfTasks > Utils.MAX_SIZE) {
            responseDTO.setTasks(updatedStatus.getTasks());
        }
        return responseDTO;
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
        int totalTasksAfterTransfer = transferStatus.getTasks().size() + tasks.size();
        boolean isTransferStatusSpecial = transferStatus.getName().equals(Utils.NO_STATUS) || transferStatus.getName().equals(Utils.DONE);
        if (totalTasksAfterTransfer > Utils.MAX_SIZE && !isTransferStatusSpecial) {
            throw new BadRequestException("Can not transfer status will exceed the limit");
        }
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
}
