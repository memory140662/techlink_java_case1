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

    public ReplaceUtil(List<Map<String, String>> replace, List<String> targets, String srcDir, String outputDir) {
        this.replace = replace;
        this.targets = targets;
        this.srcDir = srcDir;
        this.outputDir = outputDir;
    }

    private void replaceAttr(List<Map<String, String>> replace, File dist, String fileName) {
        try {
            File f = new File(fileName);
            if (f.isFile()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                FileWriter fw = new FileWriter(dist + "/" + f.getName());
                while (br.ready()) {
                    fw.append(getReplaceString(replace, br.readLine())).append("\n");
                }
                fw.close();
                br.close();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void makeOutputDir(File dist) {
        if (!dist.exists()) {
            dist.mkdir();
        } else {
            if (deleteFile(dist)) {
                dist.mkdir();
            }
        }
    }


    private void replaceAttrWithZip(String fileName, File dist, List<Map<String, String>> replace) throws IOException {
        ZipFile zipFile = new ZipFile(fileName);
        ZipOutputStream zos = new ZipOutputStream(new File(dist.getPath() + "/" + fileName));
        Enumeration enumeration = zipFile.getEntries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) enumeration.nextElement();
            if (entry.getName().contains("MACOS")) {
                continue;
            }
            if (!entry.isDirectory()) {
                File file1 = new File(dist.getPath() + "/" + entry.getName());
                zos.putNextEntry(new ZipEntry(file1.getName()));
                BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
                while (br.ready()) {
                    String line = br.readLine();
                    line = getReplaceString(replace, line).concat("\n");
                    zos.write(line.getBytes());
                }
                zos.close();
                br.close();
            }
        }

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

    public boolean deleteFile(File file) {
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

    public void start() {
        File dist = new File(outputDir);
        File file = new File(srcDir);
        makeOutputDir(dist);
        for (String fileName : file.list(new MyFilter(targets))) {
            try {
                replaceAttrWithZip(fileName, dist, replace);
            } catch (IOException e) {
                replaceAttr(replace, dist, fileName);
            }
        }
    }
}
