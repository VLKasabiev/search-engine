package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    @Modifying
    @Transactional
    void deleteByLemmaId(LemmaEntity lemmaEntity);

    @Modifying
    @Transactional
    void deleteByPageId(PageEntity pageEntity);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma = :lemmaEntity")
    List<PageEntity> findAllPagesByLemmaId(LemmaEntity lemmaEntity);

    IndexEntity findByPageAndLemma(PageEntity pageEntity, LemmaEntity lemmaEntity);
}
