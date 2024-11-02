package sit.int221.integratedproject.kanbanborad.entities.kanbanboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sit.int221.integratedproject.kanbanborad.enumeration.InvitationStatus;

import java.sql.Timestamp;

@Entity
@Table(name = "invitations")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    @Column(name = "inviter_oid", nullable = false)
    private String inviterOid;
    @Column(name = "invitee_oid", nullable = false)
    private String inviteeOid;
    @Column(name = "access_right")
    private String accessRight;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "createdOn", insertable = false, updatable = false)
    private Timestamp createdOn;
}
