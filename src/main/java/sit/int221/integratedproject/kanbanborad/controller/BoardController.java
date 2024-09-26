package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardLimitRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.request.BoardVisibilityRequestDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.BoardResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.BoardVisibilityResponseDTO;
import sit.int221.integratedproject.kanbanborad.dtos.response.StatusLimitResponseDTO;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.exceptions.BoardNameNobodyException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.services.BoardService;
import sit.int221.integratedproject.kanbanborad.services.JwtTokenUtil;
import sit.int221.integratedproject.kanbanborad.utils.Utils;

import java.util.List;

@RestController
@RequestMapping("/v3/boards")
@CrossOrigin(origins = {"http://ip23kw1.sit.kmutt.ac.th", "http://intproj23.sit.kmutt.ac.th",
        "https://ip23kw1.sit.kmutt.ac.th", "https://intproj23.sit.kmutt.ac.th"})
public class BoardController {
    private final BoardService boardService;
    private final JwtTokenUtil jwtTokenUtil;
    private final BoardRepository boardRepository;

    public BoardController(BoardService boardService, JwtTokenUtil jwtTokenUtil, BoardRepository boardRepository) {
        this.boardService = boardService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.boardRepository = boardRepository;
    }

    @GetMapping("")
    public ResponseEntity<List<Board>> getAllBoard(@RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        return ResponseEntity.ok(boardService.getAllBoard(claims));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> getBoardById(@RequestHeader(value = "Authorization", required = false) String token,
                                                         @PathVariable String id) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(boardService.getBoardById(id));
        }

        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        validateOwnership(claims, id);

        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    @PostMapping("")
    public ResponseEntity<BoardResponseDTO> createBoard(@RequestHeader("Authorization") String token,
                                                        @RequestBody(required = false) @Valid BoardRequestDTO boardRequestDTO) {
        if (boardRequestDTO == null) {
            throw new BoardNameNobodyException("Require name to create board");
        }

        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoard(claims, boardRequestDTO));
    }

    @PatchMapping("/{id}/maximum-status")
    public ResponseEntity<StatusLimitResponseDTO> updateBoardLimit(@PathVariable String id, @RequestBody @Valid BoardLimitRequestDTO boardDTO,
                                                                   @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);
        return ResponseEntity.status(HttpStatus.OK).body(boardService.updateBoardLimit(id, boardDTO));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BoardVisibilityResponseDTO> updateBoardVisibility(@PathVariable String id, @RequestBody @Valid BoardVisibilityRequestDTO boardDTO,
                                                                            @RequestHeader(value = "Authorization") String token) {
        Board board = validateBoardAndOwnership(id, token);
        return ResponseEntity.status(HttpStatus.OK).body(boardService.updateBoardVisibility(id, boardDTO));
    }

    private Board validateBoardAndOwnership(String boardId, String token) {
        Board board = getBoardOrThrow(boardId);
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        validateOwnership(claims, boardId);
        return board;
    }

    private Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
    }

    private void validateOwnership(Claims claims, String boardId) {
        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }
    }

    private boolean isPublicBoard(Board board) {
        return board.getVisibility().equalsIgnoreCase("PUBLIC");
    }

    private boolean isOwner(String oid, String boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
        return board.getOid().equals(oid);
    }
}
