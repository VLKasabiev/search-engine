package searchengine;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import searchengine.LemmaProcessing.LemmaFinder;

import java.util.*;
@RequiredArgsConstructor
public class Test {
//    private static LuceneMorphology morphology;
    private static LemmaFinder lemmaFinder;
//    @Autowired
//    private final PageRepository pageRepository;
//    private static String text;

    public static void main(String[] args) throws Exception {
        lemmaFinder = LemmaFinder.getInstance();



        try {
            Connection.Response response = Jsoup.connect("https://nopaper.ru/terms-of-use").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
                    referrer("http://www.google.com").execute();
//            Connection.Response response = Jsoup.connect("https://nikoartgallery.com").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").
//                    referrer("http://www.google.com").execute();
            String t = response.parse().outerHtml();
//            System.out.println(Jsoup.parse(t).wholeText());
            Map<String, Integer> map = lemmaFinder.collectLemmas(t);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " - " + entry.getValue());
            }
//            Document document = Jsoup.parse(t);
//            Elements elements = document.getAllElements();
//            StringBuilder builder = new StringBuilder();
//            for (Element element : elements) {
//                if (!element.ownText().isBlank()) {
//                    builder.append(element.ownText() + "\n");
//                }
//            }

//            System.out.println(builder);
//            String text = "Электронная повестка в суд, электронным феном, а также в электронном виде";
//            Set<String> lemmaSet = lemmaFinder.getLemmaSet(builder.toString());
//            lemmaSet.forEach(s -> System.out.println(s));
//            Map<String, Integer> mapLemmas = lemmaFinder.collectLemmas(text);
//            for (Map.Entry<String, Integer> entry : mapLemmas.entrySet()) {
//                System.out.println(entry.getKey() + entry.getValue());
//            }

//            String text = lemmaFinder.removeHTMLTags(response.parse().outerHtml());
//            System.out.println(text);
//
//            String[] words = lemmaFinder.snippetFinder(text);
//            List<Integer> wordNum = lemmaFinder.checkWord(words, "Субъект");
//            StringBuilder builder = new StringBuilder();
//            for (int i = 0; i < words.length; i++) {
//                for (Integer wn : wordNum) {
//                    if (i == wn) {
//                        words[i] =  words[i].replace(words[i], "<b>" + words[i] + "<b>");
////                        System.out.println(words[i]);
//                    }
//                }
//            }
//            for (String word : words) {
//                builder.append(word + " ");
//            }
//            System.out.println(builder);
//


        } catch (HttpStatusException ex) {
        }
    }

//    private static Optional<PageEntity> searchPage(String path) {
//        return pageRepository.findByPath(path);
//    }
}
