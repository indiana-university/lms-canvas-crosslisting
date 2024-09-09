package edu.iu.uits.lms.crosslist.services;

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

import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import edu.iu.uits.lms.crosslist.controller.CrosslistController;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpSession;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CrosslistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
@Slf4j
public class CrosslistControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    @Qualifier("CrosslistCacheManager")
    private SimpleCacheManager cacheManager;

    @MockBean
    private CrosslistService crosslistService;

    @MockBean
    private CourseSessionService courseSessionService;

    @MockBean
    private CourseService courseService;

    @MockBean
    private TermService termService;

    @MockBean
    private FeatureAccessServiceImpl featureAccessService;

    @MockBean
    private SisServiceImpl sisService;

    private static String COURSE_ID = "1234";
    private static String SIS_COURSE_ID = "1234_SIS";
    private static String USER_ID = "user1";


    @BeforeEach
    public void setup() {
        OidcAuthenticationToken token = TestUtils.buildToken(USER_ID, COURSE_ID, LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        CanvasTerm canvasTerm = new CanvasTerm();
        canvasTerm.setId("1111");
        canvasTerm.setName("5555");

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTerm(canvasTerm);
        course.setAccountId("9999");
        course.setSisCourseId(SIS_COURSE_ID);

        Mockito.when(courseService.getCourse(COURSE_ID)).thenReturn(course);
        Mockito.when(courseSessionService.getAttributeFromSession(any(HttpSession.class), eq(COURSE_ID), eq(OidcTokenAwareController.SESSION_TOKEN_KEY), eq(OidcAuthenticationToken.class))).thenReturn(token);
    }

    @Test
    public void testEtextMissingMessageAppears() throws Exception {
        final String sectionListJson =
        """
         [ {
          "termId" : "term1",
          "sectionId" : "sectionId1",
          "sectionName" : "Section1 Name",
          "originallyChecked" : false,
          "currentlyChecked" : true,
          "displayCrosslistedElsewhereWarning" : false
        } ]
        """;

        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(new CanvasTerm() {{
            setId("term1");
            setName("Term 1 Name");
            setStartAt("2024-10-24T04:00:00Z");
            setEndAt("2024-10-26T04:00:00Z");
        }}));

        Mockito.when(crosslistService.canCoursesBeCrosslistedBasedOnEtexts(SIS_COURSE_ID, "Section1 Name"))
                .thenReturn(false);

        MvcResult mvcResult = mvc.perform(post(String.format("/app/%s/continue", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("sectionList", sectionListJson))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResult = mvcResult.getResponse().getContentAsString();
        Document document = Jsoup.parse(htmlResult);

        Element missingEtextSectionsMessageElement = document.getElementById("missing-etext-sections-message");
        Assertions.assertNotNull(missingEtextSectionsMessageElement);

        Elements missingEtextSectionsMessageListElements = missingEtextSectionsMessageElement.select("li");
        Assertions.assertNotNull(missingEtextSectionsMessageListElements);
        Assertions.assertEquals(1, missingEtextSectionsMessageListElements.size());
        Assertions.assertNotNull(missingEtextSectionsMessageListElements.get(0));
        Assertions.assertEquals("Section1 Name", missingEtextSectionsMessageListElements.get(0).text());
    }

    @Test
    public void testEtextMissingMessageDoesNotAppear() throws Exception {
        final String sectionListJson =
                """
                 [ {
                  "termId" : "term1",
                  "sectionId" : "sectionId1",
                  "sectionName" : "Section1 Name",
                  "originallyChecked" : false,
                  "currentlyChecked" : true,
                  "displayCrosslistedElsewhereWarning" : false
                } ]
                """;

        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(new CanvasTerm() {{
            setId("term1");
            setName("Term 1 Name");
            setStartAt("2024-10-24T04:00:00Z");
            setEndAt("2024-10-26T04:00:00Z");
        }}));

        Mockito.when(crosslistService.canCoursesBeCrosslistedBasedOnEtexts(SIS_COURSE_ID, "Section1 Name"))
                .thenReturn(true);

        MvcResult mvcResult = mvc.perform(post(String.format("/app/%s/continue", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("sectionList", sectionListJson))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResult = mvcResult.getResponse().getContentAsString();
        Document document = Jsoup.parse(htmlResult);

        Element missingEtextSectionsMessageElement = document.getElementById("missing-etext-sections-message");
        Assertions.assertNull(missingEtextSectionsMessageElement);
    }
}