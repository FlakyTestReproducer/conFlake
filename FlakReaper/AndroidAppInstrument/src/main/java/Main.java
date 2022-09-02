import utils.FileUtil;
import utils.LoadProperties;

import java.io.*;
import java.util.Properties;


public class Main {
    public static void main(String[] args) {
        SootInstrument sootInstrument = new SootInstrument();
        sootInstrument.initSoot("apks/youtubeExtractor-androidTest.apk");
        sootInstrument.instrument();
//        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
//        StringBuffer sb = new StringBuffer();
//        for(int i=0; i<stackTraceElements.length; i++){
//            String s = stackTraceElements[i].toString();
//            sb.append(s);
//            if(i<stackTraceElements.length-1){
//                sb.append("+");
//            }
//        }
//        LoadProperties.set(Thread.currentThread().getName()+"+"+sb.toString(),"123");

//        String cmd = "adb shell am instrument -w -r -e debug false -e class 'at.huber.youtubeExtractor.ExtractorTestCases#testEncipheredVideo' at.huber.youtubeExtractor.test/android.support.test.runner.AndroidJUnitRunner";
//        int t = 0;
//        while(t < 100){
//            runTestCase(cmd);
//            t++;
//        }
    }

    private static void runTestCase(String command){
        try {
            Process p = Runtime.getRuntime().exec(command);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader errReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line = "";
            while((line = reader.readLine()) != null) {
                System.out.println(line);
                FileUtil.filePrintln("output.txt",line);
            }
            while((line = errReader.readLine()) != null) {
                System.out.println(line);
                FileUtil.filePrintln("output.txt",line);
            }
            p.waitFor();

        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}
