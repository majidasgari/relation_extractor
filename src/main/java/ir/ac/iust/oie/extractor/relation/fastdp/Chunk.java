package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.util.StringUtils;
import ir.ac.iust.text.utils.WordLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class Chunk {
    public List<WordLine> words = new ArrayList<>();
    public String referenceSentence;
    public void add(WordLine line) {
        words.add(line);
    }

    @Override
    public String toString() {
        return StringUtils.join(words, " ");
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }
}
