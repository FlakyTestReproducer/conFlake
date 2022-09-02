package utils;

import java.io.*;
import java.net.Inet4Address;
import java.util.Properties;

public class LoadProperties {
    private static Properties properties;

    private LoadProperties() {
    }

    public static Properties getInstance() {
        if (properties != null)
            return properties;
        try {
            properties = new Properties();
            properties.load(LoadProperties.class.getResourceAsStream("/config.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void set(String key, String value){
        try{
            Properties outProperties = new Properties();
            String filePath = "src/main/resources/config.properties";
            File file = new File(filePath);
            outProperties.load(new FileInputStream(file));
            outProperties.setProperty(key, value);
            FileOutputStream oFile = new FileOutputStream(filePath);
            outProperties.store(oFile,"The new properties file that add thread info");
            oFile.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String get(String key){
        if(properties != null)
            return properties.getProperty(key);
        else
            return getInstance().getProperty(key);
    }
}
