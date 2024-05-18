package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.services.StatusService;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost")
public class StatusController {
    @Autowired
    private StatusService statusService;

    @GetMapping("/{boardId}/statuses")
    public ResponseEntity<List<StatusResponseDetailDTO>> getAllStatus(@PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findAllStatus(boardId));
    }

    @GetMapping("/{boardId}/statuses/{id}")
    public ResponseEntity<StatusResponseDTO> getTaskById(@PathVariable Integer id, @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.findStatusById(id, boardId));
    }

    @PostMapping("/{boardId}/statuses")
    public ResponseEntity<StatusResponseDTO> addNewStatus(@RequestBody @Valid StatusRequestDTO statusDTO,
                                                          @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.createNewStatus(statusDTO, boardId));
    }

    @PutMapping("/{boardId}/statuses/{id}")
    public ResponseEntity<StatusResponseDTO> updateStatus(@PathVariable Integer id,
                                                          @RequestBody @Valid StatusRequestDTO statusDTO,
                                                          @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatus(id, statusDTO, boardId));
    }

//    @PatchMapping("/{id}/maximum-task")
//    public ResponseEntity<StatusLimitResponseDTO> updateStatusLimit(@PathVariable Integer id, @RequestBody @Valid StatusRequestDTO limitDTO) {
//        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatusLimit(id, limitDTO));
//    }


    @DeleteMapping("/{boardId}/statuses/{id}")
    public ResponseEntity<StatusResponseDTO> removeStatus(@PathVariable Integer id, @PathVariable Integer boardId) {
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteStatus(id, boardId));
    }

    @DeleteMapping("/{boardId}/statuses/{id}/{newId}")
    public ResponseEntity<StatusResponseDTO> removeStatusAndTransferName(@PathVariable Integer id,
                                                                         @PathVariable Integer newId,
                                                                         @PathVariable Integer boardId) {
        if (id.equals(newId)) {
            throw new BadRequestException("You can not transfer the same status");
        }
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteTaskAndTransferStatus(id, newId, boardId));
    }

}
