package tw.com.techlink;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;


public class TwbUtil {

    public static void remapZip(File file, File output, String siteId, String serverNameWithProtocol) throws IOException, JDOMException {
        File tmp = new File(file.toPath().getParent() + "/tmp");
        File tmpFile = new File(tmp.toPath() + "/" + output.getName());
        tmp.mkdir();
        tmpFile.createNewFile();
        ZipOutputStream outputStream = new ZipOutputStream(tmpFile);
        outputStream.setEncoding("UTF-8");
        ZipFile zipFile = new ZipFile(file);
        Enumeration enumeration = zipFile.getEntries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().contains("MACOS")) {
                continue;
            }
            if (!zipEntry.isDirectory()) {
                if (zipEntry.getName().endsWith(".twb")) {
                    InputStream is = zipFile.getInputStream(zipEntry);
                    Document document = remap(is, siteId, serverNameWithProtocol);
                    XMLOutputter outputter = new XMLOutputter();
                    Format format = Format.getRawFormat();
                    format.setEncoding("UTF-8");
                    outputter.setFormat(format);
                    outputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                    outputStream.write(outputter.outputString(document).getBytes());
                    is.close();
                } else {
                    copyFileInZip(outputStream, zipFile, zipEntry);
                }
            }
        }
        outputStream.close();
        zipFile.close();
        Files.copy(tmpFile.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tmpFile.toPath());
        Files.deleteIfExists(tmp.toPath());
    }
    public static void remap(File file, File output, String siteId, String serverNameWithProtocol) throws JDOMException, IOException {
        System.out.println("Reamp: " + file.getName());
        if (file.getName().endsWith(".twbx") || file.getName().endsWith(".tdsx")) {
            remapZip(file, output,siteId,serverNameWithProtocol);
        } else if (file.getName().endsWith(".twb") || file.getName().endsWith(".tds")) {
            InputStream is = new FileInputStream(file);
            Document document = remap(is, siteId, serverNameWithProtocol);
            XMLOutputter xmlOutput = new XMLOutputter();
            Format format = Format.getRawFormat();
            format.setEncoding("UTF-8");
            xmlOutput.setFormat(format);
            xmlOutput.output(document, new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
            is.close();
        }
    }


    public static Document remap(InputStream file, String siteId, String serverNameWithProtocol) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new InputStreamReader(file, "UTF-8"));
        Element rootElement = (Element) document.getRootElement().clone();
        remapDataServerReferences(rootElement, siteId);
        remapWorkbookGlobalReferences(rootElement, siteId, serverNameWithProtocol);
        document.detachRootElement();
        document.setRootElement(rootElement);
        return document;
    }

    private static void remapWorkbookGlobalReferences(Element rootElement, String siteId, String serverNameWithProtocol) {
        if (rootElement == null) {
            System.err.println("Workbook remapper, 'workbook' node not found.");
            return;
        }

        Attribute xmlBase = rootElement.getAttribute("xml:base");
        if (xmlBase != null) {
            xmlBase.setValue(serverNameWithProtocol);
        }

        remapSingleWorkbooksRepositoryNode(rootElement, siteId);
    }

    private static void remapSingleWorkbooksRepositoryNode(Element rootElement, String siteId) {
        Element repository = rootElement.getChild("repository-location");
        if (repository == null) {
            System.err.println("Workbook remapper, no workbook 'repository-location' node found.");
            return;
        }
        helperSetRespositorySite(rootElement, repository, siteId);
        Attribute path = repository.getAttribute("path");
        if (path != null) {
            if (siteId != null && siteId.trim().length() >= 0) {
                path.setValue("/t/".concat(siteId).concat("/workbooks"));
            } else {
                path.setValue("/workbooks");
            }
        } else {
            System.err.println("Workbook remapper 'path' attribute not found.");
        }
    }

    private static void remapDataServerReferences(Element rootElement, String siteId) {
        for (Element datasources: rootElement.getChildren("datasources")) {
            for (Element datasource: datasources.getChildren("datasource")) {
                Element connection = datasource.getChild("connection");
                if (connection != null) {
                    String dbClass = connection.getAttribute("class").getValue();
                    if ("sqlproxy".equals(dbClass)) {
                        remapSingleDataServerRepositoryNode(rootElement, datasource, siteId);
                    }
                }
            }
        }
    }

    private static void remapSingleDataServerRepositoryNode(Element rootElement, Element datasource, String siteId) {
        Element repository = datasource.getChild("repository-location");
        if (repository == null) {
            System.err.println("Workbook remapper, no datasource 'repository-location' node found.");
            return;
        }

        helperSetRespositorySite(rootElement, repository, siteId);

        Attribute path = repository.getAttribute("path");
        if (path != null) {
            if (siteId != null && siteId.trim().length() >= 0) {
                path.setValue("/t/".concat(siteId).concat("/datasources"));
            } else {
                path.setValue("/datasources");
            }
        } else {
            System.err.println("Workbook remapper 'path' attribute not found.");
        }
    }

    private static void helperSetRespositorySite(Element rootElement, Element repository, String siteId) {
        Attribute site = repository.getAttribute("site");
        if (siteId == null || siteId.trim().length() == 0) {
            if (site != null) {
                repository.removeAttribute(site);
            }
            return;
        }

        if (site == null) {
            site = new Attribute("site", siteId);
            repository.setAttribute(site);
            System.out.println(site.getValue());
        } else {
            repository.setAttribute("site", siteId);
        }

    }

    public static void replaceAttrOrder(File file) throws Exception {
        File parent = file.getParentFile();
        File temp = new File(parent.toPath().toString() + "/temp");
        File tempFile = new File(temp.toPath().toString() + "/" + file.getName());
        if (!temp.exists()) {
            temp.mkdir();
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }
        if (file.getName().endsWith(".twbx") || file.getName().endsWith(".zip")) {
            ZipOutputStream outputStream = new ZipOutputStream(tempFile);
            outputStream.setEncoding("UTF-8");
            ZipFile zipFile = new ZipFile(file);
            Enumeration enumeration = zipFile.getEntries();
            while(enumeration.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
                if (zipEntry.getName().contains("MACOS")) {
                    continue;
                }
                if (!zipEntry.isDirectory()) {
                    if (zipEntry.getName().endsWith(".tds") || zipEntry.getName().endsWith(".twb")) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry), "UTF-8"));
                        outputStream.putNextEntry(zipEntry);
                        String line;
                        while (br.ready()) {
                            line = getLine(br.readLine()) + "\n";
                            outputStream.write(line.getBytes());
                        }
                    } else {
                        copyFileInZip(outputStream, zipFile, zipEntry);
                    }
                }
            }
            outputStream.flush();
            outputStream.close();
            zipFile.close();
        } else if (file.getName().endsWith(".twb") || file.getName().endsWith(".tds")) {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader( fis, "UTF-8"));
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempFile));
            while(br.ready()) {
                osw.append(getLine(br.readLine())).append("\n");
            }
            osw.flush();
            osw.close();
            br.close();
            fis.close();
        }
        Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempFile.toPath());
        Files.deleteIfExists(temp.toPath());
    }

    private static String getLine(String line) {
        String comment;
        String[] attrs;
        String tempStr = "";
        StringBuffer newLine;
        if (line.contains("<workbook")) {
            comment = line.substring(0, line.indexOf("<workbook"));
            line = line.substring(line.indexOf("<workbook"));
            attrs = line.split(" ");
            newLine = new StringBuffer();
            for (int index = 0; index < attrs.length; index++) {
                if (attrs[index].trim().isEmpty()) continue;
                if (attrs[index].startsWith("xmlns:user")) {
                    tempStr = attrs[index].trim();
                    continue;
                }
                if (index == attrs.length -1) {
                    newLine.append(attrs[index].substring(0, attrs[index].lastIndexOf(">")))
                            .append(" ")
                            .append(tempStr)
                            .append(" ")
                            .append(attrs[index].substring(attrs[index].lastIndexOf(">")));
                } else {
                    newLine.append(attrs[index]).append(" ");
                }
            }
            line = comment.concat(newLine.toString());
        }
        return line;
    }

    private static void copyFileInZip(ZipOutputStream outputStream, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        outputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
        InputStream is = zipFile.getInputStream(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while((length = is.read(bytes)) >= 0) {
            outputStream.write(bytes, 0, length);
        }
        is.close();
    }
}
