package edu.iu.uits.lms.crosslist.services;

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

import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.config.SecurityConfig;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import edu.iu.uits.lms.crosslist.controller.CrosslistController;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.controller.InvalidTokenContextException;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CrosslistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {CrosslistController.class, SecurityConfig.class})
public class AppLaunchSecurityTest {
   @Autowired
   private MockMvc mvc;

   @MockBean
   private ToolConfig toolConfig;

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
   private SectionService sectionService;

   @MockBean
   private ResourceBundleMessageSource messageSource;

   @MockBean
   private FeatureAccessServiceImpl featureAccessService;

   @MockBean
   private SisServiceImpl sisService;

   @MockBean
   private AuthorizedUserService authorizedUserService;

   @MockBean
   private ClientRegistrationRepository clientRegistrationRepository;

   @MockBean
   private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

   @MockBean(name = ServerInfo.BEAN_NAME)
   private ServerInfo serverInfo;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index/1234")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnWrongContextLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId","asdf", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      ServletException t = Assertions.assertThrows(ServletException.class, () ->
              mvc.perform(get("/app/1234/main")
                      .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                      .contentType(MediaType.APPLICATION_JSON))
      );

      Assertions.assertInstanceOf(InvalidTokenContextException.class, t.getCause());
      Assertions.assertEquals("Context in authentication token does not match any session context", t.getCause().getMessage());
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId","1234", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      CanvasTerm canvasTerm = new CanvasTerm();
      canvasTerm.setId("1111");
      canvasTerm.setName("5555");

      Course course = new Course();
      course.setId("1234");
      course.setTerm(canvasTerm);
      course.setAccountId("9999");
      course.setSisCourseId("9999");

      Mockito.when(courseService.getCourse("1234")).thenReturn(course);

      Mockito.when(courseSessionService.getAttributeFromSession(any(HttpSession.class), eq(course.getId()), eq(OidcTokenAwareController.SESSION_TOKEN_KEY), eq(OidcAuthenticationToken.class))).thenReturn(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/1234/main")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      String courseId = "1234";

      OidcAuthenticationToken token = TestUtils.buildToken("userId","1234", LTIConstants.BASE_USER_AUTHORITY);

      Mockito.when(courseSessionService.getAttributeFromSession(any(HttpSession.class), eq(courseId), eq(OidcTokenAwareController.SESSION_TOKEN_KEY), eq(OidcAuthenticationToken.class))).thenReturn(token);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
