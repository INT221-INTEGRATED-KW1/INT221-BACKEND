package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusAddEditResponseDTO;
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

    public List<StatusResponseDTO> findAllStatus() {
        List<Status> statuses = statusRepository.findAll();
        for (Status status : statuses) {
            status.setName(Utils.trimString(status.getName()));
            status.setDescription(Utils.trimString(status.getDescription()));
        }
        return listMapper.mapList(statuses, StatusResponseDTO.class);
    }

    public StatusResponseDetailDTO findStatusById(Integer id) {
        return statusRepository.findById(id)
                .map(status -> {
                    StatusResponseDetailDTO statusResponseDTO  = modelMapper.map(status, StatusResponseDetailDTO.class);
                    statusResponseDTO.setName(Utils.trimString(status.getName()));
                    statusResponseDTO.setDescription(Utils.trimString(status.getDescription()));
                    statusResponseDTO.setColor(Utils.trimString(status.getColor()));
                    statusResponseDTO.setCountTask(status.getTasks().size());
                    return statusResponseDTO;
                })
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
    }

    @Transactional
    public StatusAddEditResponseDTO createNewStatus(StatusRequestDTO statusDTO) {
        Status status = new Status();
        status.setName(Utils.trimString(statusDTO.getName()));
        status.setDescription(Utils.trimString(statusDTO.getDescription()));
        status.setColor(Utils.trimString(statusDTO.getColor()));
        List<Status> statuses = statusRepository.findAll();
        for (Status statusLoop : statuses) {
            if (statusLoop.getName().equals(status.getName())) {
                throw new BadRequestException("Could not execute due to duplicate status name.");
            }
            if (statusLoop.getColor().equals(status.getColor())) {
                throw new BadRequestException("Could not execute due to duplicate color name.");
            }
        }

        Status savedStatus = statusRepository.save(status);
        return modelMapper.map(savedStatus, StatusAddEditResponseDTO.class);
    }

    @Transactional
    public StatusAddEditResponseDTO updateStatus(Integer id, StatusRequestDTO statusDTO) {
        if (statusDTO.getName().equals("NO_STATUS")) {
            throw new GeneralException("can not edit NO_STATUS status");
        }
        Status existingStatus = statusRepository.findById(id)
                        .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        existingStatus.setName(Utils.trimString(statusDTO.getName()));
        existingStatus.setDescription(Utils.trimString(statusDTO.getDescription()));
        existingStatus.setColor(Utils.trimString(statusDTO.getColor()));

        Status updatedStatus = statusRepository.save(existingStatus);
        return modelMapper.map(updatedStatus, StatusAddEditResponseDTO.class);
    }

    @Transactional
    public StatusResponseDTO deleteStatus(Integer id) {
        Status statusToDelete = statusRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + id + " DOES NOT EXIST !!!"));
        if (!statusToDelete.getTasks().isEmpty()) {
            throw new GeneralException("can not delete cuz status has task");
        }
        if (statusToDelete.getName().equals("NO_STATUS")) {
            throw new GeneralException("can not delete NO_STATUS status");
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

        List<Task> tasks = taskRepository.findByStatusId(id);
        for (Task task : tasks) {
            task.setStatus(transferStatus);
            taskRepository.save(task);
        }
        if (statusToDelete.getName().equals("NO_STATUS")) {
            throw new GeneralException("can not delete NO_STATUS status");
        }

        statusRepository.deleteById(statusToDelete.getId());

        return modelMapper.map(transferStatus, StatusResponseDTO.class);
    }
}
