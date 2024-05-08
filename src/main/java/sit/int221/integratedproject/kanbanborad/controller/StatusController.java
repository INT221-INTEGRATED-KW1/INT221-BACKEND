package sit.int221.integratedproject.kanbanborad.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusTransferRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.TaskRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.services.StatusService;

import java.util.List;

@RestController
@RequestMapping("/v2/statuses")
@CrossOrigin(origins = "http://localhost")
public class StatusController {
    @Autowired
    private StatusService statusService;

    @GetMapping("")
    public ResponseEntity<List<StatusResponseDTO>> getAllStatus() {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findAllStatus());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusResponseDetailDTO> getTaskById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findStatusById(id));
    }

    @PostMapping("")
    public ResponseEntity<StatusResponseDTO> addNewTask(@RequestBody StatusRequestDTO statusDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.createNewStatus(statusDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatusResponseDTO> updateStatus(@PathVariable Integer id,
                                                               @RequestBody StatusRequestDTO statusDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatus(id, statusDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StatusResponseDTO> removeStatus(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteStatus(id));
    }

    @DeleteMapping("/{id}/{newId}")
    public ResponseEntity<StatusResponseDTO> removeStatusAndTransferName(@PathVariable Integer id,
                                                                         @PathVariable Integer newId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteTaskAndTransferStatus(id, newId));
    }

}
