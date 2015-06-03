package ir.ac.iust.oie.extractor.relation.fastdp;

import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Majid on 02/06/2015.
 */
public class TestExtractor {
    public static void main(String[] args) throws IOException, MaltChainedException {
        Extractor.extract(Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\samples\\1\\test.fdp"));
    }
}
