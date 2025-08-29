package searchengine.services;

import searchengine.lemma.processing.LemmaFinder;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
public class LemmaService {
    private IndexService indexService;
    private SiteEntity siteEntity;
    private PageEntity pageEntity;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    StringBuilder builder;

    public LemmaService(SiteEntity siteEntity,
                        LemmaRepository lemmaRepository, IndexRepository indexRepository,
                        StringBuilder builder) {
        this.siteEntity = siteEntity;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.builder = builder;
    }

    public void fillLemmaEntity() throws IOException {
        indexService = new IndexService(indexRepository, builder);
        for (Map.Entry<String, Integer> lemmaEntry : fillMap(pageEntity).entrySet()) {
            try {
                lemmaRepository.upsertLemma(lemmaEntry.getKey(), siteEntity.getId());
                LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSite(
                        lemmaEntry.getKey(),
                        siteEntity
                ).orElseThrow(() -> new RuntimeException("Лемма не найдена после upsert"));
                indexService.fillIndexEntity(
                        pageEntity,
                        lemmaEntity,
                        lemmaEntry.getValue().floatValue()
                );

            } catch (Exception e) {
                siteEntity.setLastError(e.getMessage());
                builder.append("Ошибка для леммы '")
                        .append(lemmaEntry.getKey())
                        .append("': ")
                        .append(e.getMessage())
                        .append("\n");
            }
        }
    }

    private Map<String, Integer> fillMap (PageEntity pageEntity) throws IOException {
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        String text = lemmaFinder.removeHTMLTags(pageEntity.getContent());
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(text);
        return lemmas;
    }

    public void setPageEntity(PageEntity pageEntity) {
        this.pageEntity = pageEntity;
    }
}
