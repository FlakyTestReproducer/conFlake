import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) {
        RunTest.initNoDelayThreads();
        List<Map<String,String>> configs = RunTest.getAllAppConfigs(RunTest.getProperties("dconfig2.properties"));
        System.out.println(configs.size());
        for(Map<String,String> config : configs){
            System.out.println(config);
        }
    }
}
