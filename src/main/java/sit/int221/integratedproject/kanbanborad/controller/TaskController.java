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
@RequestMapping("/v2/tasks")
@CrossOrigin(origins = "http://localhost")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping("")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask(@RequestParam(required = false) String sortBy,
                                                            @RequestParam(required = false) String[] filterStatuses) {
        List<TaskResponseDTO> tasks;
        if (sortBy == null && filterStatuses == null) {
            tasks = taskService.findAllTask();
        } else if (sortBy != null && filterStatuses == null) {
            tasks = taskService.findAllTaskSorted(sortBy);
        } else if (sortBy == null && filterStatuses != null) {
            tasks = taskService.findAllTaskFiltered(filterStatuses);
        } else {
            tasks = taskService.findAllTaskSortedAndFiltered(sortBy, filterStatuses);
        }
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponseDTO> getTaskById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(id));
    }

    @PostMapping("")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody @Valid TaskRequestDTO taskDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(@PathVariable Integer id, @RequestBody @Valid TaskRequestDTO taskDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id));
    }

}
