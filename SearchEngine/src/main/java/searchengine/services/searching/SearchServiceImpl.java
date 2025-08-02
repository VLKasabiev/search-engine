package searchengine.services.searching;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.lemma.processing.LemmaFinder;
import searchengine.lemma.processing.Snippet;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class SearchServiceImpl implements SearchService{
    private Snippet snippet;
    private LemmaFinder lemmaFinder;

    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    private final SitesList sites;
    private List<LemmaEntity> wordsFromQuery;
    private HashMap<PageEntity, Double> mapOfAbsRel;
    private Map<PageEntity, Double> mapOfRel;
    private SearchResponse searchResponse;
    private List<SearchData> searchDataList;
    private double maxAbsRel = 0;
    private List<PageEntity> sortedPageList;
    private List<PageEntity> totalPageList;
    private boolean wordIsNotInDb;

    @Override
    public SearchResponse searching(String query, String site, int offset, int limit) throws IOException {
        long start = System.currentTimeMillis();
        if (offset != 0) {
            return searchDataListToShow(offset, limit, start);
        }
        List<String> listSites = sitesToSearch(site);

        fieldsInitialization();

        for (String st : listSites) {
            wordIsNotInDb = false;

            Optional<SiteEntity> siteEntity = siteRepository.findByUrl(st);
            if (siteEntity.isPresent()) {
                createLemmaList(siteEntity.get(), query);
            }

            if (wordIsNotInDb) {
                continue;
            }

            List<PageEntity> pageList = new ArrayList<>();
            for (int i = 0; i < wordsFromQuery.size(); i++) {
                if (i == 0) {
                    pageList = indexRepository.findAllPagesByLemmaId(wordsFromQuery.get(i));
                } else {
                    pageListFilter(wordsFromQuery.get(i), pageList);
                }
            }

            totalPageList.addAll(pageList);
            findAbsRel(pageList);
        }
        snippet = new Snippet(wordsFromQuery.stream()
                .map(nl -> nl.getLemma()).collect(Collectors.toList()));
        toSortByRelevance();
        return searchDataListToShow(offset, limit, start);
    }

    private void fieldsInitialization() throws IOException {
        lemmaFinder = LemmaFinder.getInstance();
        wordsFromQuery = new ArrayList<>();
        searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        totalPageList = new ArrayList<>();
        mapOfAbsRel = new HashMap<>();
    }

    private List<String> sitesToSearch(String site) {
        ArrayList<String> listSites = new ArrayList<>();
        List<Site> siteList;
        if (site == null) {
            siteList = sites.getSites();
            siteList.forEach(s -> {
                listSites.add(s.getUrl());
            });
        } else {
            listSites.add(site);
        }
        return listSites;
    }

    private void createLemmaList(SiteEntity siteEntity, String query) {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        Set<String> lemmas = lemmaFinder.getLemmaSet(query);
        lemmas.forEach(l -> {
            Optional<LemmaEntity> lemmaEntity = lemmaRepository.findByLemmaAndSite(l, siteEntity);
            if (lemmaEntity.isPresent()) {
                lemmaList.add(lemmaEntity.get());
            } else {
                wordIsNotInDb = true;
            }
        });
        if (!wordIsNotInDb) {
            int pageCount = pageRepository.getPagesCount(siteEntity);
            log.info("This ia pageCount - " + pageCount);
            wordsFromQuery = lemmaList.stream()
                    .filter((l) -> (l.getFrequency() * 100 / pageCount) < 101)
                    .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                    .collect(Collectors.toList());
            log.info("This is wordToSearch Size - " + lemmaList.size());
        }
    }

    private void pageListFilter (LemmaEntity lemmaEntity, List<PageEntity> pageList) {
        List<PageEntity> pageList1 = indexRepository.findAllPagesByLemmaId(lemmaEntity);
        List<PageEntity> newPageList = new ArrayList<>();
        for (PageEntity pagEnt : pageList) {
            boolean isPage = false;
            for (PageEntity pgEn : pageList1) {
                if (pagEnt.equals(pgEn)) {
                    isPage = true;
                }
            }
            if (isPage == false) {
                newPageList.add(pagEnt);
            }
        }
        pageList.removeAll(newPageList);
    }

    private void findAbsRel(List<PageEntity> pgs) {
        for (PageEntity p : pgs) {
            double absRel = 0;
            for (LemmaEntity lm : wordsFromQuery) {
                IndexEntity index = indexRepository.findByPageAndLemma(p, lm);
                absRel += index.getRank();
                if (absRel > maxAbsRel) {
                    maxAbsRel = absRel;
                }
            }
            mapOfAbsRel.put(p, absRel);
        }
    }

    private Document connectUrl(String path) throws IOException {
        Document document = Jsoup.connect(path)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").get();
        return document;
    }

    private SearchData fillSearchData(PageEntity pageEntity) throws IOException {
        String path = pageEntity.getSite().getUrl() + pageEntity.getPath();
        log.info("This is path - " + path);
        Document doc = connectUrl(path);
        String snip = snippet.findAndExtractSentence(pageEntity.getContent());
        double rel = mapOfRel.get(pageEntity);
        log.info("This is relevance - " + rel);
        return new SearchData(pageEntity, doc.title(), snip, rel);
    }

    private void toSortByRelevance() {
        mapOfRel = new HashMap<>();
        sortedPageList = new ArrayList<>();
        for (PageEntity pg : totalPageList) {
            double rel = mapOfAbsRel.get(pg) / maxAbsRel;
            mapOfRel.put(pg, rel);
        }

        mapOfRel.entrySet()
                .stream()
                .sorted(Map.Entry.<PageEntity, Double>comparingByValue().reversed())
                .forEach(s -> sortedPageList.add(s.getKey()));

    }

    private SearchResponse searchDataListToShow(int offset, int limit, long start) throws IOException {
        searchDataList = new ArrayList<>();
        int countToShow = offset + limit;
        if (countToShow > sortedPageList.size()) {
            countToShow = sortedPageList.size();
        }
        for (int i = offset; i < countToShow; i++) {
            try {
                SearchData searchData = fillSearchData(sortedPageList.get(i));
                searchDataList.add(searchData);
            }catch (HttpStatusException ex) {
                log.error("connection error!!! on page + " + sortedPageList.get(i).getPath());
                log.info("Status code - " + ex.getStatusCode());

                SearchData searchData = new SearchData();
                searchDataList.add(searchData);
            }
        }
        searchResponse.setCount(sortedPageList.size());
        searchResponse.setData(searchDataList);


        long executTime = System.currentTimeMillis() - start;
        log.info(executTime + " ms");
        return searchResponse;
    }
}
