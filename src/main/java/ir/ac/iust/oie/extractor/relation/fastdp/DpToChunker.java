package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.ling.TaggedWord;
import ir.ac.iust.text.utils.WordLine;
import iust.ac.ir.nlp.jhazm.*;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;
import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class DpToChunker {

    public static void main(String[] args) throws IOException, MaltChainedException {
        ArrayList<WordLine> sentences = new ArrayList<>();
        sentences.add(new WordLine("این\tDEMAJ\tO"));
        sentences.add(new WordLine("میهمانی\tIANM\tV"));
        sentences.add(new WordLine("به\tPREP\tV"));
        sentences.add(new WordLine("منظور\tIANM\tO"));
        sentences.add(new WordLine("آشنایی\tIANM\tO"));
        sentences.add(new WordLine("هم_تیمی_های\tANM\tO"));
        sentences.add(new WordLine("او\tSEPER\tO"));
        sentences.add(new WordLine("با\tPREP\tO"));
        sentences.add(new WordLine("غذاهای\tIANM\tO"));
        sentences.add(new WordLine("ایرانی\tAJP\tO"));
        sentences.add(new WordLine("ترتیب\tIANM\tV"));
        sentences.add(new WordLine("داده شد\tPASS\tO"));
        sentences.add(new WordLine(".\tPUNC\tO"));
        List<Chunk> chunks = getChunks(sentences);
        for(Chunk chunk : chunks)
            System.out.println(chunk);
    }

    private static DependencyParser parser;

    static {
        try {
            parser = new DependencyParser();
            parser.setNormalizer(new Normalizer());
            parser.setLemmatizer(new Lemmatizer());
            parser.setTagger(new POSTagger());
            parser.setWordTokenizer(new WordTokenizer());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Chunk> getChunks(List<WordLine> sentence) throws IOException, MaltChainedException {
        List<TaggedWord> taggedWords = new ArrayList<>();
        StringBuilder sentenceBuilder = new StringBuilder();
        for (WordLine line : sentence) {
            sentenceBuilder.append(line.splits[0]).append(' ');
            taggedWords.add(new TaggedWord(line.splits[0], line.splits[1]));
        }
        if (sentenceBuilder.length() > 0) {
            sentenceBuilder.setLength(sentenceBuilder.length() - 1);
        }
        ConcurrentDependencyGraph parsed = parser.RawParse(sentenceBuilder.toString()).iterator().next();
        List<Chunk> chunks = new ArrayList<>();
        Chunk lastChunk = new Chunk();
        lastChunk.add(sentence.get(0));
        for (int i = 2; i <= parsed.nTokenNodes(); i++) {
            if(sentence.size() < i) continue;
            WordLine line = sentence.get(i - 1);
            ConcurrentDependencyNode node = parsed.getDependencyNode(i);
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
        return chunks;
    }
}
