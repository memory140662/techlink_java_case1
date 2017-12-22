package tw.com.techlink;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class ReplaceUtil {
    private List<Map<String, String>> replace;
    private List<String> targets;
    private String srcDir;
    private String outputDir;
    private String encode;

    public ReplaceUtil(List<Map<String, String>> replace, List<String> targets, String srcDir, String outputDir) {
        this.replace = replace;
        this.targets = targets;
        this.srcDir = srcDir;
        this.outputDir = outputDir;
        this.encode = "UTF-8";
    }

    public ReplaceUtil(List<Map<String, String>> replace, List<String> targets, String srcDir, String outputDir, String encode) {
        this.replace = replace;
        this.targets = targets;
        this.srcDir = srcDir;
        this.outputDir = outputDir;
        this.encode = encode;
    }

    private void replaceAttr(List<Map<String, String>> replace, File dist, File src) {
        System.out.println("進件檔案：" + src.getAbsoluteFile().toString());
        try {
            File f = new File(src.getAbsoluteFile().toString());
            if (f.isFile()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(src.getAbsoluteFile().toString()), encode));
                File outputFile = null;
                if (dist.getName().endsWith(".twb") || dist.getName().endsWith(".twbx")
                        || dist.getName().endsWith(".tds") || dist.getName().endsWith(".tdsx")) {
                    outputFile = dist;
                } else {
                    outputFile = new File(dist + "/" + f.getName());
                }
                FileWriter fw = new FileWriter(outputFile);
                while (br.ready()) {
                    fw.append(getReplaceString(replace, br.readLine())).append("\n");
                }
                fw.close();
                br.close();
                System.out.println("輸出檔案：" + outputFile.getAbsoluteFile());
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void makeOutputDir(File dist) {
        if (!dist.getName().endsWith(".tds") && !dist.getName().endsWith(".tdsx")
                && !dist.getName().endsWith(".twb") && !dist.getName().endsWith(".twbx") || dist.isDirectory()) {
            if (!dist.exists()) {
                dist.mkdir();
            } else {
                if (deleteFile(dist)) {
                    dist.delete();
                    dist.mkdir();
                }
            }
        }
    }


    private void replaceAttrWithZip(File src, File dist, List<Map<String, String>> replace) throws IOException {
        ZipFile zipFile = new ZipFile(src.getAbsoluteFile().toString());
        System.out.println("進件檔案(壓縮檔)：" + src.getAbsoluteFile().toString());
        File outputFile;
        if (dist.getName().endsWith(".twb") || dist.getName().endsWith(".twbx")
                || dist.getName().endsWith(".tds") || dist.getName().endsWith(".tdsx") || dist.getName().endsWith(".zip")) {
            outputFile = dist;
            dist.getParentFile().mkdirs();
        } else {
            outputFile = new File(dist.getPath() + "/" + src.getName());
        }
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        } else {
            outputFile.delete();
            outputFile.createNewFile();
        }
        ZipOutputStream zos = new ZipOutputStream(outputFile);
        Enumeration enumeration = zipFile.getEntries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) enumeration.nextElement();
            if (entry.getName().contains("MACOS")) {
                continue;
            }
            if (!entry.isDirectory()) {
                File file1 = new File(dist.getPath() + "/" + entry.getName());
                zos.putNextEntry(new ZipEntry(file1.getName()));
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), encode));
                while (br.ready()) {
                    String line = br.readLine();
                    line = getReplaceString(replace, line).concat("\n");
                    zos.write(line.getBytes(encode));
                }
                br.close();
            }
        }
        zos.close();
        System.out.println("輸出檔案(壓縮檔)：" + outputFile.getAbsoluteFile());
    }

    private String getReplaceString(List<Map<String, String>> replace, String line) {
        for (Map<String, String> rep: replace) {
            String targetAttr = (rep.get("targetAttr") != null) ? rep.get("targetAttr").toString(): "";
            String targetNewValue = (rep.get("targetNewValue") != null) ? rep.get("targetNewValue").toString(): "";
            String targetOldValue = (rep.get("targetOldValue") != null) ? rep.get("targetOldValue").toString(): "";
            if (line.contains(String.format("%s=", targetAttr)) && targetAttr.trim().length() > 0 && targetNewValue.trim().length() > 0) {
                String[] lineChilds = line.split(" ");
                StringBuffer tmp = new StringBuffer();
                for (int index = 0; index < lineChilds.length; index++) {
                    String childs = lineChilds[index];
                    if (childs.contains(String.format("%s=", targetAttr))) {
                        System.out.print(childs);
                        System.out.print(" -> ");
                        if (targetOldValue != null && targetOldValue.trim().length() > 0) {
                            childs = childs.replace(targetOldValue, targetNewValue);
                        } else {
                            childs = childs.replaceAll("(['\"]+).*(['\"]+)", String.format("$1%s$2", targetNewValue));
                            childs = childs.replaceAll("(&apos;+).*(&apos;+)", String.format("$1%s$2", targetNewValue));
                            childs = childs.replaceAll("(&quot;+).*(&quot;+)", String.format("$1%s$2", targetNewValue));
                        }
                        System.out.println(childs);
                    }
                    tmp.append(childs).append(" ");
                }
                line = tmp.toString();
            }
        }
        return line;
    }

    public static boolean deleteFile(File file) {
        boolean flag = false;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    if (deleteFile(f)) {
                        flag = f.delete();
                    }
                } else {
                    flag = f.delete();
                }
            }
        } else {
            flag = file.delete();
        }
        return flag;
    }


    private static class MyFilter implements FilenameFilter {

        private List<String> target;

        public MyFilter(List<String> target) {
            this.target = target;
        }

        @Override
        public boolean accept(File dir, String name) {
            if (dir == null || name == null) return false;
            if (name.lastIndexOf(".") == -1) return false;
            if (target.size() == 0) return false;
            return target.contains(name.substring(name.lastIndexOf(".") + 1));
        }
    }

    public int start() {
        File dist = new File(outputDir);
        File file = new File(srcDir);
        System.out.println("來源目錄: " + file.getAbsolutePath());
        if (!file.exists()) {
            System.out.println("來源目錄不存在!");
            return 1;
        }
        System.out.println("輸出目錄: " + dist.getAbsolutePath());
        System.out.println("***************************************");
        makeOutputDir(dist);
        if (file.isDirectory()) {
            for (File f : file.listFiles(new MyFilter(targets))) {
                if (execReplace(dist, f)) return 1;
                System.out.println("***************************************");
            }
        } else {
            if (!file.getName().endsWith(".tds") && !file.getName().endsWith(".tdsx")
                    && !file.getName().endsWith(".twb") && !file.getName().endsWith(".twbx") && !file.getName().endsWith(".zip")) {
                System.out.println("檔案格式錯誤.");
                throw new RuntimeException("檔案格式錯誤.");
            }
            if (execReplace(dist, file)) return 1;
        }
        return 0;
    }

    private boolean execReplace(File dist, File f) {
        System.out.println(f.getName());
        if (f.getName().endsWith(".tdsx") || f.getName().endsWith(".twbx") || f.getName().endsWith(".zip")) {
            try {
                replaceAttrWithZip(f, dist, replace);
            } catch (IOException e) {
                System.err.println("壓縮檔案異常：".concat(e.toString()));
                e.printStackTrace();
                return true;
            }
        } else {
            replaceAttr(replace, dist, f);
        }
        return false;
    }
}
