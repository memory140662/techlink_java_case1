package tw.com.techlink;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;


public class TwbUtil {

//    public static void main(String[] args) throws IOException, JDOMException {git

    public static void remapZip(File file, File output, String siteId, String serverNameWithProtocol) throws IOException, JDOMException {
        File tmp = new File(file.toPath().getParent() + "/tmp");
        File tmpFile = new File(tmp.toPath() + "/" + output.getName());
        tmp.mkdir();
        tmpFile.createNewFile();
        ZipOutputStream outputStream = new ZipOutputStream(tmpFile);
        ZipFile zipFile = new ZipFile(file);
        Enumeration enumeration = zipFile.getEntries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            if (zipEntry.getName().contains("MACOS")) {
                continue;
            }
            if (!zipEntry.isDirectory()) {
                Document document = remap(zipFile.getInputStream(zipEntry), siteId, serverNameWithProtocol);
                XMLOutputter outputter = new XMLOutputter();
                Format format = Format.getRawFormat();
                format.setEncoding("UTF8");
                outputter.setFormat(format);
                outputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                outputStream.write(outputter.outputString(document).getBytes());
            }
        }
        outputStream.close();
        Files.copy(tmpFile.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tmpFile.toPath());
        Files.deleteIfExists(tmp.toPath());
    }
    public static void remap(File file, File output, String siteId, String serverNameWithProtocol) throws JDOMException, IOException {
        System.out.println("Reamp: " + file.getName());
        if (file.getName().endsWith("twbx")) {
            remapZip(file, output,siteId,serverNameWithProtocol);
        } else {
            Document document = remap(new FileInputStream(file), siteId, serverNameWithProtocol);
            XMLOutputter xmlOutput = new XMLOutputter();
            Format format = Format.getRawFormat();
            format.setEncoding("UTF8");
            xmlOutput.setFormat(format);
            xmlOutput.output(document, new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
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
}
