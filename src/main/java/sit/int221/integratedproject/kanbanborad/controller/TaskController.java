package sit.int221.integratedproject.kanbanborad.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.services.TaskService;

@RestController
@RequestMapping("/v1/tasks")
@CrossOrigin(origins = "http://localhost:3000")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @GetMapping("")
    public ResponseEntity<Object> getAllTask() {
        return ResponseEntity.ok(taskService.findAllTask());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getTaskById(@PathVariable Integer id) {
        return ResponseEntity.ok(taskService.findTaskById(id));
    }


}
