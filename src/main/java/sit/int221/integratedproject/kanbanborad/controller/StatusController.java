package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.services.StatusService;

import java.util.List;

@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = {"http://ip23kw1.sit.kmutt.ac.th", "http://intproj23.sit.kmutt.ac.th"})
public class StatusController {
    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/{id}/statuses")
    public ResponseEntity<List<StatusResponseDetailDTO>> getAllStatus(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findAllStatus(id));
    }

    @GetMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> getStatusById(@PathVariable String id, @PathVariable Integer statusId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findStatusById(id, statusId));
    }

    @PostMapping("/{id}/statuses")
    public ResponseEntity<StatusResponseDetailDTO> addNewStatus(@RequestBody @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable String id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.createNewStatus(statusDTO, id));
    }

    @PutMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDetailDTO> updateStatus(@PathVariable String id,
                                                                @RequestBody @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable Integer statusId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatus(id, statusDTO, statusId));
    }

    @DeleteMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> removeStatus(@PathVariable String id, @PathVariable Integer statusId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteStatus(id, statusId));
    }

    @DeleteMapping("/{id}/statuses/{oldId}/{newId}")
    public ResponseEntity<StatusResponseDTO> removeStatusAndTransferName(@PathVariable String id,
                                                                         @PathVariable Integer oldId,
                                                                         @PathVariable Integer newId) {
        System.out.println("Received oldId: " + oldId + ", newId: " + newId);

        if (oldId.equals(newId)) {
            throw new BadRequestException("destination status for task transfer must be different from current status.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteTaskAndTransferStatus(id, oldId, newId));
    }

}