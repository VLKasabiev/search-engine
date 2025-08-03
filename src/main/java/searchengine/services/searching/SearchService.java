package searchengine.services.searching;

import searchengine.dto.search.SearchResponse;
import java.io.IOException;

public interface SearchService {

    SearchResponse searching(String query, String site, int offset, int limit) throws IOException;

}
