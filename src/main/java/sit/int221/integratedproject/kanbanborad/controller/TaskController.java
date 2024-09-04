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
@RequestMapping("/v3/boards")
@CrossOrigin(origins = "http://localhost")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask(@RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String[] filterStatuses,
                                                            @PathVariable String id
    ) {
        List<TaskResponseDTO> tasks;
        if (sortBy == null && filterStatuses == null) {
            tasks = taskService.findAllTask(id);
        } else if (sortBy != null && filterStatuses == null) {
            tasks = taskService.findAllTaskSorted(sortBy, id);
        } else if (sortBy == null && filterStatuses != null) {
            tasks = taskService.findAllTaskFiltered(filterStatuses, id);
        } else {
            tasks = taskService.findAllTaskSortedAndFiltered(sortBy, filterStatuses, id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    @GetMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskDetailResponseDTO> getTaskById(@PathVariable String id,
                                                             @PathVariable Integer taskId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(id, taskId));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO, id));
    }

    @PutMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(@PathVariable String id,
                                                             @RequestBody @Valid TaskRequestDTO taskDTO,
                                                             @PathVariable Integer taskId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO, taskId));
    }

    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable String id, @PathVariable Integer taskId) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id, taskId));
    }

}
