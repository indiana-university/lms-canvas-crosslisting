package edu.iu.uits.lms.crosslist.controller;

/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2024 Indiana University
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

import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.model.DecrosslistAudit;
import edu.iu.uits.lms.crosslist.model.DecrosslistUser;
import edu.iu.uits.lms.crosslist.model.FindParentModel;
import edu.iu.uits.lms.crosslist.model.FindParentResult;
import edu.iu.uits.lms.crosslist.model.SubmissionStatus;
import edu.iu.uits.lms.crosslist.repository.DecrosslistAuditRepository;
import edu.iu.uits.lms.crosslist.repository.DecrosslistUserRepository;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;

@Controller
@Slf4j
@RequestMapping("/app")
public class DecrosslistController extends OidcTokenAwareController {

    @Autowired
    private CrosslistService crosslistService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private DecrosslistUserRepository decrosslistUserRepository;

    @Autowired
    private DecrosslistAuditRepository decrosslistAuditRepository;

    @RequestMapping("/lookup-launch")
    @Secured({LTIConstants.INSTRUCTOR_AUTHORITY})
    public String lookupLaunch(@ModelAttribute FindParentModel findParentModel, Model model, HttpSession session) {
        getTokenWithoutContext();

        return "findParentCourse";
    }

    @PostMapping(value = "/lookup-search-sisid")
    @Secured({LTIConstants.INSTRUCTOR_AUTHORITY})
    public String lookupSearchBySisId(@ModelAttribute FindParentModel findParentModel, Model model, HttpSession session) {
        getTokenWithoutContext();

        FindParentResult findParentResult = null;

        // pass off all the lookup knowledge and data we need to the service
        String sisIdSearch = findParentModel.getSisIdSearch().trim();
        findParentResult = crosslistService.processSisLookup(sisIdSearch);

        if (findParentResult != null) {
            model.addAttribute("findParentResult", findParentResult);
            // add canvasCourseId to be used in audit log purposes later
            model.addAttribute("canvasCourseId", findParentResult.getCanvasCourseId());

            // If we aren't showing the course info, there must be an error
            if (!findParentResult.isShowCourseInfo()){
                model.addAttribute("errorMsg", findParentResult.getStatusMessage());
            }
        }

        return "findParentCourse";
    }

    @PostMapping(value = "/decrosslist-sections")
    @Secured({LTIConstants.INSTRUCTOR_AUTHORITY})
    public String decrossListSections(@RequestParam("section-checkboxes") List<String> sectionIds,
                                      @RequestParam("canvasCourseId") String canvasCourseId,
                                      @ModelAttribute FindParentModel findParentModel, Model model, HttpSession session) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        // get current username for logging purposes
        String username = oidcTokenUtils.getUserLoginId();
        // get official user info from database table for the audit log
        DecrosslistUser userInfo = decrosslistUserRepository.findByUsername(username);

        boolean hasSuccesses = false;
        boolean hasErrors = false;

        // loop through the list of section ids and decrosslist them!
        for (String sectionId : sectionIds) {
            log.debug("Decrosslisting section: " + sectionId);
            Section section = sectionService.decrossList(sectionId);
            if (section != null) {
                log.debug("Decrosslisted Section: " + section);
                hasSuccesses = true;
                DecrosslistAudit audit = new DecrosslistAudit();
                audit.setDecrosslistedFrom(canvasCourseId);
                audit.setSisSection(sectionId);
                audit.setUsername(userInfo.getUsername());
                audit.setCanvasUserId(userInfo.getCanvasUserId());
                audit.setDisplayName(userInfo.getDisplayName());
                decrosslistAuditRepository.save(audit);
            } else {
                hasErrors = true;
            }
        }

        if (hasErrors || hasSuccesses) {
            SubmissionStatus status = new SubmissionStatus();
            String titleKey = null;
            String messageKey = null;

            if (hasSuccesses && !hasErrors) {
                status.setStatusClass(CrosslistConstants.STATUS_SUCCESS);
                titleKey = "decross.status.success.title";
                messageKey = "decross.status.success.msg";
            } else if (hasSuccesses) {
                status.setStatusClass(CrosslistConstants.STATUS_PARTIAL);
                titleKey = "decross.status.partial.title";
                messageKey = "decross.status.partial.msg";
            } else {
                status.setStatusClass(CrosslistConstants.STATUS_FAILED);
                titleKey = "decross.status.error.title";
                messageKey = "decross.status.error.msg";
            }

            String statusMessage = messageSource.getMessage(messageKey, null, Locale.getDefault());
            String statusTitle = messageSource.getMessage(titleKey, null, Locale.getDefault());
            status.setStatusMessage(statusMessage);
            status.setStatusTitle(statusTitle);
            model.addAttribute("submissionStatus", status);
        }

        FindParentResult findParentResult = null;

        // pass off all the lookup knowledge and data we need to the service
        String sisIdSearch = findParentModel.getSisIdSearch().trim();
        findParentResult = crosslistService.processSisLookup(sisIdSearch);

        if (findParentResult != null) {
            model.addAttribute("findParentResult", findParentResult);
            // add canvasCourseId to be used in audit log purposes later
            model.addAttribute("canvasCourseId", findParentResult.getCanvasCourseId());
        }

        return "findParentCourse";
    }
}
