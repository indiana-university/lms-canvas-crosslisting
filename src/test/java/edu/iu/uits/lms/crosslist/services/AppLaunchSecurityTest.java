package edu.iu.uits.lms.crosslist.services;

import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.controller.CrosslistController;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SudsServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CrosslistController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
public class AppLaunchSecurityTest {
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
   private SudsServiceImpl sudsService;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/app/index/1234")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnWrongContextLaunch() throws Exception {
      LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
            "asdf", "systemId",
            AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
            "unit_test");

      List<LtiAuthenticationToken> tokenList = new ArrayList<LtiAuthenticationToken>(Arrays.asList(token));

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      ResultActions mockMvcAction = mvc.perform(get("/app/1234/main")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON));

      mockMvcAction.andExpect(status().isInternalServerError());
      mockMvcAction.andExpect(MockMvcResultMatchers.view().name ("error"));
      mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists("error"));
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
            "1234", "systemId",
            AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, LTIConstants.INSTRUCTOR_AUTHORITY),
              "unit_test");

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

      Mockito.when(courseSessionService.getAttributeFromSession(any(HttpSession.class), eq(course.getId()), eq(LtiAuthenticationTokenAwareController.SESSION_TOKEN_KEY), eq(LtiAuthenticationToken.class))).thenReturn(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/app/1234/main")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      String courseId = "1234";

      LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
            courseId, "systemId",
            AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
            "unit_test");

      Mockito.when(courseSessionService.getAttributeFromSession(any(HttpSession.class), eq(courseId), eq(LtiAuthenticationTokenAwareController.SESSION_TOKEN_KEY), eq(LtiAuthenticationToken.class))).thenReturn(token);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
