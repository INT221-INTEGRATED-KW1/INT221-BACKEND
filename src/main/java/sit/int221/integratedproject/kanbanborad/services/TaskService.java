package sit.int221.integratedproject.kanbanborad.services;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Status;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.FieldNotFoundException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.TaskRepository;
import sit.int221.integratedproject.kanbanborad.utils.ListMapper;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.Arrays;
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
    @Autowired
    private BoardRepository boardRepository;

    public List<TaskResponseDTO> findAllTask(String id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        ;
        List<Task> tasks = board.getTasks();
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSorted(String sortBy, String id) {
        if (!boardRepository.existsById(id)) {
            throw new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        validateSortField(sortBy);

        List<Task> tasks = taskRepository.findByBoardId(id, Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskFiltered(String[] filterStatuses, String id) {
        if (!boardRepository.existsById(id)) {
            throw new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(id, Arrays.asList(filterStatuses));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSortedAndFiltered(String sortBy, String[] filterStatuses,
                                                              String id) {
        if (!boardRepository.existsById(id)) {
            throw new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(id, Arrays.asList(filterStatuses), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(String id, Integer taskId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!"));

        Task findTask = taskRepository.findTaskByIdAndBoardId(taskId, id);
        if (findTask == null) {
            throw new ItemNotFoundException("Task Id " + taskId + " does not belong to Board Id " + id);
        }
        return modelMapper.map(findTask, TaskDetailResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO, String id) {
        Status status = statusRepository.findById(taskDTO.getStatus())
                .orElseThrow(() -> new FieldNotFoundException("status"));
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        if (!status.getBoard().getId().equals(board.getId())) {
            throw new BadRequestException("The status does not belong to the specified board.");
        }
        checkStatusLimit(status);
        Task task = new Task();
        populateTaskFromDTO(task, taskDTO, status, board);
        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO updateTask(String id, TaskRequestDTO taskDTO, Integer taskId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!"));
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!"));
        Status status = statusRepository.findById(taskDTO.getStatus())
                .orElseThrow(() -> new FieldNotFoundException("status"));
        if (!status.getBoard().getId().equals(board.getId())) {
            throw new BadRequestException("The status does not belong to the specified board.");
        }
        checkStatusLimit(status);
        populateTaskFromDTO(existingTask, taskDTO, status, board);
        Task updatedTask = taskRepository.save(existingTask);
        return modelMapper.map(updatedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskResponseDTO deleteTask(String id, Integer taskId) {
        if (!boardRepository.existsById(id)) {
            throw new ItemNotFoundException("Board Id " + id + " DOES NOT EXIST !!!");
        }
        if (!taskRepository.existsById(taskId)) {
            throw new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!");
        }
        Task taskToDelete = taskRepository.findTaskByIdAndBoardId(taskId, id);
        if (taskToDelete == null) {
            throw new ItemNotFoundException("Task Id " + taskId + " does not belong to Board Id " + id);
        }
        taskRepository.deleteById(taskId);
        return modelMapper.map(taskToDelete, TaskResponseDTO.class);
    }

    private void validateSortField(String sortBy) {
        List<String> validFields = Arrays.asList("status.name");
        if (!validFields.contains(sortBy)) {
            throw new BadRequestException("invalid filter parameter");
        }
    }

    private void checkStatusLimit(Status status) {
        boolean isSpecialStatus = status.getName().equals(Utils.NO_STATUS) || status.getName().equals(Utils.DONE);
        if (status.getBoard().getLimitMaximumStatus() && !isSpecialStatus && status.getTasks().size() >= Utils.MAX_SIZE) {
            throw new BadRequestException("Status limit exceeded.");
        }
    }

    private void populateTaskFromDTO(Task task, TaskRequestDTO taskDTO, Status status, Board board) {
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);
        task.setBoard(board);
    }

}