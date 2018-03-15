package tw.com.techlink;

import org.jdom2.JDOMException;
import tableausoftware.documentation.api.rest.bindings.TableauCredentialsType;
import tableausoftware.documentation.api.rest.util.RestApiUtils;

import java.io.*;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class App {

    private static RestApiUtils restApiUtils = null;
    private static String OUTPUT_DIR = "outputDir";
    private static final String[] REPLACE_ATTR_NAME = {"targetAttr", "targetNewValue", "targetOldValue"};
    private static final Map<String, String> CONFIG_KEY_NAME = new HashMap<>();
    private static final String ANSI = "Cp1252";
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
        CONFIG_KEY_NAME.put("-t", "site");
        CONFIG_KEY_NAME.put("-d", "delete");
    }
    /**
     * -sd srcDir -od outputDir -tp tabcmdPath
     * -pn projectName -u username -p password
     * -s server -du dbUsername -dp dbPassword
     * -r replace
     * @param args
     * @throws UnsupportedEncodingException 
     * @throws IOException
     * @throws InterruptedException
     * @throws JDOMException
     */
    public static void main(String[] args) {
        int result = 0;
        Map<String, Object> config = null;
        try {
            config = getConfig(args);
            System.out.println("version: 2018/03/15 start.");
            result = start(config);
        } catch(Exception e) {
            result = 1;
            e.printStackTrace();
        } finally {
            if ("t".equals(config.get("delete"))) {
                if (config != null && ("ALL".equalsIgnoreCase((String) config.get("action")) || "PUBLISH".equalsIgnoreCase((String) config.get("action")))) {
                    delete(config);
                }
            }
            System.out.println("Process finished with exit code " + result);
            System.exit(result);
        }
    }

    private static void delete(Map<String, Object> config) {
        final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
        if (outputDir.endsWith(".twb") || outputDir.endsWith(".twbx")
                || outputDir.endsWith(".tds") || outputDir.endsWith(".tdsx") ||outputDir.endsWith(".zip")) {
            if (ReplaceUtil.deleteFile(new File(outputDir).getParentFile())) {
                if (!new File(outputDir).getParentFile().delete()) {
                    delete(config);
                }
            }
        } else {
            if (ReplaceUtil.deleteFile(new File(outputDir))) {
                if (!new File(outputDir).delete()) {
                    delete(config);
                }
            }
        }

    }

    private static Map<String, Object> getConfig(String[] args) throws UnsupportedEncodingException {
        String[] argsC = new String[args.length];
        for (int index = 0; index < args.length; index++) {
            argsC[index] = new String(args[index]
                    .replace("\\", "\\\\").getBytes("MS950"));
        }
        args = argsC
        ;Map<String, Object> config = new HashMap<>();
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
                        }
                        else {
                            config.put(key, args[index]);
                        }
                    }
                }
            }
        }
        
        if (config.get("site") == null) {
        	config.put("delete", "");
        }
        
        if (config.get("outputDir") == null) {
            config.put("outputDir", System.getProperty("java.io.tmpdir") + "/" + OUTPUT_DIR);
        }
        if (config.get("delete") == null ) {
            config.put("delete", "t");
        }
        return config;
    }

    private static int start(Map<String, Object> config) throws Exception {
        int result = 0;
        if (config != null) {
            String action = (config.get("action") != null) ? (String) config.get("action") : "ALL";
            System.out.println("***************************************");
            System.setProperty("file.encoding", "UTF-8");
            // 更換
            if (action.equalsIgnoreCase("ALL") ||
                    action.equalsIgnoreCase("REP") ||
                    action.equalsIgnoreCase("REPLACE")) {
                System.out.println("進行更換作業。");
                final String srcDir = (String) ((config.get("srcDir") != null) ? config.get("srcDir") : "./");
                final String outputDir = (String) ((config.get("outputDir") != null) ? config.get("outputDir") : "./output");
                final List<String> targets = Arrays.asList("tds", "tdsx", "twb", "twbx");
                final List<Map<String, String>> replace = (config.get("replace") != null) ? (List<Map<String, String>>) config.get("replace") : new ArrayList<Map<String, String>>();
                result = new ReplaceUtil(replace, targets, srcDir, outputDir).start();
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
                final String site = (String) config.get("site");
                result = execLogin(username, password, server, tabcmdPath, site);
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
                final String site = (String) config.get("site");
                System.out.println("RestApiUtils init.");
                restApiUtils = RestApiUtils.getInstance(server);
                System.out.println("RestApiUtils init success.");
                TableauCredentialsType credential = restApiUtils.invokeSignIn(username, password, site);
                for (String target: targets) {
                    result = execPublish(file, target, dbUsername, dbPassword, tabcmdPath, projectName, credential, server);
                }
                if (credential != null && credential.getToken() != null) {
                	restApiUtils.invokeSignOut(credential);
                }
                System.out.println("***************************************");
            }

        }
        return result;
    }


    private static int execPublish(File file, String type, String dbUsername, String dbPassword, String tabcmdPath, String projectName, TableauCredentialsType credential, String server) throws Exception {
        int result = 0;
        String tabcmd = getTabcmd(tabcmdPath);
        System.out.println("tabcmd: " + tabcmd);
        if (credential.getSite() == null) throw new RuntimeException("Site取得失敗。");
        for (File f: file.listFiles()) {
            if (f == null) continue;
            if (f.isDirectory()) {
                result = execPublish(f, type, dbUsername, dbPassword, tabcmdPath, projectName, credential, server);
            } else {

                if (f.getName().endsWith(type)) {
                	if (f.getName().endsWith(".twbx") || f.getName().endsWith(".twb") || f.getName().endsWith(".tds") || f.getName().endsWith(".tdsx")) {
                		TwbUtil.remap(f, f, credential.getSite().getId(), server);
                        TwbUtil.replaceAttrOrder(f);
                	}

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
                    if (result == 0) {
                        result = execCmd(cmd);
                    } else {
                        execCmd(cmd);
                    }
                }
            }
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

    private static int execLogin(String username, String password, String server, String tabcmdPath, String site) throws IOException, InterruptedException {
        String tabcmd = getTabcmd(tabcmdPath);
        System.out.println("tabcmd: " + tabcmd);
        String cmd = String.format("\"%s\" login -s %s -u %s -p %s %s",
                tabcmd, server, username, password,
                (site != null && site.trim().length() > 0) ? "-t " + site : ""
            );
        System.out.println(String.format("\"%s\" login -s %s -u %s %s",
                tabcmd, server, username, (site != null && site.trim().length() > 0) ? "-t " + site : ""));
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
        BufferedReader ebr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while (ebr.ready()) {
            System.err.println(ebr.readLine());
            res = 1;
        }
        return res;
    }
}
