package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskAddEditResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskDetailResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.TaskResponseDTO;
import sit.int221.integratedproject.kanbanborad.services.TaskService;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping("/{boardId}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask(@RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String[] filterStatuses,
                                                            @PathVariable Integer boardId) {
        List<TaskResponseDTO> tasts;
        if (sortBy == null && filterStatuses == null) {
            tasts = taskService.findAllTask(boardId);
        } else if (sortBy != null && filterStatuses == null) {
            tasts = taskService.findAllTaskSorted(sortBy, boardId);
        } else if (sortBy == null && filterStatuses != null) {
            tasts = taskService.findAllTaskFiltered(filterStatuses, boardId);
        } else {
            tasts = taskService.findAllTaskSortedAndFiltered(sortBy, filterStatuses, boardId);
        }
        return ResponseEntity.status(HttpStatus.OK).body(tasts);
    }

    @GetMapping("/{boardId}/tasks/{id}")
    public ResponseEntity<TaskDetailResponseDTO> getTaskById(@PathVariable Integer id, @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(id, boardId));
    }

    @PostMapping("/{boardId}/tasks")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO, boardId));
    }

    @PutMapping("/{boardId}/tasks/{id}")
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(@PathVariable Integer id, @RequestBody @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO, boardId));
    }

    @DeleteMapping("/{boardId}/tasks/{id}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable Integer id, @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id, boardId));
    }

}