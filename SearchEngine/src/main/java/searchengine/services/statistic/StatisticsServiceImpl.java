package searchengine.services.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Log4j2
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;

//    private final Random random = new Random();
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> siteList = sites.getSites();
        for (int i = 0; i < siteList.size(); i++) {
            Site site = siteList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            Optional<SiteEntity> siteE = siteRepository.findByUrl(site.getUrl());
            SiteEntity siteEntity;
            if (siteE.isPresent()) {
                siteEntity = siteE.get();
                item.setName(siteEntity.getName());
                item.setUrl(siteEntity.getUrl());
                item.setStatus(String.valueOf(siteEntity.getIndexStatus()));

                Instant instant = siteEntity.getDateTime().atZone(ZoneId.systemDefault()).toInstant();
                long seconds = instant.toEpochMilli();
                item.setStatusTime(seconds);

                item.setError(siteEntity.getLastError());
                item.setPages(pageRepository.getPagesCount(siteEntity));
                item.setLemmas(lemmaRepository.getLemmasBySiteId(siteEntity));

                total.setPages(pageRepository.getTotalPages());
                total.setLemmas(lemmaRepository.getTotalLemmas());

                detailed.add(item);
            }
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
