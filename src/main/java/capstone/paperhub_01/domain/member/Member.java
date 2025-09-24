package capstone.paperhub_01.domain.member;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "VARCHAR(20)")
    private String name;

    @Column(columnDefinition = "VARCHAR(20)")
    private String nickname;

    @Column(columnDefinition = "VARCHAR(60)")
    private String password;

    @Column
    private String email;

    @Builder
    public Member(String name, String nickname, String password, String email){
        this.name = name;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
    }

}
