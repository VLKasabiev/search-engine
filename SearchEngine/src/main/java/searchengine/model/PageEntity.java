package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
//@Table(name = "page")
@Getter
@Setter
@Table(
        name = "page",
        indexes = @Index(name = "page_path_index", columnList = "path") // Только path
)
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

//    @NotNull
//    @Column(name = "path", columnDefinition = "TEXT, INDEX page_path_index USING BTREE (path(50))")
//    private String path;

    @NotNull
    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    // Добавить отдельно индекс через @Table:
    //@Table(indexes = @Index(name = "page_path_index", columnList = "path"))

    @Column(name = "code", nullable = false)
    private Integer code;

//    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
//    private String content;

    // Стало:
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

}
