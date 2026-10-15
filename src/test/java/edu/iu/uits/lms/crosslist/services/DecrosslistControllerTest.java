package edu.iu.uits.lms.crosslist.services;

/*-
 * #%L
 * lms-lti-crosslist
 * %%
 * Copyright (C) 2015 - 2025 Indiana University
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
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.config.SecurityConfig;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import edu.iu.uits.lms.crosslist.controller.DecrosslistController;
import edu.iu.uits.lms.crosslist.model.FindParentResult;
import edu.iu.uits.lms.crosslist.repository.DecrosslistAuditRepository;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.model.tps.AuthUser;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = DecrosslistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {DecrosslistController.class, SecurityConfig.class})
public class DecrosslistControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    @Qualifier("CrosslistCacheManager")
    private SimpleCacheManager cacheManager;

    @MockitoBean
    @Qualifier("CanvasServicesCacheManager")
    private CacheManager canvasServicesCacheManager;

    @MockitoBean
    private CrosslistService crosslistService;

    @MockitoBean
    private CourseSessionService courseSessionService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private TermService termService;

    @MockitoBean
    private ToolConfig toolConfig;

    @MockitoBean
    private SectionService sectionService;

    @MockitoBean
    private ResourceBundleMessageSource messageSource;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

    @MockitoBean(name = ServerInfo.BEAN_NAME)
    private ServerInfo serverInfo;

    @MockitoBean
    private FeatureAccessServiceImpl featureAccessService;

    @MockitoBean
    private SisServiceImpl sisService;

    @MockitoBean
    private AuthorizedUserService authorizedUserService;

    @MockitoBean
    private DecrosslistAuditRepository decrosslistAuditRepository;

    private static final String SIS_SECTION_ID = "FA26-BL-BIOL-L100-1042";

    private OidcAuthenticationToken token;

    @BeforeEach
    public void setup() {
        token = TestUtils.buildToken("user1", "1234", LTIConstants.INSTRUCTOR_AUTHORITY);
    }

    @Test
    public void lookupLaunchSearchInputIsDescribedByHintAndExample() throws Exception {
        Document document = render(post("/app/lookup-launch"));

        Element searchInput = document.getElementById("sisid-search");
        Assertions.assertNotNull(searchInput);
        Assertions.assertEquals("sisid-search-hint sisid-search-description", searchInput.attr("aria-describedby"));
        Assertions.assertEquals("This tool will only return exact matches.", document.getElementById("sisid-search-hint").text());
        Assertions.assertNull(document.getElementById("results-message"));
    }

    @Test
    public void lookupResultsDescribeHowManySectionsCanBeDecrosslisted() throws Exception {
        mockLookup(crosslistedSection("1"), crosslistedSection("2"), nativeSection("3"));

        Document document = render(post("/app/lookup-search-sisid").param("sisIdSearch", SIS_SECTION_ID));

        Element resultsMessage = document.getElementById("results-message");
        Assertions.assertNotNull(resultsMessage);
        Assertions.assertEquals("results-count", resultsMessage.attr("aria-describedby"));
        Assertions.assertEquals("2 sections can be de-cross-listed.", document.getElementById("results-count").text());
        Assertions.assertEquals(2, document.select("input[name=section-checkboxes]").size());
    }

    @Test
    public void lookupResultsCountIsSingularForOneSection() throws Exception {
        mockLookup(crosslistedSection("1"), nativeSection("2"));

        Document document = render(post("/app/lookup-search-sisid").param("sisIdSearch", SIS_SECTION_ID));

        Assertions.assertEquals("1 section can be de-cross-listed.", document.getElementById("results-count").text());
    }

    @Test
    public void lookupResultsCountSaysNoneWhenNothingIsCrosslisted() throws Exception {
        mockLookup(nativeSection("1"));

        Document document = render(post("/app/lookup-search-sisid").param("sisIdSearch", SIS_SECTION_ID));

        Assertions.assertEquals("No sections can be de-cross-listed.", document.getElementById("results-count").text());
        Assertions.assertTrue(document.select("input[name=section-checkboxes]").isEmpty());
    }

    @Test
    public void decrosslistStatusAlertCanBeFocusedAndIsLabelled() throws Exception {
        mockLookup(nativeSection("1"));
        Mockito.when(authorizedUserService.findByUsername(any())).thenReturn(new AuthUser());
        Mockito.when(sectionService.decrossList("2")).thenReturn(crosslistedSection("2"));
        Mockito.when(messageSource.getMessage(eq("decross.status.success.title"), any(), any())).thenReturn("Success");
        Mockito.when(messageSource.getMessage(eq("decross.status.success.msg"), any(), any())).thenReturn("Sections were de-cross-listed.");

        Document document = render(post("/app/decrosslist-sections")
                .param("section-checkboxes", "2")
                .param("canvasCourseId", "2408560")
                .param("sisIdSearch", SIS_SECTION_ID));

        Element alert = document.getElementById("alert");
        Assertions.assertNotNull(alert);
        Assertions.assertTrue(alert.hasClass(CrosslistConstants.STATUS_SUCCESS));
        Assertions.assertEquals("-1", alert.attr("tabindex"));
        Assertions.assertEquals("Success", document.getElementById(alert.attr("aria-labelledby")).text());
        Assertions.assertEquals("Sections were de-cross-listed.", document.getElementById(alert.attr("aria-describedby")).text());
    }

    private Document render(MockHttpServletRequestBuilder request) throws Exception {
        String html = mvc.perform(request
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return Jsoup.parse(html);
    }

    private void mockLookup(Section... sections) {
        FindParentResult findParentResult = new FindParentResult();
        findParentResult.setShowCourseInfo(true);
        findParentResult.setSisCourseId(SIS_SECTION_ID);
        findParentResult.setCanvasCourseId("2408560");
        findParentResult.setSectionList(List.of(sections));

        Mockito.when(crosslistService.processSisLookup(SIS_SECTION_ID)).thenReturn(findParentResult);
    }

    private static Section crosslistedSection(String id) {
        Section section = nativeSection(id);
        section.setNonxlist_course_id("other-course-" + id);
        return section;
    }

    private static Section nativeSection(String id) {
        Section section = new Section();
        section.setId(id);
        section.setName("Section " + id);
        section.setCourse_id("2408560");
        return section;
    }
}
