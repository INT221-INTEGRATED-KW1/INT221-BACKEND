package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

import java.util.List;

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
        for (Task task : tasks) {
            task.setTitle(Utils.trimString(task.getTitle()));
            task.setAssignees(Utils.trimString(task.getAssignees()));
        }
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(Integer id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!"));
        task.setTitle(Utils.trimString(task.getTitle()));
        task.setDescription(Utils.trimString(task.getDescription()));
        task.setAssignees(Utils.trimString(task.getAssignees()));
        return modelMapper.map(task, TaskDetailResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO) {
        if (taskDTO.getTitle() == null) {
            throw new BadRequestException("title can not be null");
        }
        String statusName = taskDTO.getStatus();
        Status status = statusRepository.findByName(statusName).orElse(null);

        if (status == null) {
            status = statusRepository.findByName("NO_STATUS").orElse(null);
        }
        Task task = new Task();
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);

        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO updateTask(Integer id, TaskRequestDTO taskDTO) {
        String statusName = taskDTO.getStatus();
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + id + " DOES NOT EXIST !!!"));

        existingTask.setTitle(Utils.trimString(taskDTO.getTitle()));
        existingTask.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        existingTask.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));

        Status status = statusRepository.findByName(statusName).orElse(null);
        if (status == null) {
            status = statusRepository.findByName("NO_STATUS").orElse(null);
        }
        existingTask.setStatus(status);

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

}
