package sit.int221.integratedproject.kanbanborad.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.services.BoardService;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost")
public class BoardController {
    @Autowired
    private BoardService boardService;

    @GetMapping("")
    public ResponseEntity<List<StatusLimitResponseDTO>> getAllBoard() {
        return ResponseEntity.status(HttpStatus.OK).body(boardService.findAllBoard());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusLimitResponseDTO> getBoardById(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(boardService.findBoardById(id));
    }

    @PatchMapping("/{id}/maximum-status")
    public ResponseEntity<StatusLimitResponseDTO> updateBoardLimit(@PathVariable Integer id, @RequestBody @Valid BoardRequestDTO boardDTO) {
        return ResponseEntity.status(HttpStatus.OK).body(boardService.updateBoardLimit(id, boardDTO));
    }

}
