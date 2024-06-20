package searchengine.dto.statistics;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.PageEntity;

//@Data
@Getter
@Setter
public class SearchData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;

    public SearchData(PageEntity pageEntity, String title, String snippet, double relevance) {
//        this.site = site;
//        this.siteName = siteName;
//        this.uri = uri;
        site = pageEntity.getSiteId().getUrl();
        siteName = pageEntity.getSiteId().getName();
        uri = pageEntity.getPath();
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }
}
