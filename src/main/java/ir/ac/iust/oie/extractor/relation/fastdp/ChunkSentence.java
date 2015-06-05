package ir.ac.iust.oie.extractor.relation.fastdp;

import ir.ac.iust.text.utils.ListUtils;
import ir.ac.iust.text.utils.WordTagLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 03/06/2015.
 */
public class ChunkSentence {
    private String referenceSentence;
    private List<Chunk> chunks = new ArrayList<>();
    private List<WordTagLine> words = new ArrayList<>();
    private List<Chunk> wordChunks = new ArrayList<>();

    @Override
    public String toString() {
        if (words.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (WordTagLine columnedLine : words)
            builder.append(columnedLine.getText()).append(' ');
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public String toPlainString() {
        if (words.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (WordTagLine columnedLine : words)
            builder.append(columnedLine.word()).append(' ');
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public String toPOSString() {
        if (words.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (WordTagLine columnedLine : words)
            builder.append('{').append(columnedLine.word()).append(' ').append(columnedLine.pos()).append('}').append('\t');
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public String toChunkString() {
        if (words.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        int lastChunkIndex = -1;
        for (WordTagLine columnedLine : words) {
            if (lastChunkIndex != columnedLine.chunkInt()) {
                builder.append("][");
                lastChunkIndex = columnedLine.chunkInt();
            }
            builder.append(columnedLine.word());
            if (columnedLine.fdp().equals("V")) builder.append("*");
            builder.append(' ');
        }
        if (builder.length() > 0) {
            builder.append("]");
            builder.deleteCharAt(0);
        }
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

    public List<WordTagLine> getWords() {
        return words;
    }

    public void setWords(List<WordTagLine> words) {
        this.words = words;
    }

    public List<Chunk> getWordChunks() {
        return wordChunks;
    }

    public void setWordChunks(List<Chunk> wordChunks) {
        this.wordChunks = wordChunks;
    }

    public void add(WordTagLine wordTagLine) {
        Chunk chunk = ListUtils.lastElement(chunks);
        if (chunk == null) return;
        chunk.words.add(wordTagLine);
        words.add(wordTagLine);
        wordChunks.add(chunk);
    }
}
