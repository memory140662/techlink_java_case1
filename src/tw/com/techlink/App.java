package tw.com.techlink;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class App {

    public static void main(String[] args) throws IOException, InterruptedException {

        Map<String, Object> config = getConfigMap();
        if (config != null) {
            String action = (String) ((config.get("action") != null)? config.get("action"): "ALL");
            // 更換
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("REP")) {
                String srcDir = (String) ((config.get("srcDir") != null) ? config.get("srcDir") : "./");
                String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                List<String> targets = (config.get("targets") != null) ? (List<String>) config.get("targets") : new ArrayList<String>();
                List<Map<String, String>> reaplce = (config.get("replace") != null) ? (List<Map<String, String>>) config.get("replace") : new ArrayList<Map<String, String>>();
                new ReplaceUtil(reaplce, targets, srcDir, outputDir).start();
            }

            // 登入
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("LOGIN")) {
                String username = (String) config.get("username");
                String password = (String) config.get("password");
                String server = (String) config.get("server");
                execLogin(username, password, server);
            }

            // Publish
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("PUBLISH")) {
                String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                String name = (String) config.get("name");
                String dbUsername = (String) config.get("dbUsername");
                String dbPassword = (String) config.get("dbPassword");
                List<String> targets = (config.get("targets") != null) ? (List<String>) config.get("targets") : new ArrayList<String>();
                File file = new File(outputDir);
                for (String target: targets) {
                    execPublish(file, target, name, dbUsername, dbPassword);
                }
            }
        }



    }


    private static void execPublish(File file, String type, String name, String dbUsername, String dbPassword) {
        for (File f: file.listFiles()) {
            if (f.isDirectory()) {
                execPublish(f, type, name, dbUsername, dbPassword);
            } else {
                if (f.getName().endsWith(type)) {
                    try {
                        String cmd = String.format("tabcmd publish \"%s\" %s %s",
                                f.getAbsoluteFile().toString(),
                                (name != null && name.trim().length() > 0) ? "-n \"" + name: "\"",
                                (dbUsername!= null && dbUsername.trim().length() > 0) ? "--db-username \"" + dbUsername + "\"": "",
                                (dbPassword!= null && dbPassword.trim().length() > 0) ? "--db-password \"" + dbPassword + "\"": "");
                        System.out.println(cmd);
                        execCmd(cmd);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
    }

    private static void execLogin(String username, String password, String server){
        try {
            String cmd = String.format("tabcmd login -s %s -u %s -p %s", server, username, password);
            System.out.println(cmd);
            execCmd(cmd);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void execCmd(String cmd) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        process.waitFor();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (br.ready()) {
            System.out.println(br.readLine());
        }
    }

    private static Map<String, Object> getConfigMap(){
        try {
            File c = new File("config.json");
            if (c.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("config.json")));
                StringBuffer json = new StringBuffer();
                while (br.ready()) {
                    json.append(br.readLine()).append("\n");
                }
                Gson gson = new Gson();
                return gson.fromJson(json.toString(), Map.class);
            } else {
                throw new Exception("'config.json' is not found.");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }



}
