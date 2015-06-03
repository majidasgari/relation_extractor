package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.util.StringUtils;
import ir.ac.iust.text.utils.LoggerUtils;
import ir.ac.iust.text.utils.WordLine;
import ir.ac.iust.text.utils.mixer.Column;
import ir.ac.iust.text.utils.mixer.FileMixer;
import iust.ac.ir.nlp.jhazm.Stemmer;
import org.apache.log4j.Logger;
import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Majid on 31/05/2015.
 */
public class Extractor {
    private static Logger logger = LoggerUtils.getLogger(Runner.class, "extractor.log");
    private static Stemmer stemmer = new Stemmer();

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

        ArrayList<String> chunkLines = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        List<ChunkSentence> sentences = new ArrayList<>();
        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            WordLine line1 = lines.get(i);
            if (line1.isEmpty || i == linesSize - 1) {
                if (!sentence.isEmpty()) {
                    ChunkSentence chunkSentence = new ChunkSentence();
                    List<Chunk> chunks = DpToChunker.getChunks(actualSentence);
                    for (int j = 0, chunksSize = chunks.size(); j < chunksSize; j++) {
                        Chunk chunk = chunks.get(j);
                        for (WordLine wordLine : chunk.words) {
                            builder.append(wordLine.toString()).append('\t').append(j);
                            chunkLines.add(builder.toString());
                            builder.setLength(0);
                            wordLine.text = wordLine.text + "\t" + j;
                            wordLine.splits = Arrays.copyOf(wordLine.splits, wordLine.splits.length + 1);
                            wordLine.splits[wordLine.splits.length - 1] = String.valueOf(j);
                            chunkSentence.getWords().add(wordLine);
                            chunkSentence.getWordChunks().add(chunk);
                        }
                    }
                    chunkLines.add("");
                    chunkSentence.setChunks(chunks);
                    chunkSentence.setReferenceSentence(StringUtils.join(sentence));
                    sentences.add(chunkSentence);
                }
                sentence.clear();
                actualSentence.clear();
            } else {
                sentence.add(line1.splits[0]);
                actualSentence.add(line1);
            }
        }
        Files.write(predictedFile.toAbsolutePath().getParent().resolve(predictedFile.getFileName() + "c"),
                chunkLines, Charset.forName("UTF-8"));

        sentences = processConjunctions(sentences);

        for (int i = 0; i < sentences.size(); i++) {
            ChunkSentence chunkSentence = sentences.get(i);
            List<Chunk> chunks = chunkSentence.getChunks();
            int numberOfVerbs = 0;
            for (WordLine line : chunkSentence.getWords()) {
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
                                    for (WordLine w : chunk.words) chunkSentence.getWords().add(w);
                                } else if (chunk.words.size() > 1
                                        && (lastChunk != null)
                                        && (lastChunk.words.get(lastChunk.words.size() - 1).splits[0].equals("،")
                                        || lastChunk.words.get(lastChunk.words.size() - 1).splits[0].equals("و"))) {
                                    for (WordLine w : chunk.words) chunkSentence.getWords().add(w);
                                } else {
                                    descriptiveArguments.add(chunk);
                                    lastChunk = chunk;
                                }
                                break;
                            }
                        }
                    }
                }
                writeExtraction(chunkSentence.getReferenceSentence(),
                        chunkSentence.toString(),
                        arguments, descriptiveArguments, relation);
            }
        }
    }

    private static List<ChunkSentence> processConjunctions(List<ChunkSentence> sentences) {
        for (int i = 0; i < sentences.size(); i++) {
            ChunkSentence sentence = sentences.get(i);
            List<ChunkSentence> sentenceList = processSentence(sentence);
            if (sentenceList.size() > 1) {
                sentences.remove(i);
                sentences.addAll(i, sentenceList);
                return processConjunctions(sentences);
            }
        }
        return sentences;
    }

    private static List<ChunkSentence> processSentence(ChunkSentence sentence) {
        List<WordLine> words = sentence.getWords();
        for (int i = 0; i < words.size(); i++) {
            WordLine wordLine = words.get(i);
            if (wordLine.splits[1].equals("CONJ") && i > 0) {
                if (words.get(i - 1).splits[1].equals("V")) /*splitting sentences*/
                    return splitSentence(sentence, i);
                else
                    return branchSentence(sentence, i);
            }
        }
        List<ChunkSentence> list = new ArrayList<>();
        list.add(sentence);
        return list;
    }

    //    شاهنشاهی	Ne	V	0
//    هخامنشی	N	O	0
//    و	CONJ	V	0
//    یا	CONJ	O	0
//    هخامنشیان	N	O	1
//    نام	Ne	O	2
//    دودمانی	N	O	2
//    و	CONJ	V	2
//    یک	NUM	O	3
//    سلسله	CL	O	3
//    پادشاهی	N	O	3
//    در	P	O	4
//    ایران	N	O	4
//    دوره	Ne	O	5
//    باستان	AJ	O	5
//    است	V	O	6
//    .	PUNC	O	6
    private static List<ChunkSentence> branchSentence(ChunkSentence sentence, int splitPosition) {
        List<ChunkSentence> list = new ArrayList<>();
        if (splitPosition == 0 || splitPosition == sentence.getWords().size()
                || (sentence.getWords().get(splitPosition).splits[3].equals(
                getLastElement(sentence.getWords()).splits[3]))) {
            list.add(sentence);
            return list;
        }

        //[0,splitPosition],[a=next chunk start, end]
        //[0,previous chunk end],
        int first = splitPosition - 1;
        int chunkIndex = sentence.getWords().get(splitPosition - 1).getSplitAsInt(3);
        while (sentence.getWords().get(first).getSplitAsInt(3) == chunkIndex && first > 0) first--;
        if (sentence.getWords().get(first).getSplitAsInt(3) != chunkIndex) first++;
        int second = splitPosition + 1;
        while (sentence.getWords().get(second).splits[1].equals("CONJ")) second++;
        int third = second;
        chunkIndex = sentence.getWords().get(third).getSplitAsInt(3);
        while (sentence.getWords().get(third).getSplitAsInt(3) == chunkIndex) third++;

        if (sentence.getWords().get(splitPosition).splits[2].equals("V")) {
            sentence.getWords().get(second).setSplit(2, "V");
            int headOfFirst = splitPosition - 1;
            while ((headOfFirst > 0
                    && (sentence.getWords().get(headOfFirst).splits[3]
                    .equals(sentence.getWords().get(splitPosition - 1).splits[3]))))
                headOfFirst--;
            if (headOfFirst != 0) headOfFirst++;
            sentence.getWords().get(headOfFirst).setSplit(2, "V");
        }
        list.add(subSentence(sentence, 0, splitPosition, third, sentence.getWords().size()));
        list.add(subSentence(sentence, 0, first, second, sentence.getWords().size()));

        return list;
    }

    private static ChunkSentence subSentence(ChunkSentence chunkSentence, int start1, int end1,
                                             int start2, int end2) {
        ChunkSentence result = new ChunkSentence();
        result.setReferenceSentence(chunkSentence.getReferenceSentence());
        int chunkIndex = addToSentence(chunkSentence, result, 0, start1, end1);
        addToSentence(chunkSentence, result, chunkIndex, start2, end2);
        return result;
    }

    private static int addToSentence(ChunkSentence source, ChunkSentence result,
                                     int chunkIndex, int start, int end) {
        int currentChunk = -1;
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (int i = start; i < end; i++) {
            WordLine wordLine = source.getWords().get(i).copy();
            if (wordLine.getSplitAsInt(3) != currentChunk) {
                Chunk chunk = new Chunk();
                chunk.referenceSentence = source.getWordChunks().get(i).referenceSentence;
                chunks.add(chunk);
                currentChunk = wordLine.getSplitAsInt(3);
                result.getChunks().add(chunk);
            }
            getLastElement(chunks).words.add(wordLine);
        }
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            for (WordLine wl : chunk.words) {
                wl.setSplit(3, i + chunkIndex);
                result.getWords().add(wl);
                result.getWordChunks().add(chunk);
            }
        }
        return chunkIndex + chunks.size();
    }

    //    پادشاهان	Ne	V	0
//    این	DET	O	1
//    دودمان	N	O	1
//    از	P	O	2
//    پارسیان	N	O	2
//    بودند	V	O	3
//    و	CONJ	V	3
//    تبار	Ne	O	4
//    خود	PRO	O	4
//    را	POSTP	V	5
//    به	P	V	6
//    هخامنش	N	O	6
//    می‌رساندند	V	O	7
//    که	CONJ	O	7
//    سرکردهٔ	Ne	O	8
//    خاندان	Ne	O	8
//    پاسارگاد	N	O	8
//    از	P	O	9
//    خاندان‌های	Ne	O	9
//    پارسیان	Ne	O	9
//    بوده‌است	V	O	10
//    .	PUNC	O	11
    private static List<ChunkSentence> splitSentence(ChunkSentence sentence, int splitPosition) {
        List<ChunkSentence> splits = new ArrayList<>();
        if (splitPosition == sentence.getWords().size() - 1) {
            splits.add(sentence);
            return splits;
        }

        ChunkSentence firstSentence = new ChunkSentence();
        firstSentence.setReferenceSentence(sentence.getReferenceSentence());
        for (int i = 0; i < splitPosition; i++) {
            WordLine wordLine = sentence.getWords().get(i);
            firstSentence.getWords().add(wordLine);
            Chunk chunk = sentence.getWordChunks().get(i);
            firstSentence.getWordChunks().add(chunk);
            if (chunk != getLastElement(firstSentence.getChunks()))
                firstSentence.getChunks().add(chunk);
        }

        if (sentence.getWords().get(splitPosition).splits[3]
                .equals(sentence.getWords().get(splitPosition - 1).splits[3]))
            removeLastElement(sentence.getWordChunks().get(splitPosition - 1).words);

        if (sentence.getWords().get(splitPosition).splits[3]
                .equals(sentence.getWords().get(splitPosition + 1).splits[3]))
            sentence.getWordChunks().get(splitPosition + 1).words.remove(0);

        ChunkSentence secondSentence = new ChunkSentence();
        secondSentence.setReferenceSentence(sentence.getReferenceSentence());
        int lastChunkIndex = Integer.parseInt(
                sentence.getWords().get(splitPosition + 1).splits[3]);
        for (int i = splitPosition + 1; i < sentence.getWords().size(); i++) {
            WordLine wordLine = sentence.getWords().get(i);
            secondSentence.getWords().add(wordLine);
            Chunk chunk = sentence.getWordChunks().get(i);
            secondSentence.getWordChunks().add(chunk);
            if (chunk != getLastElement(secondSentence.getChunks())) {
                secondSentence.getChunks().add(chunk);
                for (WordLine chunkLine : chunk.words) {
                    chunkLine.splits[3] = String.valueOf(Integer.parseInt(chunkLine.splits[3])
                            - lastChunkIndex);
                }
            }
        }

        splits.add(firstSentence);
        splits.add(secondSentence);
        return splits;
    }

    private static void removeLastElement(List list) {
        if (!list.isEmpty()) list.remove(list.size() - 1);
    }

    private static <T> T getLastElement(List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    private static void writeExtraction(String sentence, String processedSentence, List<Chunk> arguments, List<Chunk> descriptiveArguments,
                                        Chunk relation) {
        logger.trace("main sentence: " + sentence);
        logger.trace("processed sentence: " + processedSentence);
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
