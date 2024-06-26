package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.LemmaProcessing.LemmaFinder;
import searchengine.LemmaProcessing.Snippet;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.SearchData;
import searchengine.dto.statistics.SearchResponse;
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
    private Snippet snippet = new Snippet();
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexRepository indexRepository;

    private final SitesList sites;
    private List<LemmaEntity> wordsFromQuery;

    private HashMap<PageEntity, Double> mapOfAbsRel;

    private double maxAbsRel;

    @Override
    public SearchResponse searching(String query, String site, int offset, int limit) throws IOException {
        long start = System.currentTimeMillis();
        List<String> listSites = sitesToSearch(site);
        wordsFromQuery = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setResult(true);
        List<SearchData> searchDataList = new ArrayList<>();

        for (String st : listSites) {
            Optional<SiteEntity> siteEntity = siteRepository.findByUrl(st);
            if (siteEntity.isPresent()) {
                createLemmaList(siteEntity.get(), query);
            }

            List<PageEntity> pageList = new ArrayList<>();
            for (int i = 0; i < wordsFromQuery.size(); i++) {
                if (i == 0) {
                    pageList = indexRepository.findAllPagesByLemmaId(wordsFromQuery.get(i));
                } else {
                    pageListFilter(wordsFromQuery.get(i), pageList);
                }
            }

            fillSearchDataList(pageList, searchDataList);

        }
        searchDataList = searchDataList
                .stream()
                .sorted(Comparator.comparing(SearchData::getRelevance).reversed())
                .collect(Collectors.toList());

        List<SearchData> newSearchDataList = new ArrayList<>();
        int countToShow = offset + limit;
        if (countToShow > searchDataList.size()) {
            countToShow = searchDataList.size();
        }
        for (int i = offset; i < countToShow; i++) {
            newSearchDataList.add(searchDataList.get(i));
        }
        searchResponse.setCount(searchDataList.size());
        searchResponse.setData(newSearchDataList);

        log.info(searchDataList.size() + " - Это count");
        log.info("Это offset - " + offset);
        log.info("Это limit - " +  limit);
        long executTime = System.currentTimeMillis() - start;
        log.info(executTime + " ms");
        return searchResponse;
    }

    private List<String> sitesToSearch(String site) {
        ArrayList<String> listSites = new ArrayList<>();
        List<Site> siteList;
        if (site == null) {
            siteList = sites.getSites();
            siteList.forEach(s -> {
                System.out.println("\n" + s.getName());
                listSites.add(s.getUrl());
            });
        } else {
            listSites.add(site);
        }
        return listSites;
    }

    private void createLemmaList(SiteEntity siteEntity, String query) throws IOException {
        List<LemmaEntity> lemmaList = new ArrayList<>();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();
        Set<String> lemmas = lemmaFinder.getLemmaSet(query);
        lemmas.forEach(l -> {
            Optional<LemmaEntity> lemmaEntity = lemmaRepository.findByLemma(l, siteEntity);
            if (lemmaEntity.isPresent()) {
                lemmaList.add(lemmaEntity.get());
            }
        });
        int pageCount = pageRepository.getPagesCount(siteEntity);
        wordsFromQuery = lemmaList.stream()
                .filter((l) -> (l.getFrequency() * 100 /  pageCount) < 80)
                .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                .collect(Collectors.toList());
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

    private void fillSearchDataList(List<PageEntity> pageList, List<SearchData> searchDataList) throws IOException{
        maxAbsRel = 0;
        findAbsRel(pageList);
        for (PageEntity pl : pageList) {
            try {
                SearchData searchData = fillSearchData(pl);
                searchDataList.add(searchData);
            }catch (HttpStatusException ex) {
                log.error("connection error!!!");
            }
        }
    }

    private void findAbsRel(List<PageEntity> pgs) {
        mapOfAbsRel = new HashMap<>();
        for (PageEntity p : pgs) {
            double absRel = 0;
            for (LemmaEntity lm : wordsFromQuery) {
                IndexEntity index = indexRepository.findByPageIdAndLemmaId(p, lm);
                absRel += index.getRank();
                if (absRel > maxAbsRel) {
                    maxAbsRel = absRel;
                }
            }
            mapOfAbsRel.put(p, absRel);
        }
    }

    private Document connectUrl(String pathh) throws IOException {
        Document document = Jsoup.connect(pathh)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").get();
        return document;
    }

    private SearchData fillSearchData(PageEntity pageEntity) throws IOException {
        String path = pageEntity.getSiteId().getUrl() + pageEntity.getPath();
        Document doc = connectUrl(path);
        double absRel = mapOfAbsRel.get(pageEntity);
        String snip = snippet.findAndExtractSentence(pageEntity.getContent(), wordsFromQuery.stream()
                .map(nl -> nl.getLemma()).collect(Collectors.toList()));

        double rel = absRel / maxAbsRel;
        return new SearchData(pageEntity, doc.title(), snip, rel);
    }
}
