package ir.ac.iust.oie.extractor.relation.fastdp;

import org.maltparser.core.exception.MaltChainedException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Majid on 02/06/2015.
 */
public class TestExtractor {
    public static void main(String[] args) throws IOException, MaltChainedException {
        String text = "در حقیقت مشخصه مهم این دولت احترام به آزادی فردی نظم بود.";

//        Extractor.extract(Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\samples\\3\\test.fdp"));
        Path path = Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\run_folder\\test_1st_rep.fdp.pos");
        Extractor.extract("توی فایل هست!: "
                + path.toAbsolutePath().toString(), path);
//        Extractor.extract(Paths.get("C:\\Users\\Majid\\Desktop\\Lessons\\samples\\bug.fdp"));
    }
}
