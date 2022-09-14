import utils.FileUtil;
import utils.LoadProperties;

import java.io.*;
import java.util.Properties;


public class Main {
    public static void main(String[] args) {
        SootInstrument sootInstrument = new SootInstrument();
        //sootInstrument.initSoot("apks/youtubeExtractor-androidTest.apk");
        sootInstrument.initSoot(args[0]);
        sootInstrument.instrument();
    }
}
