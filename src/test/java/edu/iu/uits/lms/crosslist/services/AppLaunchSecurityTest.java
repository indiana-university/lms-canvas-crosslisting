package edu.iu.uits.lms.crosslist.services;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.TermsApi;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.controller.CrosslistController;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import iuonly.client.generated.api.FeatureAccessApi;
import iuonly.client.generated.api.SudsApi;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(CrosslistController.class)
@Import(ToolConfig.class)
@ActiveProfiles("none")
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
   private CoursesApi coursesApi;

   @MockBean
   private TermsApi termsApi;

   @MockBean
   private FeatureAccessApi featureAccessApi;

   @MockBean
   private SudsApi sudsApi;

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
            .contentType(MediaType.APPLICATION_JSON)
            .sessionAttr(LtiAuthenticationTokenAwareController.SESSION_TOKEN_LIST_KEY, tokenList));

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

      List<LtiAuthenticationToken> tokenList = new ArrayList<LtiAuthenticationToken>(Arrays.asList(token));

      SecurityContextHolder.getContext().setAuthentication(token);

      CanvasTerm canvasTerm = new CanvasTerm();
      canvasTerm.setId("1111");
      canvasTerm.setName("5555");

      Course course = new Course();
      course.setId("1234");
      course.setTerm(canvasTerm);
      course.setAccountId("9999");
      course.setSisCourseId("9999");

      Mockito.when(coursesApi.getCourse("1234")).thenReturn(course);

      Mockito.when((courseSessionService).getAttributeFromSession(any(HttpSession.class), eq(null), eq(LtiAuthenticationTokenAwareController.SESSION_TOKEN_LIST_KEY), eq(List.class))).thenReturn(tokenList);

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
      LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
            "1234", "systemId",
            AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
            "unit_test");

      List<LtiAuthenticationToken> tokenList = new ArrayList<LtiAuthenticationToken>(Arrays.asList(token));

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON)
            .sessionAttr(LtiAuthenticationTokenAwareController.SESSION_TOKEN_LIST_KEY, tokenList))
            .andExpect(status().isNotFound());
   }
}
