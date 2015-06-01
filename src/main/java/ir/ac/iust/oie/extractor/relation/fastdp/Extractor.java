package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.util.StringUtils;
import ir.ac.iust.oie.fastdp.flexcrf.FlexCrfFeatureGenerator;
import ir.ac.iust.text.utils.LoggerUtils;
import ir.ac.iust.text.utils.WordLine;
import ir.ac.iust.text.utils.mixer.Column;
import ir.ac.iust.text.utils.mixer.FileMixer;
import iust.ac.ir.nlp.jhazm.*;
import org.apache.log4j.Logger;
import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class Extractor {
    private static Logger logger = LoggerUtils.getLogger(Runner.class, "extractor.log");

    public static void main(String[] args) throws IOException, MaltChainedException, InterruptedException {
        Path outPath = Paths.get(args[0]);
        String contents = new String(Files.readAllBytes(outPath.resolve(outPath.getFileName() + ".txt")));
        extract(createPredictedFile(contents, outPath));

//        Path path1 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.pos");
//        Path path2 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\data.untagged.model");
//        Path path3 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.fdp");
//        FileMixer.mix(path1, "\\t", path2, "\\s+", path3, new Column(0, 0), new Column(0, 1), new Column(1, -1));

//        Path outPath = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.tran");
//        FlexCrfFeatureGenerator.main(array("-ulb", outPath.toAbsolutePath().toString(),
//                outPath.getParent().resolve("hey.tran").toString(), "no"));
//        extract(outPath);
    }
    private static String[] array(String... input) {
        return input;
    }

    private static Stemmer stemmer = new Stemmer();

    public static Path createPredictedFile(String text, Path outputPath) throws IOException, InterruptedException {
        ir.ac.iust.oie.fastdp.Runner.prediction(text, outputPath);
        Path fdpOutputPath = outputPath.resolve(outputPath.getFileName().toString() + ".fdp");
        FileMixer.mix(outputPath.resolve(outputPath.getFileName().toString() + ".pos"), "\\t",
                outputPath.resolve("data.untagged.model"), "\\s+",
                fdpOutputPath,
                new Column(0, 0), new Column(0, 1), new Column(1, -1));
        return fdpOutputPath;
    }

    public static void extract(Path predictedFile) throws IOException, MaltChainedException {
        List<WordLine> lines = WordLine.getLines(predictedFile);
        List<String> sentence = new ArrayList<>();
        List<WordLine> actualSentence = new ArrayList<>();
        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            WordLine line1 = lines.get(i);
            if (line1.isEmpty || i == linesSize - 1) {
                if (!sentence.isEmpty()) {
                    List<Chunk> chunks = DpToChunker.getChunks(actualSentence);
                    int numberOfVerbs = 0;
                    for (WordLine line : actualSentence) {
                        if (line.splits[1].equals("V") || line.splits[1].equals("ACT") || line.splits[1].equals("PASS")) {
                            numberOfVerbs++;
                        }
                    }
                    if (numberOfVerbs == 1) {
                        List<Chunk> arguments = new ArrayList<>();
                        List<Chunk> descriptiveArguments = new ArrayList<>();
                        Chunk lastChunk = null;
                        Chunk relation = null;
                        for (Chunk chunk : chunks) {
                            boolean isRelation = false;
                            for (WordLine word : chunk.words) {
                                if (word.splits[1].equals("V") || word.splits[1].equals("ACT") || word.splits[1].equals("PASS")) {
                                    relation = chunk;
                                    isRelation = true;
                                    break;
                                }
                            }
                            if (isRelation) {
                                List<WordLine> toRemove = new ArrayList<>();
                                for (WordLine wordLine : relation.words) {
                                    if (wordLine.splits[1].equals("PUNC"))
                                        toRemove.add(wordLine);
                                    else wordLine.splits[1] = stemmer.Stem(wordLine.splits[1]);
                                }
                                relation.words.removeAll(toRemove);
                            }
                            if (!isRelation) {
                                for (WordLine word : chunk.words) {
                                    if (word.splits[2].equals("V")) {
                                        if (!word.splits[1].equals("P")
                                                & !word.splits[1].equals("PREP")
                                                & !word.splits[1].equals("INAM")
                                                & !(chunk.words.size() == 1 && word.splits[1].equals("POSTP"))) {
                                            lastChunk = chunk;
                                            arguments.add(chunk);
                                        } else if (chunk.words.size() == 1 && word.splits[1].equals("POSTP")
                                                && lastChunk != null) {
                                            for (WordLine w : chunk.words) actualSentence.add(w);
                                        } else if (chunk.words.size() > 1
                                                && (lastChunk != null)
                                                && (lastChunk.words.get(lastChunk.words.size() - 1).splits[0].equals("،")
                                                || lastChunk.words.get(lastChunk.words.size() - 1).splits[0].equals("و"))) {
                                            for (WordLine w : chunk.words) actualSentence.add(w);
                                        } else {
                                            descriptiveArguments.add(chunk);
                                            lastChunk = chunk;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        writeExtraction(sentence, arguments, descriptiveArguments, relation);
                    }
                    actualSentence.clear();
                    sentence.clear();
                }
            } else {
                sentence.add(line1.splits[0]);
                actualSentence.add(line1);
            }
        }
    }

    private static void writeExtraction(List<String> sentence, List<Chunk> arguments, List<Chunk> descriptiveArguments,
                                        Chunk relation) {
        logger.trace("sentence: " + StringUtils.join(sentence, " "));
//                        logger.trace("parsed: " + StringUtils.join(actualSentence, " "));
        logger.trace("**************************");
        for (Chunk argument : arguments)
            logger.trace("argument: " + argument);
        if (!descriptiveArguments.isEmpty()) {
            for (Chunk argument : descriptiveArguments)
                logger.trace("** descriptive argument: " + argument);
        }
        logger.trace("relation: " + relation);
        logger.trace("---------------------------");
    }
}
