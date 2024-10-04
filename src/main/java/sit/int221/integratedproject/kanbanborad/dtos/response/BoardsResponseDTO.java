package sit.int221.integratedproject.kanbanborad.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardsResponseDTO {
    private List<BoardResponseDTO> personalBoards;
    private List<BoardResponseDTO> collabBoards;
}
