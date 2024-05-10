package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusResponseDetailDTO;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.GeneralException;
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
    @Autowired
    private ListMapper listMapper;

    public List<StatusResponseDetailDTO> findAllStatus() {
        List<Status> statuses = statusRepository.findAll();
        List<StatusResponseDetailDTO> statusResponseDTOs = new ArrayList<>();
        for (Status status : statuses) {
            StatusResponseDetailDTO statusResponseDTO = modelMapper.map(status, StatusResponseDetailDTO.class);
            statusResponseDTO.setName(Utils.trimString(status.getName()));
            statusResponseDTO.setDescription(Utils.trimString(status.getDescription()));
            statusResponseDTO.setColor(Utils.trimString(status.getColor()));
            statusResponseDTO.setCountTask(status.getTasks().size());
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
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.trimString(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        List<Status> statuses = statusRepository.findAll();
        boolean isDuplicateName = statuses.stream().anyMatch(s -> s.getName().equals(status.getName()));
        boolean isDuplicateColor = statuses.stream().anyMatch(s -> s.getColor().equals(status.getColor()));
        if (isDuplicateName) {
            throw new BadRequestException("Status with name '" + status.getName() + "' already exists.");
        }
        if (isDuplicateColor) {
            throw new BadRequestException("Status with color '" + status.getColor() + "' already exists.");
        }

        Status savedStatus = statusRepository.save(status);
        return modelMapper.map(savedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        Status existingStatus = statusRepository.findById(id)
                        .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (existingStatus.getName().equals("NO_STATUS")) {
            throw new GeneralException("Cannot edit 'NO_STATUS' status.");
        }

        existingStatus.setName(Utils.trimString(statusDTO.getName()));
        existingStatus.setDescription(Utils.trimString(statusDTO.getDescription()));
        existingStatus.setColor(Utils.trimString(statusDTO.getColor()));

        Status updatedStatus = statusRepository.save(existingStatus);
        return modelMapper.map(updatedStatus, StatusResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id) {
        Status statusToDelete = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new GeneralException("Cannot delete status because it has associated tasks.");
        }
        if (statusToDelete.getName().equals("NO_STATUS")) {
            throw new GeneralException("Cannot delete 'NO_STATUS' status.");
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
        if (statusToDelete.getName().equals("NO_STATUS")) {
            throw new GeneralException("Cannot delete 'NO_STATUS' status.");
        }
        List<Task> tasks = taskRepository.findByStatusId(id);
        tasks.forEach(task -> task.setStatus(transferStatus));
        taskRepository.saveAll(tasks);

        statusRepository.deleteById(statusToDelete.getId());

        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }
}
