package searchengine.LemmaProcessing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        StringBuilder builder = new StringBuilder();
        String text = lemmaFinder.removeHTMLTags(t);
        String newText = findSnippet(text, wordsToSearch);
        String[] sentences = newText.split("[.!?:;]\\s*");
        for (String word : wordsToSnippet) {
            for (String sentence : sentences) {
                if (sentence.contains(word)) {
                    builder.append(sentence + "  ....  ");
                    break;
                }
            }
        }
        return builder.toString();
    }

    public String findSnippet(String text, List<String> wordsToSearch){
        StringBuilder builder = new StringBuilder();
        String[] words = lemmaFinder.textSplitToWords(text);
        for (String wordToSearch : wordsToSearch) {
            builder.append(lemmaFinder.checkWord(words, wordToSearch, wordsToSnippet));
        }
        return builder.toString();
    }
}
