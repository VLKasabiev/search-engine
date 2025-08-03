package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
//@Table(name = "lemmas")
//@Table(name = "lemmas", indexes = {
//        @Index(name = "idx_lemma_site", columnList = "lemma, site_id")
//}, uniqueConstraints = {
//        @UniqueConstraint(
//                name = "lemma_unique",
//                columnNames = {"lemma", "site_id"}
//        )
//})
@Table(name = "lemmas",
        indexes = {
                @Index(name = "idx_lemma_site", columnList = "lemma, site_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "lemma_unique",
                        columnNames = {"lemma", "site_id"}
                )
        }
)
@Getter
@Setter
public class LemmaEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(name = "lemma", nullable = false, columnDefinition = "TEXT")
    private String lemma;

    @Column(name = "frequency")
    private Long frequency;
}
