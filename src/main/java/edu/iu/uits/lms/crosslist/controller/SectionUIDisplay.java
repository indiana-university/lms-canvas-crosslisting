package edu.iu.uits.lms.crosslist.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chmaurer on 2/29/16.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectionUIDisplay implements Serializable {
    private final static String ORIGINALLY_CHECKED = "originally_checked";
    private final static String CURRENTLY_CHECKED = "currently_checked";
    private final static String CROSSLISTED_ELSEWHERE = "crosslisted_elsewhere";


    public enum TYPE {
        NONE,
        ADDED,
        REMOVED,
        SAVED
    }

    @XmlElement(nillable=true)
    String termId;

    @XmlElement(nillable=true)
    String sectionId;
    @XmlElement(nillable=true)
    String sectionName;

    @XmlElement(nillable=true)
    boolean originallyChecked;

    @XmlElement(nillable=true)
    boolean currentlyChecked;

    @XmlElement(nillable=true)
    boolean displayCrosslistedElsewhereWarning;

    public String getAppropriateCssClass() {
        List<String> classes = new ArrayList<>();
        if (originallyChecked) {
            classes.add(ORIGINALLY_CHECKED);
        }
        if (currentlyChecked) {
            classes.add(CURRENTLY_CHECKED);
        }
        if (displayCrosslistedElsewhereWarning) {
            classes.add(CROSSLISTED_ELSEWHERE);
        }

        return String.join(" ", classes);
    }

    public String getResultType() {
        TYPE returnType = TYPE.NONE;
        if (originallyChecked && currentlyChecked) {
            returnType = TYPE.SAVED;
        } else if (originallyChecked && !currentlyChecked) {
            returnType = TYPE.REMOVED;
        } else if (!originallyChecked && currentlyChecked) {
            returnType = TYPE.ADDED;
        }

        return returnType.name();
    }
}
