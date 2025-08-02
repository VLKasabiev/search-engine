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
            //Optional<LemmaEntity> lemmaEntity;
//            try {
////                synchronized (lemmaRepository) {
//                    lemmaEntity = lemmaRepository.findByLemmaAndSite(lemma.getKey(), siteEntity);
//                    if (lemmaEntity.isPresent()) {
//                        LemmaEntity l = lemmaEntity.get();
//                        lemmaRepository.incrementFrequency(l.getId());
//                        //setFrequencyIfExist(l);
//                        //lemmaRepository.save(l);
//                        indexService.fillIndexEntity(pageEntity, l, lemma.getValue().floatValue());
//                    } else {
//                        LemmaEntity lemmaEntity1 = new LemmaEntity();
//                        lemmaEntity1.setLemma(lemma.getKey());
//                        lemmaEntity1.setSite(siteEntity);
//                        lemmaEntity1.setFrequency(1L);
//                        lemmaRepository.save(lemmaEntity1);
//                        indexService.fillIndexEntity(pageEntity, lemmaEntity1, lemma.getValue().floatValue());
//                    }
////                }
//
//            } catch (Exception e) {
//                siteEntity.setLastError(e.getMessage());
//                builder.append(" Здесь выскочила ошибка: " +
//                        " Лемма - " + lemma.getKey() + "\n");
//            }
            try {
                // 1. Атомарный upsert (без возвращаемого значения)
                lemmaRepository.upsertLemma(lemmaEntry.getKey(), siteEntity.getId());

                // 2. Получаем обновлённую сущность
                LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSite(
                        lemmaEntry.getKey(),
                        siteEntity
                ).orElseThrow(() -> new RuntimeException("Лемма не найдена после upsert"));

                // 3. Обновляем индекс
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

//    private LemmaEntity setFrequencyIfExist(LemmaEntity lemmaEntity) {
//        lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
//        return lemmaEntity;
//    }

    public void setPageEntity(PageEntity pageEntity) {
        this.pageEntity = pageEntity;
    }
}
