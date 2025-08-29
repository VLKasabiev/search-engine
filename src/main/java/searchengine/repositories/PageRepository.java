package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository <PageEntity, Long> {
    @Modifying
    @Transactional
    void deleteAllBySiteId(SiteEntity siteEntity);

    @Query("SELECT COUNT(*) FROM PageEntity p Where p.site = :siteEntity")
    int getPagesCount(SiteEntity siteEntity);

    @Query("SELECT COUNT(*) FROM PageEntity p")
    int getTotalPages();

    Optional<PageEntity> findByPath(String path);

}
