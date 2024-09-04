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
    private final TaskRepository taskRepository;
    private final StatusRepository statusRepository;
    private final ModelMapper modelMapper;
    private final ListMapper listMapper;
    private final BoardRepository boardRepository;

    public TaskService(TaskRepository taskRepository, StatusRepository statusRepository, ModelMapper modelMapper, ListMapper listMapper, BoardRepository boardRepository) {
        this.taskRepository = taskRepository;
        this.statusRepository = statusRepository;
        this.modelMapper = modelMapper;
        this.listMapper = listMapper;
        this.boardRepository = boardRepository;
    }

    public List<TaskResponseDTO> findAllTask(String id) {
        Board board = getBoardById(id);
        List<Task> tasks = board.getTasks();
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSorted(String sortBy, String id) {
        Board board = getBoardById(id);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardId(board.getId(), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskFiltered(String[] filterStatuses, String id) {
        Board board = getBoardById(id);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(board.getId(), Arrays.asList(filterStatuses));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public List<TaskResponseDTO> findAllTaskSortedAndFiltered(String sortBy, String[] filterStatuses, String id) {
        Board board = getBoardById(id);
        validateSortField(sortBy);
        List<Task> tasks = taskRepository.findByBoardIdAndStatusNameIn(board.getId(), Arrays.asList(filterStatuses), Sort.by(sortBy));
        return listMapper.mapList(tasks, TaskResponseDTO.class);
    }

    public TaskDetailResponseDTO findTaskById(String id, Integer taskId) {
        Board board = getBoardById(id);
        Task task = getTaskById(taskId);
        validateTaskBelongsToBoard(task, board.getId());
        return modelMapper.map(task, TaskDetailResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO createNewTask(TaskRequestDTO taskDTO, String id) {
        Status status = getStatusById(taskDTO.getStatus());
        Board board = getBoardById(id);
        validateStatusBelongsToBoard(status, board.getId());
        checkStatusLimit(status);

        Task task = new Task();
        populateTaskFromDTO(task, taskDTO, status, board);
        Task savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskAddEditResponseDTO updateTask(String id, TaskRequestDTO taskDTO, Integer taskId) {
        Board board = getBoardById(id);
        Task existingTask = getTaskById(taskId);
        Status status = getStatusById(taskDTO.getStatus());
        validateStatusBelongsToBoard(status, board.getId());
        checkStatusLimit(status);

        populateTaskFromDTO(existingTask, taskDTO, status, board);
        Task updatedTask = taskRepository.save(existingTask);
        return modelMapper.map(updatedTask, TaskAddEditResponseDTO.class);
    }

    @Transactional
    public TaskResponseDTO deleteTask(String id, Integer taskId) {
        Board board = getBoardById(id);
        Task taskToDelete = getTaskById(taskId);
        validateTaskBelongsToBoard(taskToDelete, board.getId());
        taskRepository.deleteById(taskId);
        return modelMapper.map(taskToDelete, TaskResponseDTO.class);
    }

    private void validateSortField(String sortBy) {
        List<String> validFields = Arrays.asList("status.name");
        if (!validFields.contains(sortBy)) {
            throw new BadRequestException("Invalid filter parameter: " + sortBy);
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

    private Board getBoardById(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private Task getTaskById(Integer taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ItemNotFoundException("Task Id " + taskId + " DOES NOT EXIST !!!"));
    }

    private Status getStatusById(Integer statusId) {
        return statusRepository.findById(statusId)
                .orElseThrow(() -> new FieldNotFoundException("Status Id " + statusId + " DOES NOT EXIST !!!"));
    }

    private void validateTaskBelongsToBoard(Task task, String boardId) {
        if (!task.getBoard().getId().equals(boardId)) {
            throw new ItemNotFoundException("Task Id " + task.getId() + " does not belong to Board Id " + boardId);
        }
    }

    private void validateStatusBelongsToBoard(Status status, String boardId) {
        if (!status.getBoard().getId().equals(boardId)) {
            throw new BadRequestException("The status does not belong to the specified board.");
        }
    }

}