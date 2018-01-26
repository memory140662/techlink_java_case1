
package tableausoftware.documentation.api.rest.bindings;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for fileUploadType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="fileUploadType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="uploadSessionId" use="required" type="{http://tableau.com/api}fileUploadSessionIdType" />
 *       &lt;attribute name="fileSize" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fileUploadType")
public class FileUploadType {

    @XmlAttribute(name = "uploadSessionId", required = true)
    protected String uploadSessionId;
    @XmlAttribute(name = "fileSize")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger fileSize;

    /**
     * Gets the value of the uploadSessionId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUploadSessionId() {
        return uploadSessionId;
    }

    /**
     * Sets the value of the uploadSessionId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUploadSessionId(String value) {
        this.uploadSessionId = value;
    }

    /**
     * Gets the value of the fileSize property.
     *
     * @return
     *     possible object is
     *     {@link BigInteger }
     *
     */
    public BigInteger getFileSize() {
        return fileSize;
    }

    /**
     * Sets the value of the fileSize property.
     *
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *
     */
    public void setFileSize(BigInteger value) {
        this.fileSize = value;
    }

}
