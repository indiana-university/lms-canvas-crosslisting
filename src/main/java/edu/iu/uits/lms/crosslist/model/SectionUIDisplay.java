package edu.iu.uits.lms.crosslist.model;

/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
