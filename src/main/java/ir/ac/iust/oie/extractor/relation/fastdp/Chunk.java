package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.util.StringUtils;
import ir.ac.iust.text.utils.WordTagLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class Chunk {
    public List<WordTagLine> words = new ArrayList<>();
    public String referenceSentence;

    public void add(WordTagLine line) {
        words.add(line);
    }

    @Override
    public String toString() {
        return StringUtils.join(words, " ");
    }

    public String toPlainString() {
        StringBuilder builder = new StringBuilder();
        for (WordTagLine word : words)
            builder.append(word.word()).append(' ');
        if (builder.length() > 0) builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }
}
