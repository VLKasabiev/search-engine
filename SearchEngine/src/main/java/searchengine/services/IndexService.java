package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repositories.IndexRepository;
public class IndexService {
    @Autowired
    private final IndexRepository indexRepository;
    private StringBuilder builder;
    public IndexService(IndexRepository indexRepository, StringBuilder builder) {
        this.indexRepository = indexRepository;
        this.builder = builder;
    }

    public void fillIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, Float rank) {
//        if (pageEntity.getId().equals(2)) {
//            builder.append("\n Lемма - " +  lemmaEntity.getLemma() + " - " + " LemmaId - " + lemmaEntity.getId() +
//                    " Количество - " + rank + " PageId - " + pageEntity.getId());
//                + " Частота = " + rank + " - " + pageEntity.getId() + "\n");
//        }
        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPageId(pageEntity);
        indexEntity.setLemmaId(lemmaEntity);
        indexEntity.setRank(rank);
//        builder.append(lemmaEntity.getLemma() + " - " + " LemmaId - " + lemmaEntity.getId()
//                + " Частота = " + rank + " - " + pageEntity.getId() + "\n");
        indexRepository.save(indexEntity);
    }
}
