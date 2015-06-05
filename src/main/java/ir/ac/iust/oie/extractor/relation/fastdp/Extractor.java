package ir.ac.iust.oie.extractor.relation.fastdp;

import edu.stanford.nlp.util.StringUtils;
import ir.ac.iust.text.utils.Color;
import ir.ac.iust.text.utils.*;
import ir.ac.iust.text.utils.mixer.Column;
import ir.ac.iust.text.utils.mixer.FileMixer;
import org.apache.log4j.Logger;
import org.maltparser.core.exception.MaltChainedException;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
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
        String mainContent = contents;
        contents = clearText(contents);
        Path posFile = ir.ac.iust.oie.fastdp.Runner.makePosFile(contents, outPath);
        List<ChunkSentence> sentences = getSentences(posFile, "word pos chunk");
        sentences = SentenceExpander.processConjunctions(sentences);
        StringBuilder builder = new StringBuilder();
        for (ChunkSentence sentence : sentences)
            builder.append(sentence.toPlainString()).append(' ');
        extract(mainContent, createPredictedFile(builder.toString(), outPath));

//        Path path1 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.pos");
//        Path path2 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\data.untagged.model");
//        Path path3 = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.fdp");
//        FileMixer.mix(path1, "\\t", path2, "\\s+", path3, new Column(0, 0), new Column(0, 1), new Column(1, -1));

//        Path outPath = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test.tran");
//        FlexCrfFeatureGenerator.main(array("-ulb", outPath.toAbsolutePath().toString(),
//                outPath.getParent().resolve("hey.tran").toString(), "no"));
//        extract(outPath);
    }

    private static String clearText(String contents) {
        StringBuilder builder = new StringBuilder();
        int numberOfPren = 0;
        for (int i = 0; i < contents.length(); i++) {
            char ch = contents.charAt(i);
            if (ch == '(' || ch == '[' || ch == '{' || ch == '«') {
                numberOfPren++;
                continue;
            } else if (ch == ')' || ch == ']' || ch == '}' || ch == '»') {
                numberOfPren--;
                continue;
            }
            if (numberOfPren > 0) continue;
//            if (ch == '\u200C') ch = '_';
            builder.append(ch);
        }
        return builder.toString();
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

    public static List<ChunkSentence> getSentences(Path predictedFile, String wordDefinitionPattern) throws IOException, MaltChainedException {
        List<WordTagLine> lines = WordTagLine.getLines(wordDefinitionPattern, predictedFile);
        List<String> sentence = new ArrayList<>();
        List<WordTagLine> actualSentence = new ArrayList<>();

        ArrayList<String> chunkLines = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        List<ChunkSentence> sentences = new ArrayList<>();
        for (int i = 0, linesSize = lines.size(); i < linesSize; i++) {
            WordTagLine line1 = lines.get(i);
            if (line1.isEmpty() || i == linesSize - 1) {
                if (!sentence.isEmpty()) {
                    ChunkSentence chunkSentence = new ChunkSentence();
                    List<Chunk> chunks = DpToChunker.getChunks(actualSentence);
                    for (int j = 0, chunksSize = chunks.size(); j < chunksSize; j++) {
                        Chunk chunk = chunks.get(j);
                        for (WordTagLine columnedLine : chunk.words) {
                            builder.append(columnedLine.toString()).append('\t').append(j);
                            chunkLines.add(builder.toString());
                            builder.setLength(0);
                            columnedLine.addSplit(j);
                            chunkSentence.getWords().add(columnedLine);
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
                sentence.add(line1.word());
                actualSentence.add(line1);
            }
        }
        Files.write(predictedFile.toAbsolutePath().getParent().resolve(predictedFile.getFileName() + "c"),
                chunkLines, Charset.forName("UTF-8"));
        return sentences;
    }

    public static void extract(String mainContent, Path predictedFile) throws IOException, MaltChainedException {
        List<ChunkSentence> sentences = getSentences(predictedFile, "word pos fdp chunk");
        StringBuilder builder = new StringBuilder();
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html>").append("<body dir='rtl'>");
        htmlBuilder.append("<head><meta charset='UTF-8'></head>");
        htmlBuilder.append("<div><h1>محتوای اصلی:</h1>").append(mainContent).append("</div>");
        htmlBuilder.append("<h1>رابطه‌های استخراج شده:</h1>");
        for (int sentenceNumber = 0; sentenceNumber < sentences.size(); sentenceNumber++) {
            ChunkSentence chunkSentence = sentences.get(sentenceNumber);
//            if(chunkSentence.getReferenceSentence().split("\\s+").length > 25)
//                continue;
            List<Chunk> chunks = chunkSentence.getChunks();
            int numberOfVerbs = 0;
            for (WordTagLine line : chunkSentence.getWords()) {
                if (TagUtils.isVerbTag(line)) {
                    numberOfVerbs++;
                }
            }
            if (numberOfVerbs == 1) {
                List<Integer> arguments = new ArrayList<>();
                List<Integer> descriptiveArguments = new ArrayList<>();
                Chunk lastChunk = null;
                Chunk relation = null;
                builder.setLength(0);
                builder.append("(");
                for (int chunkNumber = 0; chunkNumber < chunks.size(); chunkNumber++) {
                    Chunk chunk = chunks.get(chunkNumber);
                    boolean isRelation = false;
                    for (WordTagLine word : chunk.words) {
                        if (TagUtils.isVerbTag(word)) {
                            relation = chunk;
                            isRelation = true;
                            break;
                        }
                    }
                    if (isRelation) {
                        List<WordTagLine> toRemove = new ArrayList<>();
                        for (WordTagLine columnedLine : relation.words)
                            if (TagUtils.isPunctuationTag(columnedLine) || TagUtils.isPostPositionTag(columnedLine))
                                toRemove.add(columnedLine);
//                            else
//                                columnedLine.word(stemmer.Stem(columnedLine.word()));
                        relation.words.removeAll(toRemove);
                        if (arguments.size() == 1 || (arguments.size() + descriptiveArguments.size() == 1)) {
                            int mainArgument = arguments.isEmpty() ? descriptiveArguments.get(0) : arguments.get(0);
                            boolean main = true;
                            List<Chunk> chunks1 = chunkSentence.getChunks();
                            for (int k = 0; k < chunks1.size(); k++) {
                                Chunk ch = chunks1.get(k);
                                if (k != mainArgument && k != chunkNumber) {
                                    arguments.add(k);
                                    addArgument(builder, htmlBuilder, !main, ch);
                                    main = false;
                                }
                            }
                        }
                        builder.append(StringColorizer.colorize(relation.toPlainString(), Color.red)).append(")");
                        htmlBuilder.append(StringColorizer.colorize(relation.toPlainString(), "red", true));
                        htmlBuilder.append(" (").append(chunkSentence.toChunkString()).append(")").append("<br/>");
                    }
                    if (!isRelation) {
                        for (WordTagLine word : chunk.words) {
                            if (word.fdp().equals("V")) {
                                if (!TagUtils.isPrepTag(word)
                                        & !(chunk.words.size() == 1 && TagUtils.isPostPositionTag(word))) {
                                    lastChunk = chunk;
                                    if (TagUtils.isConnectorWord(ListUtils.lastElement(lastChunk.words)))
                                        ListUtils.removeLastElement(lastChunk.words);
                                    arguments.add(chunkNumber);
                                    addArgument(builder, htmlBuilder, false, chunk);
                                } else if (chunk.words.size() == 1 && TagUtils.isPostPositionTag(word)
                                        && lastChunk != null) {
                                    for (WordTagLine w : chunk.words) chunkSentence.getWords().add(w);
                                } else if (chunk.words.size() > 1
                                        && (lastChunk != null)
                                        && (TagUtils.isConnectorWord(ListUtils.lastElement(lastChunk.words)))) {
                                    for (WordTagLine w : chunk.words) chunkSentence.getWords().add(w);
                                } else {
                                    lastChunk = chunk;
                                    if (TagUtils.isConnectorWord(ListUtils.lastElement(lastChunk.words)))
                                        ListUtils.removeLastElement(lastChunk.words);
                                    descriptiveArguments.add(chunkNumber);
                                    addArgument(builder, htmlBuilder, true, chunk);
                                }
                                break;
                            }
                        }
                    }
                }
                writeExtraction(chunkSentence.getReferenceSentence(), chunkSentence,
                        arguments, descriptiveArguments, relation, builder.toString());
                builder.setLength(0);
            }
        }
        htmlBuilder.append("</body></html>");
        Files.write(Paths.get("test.html"), htmlBuilder.toString().getBytes("UTF-8"));
        Desktop.getDesktop().open(Paths.get("test.html").toFile());
    }

    private static void addArgument(StringBuilder builder, StringBuilder htmlBuilder, boolean descriptive,
                                    Chunk chunk) {
        if (descriptive) {
            builder.append(StringColorizer.colorize(chunk.toPlainString(), Color.green)).append(",\t");
            htmlBuilder.append(StringColorizer.colorize(chunk.toPlainString(), "gray", false)).append(" / ");
        } else {
            builder.append(chunk.toPlainString()).append(",\t");
            htmlBuilder.append(StringColorizer.colorize(chunk.toPlainString(), "black", true)).append(" / ");
        }
    }

    private static void writeExtraction(String referenceString, ChunkSentence chunkSentence,
                                        List<Integer> arguments, List<Integer> descriptiveArguments,
                                        Chunk relation, String relationString) {
        logger.info("main sentence: " + referenceString);
        logger.info("sentence: " + chunkSentence.toPlainString());
        logger.info(chunkSentence.toPOSString());
        logger.info(chunkSentence.getChunks());
//                        logger.trace("parsed: " + StringUtils.join(actualSentence, " "));
        logger.info(relationString);
        logger.trace("**************************");
        for (Integer argument : arguments)
            logger.trace("argument: " + chunkSentence.getChunks().get(argument).toPlainString());
        if (!descriptiveArguments.isEmpty()) {
            for (Integer argument : descriptiveArguments)
                logger.trace("** descriptive argument: " + chunkSentence.getChunks().get(argument).toPlainString());
        }
        Color redBolds = Color.red;
        redBolds.setBold();
        logger.trace("relation: " + StringColorizer.colorize(relation.toString(), redBolds));
        logger.info("---------------------------");
    }
}
