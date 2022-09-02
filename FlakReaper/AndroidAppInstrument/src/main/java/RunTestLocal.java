import utils.FileUtil;

import java.io.*;
import java.util.*;

public class RunTestLocal {
    private static Set<String> nodelayThreads = new HashSet<>();
    public static void main(String[] args) {
//        String command = "adb shell am instrument -w -r --no-window-animation -e debug false -e class 'de.test.antennapod.ui.MainActivityTest#testAddFeed' de.test.antennapod/androidx.test.runner.AndroidJUnitRunner";
        initNoDelayThreads();
        long beginApp = System.currentTimeMillis();
        String cmd1 = "adb shell am instrument -w -r -e debug false -e class '";
        String cmd2 = "' ";
        //String cmd3 = "/androidx.test.runner.AndroidJUnitRunner";
        String command = cmd1 + args[0] + cmd2 + args[1];
        String filePath = "dconfig.properties";
        runTest(command);
        //String pullSrc = "/storage/emulated/0/delay/dconfig.properties";
        String pullSrc = "/data/user/0/" + args[2] + "/files/dconfig.properties";
        String pullTgt = "D:/Javawork/AndroidAppInstrument";
        //String pullTgt = "/home/dongzhen/AndroidAppInstrument";
        pullFile(pullSrc,pullTgt);
        sleepAfterAdb();
        Properties properties = getProperties(filePath);
        List<Map<String,String>> allConfigs = getAllAppConfigs(properties);
        FileUtil.filePrintln("output.txt","The number of tasks in app: "+properties.stringPropertyNames().size());
        FileUtil.filePrintln("output.txt","The number of threads in app: "+getThreadNum(properties));
        FileUtil.filePrintln("output.txt","The number of delay combinations in app: "+allConfigs.size());
        for(Map<String,String> config : allConfigs){
            //System.out.println(config);
            for(String key : config.keySet()){
                properties.setProperty(key, config.get(key));
            }
            writeProperties(properties,filePath);
            String pushSrc = "D:/Javawork/AndroidAppInstrument/dconfig.properties";
            //String pushSrc = "/home/dongzhen/AndroidAppInstrument/dconfig.properties";
            //String pushTgt = "/storage/emulated/0/delay";
            String pushTgt = "/data/user/0/" + args[2] + "/files";
            pushFile(pushSrc,pushTgt);
            sleepAfterAdb();
            for(String key : config.keySet()){
                String delay = config.get(key);
                if(!delay.equals("null")){
                    String content = key + ": " + delay + "ms";
                    System.out.println(content);
                    FileUtil.filePrintln("output.txt",content);
                }
            }
            runTest(command);
        }
        long endApp = System.currentTimeMillis();
        FileUtil.filePrintln("output.txt","Time of delay combinations in app: "+(endApp-beginApp)+"ms");
    }

    public static void initNoDelayThreads(){
        nodelayThreads.add("main");
        nodelayThreads.add("SharedPreferencesImpl-load");
        nodelayThreads.add("queued-work-looper");
        nodelayThreads.add("Instr: androidx.test.runner.AndroidJUnitRunner");
    }

    private static int getThreadNum(Properties properties){
        Set<String> keys = new HashSet<>();
        Set<String> names = properties.stringPropertyNames();
        for(String name : names){
            String[] strings = name.split("\\+");
            String threadName = strings[0];
            keys.add(threadName);
        }
        return keys.size();
    }

    private static void pullFile(String sourceFile, String targetDir){
        try {
            String pullCmd = "adb pull " + sourceFile + " " + targetDir;
            //Process p = Runtime.getRuntime().exec("adb pull /storage/emulated/0/delay/dconfig.properties D:/Javawork/AndroidAppInstrument");
            Process p = Runtime.getRuntime().exec(pullCmd);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader errReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line = "";
            while((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            while((line = errReader.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();

        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private static void pushFile(String sourceFile, String targetDir){
        try {
            String pushCmd = "adb push " + sourceFile + " " + targetDir;
            //Process p = Runtime.getRuntime().exec("adb push D:/Javawork/AndroidAppInstrument/dconfig.properties /storage/emulated/0/delay");
            Process p = Runtime.getRuntime().exec(pushCmd);

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader errReader =
                    new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line = "";
            while((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            while((line = errReader.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();

        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    public static Properties getProperties(String filePath){
        Properties outProperties = new Properties();
        //String filePath = "dconfig.properties";
        File file = new File(filePath);
        try {
            outProperties.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outProperties;
    }

    private static String get(Properties properties, String key){
        return properties.getProperty(key);
    }

//    private static void set(Properties properties, String key, String value){
//        String filePath = "dconfig.properties";
//        properties.setProperty(key, value);
//        FileOutputStream oFile = null;
//        try {
//            oFile = new FileOutputStream(filePath);
//            properties.store(oFile,"The new properties file that add thread info");
//            oFile.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private static void writeProperties(Properties properties, String filePath){
        try{
            FileOutputStream oFile = new FileOutputStream(filePath);
            properties.store(oFile,"The new properties file that add thread info");
            oFile.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static List<Map<String,String>> getAllAppConfigs(Properties properties){
        List<Map<String, String>> configList = new ArrayList<>();
        Set<String> keys = properties.stringPropertyNames();
        List<String> names = new ArrayList<>();
        for(String key : keys){
            names.add(key);
        }

        //delay one thread
        for(int i = 0; i < names.size(); i++){
            Map<String, String> oneDelay = new HashMap<>();
            boolean flag = false;
            for(String key : keys){
                if(key.equals(names.get(i)) && (!isNoDelay(key))){
                    oneDelay.put(key, "100");
                    flag = true;
                }else{
                    oneDelay.put(key, "null");
                }
            }
            if(flag)
                configList.add(oneDelay);
        }

        //delay two threads
        for(int i = 0; i < names.size(); i++){
            for(int j = i + 1; j < names.size(); j++){
                Map<String, String> twoDelays = new HashMap<>();
                boolean flag1 = false;
                boolean flag2 = false;
                for(String key : keys){
                    if(key.equals(names.get(i)) && (!isNoDelay(key))){
                        twoDelays.put(key, "100");
                        flag1 = true;
                    }else if(key.equals(names.get(j)) && (!isNoDelay(key))){
                        twoDelays.put(key, "100");
                        flag2 = true;
                    }else{
                        twoDelays.put(key, "null");
                    }
                }
                if(flag1 && flag2)
                    configList.add(twoDelays);
            }
        }
        return configList;
    }

    private static boolean isNoDelay(String key){
        String[] strings = key.split("\\+");
        String threadName = strings[0];
        if(nodelayThreads.contains(threadName)){
            return true;
        }
        return false;
    }

    private static void runTest(String command){
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

    private static void sleepAfterAdb(){
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
