package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.StatusService;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.List;

@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = "http://localhost")
public class StatusController {
    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/{id}/statuses")
    public ResponseEntity<List<StatusResponseDetailDTO>> getAllStatus(@PathVariable String id,
                                                                      @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = statusService.getBoardOrThrow(id);

        if (statusService.isPublicBoard(board)) {
            return ResponseEntity.ok(statusService.findAllStatus(null, id));
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);
        Claims claims;

        if (Utils.isMicrosoftToken(jwtToken)) {
            claims = statusService.validateMicrosoftTokenAndAuthorization(jwtToken, id);
        } else {
            claims = statusService.validateReadTokenAndOwnership(token, id);
        }
        return ResponseEntity.ok(statusService.findAllStatus(claims, id));
    }

    @GetMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> getStatusById(@PathVariable String id, @PathVariable Integer statusId,
                                                           @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = statusService.getBoardOrThrow(id);

        if (statusService.isPublicBoard(board)) {
            return ResponseEntity.ok(statusService.findStatusById(null, id, statusId));
        }

        String jwtToken = JwtTokenUtil.getJwtFromAuthorizationHeader(token);

        Claims claims;

        if (Utils.isMicrosoftToken(jwtToken)) {
            claims = statusService.validateMicrosoftTokenAndAuthorization(jwtToken, id);
        } else {
            claims = statusService.validateReadTokenAndOwnership(token, id);
        }
        return ResponseEntity.ok(statusService.findStatusById(claims, id, statusId));
    }

    @PostMapping("/{id}/statuses")
    public ResponseEntity<StatusResponseDetailDTO> addNewStatus(@RequestBody(required = false) @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable String id,
                                                                @RequestHeader(value = "Authorization") String token) {
        Board board = statusService.validateBoardAndOwnership(id, token);

        if (statusDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.createNewStatus(statusDTO, id));
    }

    @PutMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDetailDTO> updateStatus(@PathVariable String id,
                                                                @RequestBody(required = false) @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable Integer statusId,
                                                                @RequestHeader(value = "Authorization") String token) {
        Board board = statusService.validateBoardAndOwnership(id, token);

        if (statusDTO == null) {
            throw new BadRequestException("Missing required fields.");
        }

        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatus(id, statusDTO, statusId));
    }

    @DeleteMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> removeStatus(@PathVariable String id,
                                                          @PathVariable Integer statusId,
                                                          @RequestHeader(value = "Authorization") String token) {
        Board board = statusService.validateBoardAndOwnership(id, token);
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteStatus(id, statusId));
    }

    @DeleteMapping("/{id}/statuses/{oldId}/{newId}")
    public ResponseEntity<StatusResponseDTO> removeStatusAndTransferName(@PathVariable String id,
                                                                         @PathVariable Integer oldId,
                                                                         @PathVariable Integer newId,
                                                                         @RequestHeader(value = "Authorization") String token) {
        Board board = statusService.validateBoardAndOwnership(id, token);
        if (oldId.equals(newId)) {
            throw new BadRequestException("destination status for task transfer must be different from current status.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteTaskAndTransferStatus(id, oldId, newId));
    }

}
