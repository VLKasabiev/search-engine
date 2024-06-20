package searchengine.services;

import searchengine.dto.statistics.IndexingResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface IndexingService {
    IndexingResponse startInMiltithread() throws ExecutionException, InterruptedException;
    void startIndexing() throws InterruptedException;
    void dataBaseClearing(String url);
    IndexingResponse stopIndexing();

    IndexingResponse indexingOnePage(String url) throws IOException;
}
