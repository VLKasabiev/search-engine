package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository <SiteEntity, Long> {
    @Modifying
    @Transactional
    void deleteByUrl(String url);
    Optional<SiteEntity> findByUrl(String url);
}
