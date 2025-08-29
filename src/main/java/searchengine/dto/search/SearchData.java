package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;
import searchengine.model.PageEntity;


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
        site = pageEntity.getSite().getUrl();
        siteName = pageEntity.getSite().getName();
        uri = pageEntity.getPath();
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public SearchData() {
        site = null;
        siteName = " ";
        uri = null;
        title = " ";
        snippet = " ";
        relevance = 0.0;
    }
}
