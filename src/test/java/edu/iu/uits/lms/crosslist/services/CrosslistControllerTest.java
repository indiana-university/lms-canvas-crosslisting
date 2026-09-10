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

import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.canvas.utils.CacheConstants;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.config.SecurityConfig;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import edu.iu.uits.lms.crosslist.controller.CrosslistController;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.model.SectionWrapper;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CrosslistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {CrosslistController.class, SecurityConfig.class})
@Slf4j
public class CrosslistControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private CrosslistController crosslistController;

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

    @Test
    public void mainAllowsInstructorInSisCourse() throws Exception {
        CanvasTerm canvasTerm = new CanvasTerm();
        canvasTerm.setId("1111");
        canvasTerm.setName("5555");

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTerm(canvasTerm);
        course.setAccountId("9999");
        course.setSisCourseId(SIS_COURSE_ID);

        Mockito.when(courseService.getCourse(COURSE_ID)).thenReturn(course);
        Mockito.when(sisService.isLegitSisCourse(SIS_COURSE_ID)).thenReturn(true);
        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(canvasTerm));
        Mockito.when(crosslistService.getCoursesTaughtBy(USER_ID, false)).thenReturn(List.of(course));
        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(canvasTerm));
        Mockito.when(crosslistService.buildSectionsMap(Mockito.anyList(), Mockito.anyMap(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(new java.util.HashMap<>(java.util.Map.of(canvasTerm, List.of())));

        MvcResult mvcResult = mvc.perform(post(String.format("/app/%s/main", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResult = mvcResult.getResponse().getContentAsString();
        Assertions.assertTrue(htmlResult.contains("Cross-listing Assistant"));
    }

    @Test
    public void mainDeniesInstructorInNonSisCourse() throws Exception {
        CanvasTerm canvasTerm = new CanvasTerm();
        canvasTerm.setId("1111");
        canvasTerm.setName("5555");

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTerm(canvasTerm);
        course.setAccountId("9999");
        course.setSisCourseId(SIS_COURSE_ID);

        Mockito.when(courseService.getCourse(COURSE_ID)).thenReturn(course);
        Mockito.when(sisService.isLegitSisCourse(SIS_COURSE_ID)).thenReturn(false);
        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(canvasTerm));
        Mockito.when(crosslistService.getCoursesTaughtBy(USER_ID, false)).thenReturn(List.of(course));

        MvcResult mvcResult = mvc.perform(post(String.format("/app/%s/main", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResult = mvcResult.getResponse().getContentAsString();
        Assertions.assertTrue(htmlResult.contains("Access Denied to Cross-listing Assistant"));
    }

    @Test
    public void mainAllowsAdminInNonSisCourse() throws Exception {
        CanvasTerm canvasTerm = new CanvasTerm();
        canvasTerm.setId("1111");
        canvasTerm.setName("5555");

        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTerm(canvasTerm);
        course.setAccountId("9999");
        course.setSisCourseId(SIS_COURSE_ID);

        Mockito.when(courseService.getCourse(COURSE_ID)).thenReturn(course);
        Mockito.when(sisService.isLegitSisCourse(SIS_COURSE_ID)).thenReturn(false);
        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(canvasTerm));
        Mockito.when(crosslistService.getCoursesTaughtBy(USER_ID, false)).thenReturn(List.of(course));
        Mockito.when(crosslistService.buildSectionsMap(Mockito.anyList(), Mockito.anyMap(), Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(new java.util.HashMap<>(java.util.Map.of(canvasTerm, List.of())));

        // Simulate admin role
        OidcAuthenticationToken adminToken = TestUtils.buildToken(USER_ID, COURSE_ID, LTIConstants.ADMIN_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(adminToken);

        MvcResult mvcResult = mvc.perform(post(String.format("/app/%s/main", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResult = mvcResult.getResponse().getContentAsString();
        Assertions.assertTrue(htmlResult.contains("Cross-listing Assistant"));
    }

    @Test
    public void evictCourseIdAndSectionsFromCacheEvictsTeacherEnrollmentForImpactedCourses() throws Exception {
        Cache courseSectionsCache = Mockito.mock(Cache.class);
        Cache coursesTaughtByCache = Mockito.mock(Cache.class);
        Cache teacherCourseEnrollmentCache = Mockito.mock(Cache.class);

        Mockito.when(cacheManager.getCache(CrosslistConstants.COURSE_SECTIONS_CACHE_NAME)).thenReturn(courseSectionsCache);
        Mockito.when(cacheManager.getCache(CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME)).thenReturn(coursesTaughtByCache);
        Mockito.when(canvasServicesCacheManager.getCache(CacheConstants.TEACHER_COURSE_ENROLLMENT_CACHE_NAME)).thenReturn(teacherCourseEnrollmentCache);

        SectionUIDisplay addUi = new SectionUIDisplay();
        addUi.setSectionId("section-add");
        SectionUIDisplay removeUi = new SectionUIDisplay();
        removeUi.setSectionId("section-remove");

        SectionWrapper sectionWrapper = new SectionWrapper();
        sectionWrapper.setAddList(new ArrayList<>(List.of(addUi)));
        sectionWrapper.setRemoveList(new ArrayList<>(List.of(removeUi)));
        sectionWrapper.setFinalList(new ArrayList<>());

        Section addSection = new Section();
        addSection.setNonxlist_course_id("old-parent-course");
        Mockito.when(sectionService.getSection("section-add")).thenReturn(addSection);

        Section removeSection = new Section();
        removeSection.setCourse_id("decrosslisted-from-course");
        Mockito.when(sectionService.getSection("section-remove")).thenReturn(removeSection);

        Set<String> courses2Evict = new HashSet<>();
        courses2Evict.add(COURSE_ID);

        Method method = CrosslistController.class.getDeclaredMethod(
                "evictCourseIdAndSectionsFromCache", Set.class, SectionWrapper.class, String.class);
        method.setAccessible(true);
        method.invoke(crosslistController, courses2Evict, sectionWrapper, USER_ID);

        Mockito.verify(teacherCourseEnrollmentCache).evictIfPresent(COURSE_ID);
        Mockito.verify(teacherCourseEnrollmentCache).evictIfPresent("old-parent-course");
        Mockito.verify(teacherCourseEnrollmentCache).evictIfPresent("decrosslisted-from-course");
    }

    @Test
    public void mainDeduplicatesCoursesBeforeBuildSectionsMap() throws Exception {
        CanvasTerm currentTerm = new CanvasTerm();
        currentTerm.setId("term-current");
        currentTerm.setName("Current Term");

        CanvasTerm otherTerm1 = new CanvasTerm();
        otherTerm1.setId("term-1");
        otherTerm1.setName("Other Term 1");

        CanvasTerm otherTerm2 = new CanvasTerm();
        otherTerm2.setId("term-2");
        otherTerm2.setName("Other Term 2");

        Course currentCourse = new Course();
        currentCourse.setId(COURSE_ID);
        currentCourse.setTerm(currentTerm);
        currentCourse.setEnrollmentTermId(currentTerm.getId());
        currentCourse.setAccountId("9999");
        currentCourse.setSisCourseId(SIS_COURSE_ID);

        Course parentCourse1 = new Course();
        parentCourse1.setId("parent-course-1");
        parentCourse1.setTerm(otherTerm1);

        Course parentCourse2 = new Course();
        parentCourse2.setId("parent-course-2");
        parentCourse2.setTerm(otherTerm2);

        edu.iu.uits.lms.canvas.model.Section crosslistedSection1 = new edu.iu.uits.lms.canvas.model.Section();
        crosslistedSection1.setNonxlist_course_id(parentCourse1.getId());

        edu.iu.uits.lms.canvas.model.Section crosslistedSection2 = new edu.iu.uits.lms.canvas.model.Section();
        crosslistedSection2.setNonxlist_course_id(parentCourse2.getId());

        // Duplicate parent term reference - the root cause of the multi-term UI duplication
        edu.iu.uits.lms.canvas.model.Section crosslistedSection3 = new edu.iu.uits.lms.canvas.model.Section();
        crosslistedSection3.setNonxlist_course_id(parentCourse2.getId());

        Course dupCourse1a = new Course();
        dupCourse1a.setId("dedupe-1");
        dupCourse1a.setEnrollmentTermId(otherTerm1.getId());

        Course dupCourse1b = new Course();
        dupCourse1b.setId("dedupe-1");
        dupCourse1b.setEnrollmentTermId(otherTerm1.getId());

        Course dupCourse2a = new Course();
        dupCourse2a.setId("dedupe-2");
        dupCourse2a.setEnrollmentTermId(otherTerm2.getId());

        Course dupCourse2b = new Course();
        dupCourse2b.setId("dedupe-2");
        dupCourse2b.setEnrollmentTermId(otherTerm2.getId());

        Mockito.when(courseService.getCourse(COURSE_ID)).thenReturn(currentCourse);
        Mockito.when(sisService.isLegitSisCourse(SIS_COURSE_ID)).thenReturn(true);
        Mockito.when(courseService.getCourseSections(COURSE_ID))
                .thenReturn(List.of(crosslistedSection1, crosslistedSection2, crosslistedSection3));
        Mockito.when(courseService.getCourse(parentCourse1.getId())).thenReturn(parentCourse1);
        Mockito.when(courseService.getCourse(parentCourse2.getId())).thenReturn(parentCourse2);
        Mockito.when(termService.getEnrollmentTerms()).thenReturn(List.of(currentTerm, otherTerm1, otherTerm2));
        Mockito.when(crosslistService.getCoursesTaughtBy(Mockito.isNull(), Mockito.eq(false)))
                .thenReturn(List.of(currentCourse, dupCourse1a, dupCourse1b, dupCourse2a, dupCourse2b));
        Mockito.when(crosslistService.buildSectionsMap(Mockito.anyList(), Mockito.anyMap(), Mockito.any(),
                        Mockito.nullable(String.class), Mockito.anyBoolean(), Mockito.anyBoolean(),
                        Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(new java.util.HashMap<>(java.util.Map.of(currentTerm, List.of())));

        mvc.perform(post(String.format("/app/%s/main", COURSE_ID))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk());

        ArgumentCaptor<List<Course>> coursesCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(crosslistService).buildSectionsMap(
                coursesCaptor.capture(), Mockito.anyMap(), Mockito.any(), Mockito.nullable(String.class),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean());

        List<Course> passedCourses = coursesCaptor.getValue();
        Set<String> distinctIds = passedCourses.stream().map(Course::getId).collect(Collectors.toSet());

        // No course ID should appear more than once
        Assertions.assertEquals(distinctIds.size(), passedCourses.size(),
                "Duplicate course IDs were passed to buildSectionsMap: " + passedCourses.stream().map(Course::getId).collect(Collectors.toList()));
        Assertions.assertTrue(distinctIds.contains(COURSE_ID));
        Assertions.assertTrue(distinctIds.contains("dedupe-1"));
        Assertions.assertTrue(distinctIds.contains("dedupe-2"));
    }
}
