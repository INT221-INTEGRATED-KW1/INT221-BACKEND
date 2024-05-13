package sit.int221.integratedproject.kanbanborad.controller;

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
@CrossOrigin(origins = {"http://ip23kw1.sit.kmutt.ac.th", "http://intproj23.sit.kmutt.ac.th"})
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping("")
    public ResponseEntity<List<TaskResponseDTO>> getAllTask() {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findAllTask());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponseDTO> getTaskById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(id));
    }

    @PostMapping("")
    public ResponseEntity<TaskAddEditResponseDTO> addNewTask(@RequestBody TaskRequestDTO taskDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(taskDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskAddEditResponseDTO> updateTask(@PathVariable Integer id, @RequestBody TaskRequestDTO taskDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, taskDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> removeTask(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id));
    }

}
