package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.integratedproject.kanbanborad.dtos.request.*;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
import sit.int221.integratedproject.kanbanborad.enumeration.JwtErrorType;
import sit.int221.integratedproject.kanbanborad.exceptions.BadRequestException;
import sit.int221.integratedproject.kanbanborad.exceptions.BoardNameNobodyException;
import sit.int221.integratedproject.kanbanborad.exceptions.ForbiddenException;
import sit.int221.integratedproject.kanbanborad.exceptions.ItemNotFoundException;
import sit.int221.integratedproject.kanbanborad.repositories.kanbanboard.BoardRepository;
import sit.int221.integratedproject.kanbanborad.services.BoardService;
import sit.int221.integratedproject.kanbanborad.services.CollaboratorService;
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
    private final CollaboratorService collaboratorService;

    public BoardController(BoardService boardService, JwtTokenUtil jwtTokenUtil, BoardRepository boardRepository, CollaboratorService collaboratorService) {
        this.boardService = boardService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.boardRepository = boardRepository;
        this.collaboratorService = collaboratorService;
    }

    @GetMapping("")
    public ResponseEntity<BoardsResponseDTO> getAllBoard(@RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        return ResponseEntity.ok(boardService.getAllBoards(claims));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> getBoardById(@RequestHeader(value = "Authorization", required = false) String token,
                                                         @PathVariable String id) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            // Call the overloaded method without claims for public boards
            return ResponseEntity.ok(boardService.getBoardById(id));
        }

        // If the board is private, validate the token and retrieve claims
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        return ResponseEntity.ok(boardService.getBoardById(id, claims));  // Call the overloaded method with claims
    }

    @GetMapping("/{id}/collabs")
    public ResponseEntity<List<CollaboratorResponseDTO>> getCollaborators(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id) {
        Board board = getBoardOrThrow(id); // Fetch board and throw if not found

        // If the board is public, no need for token validation
        if (isPublicBoard(board)) {
            return ResponseEntity.ok(boardService.getCollaborators(id));
        }

        // If the board is private, validate token and access
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        return ResponseEntity.ok(boardService.getCollaborators(id, claims));
    }

    @GetMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponseDTO> getCollaboratorById(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id,
            @PathVariable String collabOid) {
        Board board = getBoardOrThrow(id); // Fetch board and throw if not found

        // If the board is public, no need for token validation
        if (isPublicBoard(board)) {
            return ResponseEntity.ok(boardService.getCollaboratorById(id, collabOid));
        }

        // If the board is private, validate token and access
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        return ResponseEntity.ok(boardService.getCollaboratorById(id, collabOid, claims));
    }

    @PostMapping("/{id}/collabs")
    public ResponseEntity<CollabAddEditResponseDTO> addNewCollaborator(
            @PathVariable String id,
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) @Valid CollaboratorRequestDTO collaboratorRequestDTO) {
        CollabAddEditResponseDTO responseDTO = collaboratorService.addNewCollaborator(id, token, collaboratorRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @PatchMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<BoardAccessRightResponseDTO> updateBoardAccessRight(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestBody(required = false) @Valid BoardAccessRightRequestDTO boardAccessRightRequestDTO) {
        BoardAccessRightResponseDTO responseDTO = collaboratorService.updateBoardAccessRight(id, collabOid,token, boardAccessRightRequestDTO);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @DeleteMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponseDTO> deleteCollaborator(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String id,
            @PathVariable String collabOid) {
        CollaboratorResponseDTO responseDTO = collaboratorService.deleteBoardCollaborator(id, collabOid ,token);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
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
    public ResponseEntity<BoardVisibilityResponseDTO> updateBoardVisibility(@PathVariable String id, @RequestBody(required = false) @Valid BoardVisibilityRequestDTO boardDTO,
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

    private void validateOwnership(Claims claims, String boardId) {
        String oid = (String) claims.get("oid");
        if (!isOwner(oid, boardId)) {
            throw new ForbiddenException("You are not allowed to access this board.");
        }
    }

    private Board getBoardOrThrow(String boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board Id " + boardId + " DOES NOT EXIST !!!"));
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
