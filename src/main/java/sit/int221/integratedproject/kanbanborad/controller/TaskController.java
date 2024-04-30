package sit.int221.integratedproject.kanbanborad.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.entities.Task;
import sit.int221.integratedproject.kanbanborad.services.TaskService;

@RestController
@RequestMapping("/v1/tasks")
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping("")
    public ResponseEntity<Object> getAllTask() {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findAllTask());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getTaskById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.findTaskById(id));
    }

    @PostMapping("")
    public ResponseEntity<Object> addNewTask(@RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createNewTask(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateTask(@PathVariable Integer id, @RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.updateTask(id, task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> removeTask(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(taskService.deleteTask(id));
    }

}
