package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.w3c.dom.ls.LSException;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ListMapper listMapper;

    public List<TaskResponseDTO> findAllTask() {
        List<Task> tasks = taskRepository.findAll();
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSorted(String sortBy) {
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findAll(Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskFiltered(String[] filterStatuses) {
        List<Task> tasks = taskRepository.findByStatusNameIn(Arrays.asList(filterStatuses));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSortedAndFiltered(String sortBy, String[] filterStatuses) {
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByStatusNameIn(Arrays.asList(filterStatuses), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(Integer id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!"));
        return modelMapper.map(task, TaskDetailResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO) {
        Status status = findStatusByIdOrThrow(taskDTO.getStatus());
        if (status.getLimitMaximumTask() && status.getTasks().size() >= Utils.MAX_SIZE) {
            throw new BadRequestException("Can not add task with status exceed limit");
        }
        Task task = new Task();
        populateTaskFromDTO(task, taskDTO, status);
        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO updateTask(Integer id, TaskRequestDTO taskDTO) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!"));
        Status status = findStatusByIdOrThrow(taskDTO.getStatus());
        if (status.getLimitMaximumTask() && status.getTasks().size() >= Utils.MAX_SIZE) {
            throw new BadRequestException("Cannot update task. Status limit exceeded.");
        }
        populateTaskFromDTO(existingTask, taskDTO, status);
        Task updatedTask = taskRepository.save(existingTask);
        return modelMapper.map(updatedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskResponseDTO deleteTask(Integer id) {
        Task taskToDelete = taskRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!"));
        taskRepository.deleteById(id);
        return modelMapper.map(taskToDelete, TaskResponseDTO.class);
    }

    private void validateSortField(String sortBy) {
        List<String> validFields = Arrays.asList("status.name", "status.id", "id", "title", "assignees");
        if (!validFields.contains(sortBy)) {
            throw new BadRequestException("Cannot sort by a field that does not exist");
        }
    }

    private Status findStatusByIdOrThrow(Integer statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ItemNotFoundException("Can not add or update New Task with non-existing status id"));
    }

    private void populateTaskFromDTO(Task task, TaskRequestDTO taskDTO, Status status) {
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);
    }

}
