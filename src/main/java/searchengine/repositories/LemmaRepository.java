package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma = :lemma AND l.site = :site")
    Optional<LemmaEntity> findByLemmaAndSite(
            @Param("lemma") String lemma,
            @Param("site") SiteEntity site);


    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
        INSERT INTO lemmas (lemma, site_id, frequency)
        VALUES (:lemma, :siteId, 1)
        ON CONFLICT ON CONSTRAINT lemma_unique
        DO UPDATE SET frequency = lemmas.frequency + 1""")
    void upsertLemma(@Param("lemma") String lemma,
                     @Param("siteId") Long siteId);

    @Query("SELECT COUNT(*) FROM LemmaEntity l WHERE l.site = :siteEntity")
    int getLemmasBySiteId(SiteEntity siteEntity);
    @Query("SELECT COUNT(*) FROM LemmaEntity l")
    int getTotalLemmas();
    @Modifying
    @Transactional
    void deleteAllBySiteId(SiteEntity siteEntity);
    List<LemmaEntity> findAllBySiteId(SiteEntity siteEntity);

}
