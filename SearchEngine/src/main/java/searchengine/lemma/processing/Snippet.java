package searchengine.lemma.processing;

import java.io.IOException;
import java.util.*;

public class Snippet {
    private static LemmaFinder lemmaFinder;
    private List<String> wordsToSearch;
    private TreeSet<Integer> wordsOnSentenceNum;
    private String[] words;
    private boolean wordIsPresent;
    private StringBuilder builder;

    public Snippet(List<String> wordsToSearch) throws IOException {
        this.wordsToSearch = wordsToSearch;
        lemmaFinder = LemmaFinder.getInstance();
    }

    public String findAndExtractSentence(String t) throws IOException {
        String text = lemmaFinder.removeHTMLTags(t);
        return findSnippet(text);

    }
    public String findSnippet(String text){
        List<Integer> wts = new ArrayList<>();
        for (int i = 0; i < wordsToSearch.size(); i++) {
            wts.add(i);
        }
        boolean snippetIsTooSmall = false;
        builder = new StringBuilder();
        String[] sentences = text.split("[.!?:;]\\s*");
        for (String sentence : sentences) {

            checkSentence(sentence);

            if (snippetIsTooSmall && builder.length() < 250) {
                sentenceToString();
            } else  if (snippetIsTooSmall && builder.length() > 250){
                return builder.toString();
            }

            if (wordsOnSentenceNum.size() == wordsToSearch.size()) {
                String snippet = wordsToSentence();
                if (snippet.length() < 250) {
                    snippetIsTooSmall = true;
                    builder.append(snippet + ". ! ");
                    continue;
               }
                return snippet;
            }

            if (wts.size() != 0 && wordsOnSentenceNum.size() != 0) {
                wordsToSentences(wts);
            }
        }
        return builder.toString();
    }

    private void checkSentence(String sentence) {
        int c = 0;
        wordsOnSentenceNum = new TreeSet<>();
        words = lemmaFinder.textSplitToWords(sentence);

        for (String wordToSearch : wordsToSearch) {

            for (int i = 0; i < words.length; i++) {
                wordIsPresent = lemmaFinder.checkWord(words, wordToSearch, i);

                if (wordIsPresent) {
                    wordsOnSentenceNum.add(c);

                }
            }
            c++;
        }
    }

    private String wordsToSentence() {
        StringBuilder builder1 = new StringBuilder();

        for (String word : words) {
            builder1.append(word + " ");
        }

        return builder1.toString();
    }

    private void wordsToSentences(List<Integer> wts) {
        for (Integer w : wordsOnSentenceNum) {

            boolean wasRemoved = wts.remove(w);

            if (wasRemoved) {
                for (String word : words) {
                    builder.append(word + " ");
                }
                builder.append("  .......  ");
            }

        }
    }

    private String sentenceToString() {
        for (String word : words) {
            builder.append(word + " ");
        }
        return builder.toString();
    }
}
