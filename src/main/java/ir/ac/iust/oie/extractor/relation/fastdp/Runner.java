package ir.ac.iust.oie.extractor.relation.fastdp;

import ir.ac.iust.text.utils.LoggerUtils;
import ir.ac.iust.text.utils.StringBuilderWriter;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Majid on 30/05/2015.
 */
public class Runner {

    private static Logger logger = LoggerUtils.getLogger(Runner.class, "relation-extractor.log");

    public static void main(String[] args) {
        // create the Options
        Options options = new Options();
        options.addOption("a", "action", true, "action to do. extract is the only option.");
        options.addOption("l", "locale", true, "locale of action. fa is the only option.");
        options.addOption("i", "input", true, "input, standard CONLL dependency parsing corpus file.");
        options.addOption("o", "output", true, "output, standard CONLL NER/POS corpus file.");
        options.addOption("m", "model", true, "fast-dp model file.");

        CommandLineParser parser = new BasicParser();
        Action action = null;
        String locale = null;
        Path inputPath = null, outputPath = null, modelPath = null;
        try {
            CommandLine line = parser.parse(options, args);
            if (!line.hasOption("a") || !line.hasOption("i"))
                showHelp(options);
            action = Action.valueOf(line.getOptionValue("a"));
            if (action == null) showHelp(options);
            if (action == Action.extract && (!line.hasOption("l") || !line.hasOption("o"))) showHelp(options);
            else if (action != Action.extract && !line.hasOption("m")) showHelp(options);
            locale = line.getOptionValue("l");
            if (!locale.equals("fa")) showHelp(options);
            inputPath = Paths.get(line.getOptionValue("i"));
            if (!Files.exists(inputPath)) {
                logger.info("file does not exists: " + inputPath.toFile().getAbsolutePath());
                System.exit(1);
            } else logger.info("input file: " + inputPath.toFile().getAbsolutePath());
            if (line.hasOption("m")) {
                modelPath = Paths.get(line.getOptionValue("m"));
                if (!Files.exists(inputPath)) {
                    logger.info("model file does not exists: " + modelPath.toFile().getAbsolutePath());
                    System.exit(1);
                }
            }
            if (line.hasOption("o")) {
                outputPath = Paths.get(line.getOptionValue("o"));
                logger.info("output path is: " + outputPath.toFile().getAbsolutePath());
            }
        } catch (ParseException exp) {
            logger.trace(exp);
            showHelp(options);
        }

        assert action != null;
        try {
            switch (action) {
                case extract:
                    logger.trace("convert, locale = " + locale);
                    if (locale.equals("fa"))
                        System.out.println("put your code here.");
                    break;
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private static void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        final StringBuilder helpBuilder = new StringBuilder().append('\n');
        helpBuilder.append("Welcome to Fast Dependency Parser.").append('\n');
        helpBuilder.append("Required options for extract: a,l,i,o,m").append('\n');
        formatter.printHelp(new StringBuilderWriter(helpBuilder), 80, "java -jar relation-extractor.jar", null,
                options, 0, 0, "Thank you", false);
        logger.info(helpBuilder);
        System.exit(0);
    }
}
