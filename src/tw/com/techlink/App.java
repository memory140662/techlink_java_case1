package tw.com.techlink;

import org.jdom2.JDOMException;
import tableausoftware.documentation.api.rest.bindings.TableauCredentialsType;
import tableausoftware.documentation.api.rest.util.RestApiUtils;

import java.io.*;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class App {

    private static RestApiUtils restApiUtils = null;
    private static final String[] REPLACE_ATTR_NAME = {"targetAttr", "targetNewValue", "targetOldValue"};
    private static final Map<String, String> CONFIG_KEY_NAME = new HashMap<>();
    static {
        CONFIG_KEY_NAME.put("-sd", "srcDir");
        CONFIG_KEY_NAME.put("-od", "outputDir");
        CONFIG_KEY_NAME.put("-tp", "tabcmdPath");
        CONFIG_KEY_NAME.put("-pn", "projectName");
        CONFIG_KEY_NAME.put("-u", "username");
        CONFIG_KEY_NAME.put("-p", "password");
        CONFIG_KEY_NAME.put("-s", "server");
        CONFIG_KEY_NAME.put("-du", "dbUsername");
        CONFIG_KEY_NAME.put("-dp", "dbPassword");
        CONFIG_KEY_NAME.put("-r", "replace");
        CONFIG_KEY_NAME.put("-a", "action");
    }
    /**
     * -sd srcDir -od outputDir -tp tabcmdPath
     * -pn projectName -u username -p password
     * -s server -du dbUsername -dp dbPassword
     * -r replace
     * @param args
     * @throws IOException
     * @throws InterruptedException
     * @throws JDOMException
     */
    public static void main(String[] args) {
        int result = 0;
        try {
            Map<String, Object> config = getConfig(args);
            System.out.println("version: 2017/12/22 start.");
            result = start(config);
        } catch(Exception e) {
            result = 1;
            e.printStackTrace();
        } finally {
            delete(getConfig(args));
            System.out.println("Process finished with exit code " + result);
            System.exit(result);
        }
    }

    private static void delete(Map<String, Object> config) {
        final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
        ReplaceUtil.deleteFile(new File(outputDir));
    }

    private static Map<String, Object> getConfig(String[] args) {
        Map<String, Object> config = new HashMap<>();
        List<Map<String, String>> replaces = new ArrayList<>();
        String key = null;
        Map<String, String> replace = null;
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (arg.startsWith("-")) {
                key = CONFIG_KEY_NAME.get(arg);
                if (key == null) {
                    throw new RuntimeException("指定錯誤");
                }
                if (args.length > index + 1) {
                    if (!args[index + 1].startsWith("-")) {
                        ++ index;
                        if (arg.equals("-r")) {
                            replace = new HashMap<>();
                            for (int position = 0;index + position < args.length && !args[index + position].startsWith("-"); position ++) {
                                replace.put(REPLACE_ATTR_NAME[position % REPLACE_ATTR_NAME.length], args[index + position]);
                            }
                            replaces.add(replace);
                            config.put(key, replaces);
                        } else {
                            config.put(key, args[index]);
                        }
                    }
                }
            }
        }
        return config;
    }

    private static int start(Map<String, Object> config) throws JDOMException, IOException, InterruptedException {
        int result = 0;
        if (config != null) {
            String action = (config.get("action") != null) ? (String) config.get("action") : "ALL";
            System.out.println("***************************************");
            // 更換
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("REP") ||
                    action.equalsIgnoreCase("REPLACE")) {
                System.out.println("進行更換作業。");
                final String srcDir = (String) ((config.get("srcDir") != null) ? config.get("srcDir") : "./");
                final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                final List<String> targets = Arrays.asList("tds", "tdsx", "twb", "twbx");
                final List<Map<String, String>> replace = (config.get("replace") != null) ? (List<Map<String, String>>) config.get("replace") : new ArrayList<Map<String, String>>();
                new ReplaceUtil(replace, targets, srcDir, outputDir).start();
            }
            // 登入
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("LOGIN") ||
                    action.equalsIgnoreCase("PUBLISH")) {
                System.out.println("tabcmd login.");
                final String username = (String) config.get("username");
                final String password = (String) config.get("password");
                final String server = (String) config.get("server");
                final String tabcmdPath = (String) config.get("tabcmdPath");
                result = execLogin(username, password, server, tabcmdPath);
                System.out.println("***************************************");
            }

            // Publish
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("PUBLISH")) {
                System.out.println("tabcmd publish.");
                final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                final String username = (String) config.get("username");
                final String password = (String) config.get("password");
                final String dbUsername = (String) config.get("dbUsername");
                final String dbPassword = (String) config.get("dbPassword");
                final String server = (String) config.get("server");
                final List<String> targets = Arrays.asList("tds", "tdsx", "twb", "twbx");
                final String tabcmdPath = (String) config.get("tabcmdPath");
                final String projectName = (String) config.get("projectName");
                final File file = new File(outputDir);
                restApiUtils = RestApiUtils.getInstance(server);
                TableauCredentialsType credential = restApiUtils.invokeSignIn(username, password, null);
                for (String target: targets) {
                    result = execPublish(file, target, null, dbUsername, dbPassword, tabcmdPath, projectName, credential, server);
                }
                restApiUtils.invokeSignOut(credential);
                restApiUtils = null;
                System.out.println("***************************************");
            }

        }
        return result;
    }


    private static int execPublish(File file, String type, String name, String dbUsername, String dbPassword, String tabcmdPath, String projectName, TableauCredentialsType credential, String server) throws JDOMException, IOException, InterruptedException {
        int result = 1;
        for (File f: file.listFiles()) {
            if (f.isDirectory()) {
                execPublish(f, type, name, dbUsername, dbPassword, tabcmdPath, projectName, credential, server);
            } else {

                if (f.getName().endsWith(type)) {

                    if (type.endsWith("twb") || type.endsWith("twbx")) {
                        TwbUtil.remap(f, f, credential.getSite().getId(), server);
                    }

                    String tabcmd = getTabcmd(tabcmdPath);
                    System.out.println("tabcmd: " + tabcmd);
                    String cmd = String.format("\"%s\" publish  \"%s\" %s %s %s %s -o ",
                            tabcmd,
                            f.getAbsoluteFile().toString(),
                            "-n \"" + f.getName().substring(0, f.getName().lastIndexOf(".")) + "\"",
                            (projectName != null && projectName.trim().length() > 0) ? "-r \"" + projectName + "\"" : "",
                            (dbUsername!= null && dbUsername.trim().length() > 0) ? "--db-username \"" + dbUsername + "\"": "",
                            (dbPassword!= null && dbPassword.trim().length() > 0) ? "--db-password \"" + dbPassword + "\" -save-db-password": ""
                    );
                    System.out.println(String.format("\"%s\" publish  \"%s\" %s %s %s -o ",
                            tabcmd,
                            f.getAbsoluteFile().toString(),
                            "-n \"" + f.getName().substring(0, f.getName().lastIndexOf(".")) + "\"",
                            (projectName != null && projectName.trim().length() > 0) ? "-r \"" + projectName + "\"" : "",
                            (dbUsername!= null && dbUsername.trim().length() > 0) ? "--db-username \"" + dbUsername + "\"": ""
                    ));
                    result = execCmd(cmd);
                }
            }
            if (result == 1) return 1;
        }
        return result;
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

    private static int execLogin(String username, String password, String server, String tabcmdPath) throws IOException, InterruptedException {
        String tabcmd = getTabcmd(tabcmdPath);
        System.out.println("tabcmd: " + tabcmd);
        String cmd = String.format("\"%s\" login -s %s -u %s -p %s",
                tabcmd, server, username, password);
        System.out.println(String.format("\"%s\" login -s %s -u %s",
                tabcmd, server, username));
        return execCmd(cmd);
    }

    private static int execCmd(String cmd) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(cmd);
        int res = process.waitFor();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (br.ready()) {
            System.out.println(br.readLine());
        }
        return res;
    }

}
