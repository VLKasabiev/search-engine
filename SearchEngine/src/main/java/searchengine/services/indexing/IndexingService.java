package searchengine.services.indexing;

import searchengine.config.Site;
import searchengine.dto.index.IndexingResponse;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface IndexingService {
    IndexingResponse startInMiltithread() throws ExecutionException, InterruptedException;
    void startIndexing() throws InterruptedException;
    void dataBaseClearing(String url);
    IndexingResponse stopIndexing();

    IndexingResponse indexingOnePage(String url) throws IOException;

}
