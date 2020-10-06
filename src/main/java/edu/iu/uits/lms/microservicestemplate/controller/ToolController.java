package edu.iu.uits.lms.microservicestemplate.controller;

import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.microservicestemplate.config.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/app")
@Slf4j
public class ToolController extends LtiAuthenticationTokenAwareController {

   @Autowired
   private ToolConfig toolConfig = null;

   @RequestMapping("/index/{courseId}")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView index(@PathVariable("courseId") String courseId, Model model, HttpServletRequest request) {
      log.debug("in /index");
      LtiAuthenticationToken token = getValidatedToken(courseId);

      return new ModelAndView("index");
   }
}
