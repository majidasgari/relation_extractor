package ir.ac.iust.oie.extractor.relation.fastdp;

import ir.ac.iust.text.utils.ListUtils;
import ir.ac.iust.text.utils.LoggerUtils;
import ir.ac.iust.text.utils.TagUtils;
import ir.ac.iust.text.utils.WordTagLine;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Majid on 04/06/2015.
 */
public class SentenceExpander {
    private static Logger logger = LoggerUtils.getLogger(Runner.class, "expansions.log");

    public static List<ChunkSentence> processConjunctions(List<ChunkSentence> sentences) {
        List<ChunkSentence> result = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            ChunkSentence sentence = sentences.get(i);
            result.addAll(processSentence(0, sentence));
            logger.trace("line: " + i);
        }
        return result;
    }

    private static List<ChunkSentence> processSentence(int depth, ChunkSentence sentence) {
        List<WordTagLine> words = sentence.getWords();
        if (depth < 5)
            for (int i = 0; i < words.size(); i++) {
                WordTagLine columnedLine = words.get(i);
                if (TagUtils.isConjunctionTag(columnedLine) && i > 0) {
                    List<ChunkSentence> newSentences;
                    if (TagUtils.isVerbTag(words.get(i - 1))) /*splitting sentences*/
                        newSentences = splitSentence(sentence, i);
                    else
                        newSentences = branchSentence(sentence, i);//disable branching by passing 0 instead i
                    List<ChunkSentence> result = new ArrayList<>();
                    if (newSentences.size() < 2)
                        result.addAll(newSentences);
                    else
                        for (ChunkSentence newSentence : newSentences) {
                            logger.trace(newSentence);
                            if (!newSentence.toPlainString().equals(sentence.toPlainString()))
                                result.addAll(processSentence(++depth, newSentence));
                        }
                    return result;
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
        if (splitPosition == 0 || (splitPosition >= sentence.getWords().size() - 1)
                || (sentence.getWords().get(splitPosition).chunk().equals(
                ListUtils.lastElement(sentence.getWords()).chunk()))) {
            list.add(sentence);
            return list;
        }

        //[0,splitPosition],[a=next chunk start, end]
        //[0,previous chunk end],
        int first = splitPosition - 1;
        int chunkIndex = sentence.getWords().get(splitPosition - 1).chunkInt();
        while (sentence.getWords().get(first).chunkInt() == chunkIndex && first > 0) first--;
        if (sentence.getWords().get(first).chunkInt() != chunkIndex) first++;
        int second = splitPosition + 1;
        while (TagUtils.isConjunctionTag(sentence.getWords().get(second))) second++;
        int third = second;
        chunkIndex = sentence.getWords().get(third).chunkInt();
        while (sentence.getWords().get(third).chunkInt() == chunkIndex
                && third < sentence.getWords().size() - 1) third++;
//        if(third == sentence.getWords().size() - 1) {
//            list.add(sentence);
//            return list;
//        }

        if (sentence.getWords().get(splitPosition).getDefinition().hasFDP())
            if (sentence.getWords().get(splitPosition).fdp().equals("V")) {
                sentence.getWords().get(second).fdp("V");
                int headOfFirst = splitPosition - 1;
                while ((headOfFirst > 0
                        && (sentence.getWords().get(headOfFirst).chunk()
                        .equals(sentence.getWords().get(splitPosition - 1).chunk()))))
                    headOfFirst--;
                if (headOfFirst != 0) headOfFirst++;
                sentence.getWords().get(headOfFirst).fdp("V");
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
            WordTagLine columnedLine = source.getWords().get(i).copy();
            if (columnedLine.chunkInt() != currentChunk) {
                Chunk chunk = new Chunk();
                chunk.referenceSentence = source.getWordChunks().get(i).referenceSentence;
                chunks.add(chunk);
                currentChunk = columnedLine.chunkInt();
                result.getChunks().add(chunk);
            }
            ListUtils.lastElement(chunks).words.add(columnedLine);
        }
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            for (WordTagLine wl : chunk.words) {
                wl.chunkInt(i + chunkIndex);
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
            WordTagLine columnedLine = sentence.getWords().get(i);
            firstSentence.getWords().add(columnedLine);
            Chunk chunk = sentence.getWordChunks().get(i);
            firstSentence.getWordChunks().add(chunk);
            if (chunk != ListUtils.lastElement(firstSentence.getChunks()))
                firstSentence.getChunks().add(chunk);
        }

        if (sentence.getWords().get(splitPosition).chunk()
                .equals(sentence.getWords().get(splitPosition - 1).chunk()))
            ListUtils.removeLastElement(sentence.getWordChunks().get(splitPosition - 1).words);

        Chunk lastChunk = ListUtils.lastElement(firstSentence.getChunks());
        if (!ListUtils.lastElement(lastChunk.words).word().equals(".")) {
            WordTagLine wordTagLine = new WordTagLine(".\tPUNC\t0\t" + lastChunk.words.get(0).chunk(),
                    lastChunk.words.get(0).getDefinition());
            firstSentence.add(wordTagLine);
        }

        if (sentence.getWords().get(splitPosition).chunk()
                .equals(sentence.getWords().get(splitPosition + 1).chunk()))
            sentence.getWordChunks().get(splitPosition + 1).words.remove(0);

        ChunkSentence secondSentence = new ChunkSentence();
        secondSentence.setReferenceSentence(sentence.getReferenceSentence());
        int lastChunkIndex = Integer.parseInt(
                sentence.getWords().get(splitPosition + 1).chunk());
        for (int i = splitPosition + 1; i < sentence.getWords().size(); i++) {
            WordTagLine columnedLine = sentence.getWords().get(i);
            secondSentence.getWords().add(columnedLine);
            Chunk chunk = sentence.getWordChunks().get(i);
            secondSentence.getWordChunks().add(chunk);
            if (chunk != ListUtils.lastElement(secondSentence.getChunks())) {
                secondSentence.getChunks().add(chunk);
                for (WordTagLine chunkLine : chunk.words)
                    chunkLine.chunkInt(chunkLine.chunkInt() - lastChunkIndex);
            }
        }

        splits.add(firstSentence);
        splits.add(secondSentence);
        return splits;
    }
}
