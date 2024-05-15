package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
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
    private ModelMapper modelMapper;

    public List<StatusResponseDetailDTO> findAllStatus() {
        List<Status> statuses = statusRepository.findAll();
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setName(Utils.trimString(status.getName()));
            statusResponseDTO.setDescription(Utils.trimString(status.getDescription()));
            statusResponseDTO.setColor(Utils.trimString(status.getColor()));
            statusResponseDTO.setNoOfTasks(status.getTasks().size());
            statusResponseDTOs.add(statusResponseDTO);
        }
        return statusResponseDTOs;
    }

    public StatusResponseDTO findStatusById(Integer id) {
        return statusRepository.findById(id)
                .map(status -> {
                    StatusResponseDTO statusResponseDTO  = modelMapper.map(status, StatusResponseDTO.class);
                    statusResponseDTO.setName(Utils.trimString(status.getName()));
                    statusResponseDTO.setDescription(Utils.trimString(status.getDescription()));
                    statusResponseDTO.setColor(Utils.trimString(status.getColor()));
                    return statusResponseDTO;
                })
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
    }

    @Transactional
    public StatusResponseDTO createNewStatus(StatusRequestDTO statusDTO) {
        Status status = new Status();
        return getStatusResponseDTO(statusDTO, status);
    }

    @Transactional
    public StatusResponseDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = statusRepository.findById(id)
                        .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (existingStatus.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("Cannot edit 'No Status' status.");
        }
        if (existingStatus.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Cannot edit 'Done' status.");
        }

        return getStatusResponseDTO(statusDTO, existingStatus);
    }

    private StatusResponseDTO getStatusResponseDTO(StatusRequestDTO statusDTO, Status existingStatus) {
        existingStatus.setName(Utils.trimString(statusDTO.getName()));
        existingStatus.setDescription(Utils.checkAndSetDefaultNull(statusDTO.getDescription()));
        existingStatus.setLimitMaximumTask(Utils.DEFAULT_LIMIT);
        existingStatus.setColor(Utils.trimString(statusDTO.getColor()));

        Status updatedStatus = statusRepository.save(existingStatus);
        return modelMapper.map(updatedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusLimitResponseDTO updateStatusLimit(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (existingStatus.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("Cannot enable/disable 'No Status' status.");
        }
        if (existingStatus.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Cannot enable/disable 'Done' status.");
        }
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

        if (noOfTasks > 10) {
            responseDTO.setTasks(updatedStatus.getTasks());
        }

        return responseDTO;
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id) {
        Status statusToDelete = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new BadRequestException("Cannot delete status because it has associated tasks.");
        }
        if (statusToDelete.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("Cannot delete 'No Status' status.");
        }
        if (statusToDelete.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Cannot delete 'Done' status.");
        }
        statusRepository.deleteById(id);
        return modelMapper.map(statusToDelete, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteTaskAndTransferStatus(Integer id, Integer newId) {
        Status statusToDelete = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        Status transferStatus = statusRepository.findById(newId)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + newId + " DOES NOT EXIST !!!"));
        if (statusToDelete.getName().equals(Utils.NO_STATUS)) {
            throw new BadRequestException("Cannot delete 'No Status' status.");
        }
        if (statusToDelete.getName().equals(Utils.DONE)) {
            throw new BadRequestException("Cannot delete 'Done' status.");
        }
        List<Task> tasks = taskRepository.findByStatusId(id);
        if (tasks.isEmpty()) {
            throw new BadRequestException("Cannot transfer status because there are no tasks to transfer.");
        }
        int totalTasksAfterTransfer = transferStatus.getTasks().size() + tasks.size();
        if (totalTasksAfterTransfer > Utils.MAX_SIZE) {
            throw new BadRequestException("Can not transfer status will exceed the limit");
        }
        tasks.forEach(task -> task.setStatus(transferStatus));
        taskRepository.saveAll(tasks);

        statusRepository.deleteById(statusToDelete.getId());

        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }
}
