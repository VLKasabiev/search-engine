package searchengine.lemma.processing;

import java.io.IOException;
import java.util.*;

public class Snippet {

    private List<String> wordsToSnippet;
    private static LemmaFinder lemmaFinder;

    static {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String findAndExtractSentence(String t, List<String> wordsToSearch) throws IOException {
        wordsToSnippet = new ArrayList<>();
        String text = lemmaFinder.removeHTMLTags(t);
        return findSnippet(text, wordsToSearch);
    }
    public String findSnippet(String text, List<String> wordsToSearch){
        List<Integer> wts = new ArrayList<>();
        for (int i = 0; i < wordsToSearch.size(); i++) {
            wts.add(i);
        }
        StringBuilder b = new StringBuilder();
        String[] sentences = text.split("[.!?:;]\\s*");
        for (String sentence : sentences) {

            StringBuilder builder = new StringBuilder();
            int c = 0;
            boolean wordIsPresent = false;
            TreeSet<Integer> wordsOnSentenceNum = new TreeSet<>();
            String[] words = lemmaFinder.textSplitToWords(sentence);

            for (String wordToSearch : wordsToSearch) {

                for (int i = 0; i < words.length; i++) {
                    wordIsPresent = lemmaFinder.checkWord(words, wordToSearch, i);

                    if (wordIsPresent) {
                        wordsOnSentenceNum.add(c);

                    }
                }
                c++;
            }
            if (wordsOnSentenceNum.size() == wordsToSearch.size()) {
                StringBuilder builder1 = new StringBuilder();

                for (String word : words) {
                    builder1.append(word + " ");
                }

                return builder1.toString();
            }
            if (wts.size() != 0 && wordsOnSentenceNum.size() != 0) {
                for (Integer w : wordsOnSentenceNum) {
                    boolean wasRemoved = wts.remove(w);
                    if (wasRemoved) {
                        for (String word : words) {
                            b.append(word + " ");
                        }
                        b.append("  .......  ");
                    }
                }
            }
        }
        return b.toString();
    }
}
