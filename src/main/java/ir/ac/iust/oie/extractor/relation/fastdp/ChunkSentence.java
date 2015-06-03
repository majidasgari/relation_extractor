package ir.ac.iust.oie.extractor.relation.fastdp;

import ir.ac.iust.text.utils.WordLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 03/06/2015.
 */
public class ChunkSentence {
    private String referenceSentence;
    private List<Chunk> chunks = new ArrayList<>();
    private List<WordLine> words = new ArrayList<>();
    private List<Chunk> wordChunks = new ArrayList<>();

    @Override
    public String toString() {
        if (words.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (WordLine wordLine : words)
            builder.append(wordLine.text).append(' ');
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public String getReferenceSentence() {
        return referenceSentence;
    }

    public void setReferenceSentence(String referenceSentence) {
        this.referenceSentence = referenceSentence;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<Chunk> chunks) {
        this.chunks = chunks;
    }

    public List<WordLine> getWords() {
        return words;
    }

    public void setWords(List<WordLine> words) {
        this.words = words;
    }

    public List<Chunk> getWordChunks() {
        return wordChunks;
    }

    public void setWordChunks(List<Chunk> wordChunks) {
        this.wordChunks = wordChunks;
    }
}
