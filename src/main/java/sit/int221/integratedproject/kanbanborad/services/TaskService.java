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
import sit.int221.integratedproject.kanbanborad.entities.Board;
import sit.int221.integratedproject.kanbanborad.entities.Status;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.BoardRepository;
import sit.int221.integratedproject.kanbanborad.repositories.StatusRepository;
import sit.int221.integratedproject.kanbanborad.repositories.TaskRepository;
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
    private BoardRepository boardRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ListMapper listMapper;

    public List<TaskResponseDTO> findAllTask(Integer boardId) {
        Board board = getBoardById(boardId);
        return listMapper.mapList(board.getTasks(), TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSorted(String sortBy, Integer boardId) {
        validateBoardExistence(boardId);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardId(boardId, Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskFiltered(String[] filterStatuses, Integer boardId) {
        validateBoardExistence(boardId);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(boardId, Arrays.asList(filterStatuses));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSortedAndFiltered(String sortBy, String[] filterStatuses, Integer boardId) {
        validateBoardExistence(boardId);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(boardId, Arrays.asList(filterStatuses), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(Integer id, Integer boardId) {
        validateTaskExistence(id);
        validateBoardExistence(boardId);
        Task task = getTaskByIdAndBoardId(id, boardId);
        return modelMapper.map(task, TaskDetailResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO, Integer boardId) {
        Status status = getStatusById(taskDTO.getStatus());
        Board board = getBoardById(boardId);
        validateStatusLimit(status);
        Task task = new Task();
        populateTaskFromDTO(task, taskDTO, status, board);
        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO updateTask(Integer id, TaskRequestDTO taskDTO, Integer boardId) {
        Task existingTask = getTaskById(id);
        Status status = getStatusById(taskDTO.getStatus());
        Board board = getBoardById(boardId);
        validateStatusLimit(status);
        populateTaskFromDTO(existingTask, taskDTO, status, board);
        Task updatedTask = taskRepository.save(existingTask);
        return modelMapper.map(updatedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskResponseDTO deleteTask(Integer id, Integer boardId) {
        validateTaskExistence(id);
        validateBoardExistence(boardId);
        Task taskToDelete = getTaskByIdAndBoardId(id, boardId);
        taskRepository.deleteById(id);
        return modelMapper.map(taskToDelete, TaskResponseDTO.class);
    }

    private void validateSortField(String sortBy) {
        List<String> validFields = Arrays.asList("status.name", "status.id", "id", "title", "assignees");
        if (!validFields.contains(sortBy)) {
            throw new BadRequestException("Cannot sort by a field that does not exist");
        }
    }

    private void validateTaskExistence(Integer taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new BadRequestException("Task Id " + taskId + " DOES NOT EXIST !!!");
        }
    }

    private void validateBoardExistence(Integer boardId) {
        if (!boardRepository.existsById(boardId)) {
            throw new BadRequestException("Board Id " + boardId + " DOES NOT EXIST !!!");
        }
    }

    private void validateStatusLimit(Status status) {
        boolean isTransferStatusSpecial = status.getName().equalsIgnoreCase(Utils.NO_STATUS) ||
                status.getName().equalsIgnoreCase(Utils.DONE);
        if (status.getBoard().getLimitMaximumStatus() && !isTransferStatusSpecial && status.getTasks().size() >= Utils.MAX_SIZE) {
            throw new BadRequestException("Status limit exceeded.");
        }
    }

    private Status getStatusById(Integer statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new ItemNotFoundException("Status Id " + statusId + " DOES NOT EXIST !!!"));
    }

    private Board getBoardById(Integer boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private Task getTaskById(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!"));
    }

    private Task getTaskByIdAndBoardId(Integer taskId, Integer boardId) {
        return taskRepository.findTaskByIdAndBoardId(taskId, boardId);
    }

    private void populateTaskFromDTO(Task task, TaskRequestDTO taskDTO, Status status, Board board) {
        task.setTitle(Utils.trimString(taskDTO.getTitle()));
        task.setDescription(Utils.checkAndSetDefaultNull(taskDTO.getDescription()));
        task.setAssignees(Utils.checkAndSetDefaultNull(taskDTO.getAssignees()));
        task.setStatus(status);
        task.setBoard(board);
    }

}
