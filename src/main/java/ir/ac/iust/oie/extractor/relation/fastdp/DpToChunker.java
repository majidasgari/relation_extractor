package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.ling.TaggedWord;
import ir.ac.iust.text.utils.WordTagDefinition;
import ir.ac.iust.text.utils.WordTagLine;
import iust.ac.ir.nlp.jhazm.*;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;
import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class DpToChunker {

    private static DependencyParser parser;

    static {
        try {
            parser = DependencyParser.i();
            parser.setNormalizer(Normalizer.i());
            parser.setLemmatizer(Lemmatizer.i());
            parser.setTagger(POSTagger.i());
            parser.setWordTokenizer(WordTokenizer.i());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, MaltChainedException {
        ArrayList<WordTagLine> sentences = new ArrayList<>();
        final WordTagDefinition definition = new WordTagDefinition("word pos fdp");
        sentences.add(new WordTagLine("این\tDEMAJ\tO", definition));
        sentences.add(new WordTagLine("میهمانی\tIANM\tV", definition));
        sentences.add(new WordTagLine("به\tPREP\tV", definition));
        sentences.add(new WordTagLine("منظور\tIANM\tO", definition));
        sentences.add(new WordTagLine("آشنایی\tIANM\tO", definition));
        sentences.add(new WordTagLine("هم_تیمی_های\tANM\tO", definition));
        sentences.add(new WordTagLine("او\tSEPER\tO", definition));
        sentences.add(new WordTagLine("با\tPREP\tO", definition));
        sentences.add(new WordTagLine("غذاهای\tIANM\tO", definition));
        sentences.add(new WordTagLine("ایرانی\tAJP\tO", definition));
        sentences.add(new WordTagLine("ترتیب\tIANM\tV", definition));
        sentences.add(new WordTagLine("داده شد\tPASS\tO", definition));
        sentences.add(new WordTagLine(".\tPUNC\tO", definition));
        List<Chunk> chunks = getChunks(sentences);
        for (Chunk chunk : chunks)
            System.out.println(chunk);
    }

    public static List<Chunk> getChunks(List<WordTagLine> sentence) throws IOException, MaltChainedException {
        List<TaggedWord> taggedWords = new ArrayList<>();
        StringBuilder sentenceBuilder = new StringBuilder();
        for (WordTagLine line : sentence) {
            sentenceBuilder.append(line.column(0)).append(' ');
            taggedWords.add(new TaggedWord(line.column(0), line.column(1)));
        }
        if (sentenceBuilder.length() > 0) {
            sentenceBuilder.setLength(sentenceBuilder.length() - 1);
        }
        ConcurrentDependencyGraph parsed = parser.RawParse(sentenceBuilder.toString()).iterator().next();
        List<Chunk> chunks = new ArrayList<>();
        Chunk lastChunk = new Chunk();
        HashMap<Integer, Integer> head = new HashMap<>();
        ArrayList<Integer> wordHeads = new ArrayList<>();
        lastChunk.add(sentence.get(0));
        wordHeads.add(parsed.getDependencyNode(1).getHeadIndex());
        for (int i = 2; i <= parsed.nTokenNodes(); i++) {
            if (sentence.size() < i) continue;
            WordTagLine line = sentence.get(i - 1);
            ConcurrentDependencyNode node = parsed.getDependencyNode(i);
            if (node.hasHead()) {
                if (head.containsKey(node.getHeadIndex()))
                    head.put(node.getHeadIndex(), head.get(node.getHeadIndex()) + 1);
                else head.put(node.getHeadIndex(), 1);
            }
            wordHeads.add(node.getHeadIndex());

            ConcurrentDependencyNode previousNode = parsed.getDependencyNode(i - 1);
            if ((node.hasHead() && node.getHeadIndex() == i - 1)
                    || (previousNode.hasHead() && previousNode.getHeadIndex() == i))
                lastChunk.add(line);
            else {
                chunks.add(lastChunk);
                lastChunk = new Chunk();
                lastChunk.add(line);
            }
        }
        if (!lastChunk.isEmpty())
            chunks.add(lastChunk);
        int maxHead = -1;
        int max = 0;
        for (Integer key : head.keySet()) {
            Integer value = head.get(key);
            if (value > max) {
                max = value;
                maxHead = key;
            }
        }

        List<Chunk> toRemove = new ArrayList<>();
        int wordNumber = 0;
        lastChunk = null;
        for (Chunk chunk : chunks) {
            boolean hasMaxHead = false;
            for (WordTagLine word : chunk.words) {
                if (wordHeads.get(wordNumber) == maxHead) hasMaxHead = true;
                wordNumber++;
            }
            if (!hasMaxHead && lastChunk != null) {
                for (WordTagLine word : chunk.words)
                    lastChunk.words.add(word);
                toRemove.add(chunk);
            } else lastChunk = chunk;
        }
        chunks.removeAll(toRemove);

        return chunks;
    }
}
