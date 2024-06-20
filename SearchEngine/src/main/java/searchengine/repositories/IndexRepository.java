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

//    List<IndexEntity> findAllByLemmaId(LemmaEntity lemmaEntity);
    @Query("SELECT i.pageId FROM IndexEntity i WHERE i.lemmaId = :lemmaEntity")
    List<PageEntity> findAllPagesByLemmaId(LemmaEntity lemmaEntity);

    IndexEntity findByPageIdAndLemmaId(PageEntity pageEntity, LemmaEntity lemmaEntity);
}
