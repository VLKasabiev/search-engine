package searchengine.services.indexing;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.crawler.LinksCrawler;
import searchengine.dto.index.IndexingResponse;
import searchengine.model.IndexStatus;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Log4j2
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final TransactionTemplate transactionTemplate;

    private final SitesList sites;
    private String url;
    private AtomicBoolean isClosed = new AtomicBoolean();
    private StringBuilder builder;
    private ExecutorService executorService;

    @Override
    public IndexingResponse startAsync(){
        IndexingResponse response = new IndexingResponse();
            response.setResult(true);
            CompletableFuture<Void> future = CompletableFuture
                    .runAsync(() -> {
                        startIndexing();
                    });
        return response;
    }

    @Override
    public void startIndexing(){
        long start = System.currentTimeMillis();
        isClosed.set(false);
        List<Site> siteList = sites.getSites();
        executorService = Executors.newFixedThreadPool(siteList.size());
        for (int i = 0; i < siteList.size(); i ++) {
            HashSet<String> uniqueUrl = new HashSet<>();
            StringBuilder builder1 = new StringBuilder();
            Site site = siteList.get(i);
            dataBaseClearing(site.getUrl());
            url = site.getUrl();
            SiteEntity siteEntity = fillingSiteEntity(site);
            siteRepository.save(siteEntity);
            parallelization(siteEntity, builder1, uniqueUrl);
        }
        executorService.shutdown();
        log.info("Execution time for method start indexing is: " + start);
    }

    @Override
    public IndexingResponse indexingOnePage(String siteUrl) throws IOException {
        IndexingResponse response = new IndexingResponse();
        response.setResult(false);
        response.setError("Данная страница находится за пределами сайтов, \n" +
                "указанных в конфигурационном файле\n");

        url = URLDecoder.decode(siteUrl.replace("url=", ""), "UTF-8");
        List<Site> siteList = sites.getSites();
        for (int i = 0; i < siteList.size(); i++) {
            Site site = siteList.get(i);
            if (url.startsWith(site.getUrl())) {
                response.setResult(true);
                response.setError(" ");
                Optional<SiteEntity> siteEntity = siteRepository.findByUrl(site.getUrl());
                checkSiteAndPage(siteEntity, response);
            }
        }
        return response;
    }

    private void checkSiteAndPage(Optional<SiteEntity> siteEntity, IndexingResponse response) throws IOException {
        if (siteEntity.isPresent()) {
            try {
                builder = new StringBuilder();
                Optional<PageEntity> pgEnt = pageRepository.findByPath(url.replace(siteEntity.get().getUrl(), ""));
                if (pgEnt.isPresent()) {
                    pageRepository.save(pgEnt.get());
                } else {
                    PageEntity pageEntity = fillingPageEntity(url, siteEntity);
                    fillLemmaService(siteEntity.get(), builder, pageEntity);
                }
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == 404) {
                    response.setError("This page does not exist!");
                    response.setResult(false);
                    log.error("This page does not exist!");
                }
            }
        }
    }

    private void fillLemmaService(SiteEntity siteEntity, StringBuilder builder, PageEntity pageEntity) throws IOException {
        LemmaService lemmaService = new LemmaService(siteEntity,
                lemmaRepository, indexRepository, builder);
        lemmaService.setPageEntity(pageEntity);
        lemmaService.fillLemmaEntity();
    }

    @Override
    public void dataBaseClearing(String url) {
        Optional<SiteEntity> site = siteRepository.findByUrl(url);
        if (site.isPresent()) {
            List<LemmaEntity> listLemmas = lemmaRepository.findAllBySiteId(site.get());
            listLemmas.forEach(l -> indexRepository.deleteByLemmaId(l));
            lemmaRepository.deleteAllBySiteId(site.get());
            pageRepository.deleteAllBySiteId(site.get());
            siteRepository.deleteByUrl(url);
        }
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response = new IndexingResponse();
        if (executorService.isTerminated()) {
            response.setResult(false);
            response.setError("Индексация не запущена!");
        } else {
            isClosed.set(true);
            response.setResult(true);
        }
        return response;
    }

    private SiteEntity fillingSiteEntity(Site site) {
        SiteEntity siteEntity = new SiteEntity();

        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setIndexStatus(IndexStatus.INDEXING);
        siteEntity.setDateTime(LocalDateTime.now());

        return siteEntity;
    }

    private void parallelization(SiteEntity siteEntity, StringBuilder builder1,
                                 HashSet<String> uniqueUrl) {
        executorService.submit(() -> {
            try {
                long start = System.currentTimeMillis();
                log.info("Starting indexing for site: " + siteEntity.getUrl());

                ForkJoinPool FJP = new ForkJoinPool();
                FJP.invoke(new LinksCrawler(
                        url, url, siteEntity,
                        pageRepository, lemmaRepository, indexRepository,
                        builder1, uniqueUrl, isClosed, siteRepository, transactionTemplate
                ));
                FJP.shutdown();

                log.info("Finished indexing for site: " + siteEntity.getUrl());
                changeSiteStatusIfOk(siteEntity);
                log.info("Execution time: " + (System.currentTimeMillis() - start) + " ms");
            } catch (Exception e) {
                log.error("Error during indexing for site: " + siteEntity.getUrl(), e);
                siteEntity.setIndexStatus(IndexStatus.FAILED);
                siteEntity.setLastError("Error during indexing: " + e.getMessage());
                siteRepository.save(siteEntity);
            }
        });


//        executorService.submit(() -> {
//            long start = System.currentTimeMillis();
//            ForkJoinPool FJP = new ForkJoinPool();
//            FJP.invoke(new LinksCrawler(url, url, siteEntity,
//                    pageRepository, lemmaRepository, indexRepository,
//                    builder1, uniqueUrl, isClosed, siteRepository));
//            FJP.shutdown();
//            changeSiteStatusIfOk(siteEntity);
//            log.info("Execution time - " + (System.currentTimeMillis() - start) + " ms." );
//        });
    }

    private void changeSiteStatusIfOk(SiteEntity siteEntity) {
        if (!siteEntity.getIndexStatus().equals(IndexStatus.FAILED)) {
            siteEntity.setIndexStatus(IndexStatus.INDEXED);
            siteEntity.setDateTime(LocalDateTime.now());
        }
        if (isClosed.get() == true) {
            siteEntity.setLastError("Индексация остановлена пользователем");
        }
        if (siteEntity.getLastError() == null) {
            siteEntity.setLastError(" ");
        }
        siteRepository.save(siteEntity);
    }

    private PageEntity fillingPageEntity(String url, Optional<SiteEntity> siteEntity) throws IOException {
        Document document = Jsoup.connect(url)
                .ignoreContentType(true).get();

        String path = url.replace(siteEntity.get().getUrl(), "");

        PageEntity pageEntity = new PageEntity();
        pageEntity.setCode(200);
        pageEntity.setPath(path);
        pageEntity.setContent(document.outerHtml());
        pageEntity.setSite(siteEntity.get());

        pageRepository.save(pageEntity);

        return pageEntity;
    }
}
