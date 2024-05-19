package sit.int221.integratedproject.kanbanborad.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.services.StatusLimitService;

import java.util.List;

@RestController
@RequestMapping("/v2/statusesLimit")
@CrossOrigin(origins = "http://localhost")
public class StatusLimitController {
    @Autowired
    private StatusLimitService statusLimitService;

    @GetMapping("")
    public ResponseEntity<List<StatusLimitResponseDTO>> getAllStatusLimit() {
        return ResponseEntity.status(HttpStatus.OK).body(statusLimitService.findAllStatusLimit());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusLimitResponseDTO> getStatusLimitById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(statusLimitService.findStatusLimitById(id));
    }

    @PatchMapping("/{id}/maximum-status")
    public ResponseEntity<StatusLimitResponseDTO> updateStatusLimit(@PathVariable Integer id, @RequestBody StatusLimitRequestDTO statusLimitRequestDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(statusLimitService.updateStatusLimit(id, statusLimitRequestDTO));
    }

}
