package searchengine.services;

import searchengine.dto.statistics.SearchResponse;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.util.List;

public interface SearchService {

    SearchResponse searching(String query, String site, int offset, int limit) throws IOException;

}
