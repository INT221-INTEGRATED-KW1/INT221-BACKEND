package sit.int221.integratedproject.kanbanborad.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sit.int221.integratedproject.kanbanborad.dtos.request.*;
import sit.int221.integratedproject.kanbanborad.dtos.response.*;
import sit.int221.integratedproject.kanbanborad.entities.kanbanboard.Board;
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
            return ResponseEntity.ok(boardService.getBoardById(id));
        }

        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        return ResponseEntity.ok(boardService.getBoardById(id, claims));
    }

    @GetMapping("/{id}/collabs")
    public ResponseEntity<List<CollaboratorResponseDTO>> getCollaborators(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(boardService.getCollaborators(id));
        }

        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        return ResponseEntity.ok(boardService.getCollaborators(id, claims));
    }

    @GetMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponseDTO> getCollaboratorById(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String id,
            @PathVariable String collabOid) {
        Board board = getBoardOrThrow(id);

        if (isPublicBoard(board)) {
            return ResponseEntity.ok(boardService.getCollaboratorById(id, collabOid));
        }

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

    @PatchMapping("/{id}/collabs/{collabOid}/status")
    public ResponseEntity<CollabAddEditResponseDTO> updateCollaboratorStatus(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestHeader("Authorization") String token,
            @RequestBody CollaboratorStatusUpdateDTO statusUpdateDTO) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        CollabAddEditResponseDTO responseDTO = collaboratorService.updateCollaboratorStatus(id, collabOid, statusUpdateDTO, claims);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping("/{boardId}/collabs/invitations")
    public ResponseEntity<List<CollaboratorResponseDTO>> getActiveInvitations(
            @PathVariable String boardId,
            @RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        String userId = (String) claims.get("oid");

        List<CollaboratorResponseDTO> activeInvitations = collaboratorService.getActiveInvitations(boardId, userId);
        return ResponseEntity.ok(activeInvitations);
    }

    @GetMapping("/collabs/invitations/pending")
    public ResponseEntity<List<CollaboratorResponseDTO>> getPendingInvitations(
            @RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        String userId = (String) claims.get("oid");

        List<CollaboratorResponseDTO> pendingInvitations = collaboratorService.getPendingInvitationsForCollaborator(userId);
        return ResponseEntity.ok(pendingInvitations);
    }

    @PatchMapping("/{id}/collabs/{collabOid}/accept")
    public ResponseEntity<CollabAddEditResponseDTO> acceptInvitation(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        CollabAddEditResponseDTO responseDTO = collaboratorService.acceptCollaboratorInvitation(id, collabOid, claims);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PatchMapping("/{id}/collabs/{collabOid}/decline")
    public ResponseEntity<CollabAddEditResponseDTO> declineInvitation(
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestHeader("Authorization") String token) {
        Claims claims = Utils.getClaims(token, jwtTokenUtil);

        CollabAddEditResponseDTO responseDTO = collaboratorService.declineCollaboratorInvitation(id, collabOid, claims);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @GetMapping("/{boardId}/collabs/invitation")
    public ResponseEntity<CollaboratorInvitationResponseDTO> getInvitationDetails(
            @PathVariable String boardId,
            @RequestHeader("Authorization") String token) {
        System.out.println("test");
        Claims claims = Utils.getClaims(token, jwtTokenUtil);
        String userId = (String) claims.get("oid");

        CollaboratorInvitationResponseDTO invitationDetails = collaboratorService.getInvitationDetails(boardId, userId);
        return ResponseEntity.ok(invitationDetails);
    }

    @PatchMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<BoardAccessRightResponseDTO> updateBoardAccessRight(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String id,
            @PathVariable String collabOid,
            @RequestBody(required = false) @Valid BoardAccessRightRequestDTO boardAccessRightRequestDTO) {
        BoardAccessRightResponseDTO responseDTO = collaboratorService.updateBoardAccessRight(id, collabOid, token, boardAccessRightRequestDTO);

        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @DeleteMapping("/{id}/collabs/{collabOid}")
    public ResponseEntity<CollaboratorResponseDTO> deleteCollaborator(
            @RequestHeader(value = "Authorization") String token,
            @PathVariable String id,
            @PathVariable String collabOid) {
        CollaboratorResponseDTO responseDTO = collaboratorService.deleteBoardCollaborator(id, collabOid, token);

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