package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.StatusRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.services.StatusService;

import java.util.List;

@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = "http://localhost")
public class StatusController {
    private final StatusService statusService;
    private final BoardRepository boardRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public StatusController(StatusService statusService, BoardRepository boardRepository, JwtTokenUtil jwtTokenUtil) {
        this.statusService = statusService;
        this.boardRepository = boardRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }

    @GetMapping("/{id}/statuses")
    public ResponseEntity<List<StatusResponseDetailDTO>> getAllStatus(@PathVariable String id,
                                                                      @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(statusService.findAllStatus(null, id));
        }

        Claims claims = validateTokenAndOwnership(token, id);
        return ResponseEntity.ok(statusService.findAllStatus(claims, id));
    }

    @GetMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> getStatusById(@PathVariable String id, @PathVariable Integer statusId,
                                                           @RequestHeader(value = "Authorization", required = false) String token) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(statusService.findStatusById(null, id, statusId));
        }

        Claims claims = validateTokenAndOwnership(token, id);
        return ResponseEntity.ok(statusService.findStatusById(claims, id, statusId));
    }

    @PostMapping("/{id}/statuses")
    public ResponseEntity<StatusResponseDetailDTO> addNewStatus(@RequestBody @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable String id,
                                                                @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

        // Proceed to add the new status
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.createNewStatus(statusDTO, id));
    }

    @PutMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDetailDTO> updateStatus(@PathVariable String id,
                                                                @RequestBody @Valid StatusRequestDTO statusDTO,
                                                                @PathVariable Integer statusId,
                                                                @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);

        return ResponseEntity.status(HttpStatus.OK).body(statusService.updateStatus(id, statusDTO, statusId));
    }

    @DeleteMapping("/{id}/statuses/{statusId}")
    public ResponseEntity<StatusResponseDTO> removeStatus(@PathVariable String id,
                                                          @PathVariable Integer statusId,
                                                          @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteStatus(id, statusId));
    }

    @DeleteMapping("/{id}/statuses/{oldId}/{newId}")
    public ResponseEntity<StatusResponseDTO> removeStatusAndTransferName(@PathVariable String id,
                                                                         @PathVariable Integer oldId,
                                                                         @PathVariable Integer newId,
                                                                         @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);
        if (oldId.equals(newId)) {
            throw new BadRequestException("destination status for task transfer must be different from current status.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(statusService.deleteTaskAndTransferStatus(id, oldId, newId));
    }


    private Board validateBoardAndOwnership(String boardId, String token) {
        Board board = getBoardOrThrow(boardId);
        Claims claims = validateTokenAndOwnership(token, boardId);
        return board;
    }

    private Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private Claims validateTokenAndOwnership(String token, String boardId) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new ForbiddenException("Authentication required to access this board.");
        }

        String jwtToken = token.substring(7);
        Claims claims;
        try {
            claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to get JWT Token");
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT Token has expired");
        }

        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to modify this board.");
        }

        return claims;
    }

    private boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }
}
