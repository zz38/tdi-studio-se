
package com.marketo.mktows;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour SuccessDescribeMObject complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="SuccessDescribeMObject">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="result" type="{http://www.marketo.com/mktows/}ResultDescribeMObject"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SuccessDescribeMObject", propOrder = {
    "result"
})
@XmlRootElement(name = "successDescribeMObject")
public class SuccessDescribeMObject {

    @XmlElement(required = true)
    protected ResultDescribeMObject result;

    /**
     * Obtient la valeur de la propriété result.
     * 
     * @return
     *     possible object is
     *     {@link ResultDescribeMObject }
     *     
     */
    public ResultDescribeMObject getResult() {
        return result;
    }

    /**
     * Définit la valeur de la propriété result.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultDescribeMObject }
     *     
     */
    public void setResult(ResultDescribeMObject value) {
        this.result = value;
    }

}
