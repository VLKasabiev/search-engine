package searchengine.crawler;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.IndexStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
@Log4j2
public class LinksCrawler extends RecursiveAction {
    private String url;
    private SiteEntity site;
    private String regexUrl;
    private Integer statusCode;
    private String content;
    private String path;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private StringBuilder builder;

    private HashSet<String> uniqueUrl;
    private AtomicBoolean isClosed;
    private PageEntity pageEntity;

    public LinksCrawler(String url, String regexUrl,
                        SiteEntity site, PageRepository pageRepository,
                        LemmaRepository lemmaRepository, IndexRepository indexRepository,
                        StringBuilder builder, HashSet<String> uniqueUrl, AtomicBoolean isClosed,
                        SiteRepository siteRepository) {
        this.url = url;
        this.regexUrl = regexUrl;
        this.site = site;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.builder = builder;
        this.uniqueUrl = uniqueUrl;
        this.isClosed = isClosed;
        this.siteRepository = siteRepository;
    }

    @Override
    protected void compute() {
        if (isClosed.get() == true) {
            site.setIndexStatus(IndexStatus.FAILED);
            log.info("a closing operation occurred!!!");
            return;
        }
        try {
            path = url.replace(regexUrl, "");
            if (path.equals("")) {
                path = "/";
                uniqueUrl.add(regexUrl + path);
            }
            Thread.sleep(150);
            try {
                Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
                        referrer("http://www.google.com").execute();
                statusCode = response.statusCode();

                if (statusCode == 200L) {
                    Document document = response.parse();
                    content = document.outerHtml();

                    pageEntity = fillingPage();
                    builder.append("\n" + regexUrl + path);

                    LemmaService lemmaService = new LemmaService(site,
                            lemmaRepository, indexRepository, builder);

                    lemmaService.setPageEntity(pageEntity);
                    lemmaService.fillLemmaEntity();

                    Elements elements = document.select("a[href]");
                    List<LinksCrawler> taskList = new ArrayList<>();;

                    elementsCrawling(elements, taskList);

                    for (LinksCrawler map : taskList) {
                        map.join();
                    }
                }
            } catch (HttpStatusException e) {
                statusCode = e.getStatusCode();
                content = " ";
                pageEntity = fillingPage();
                builder.append("\n" + regexUrl + path);
            }
        } catch(Exception e) {
            fillSiteInCaseEx(e);
        }
    }

    private void elementsCrawling (Elements elements, List<LinksCrawler> taskList) throws IOException {
        for (Element elem : elements) {
            String link = elem.attr("abs:href");
            boolean checked = uniqueUrl.add(link);
            if (checkLink(checked, link)) {
            LinksCrawler task = new LinksCrawler(link, regexUrl,
                    site, pageRepository, lemmaRepository, indexRepository,
                    builder, uniqueUrl, isClosed, siteRepository);

            task.fork();
            taskList.add(task);
            }
        }
    }

    private boolean checkLink(boolean checked, String link) {
        return checked && !link.contains("#")
                && !link.contains("%")
                && link.startsWith(regexUrl)
                && !link.equals(regexUrl)
                && !link.equals(regexUrl + "/null")
                && !link.endsWith(".png") && !link.endsWith(".JPG") && !link.endsWith(".pdf") && !link.endsWith(".xlsx")
                    && !link.endsWith(".doc") && !link.endsWith(".jpg") && !link.endsWith(".yaml") && !link.endsWith(".xml");

    }

    private PageEntity fillingPage() {
        PageEntity pageEntity = new PageEntity();

        pageEntity.setCode(statusCode);
        pageEntity.setPath(path);
        pageEntity.setContent(content);
        pageEntity.setSiteId(site);
        pageRepository.save(pageEntity);

        builder.append(path + "\n");

        return pageEntity;
    }

    private void fillSiteInCaseEx (Exception e) {
        if (path.equals("/")) {
            site.setIndexStatus(IndexStatus.FAILED);
            site.setLastError(e.getMessage());
            log.error(e.getMessage());
            site.setDateTime(LocalDateTime.now());
        }
        else {
            site.setLastError(e.getMessage());
            log.error(e.getMessage());
        }
    }
}
