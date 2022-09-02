package utils;

import java.io.*;

public class FileUtil {
    public static void filePrintln(String filePath, String content){
        try{
//            File file = new File("output.txt");
            File file = new File(filePath);
            if(!file.exists()){
//                file.createNewFile();
                File parent = file.getParentFile();
                if(parent != null){
                    if(!parent.exists()){
                        parent.mkdirs();
                    }
                }
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file,true);
            OutputStreamWriter writer = new OutputStreamWriter(fos,"UTF-8");
            //FileWriter writer = new FileWriter(file,true);
            BufferedWriter out=new BufferedWriter(writer);
            out.write(content);
            out.newLine();
            out.flush();
            out.close();
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createFile(String filePath){
        File file = new File(filePath);
        if(!file.exists()){
            File parent = file.getParentFile();
            if(parent != null){
                if(!parent.exists()){
                    parent.mkdirs();
                }
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
