import utils.FileUtil;

import java.io.*;
import java.util.*;

public class RunTest {
    private static Set<String> nodelayThreads = new HashSet<>();
    private static String output;
    private static String delayValue = "100";
    private static String delayZero = "0";
    private static String delayNull = "null";
    public static void main(String[] args) {
//        String command = "adb shell am instrument -w -r --no-window-animation -e debug false -e class 'de.test.antennapod.ui.MainActivityTest#testAddFeed' de.test.antennapod/androidx.test.runner.AndroidJUnitRunner";
        initNoDelayThreads();
        String appPackage = args[2];
        output = appPackage + ".txt";
        long beginApp = System.currentTimeMillis();
        String cmd1 = "adb -s emulator-5554 shell am instrument -w -r -e debug false -e class '";
        String cmd2 = "' ";
        //String cmd3 = "/androidx.test.runner.AndroidJUnitRunner";
        String command = cmd1 + args[0] + cmd2 + args[1];
        String filePath = "dconfig.properties";
        runTest(command);
        //String pullSrc = "/storage/emulated/0/delay/dconfig.properties";
        String pullSrc = "/data/user/0/" + args[2] + "/files/dconfig.properties";
//        String pullTgt = "D:/Javawork/AndroidAppInstrument";
        String pullTgt = "/home/dongzhen/AndroidAppInstrument";
        pullFile(pullSrc,pullTgt);
        sleepAfterAdb();
        Properties properties = getProperties(filePath);
        List<Map<String,String>> allConfigs = getAllAppConfigs(properties);
        FileUtil.filePrintln(output,"The number of tasks in app: "+properties.stringPropertyNames().size());
        FileUtil.filePrintln(output,"The number of threads in app: "+getThreadNum(properties));
        FileUtil.filePrintln(output,"The number of delay combinations in app: "+allConfigs.size());

        String pullFsrc = "/data/user/0/" + appPackage + "/" + "files/config.properties";
        String pullFtgt = "/home/dongzhen/AndroidAppInstrument";
        pullFile(pullFsrc,pullFtgt);
        sleepAfterAdb();
        String frameworkDelays = "config.properties";
        Properties fProperties = getProperties(frameworkDelays);
        List<Map<String,String>> fConfigs = getAllFrameworkConfigs(fProperties);
        FileUtil.filePrintln(output,"The number of tasks in framework: "+fProperties.stringPropertyNames().size());
        FileUtil.filePrintln(output,"The number of threads in framework: "+getThreadNum(fProperties));
        FileUtil.filePrintln(output,"The number of delay combinations in framework: "+fConfigs.size());

        List<DelayPair> faConfigs = getAllFAConfigs(properties,fProperties);
        FileUtil.filePrintln(output,"The number of delay combinations in app and framework: " + faConfigs.size());

        int[] num = getAllThreadTaskNum(properties,fProperties);
        FileUtil.filePrintln(output,"The number of all threads: "+num[0]);
        FileUtil.filePrintln(output, "The number of all tasks: "+num[1]);
        FileUtil.filePrintln(output, "The number of all delay combinations: "+(allConfigs.size()+fConfigs.size()+faConfigs.size()));

        for(Map<String,String> config : allConfigs){
            //System.out.println(config);
            for(String key : config.keySet()){
                properties.setProperty(key, config.get(key));
            }
            writeProperties(properties,filePath);
//            String pushSrc = "D:/Javawork/AndroidAppInstrument/dconfig.properties";
            String pushSrc = "/home/dongzhen/AndroidAppInstrument/dconfig.properties";
            //String pushTgt = "/storage/emulated/0/delay";
            String pushTgt = "/data/user/0/" + args[2] + "/files";
            pushFile(pushSrc,pushTgt);
            sleepAfterAdb();
            for(String key : config.keySet()){
                String delay = config.get(key);
                if(!delay.equals(delayNull)){
                    String content = key + ": " + delay + "ms";
                    System.out.println(content);
                    FileUtil.filePrintln(output,content);
                    break;
                }
            }
            runTest(command);
        }
        long endApp = System.currentTimeMillis();
        FileUtil.filePrintln(output,"Time of delay combinations in app: "+(endApp-beginApp)+"ms");

        //delay threads in framework
        resetProperties(appPackage,properties,filePath);
        FileUtil.filePrintln(output,"Start to delay threads in framework!");
//        String pullFsrc = "/data/user/0/" + appPackage + "/" + "files/config.properties";
//        String pullFtgt = "/home/dongzhen/AndroidAppInstrument";
//        pullFile(pullFsrc,pullFtgt);
//        sleepAfterAdb();
//        String frameworkDelays = "config.properties";
//        Properties fProperties = getProperties(frameworkDelays);
//        List<Map<String,String>> fConfigs = getAllFrameworkConfigs(fProperties);
//        FileUtil.filePrintln("output.txt","The number of tasks in framework: "+fProperties.stringPropertyNames().size());
//        FileUtil.filePrintln("output.txt","The number of threads in framework: "+getThreadNum(fProperties));
//        FileUtil.filePrintln("output.txt","The number of delay combinations in framework: "+fConfigs.size());
        for(Map<String,String> fconfig : fConfigs){
            //System.out.println(fconfig);
            for(String key : fconfig.keySet()){
                fProperties.setProperty(key,fconfig.get(key));
            }
            writeProperties(fProperties,frameworkDelays);
            String pushFsrc = "/home/dongzhen/AndroidAppInstrument/config.properties";
            String pushFtgt = "/data/user/0/" + appPackage + "/" + "files";
            pushFile(pushFsrc,pushFtgt);
            sleepAfterAdb();
            for(String key : fconfig.keySet()){
                String delay = fconfig.get(key);
                if(!delay.equals(delayZero)){
                    String content = key + ": " + delay + "ms";
                    System.out.println(content);
                    FileUtil.filePrintln(output,content);
                    break;
                }
            }
            runTest(command);
        }

        //delay threads in app and framework
        FileUtil.filePrintln(output,"Start to delay threads in app and framework!");
        for(DelayPair delayPair : faConfigs){
            Map<String,String> appConfig = delayPair.appConfig;
            Map<String,String> frameworkConfig = delayPair.frameworkConfig;
            for(String key : appConfig.keySet()){
                String appDelay = appConfig.get(key);
                properties.setProperty(key, appDelay);
                if(!appDelay.equals(delayNull)){
                    String content = key + ": " + appDelay + "ms";
                    System.out.println(content);
                    FileUtil.filePrintln(output,content);
                }
            }
            writeProperties(properties,filePath);
            String pushSrc = "/home/dongzhen/AndroidAppInstrument/dconfig.properties";
            String pushTgt = "/data/user/0/" + args[2] + "/files";
            pushFile(pushSrc,pushTgt);
            sleepAfterAdb();

            for(String key : frameworkConfig.keySet()){
                String frameworkDelay = frameworkConfig.get(key);
                fProperties.setProperty(key, frameworkDelay);
                if(!frameworkDelay.equals(delayZero)){
                    String content = key + ": " + frameworkDelay + "ms";
                    System.out.println(content);
                    FileUtil.filePrintln(output,content);
                }
            }
            writeProperties(fProperties,frameworkDelays);
            String pushFsrc = "/home/dongzhen/AndroidAppInstrument/config.properties";
            String pushFtgt = "/data/user/0/" + appPackage + "/" + "files";
            pushFile(pushFsrc,pushFtgt);
            sleepAfterAdb();
            runTest(command);
        }

        long endFramework = System.currentTimeMillis();
        FileUtil.filePrintln(output,"Time of delay combinations in framework: "+(endFramework-endApp)+"ms");
        FileUtil.filePrintln(output,"Total time: "+(endFramework-beginApp)+"ms");
    }

    public static void initNoDelayThreads(){
        nodelayThreads.add("main");
        nodelayThreads.add("SharedPreferencesImpl-load");
        nodelayThreads.add("queued-work-looper");
        nodelayThreads.add("Instr: androidx.test.runner.AndroidJUnitRunner");
        nodelayThreads.add("Instr: android.support.test.runner.AndroidJUnitRunner");
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

    private static int[] getAllThreadTaskNum(Properties aProperties, Properties fProperties){
        int[] num = new int[2];
        Set<String> threads = new HashSet<>();
        Set<String> tasks = new HashSet<>();
        Set<String> aNames = aProperties.stringPropertyNames();
        Set<String> fNames = fProperties.stringPropertyNames();
        for(String aName : aNames){
            String[] strings = aName.split("\\+");
            String threadName = strings[0];
            if(!nodelayThreads.contains(threadName)){
                threads.add(threadName);
                tasks.add(aName);
            }
        }
        for(String fName : fNames){
            String[] strings = fName.split("\\+");
            String threadName = strings[0];
            if(!nodelayThreads.contains(threadName)){
                threads.add(threadName);
                tasks.add(fName);
            }
        }
        num[0] = threads.size();
        num[1] = tasks.size();
        return num;
    }

    private static void pullFile(String sourceFile, String targetDir){
        try {
            String pullCmd = "adb -s emulator-5554 pull " + sourceFile + " " + targetDir;
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
            String pushCmd = "adb -s emulator-5554 push " + sourceFile + " " + targetDir;
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
            FileInputStream fileInputStream = new FileInputStream(file);
            outProperties.load(new InputStreamReader(fileInputStream,"UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outProperties;
    }

    /**
     * reset all delays to null
     * @param properties
     * @param filePath
     */
    private static void resetProperties(String pack, Properties properties,String filePath){
        for(String key : properties.stringPropertyNames()){
            properties.setProperty(key, delayNull);
        }
        writeProperties(properties,filePath);
        String sourceFile = "/home/dongzhen/AndroidAppInstrument/dconfig.properties";
        //String targetDir = "/storage/emulated/0/delay";
        String targetDir = "/data/user/0/" + pack + "/files";
        pushFile(sourceFile,targetDir);
        sleepAfterAdb();
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
            properties.store(new OutputStreamWriter(oFile,"UTF-8"),"The new properties file that add thread info");
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
                    oneDelay.put(key, delayValue);
                    flag = true;
                }else{
                    oneDelay.put(key, delayNull);
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
                        twoDelays.put(key, delayValue);
                        flag1 = true;
                    }else if(key.equals(names.get(j)) && (!isNoDelay(key))){
                        twoDelays.put(key, delayValue);
                        flag2 = true;
                    }else{
                        twoDelays.put(key, delayNull);
                    }
                }
                if(flag1 && flag2)
                    configList.add(twoDelays);
            }
        }
        return configList;
    }

    private static List<DelayPair> getAllFAConfigs(Properties aProperties, Properties fProperties){
        List<DelayPair> faConfigs = new ArrayList<>();
        Set<String> aKeys = aProperties.stringPropertyNames();
        Set<String> fKeys = fProperties.stringPropertyNames();
        List<String> aNames = new ArrayList<>();
        List<String> fNames = new ArrayList<>();
        for(String aKey : aKeys){
            aNames.add(aKey);
        }
        for(String fKey : fKeys){
            fNames.add(fKey);
        }
        for(int i = 0; i < aNames.size(); i++){
            for(int j = 0; j < fNames.size(); j++){
                if((!isNoDelay(aNames.get(i))) && (!isNoDelay(fNames.get(j)))){
                    Map<String, String> aConfig = new HashMap<>();
                    Map<String, String> fConfig = new HashMap<>();
                    for(String akey : aKeys){
                        if(akey.equals(aNames.get(i))){
                            aConfig.put(akey,delayValue);
                        }else{
                            aConfig.put(akey, delayNull);
                        }
                    }
                    for(String fKey : fKeys){
                        if(fKey.equals(fNames.get(j))){
                            fConfig.put(fKey,delayValue);
                        }else{
                            fConfig.put(fKey,delayZero);
                        }
                    }
                    DelayPair delayPair = new DelayPair();
                    delayPair.appConfig = aConfig;
                    delayPair.frameworkConfig = fConfig;
                    faConfigs.add(delayPair);
                }
            }
        }
        return faConfigs;
    }

    private static boolean isNoDelay(String key){
        String[] strings = key.split("\\+");
        String threadName = strings[0];
        if(nodelayThreads.contains(threadName)){
            return true;
        }
        return false;
    }

    private static List<Map<String,String>> getAllFrameworkConfigs(Properties properties){
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
                    oneDelay.put(key, delayValue);
                    flag = true;
                }else{
                    oneDelay.put(key, delayZero);
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
                        twoDelays.put(key, delayValue);
                        flag1 = true;
                    }else if(key.equals(names.get(j)) && (!isNoDelay(key))){
                        twoDelays.put(key, delayValue);
                        flag2 = true;
                    }else{
                        twoDelays.put(key, delayZero);
                    }
                }
                if(flag1 && flag2)
                    configList.add(twoDelays);
            }
        }
        return configList;
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
                FileUtil.filePrintln(output,line);
            }
            while((line = errReader.readLine()) != null) {
                System.out.println(line);
                FileUtil.filePrintln(output,line);
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
