//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2015.01.30 at 12:49:43 PM PST
//

package tableausoftware.documentation.api.rest.bindings;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for errorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="errorType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="summary" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="detail" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *       &lt;attribute name="code" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "errorType", propOrder = { "summary", "detail" })
public class ErrorType {

    @XmlElement(required = true)
    protected String summary;
    @XmlElement(required = true)
    protected String detail;
    @XmlAttribute(name = "code", required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger code;

    /**
     * Gets the value of the summary property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the value of the summary property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setSummary(String value) {
        this.summary = value;
    }

    /**
     * Gets the value of the detail property.
     *
     * @return possible object is {@link String }
     *
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Sets the value of the detail property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setDetail(String value) {
        this.detail = value;
    }

    /**
     * Gets the value of the code property.
     *
     * @return possible object is {@link BigInteger }
     *
     */
    public BigInteger getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     *
     * @param value
     *            allowed object is {@link BigInteger }
     *
     */
    public void setCode(BigInteger value) {
        this.code = value;
    }

}
