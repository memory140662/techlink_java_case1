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
        System.out.println("version: 2017/12/07 start.");
        start(config);
    }

    private static void start(Map<String, Object> config) {
        if (config != null) {
            String action = (String) ((config.get("action") != null)? config.get("action"): "ALL");
            System.out.println("Action: " + action);
            // 更換
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("REP") ||
                    action.equalsIgnoreCase("REPLACE")) {
                System.out.println("進行更換作業。");
                final String srcDir = (String) ((config.get("srcDir") != null) ? config.get("srcDir") : "./");
                final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                final List<String> targets = (config.get("targets") != null) ? (List<String>) config.get("targets") : new ArrayList<String>();
                final List<Map<String, String>> reaplce = (config.get("replace") != null) ? (List<Map<String, String>>) config.get("replace") : new ArrayList<Map<String, String>>();
                new ReplaceUtil(reaplce, targets, srcDir, outputDir).start();
            }

            // 登入
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("LOGIN")) {
                System.out.println("tabcmd login.");
                final String username = (String) config.get("username");
                final String password = (String) config.get("password");
                final String server = (String) config.get("server");
                final String tabcmdPath = (String) config.get("tabcmdPath");
                execLogin(username, password, server, tabcmdPath);
            }

            // Publish
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("PUBLISH")) {
                System.out.println("tabcmd publish.");
                final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                final String name = (String) config.get("name");
                final String dbUsername = (String) config.get("dbUsername");
                final String dbPassword = (String) config.get("dbPassword");
                final List<String> targets = (config.get("targets") != null) ? (List<String>) config.get("targets") : new ArrayList<String>();
                final String tabcmdPath = (String) config.get("tabcmdPath");
                final String projectName = (String) config.get("projectName");
                final File file = new File(outputDir);
                for (String target: targets) {
                    execPublish(file, target, name, dbUsername, dbPassword, tabcmdPath, projectName);
                }
            }
        }
    }


    private static void execPublish(File file, String type, String name, String dbUsername, String dbPassword, String tabcmdPath, String projectName) {
        for (File f: file.listFiles()) {
            if (f.isDirectory()) {
                execPublish(f, type, name, dbUsername, dbPassword, tabcmdPath, projectName);
            } else {
                if (f.getName().endsWith(type)) {
                    try {
                        String tabcmd = getTabcmd(tabcmdPath);
                        System.out.println("tabcmd: " + tabcmd);
                        String cmd = String.format("%s publish  \"%s\" %s %s %s %s -o ",
                                tabcmd,
                                f.getAbsoluteFile().toString(),
                                (name != null && name.trim().length() > 0) ? "-n \"" + name + "\"" : "",
                                (projectName != null && projectName.trim().length() > 0) ? "-r \"" + projectName + "\"" : "",
                                (dbUsername!= null && dbUsername.trim().length() > 0) ? "--db-username \"" + dbUsername + "\"": "",
                                (dbPassword!= null && dbPassword.trim().length() > 0) ? "--db-password \"" + dbPassword + "\" -save-db-password": ""
                        );
                        execCmd(cmd);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
            }
        }
    }

    private static String getTabcmd(String tabcmdPath) {
        if (tabcmdPath != null && tabcmdPath.trim().length() > 0) {
            tabcmdPath = tabcmdPath.trim();
            if (!(tabcmdPath.endsWith("\\tabcmd") || tabcmdPath.endsWith("/tabcmd"))) {
                if (tabcmdPath.endsWith("\\") || tabcmdPath.endsWith("/")) {
                    tabcmdPath = tabcmdPath.trim().concat("tabcmd");
                } else {
                    tabcmdPath = tabcmdPath.trim().concat("/tabcmd");
                }
            }
        } else {
            return "tabcmd";
        }
        return tabcmdPath;
    }

    private static void execLogin(String username, String password, String server, String tabcmdPath){
        try {
            String tabcmd = getTabcmd(tabcmdPath);
            System.out.println("tabcmd: " + tabcmd);
            String cmd = String.format("%s login -s %s -u %s -p %s",
                    tabcmd, server, username, password);
            execCmd(cmd);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void execCmd(String cmd) throws IOException, InterruptedException {
        System.out.println(cmd);
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
