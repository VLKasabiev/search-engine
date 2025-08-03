package searchengine.crawler;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.transaction.support.TransactionTemplate;
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

    private final TransactionTemplate transactionTemplate;

    private HashSet<String> uniqueUrl;
    private AtomicBoolean isClosed;
    private PageEntity pageEntity;

    public LinksCrawler(String url, String regexUrl,
                        SiteEntity site, PageRepository pageRepository,
                        LemmaRepository lemmaRepository, IndexRepository indexRepository,
                        StringBuilder builder, HashSet<String> uniqueUrl, AtomicBoolean isClosed,
                        SiteRepository siteRepository, TransactionTemplate transactionTemplate) {
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
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected void compute() {
        try {
            log.debug("Processing URL: " + url);

            if (isClosed.get()) {
                log.warn("Indexing was stopped by user for URL: " + url);
                site.setIndexStatus(IndexStatus.FAILED);
                return;
            }

            path = url.replace(regexUrl, "");
            if (path.isEmpty()) {
                path = "/";
                uniqueUrl.add(regexUrl + path);
            }

            Thread.sleep(150); // Задержка для избежания блокировки

            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .referrer("https://www.google.com")
                    .execute();

            log.debug("Response status for " + url + ": " + response.statusCode());

            if (response.statusCode() == 200) {
                statusCode = response.statusCode();
                String contentType = response.contentType();
                log.debug("Content-Type for " + url + ": " + contentType);

                if (contentType == null || !isContentTypeSupported(contentType)) {
                    log.warn("Unsupported Content-Type for URL: " + url + " (" + contentType + ")");
                    content = " ";
                    pageEntity = fillingPage();
                    builder.append("\n" + regexUrl + path);
                    return;
                }

                Document document = response.parse();
                content = document.outerHtml();
                pageEntity = fillingPage();
                builder.append("\n" + regexUrl + path);

                // Логирование перед обработкой лемм
                log.debug("Filling lemmas for URL: " + url);
                LemmaService lemmaService = new LemmaService(site, lemmaRepository, indexRepository, builder);
                lemmaService.setPageEntity(pageEntity);
                lemmaService.fillLemmaEntity();

                // Логирование перед парсингом ссылок
                log.debug("Extracting links from URL: " + url);
                Elements links = document.select("a[href]");
                List<LinksCrawler> tasks = new ArrayList<>();
                elementsCrawling(links, tasks);

                for (LinksCrawler task : tasks) {
                    task.join();
                }
            }
        } catch (HttpStatusException e) {
            log.error("HTTP Error for URL: " + url + " (Status: " + e.getStatusCode() + ")");
            statusCode = e.getStatusCode();
            content = " ";
            pageEntity = fillingPage();
            builder.append("\n" + regexUrl + path);
        } catch (UnsupportedMimeTypeException e) {
            log.error("Unsupported MIME type for URL: " + url + " - " + e.getMessage());
            statusCode = 415;
            content = " ";
            pageEntity = fillingPage();
            builder.append("\n" + regexUrl + path);
        } catch (IOException e) {
            log.error("IO Error while processing URL: " + url, e);
            fillSiteInCaseEx(e);
        } catch (InterruptedException e) {
            log.error("Thread interrupted for URL: " + url, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected error while processing URL: " + url, e);
            fillSiteInCaseEx(e);
        }
    }

    private boolean isContentTypeSupported(String contentType) {
        return contentType.startsWith("text/")
                || contentType.equals("application/xml")
                || contentType.contains("+xml");
    }

//    @Override
//    protected void compute() {
//        if (isClosed.get() == true) {
//            site.setIndexStatus(IndexStatus.FAILED);
//            log.info("a closing operation occurred!!!");
//            return;
//        }
//        try {
//            path = url.replace(regexUrl, "");
//            if (path.equals("")) {
//                path = "/";
//                uniqueUrl.add(regexUrl + path);
//            }
//
//            Thread.sleep(150);
//
//            try {
//                Connection.Response response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
//                        referrer("http://www.google.com").execute();
//                statusCode = response.statusCode();
//
//                if (statusCode == 200L) {
//                    String contentType = response.contentType();
//                    if (contentType == null ||
//                            (!contentType.startsWith("text/") &&
//                                    !contentType.contains("application/xml") &&
//                                    !contentType.contains("application/+xml"))) {
//                        log.warn("Unhandled content type for URL: " + url + " (Content-Type: " + contentType + ")");
//                        content = " ";  // Сохраняем пустой контент
//                        pageEntity = fillingPage();
//                        builder.append("\n" + regexUrl + path);
//                        return;
//                    }
//                    try {
//                        Document document = response.parse();
//                        content = document.outerHtml();
//
//                        pageEntity = fillingPage();
//                        builder.append("\n" + regexUrl + path);
//
//                        LemmaService lemmaService = new LemmaService(site,
//                                lemmaRepository, indexRepository, builder);
//
//                        lemmaService.setPageEntity(pageEntity);
//                        lemmaService.fillLemmaEntity();
//
//                        Elements elements = document.select("a[href]");
//                        List<LinksCrawler> taskList = new ArrayList<>();;
//
//                        elementsCrawling(elements, taskList);
//
//                        for (LinksCrawler map : taskList) {
//                            map.join();
//                        }
//                    } catch (UnsupportedMimeTypeException e) {
//                        log.warn("Unsupported MIME type for URL: " + url + " - " + e.getMessage());
//                        content = " ";
//                        pageEntity = fillingPage();
//                        builder.append("\n" + regexUrl + path);
//                    }
//
//                }
//            } catch (HttpStatusException e) {
//                statusCode = e.getStatusCode();
//                content = " ";
//                pageEntity = fillingPage();
//                builder.append("\n" + regexUrl + path);
//            }
//        } catch(Exception e) {
//            fillSiteInCaseEx(e);
//        }
//    }

    private void elementsCrawling (Elements elements, List<LinksCrawler> taskList) throws IOException {
        for (Element elem : elements) {
            String link = elem.attr("abs:href");
            boolean checked = uniqueUrl.add(link);
            if (checkLink(checked, link)) {
            LinksCrawler task = new LinksCrawler(link, regexUrl,
                    site, pageRepository, lemmaRepository, indexRepository,
                    builder, uniqueUrl, isClosed, siteRepository, transactionTemplate);

            task.fork();
            taskList.add(task);
            }
        }
    }

//    private boolean checkLink(boolean checked, String link) {
//        return checked && !link.contains("#")
//                && !link.contains("%")
//                && link.startsWith(regexUrl)
//                && !link.equals(regexUrl)
//                && !link.equals(regexUrl + "/null")
//                && !link.endsWith(".png") && !link.endsWith(".JPG") && !link.endsWith(".pdf") && !link.endsWith(".xlsx")
//                && !link.endsWith(".doc") && !link.endsWith(".jpg") && !link.endsWith(".yaml") && !link.endsWith(".xml")
//                && !link.endsWith(".zip") && !link.endsWith(".rar") && !link.endsWith(".exe")
//                && !link.endsWith(".gif") && !link.endsWith(".svg") && !link.endsWith(".mp4");
//
//    }

    private boolean checkLink(boolean checked, String link) {
        return checked
                && !link.contains("#")
                && !link.contains("%")
                && link.startsWith(regexUrl)
                && !link.equals(regexUrl)
                && !link.equals(regexUrl + "/null")
                // Блокируем картинки, архивы, бинарные файлы
                && !link.matches("(?i).*\\.(png|jpg|jpeg|gif|webp|pdf|zip|rar|exe|docx?|xlsx?|mp4|svg)$");
    }

    private PageEntity fillingPage() {
        return transactionTemplate.execute(status -> {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setCode(statusCode != null ? statusCode : 200);
            pageEntity.setPath(path);
            pageEntity.setContent(content);
            pageEntity.setSite(site);
            return pageRepository.save(pageEntity);
        });
//        PageEntity pageEntity = new PageEntity();
//
//        pageEntity.setCode(statusCode);
//        pageEntity.setPath(path);
//        pageEntity.setContent(content);
//        pageEntity.setSite(site);
//        pageRepository.save(pageEntity);
//
//        builder.append(path + "\n");
//
//        return pageEntity;
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
