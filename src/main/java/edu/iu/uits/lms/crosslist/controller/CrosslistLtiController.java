package edu.iu.uits.lms.crosslist.controller;

import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.controller.LtiController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.tsugi.basiclti.BasicLTIConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static canvas.helpers.CanvasConstants.ADMIN_ROLE;

@Controller
@RequestMapping({"/lti"})
@Slf4j
public class CrosslistLtiController extends LtiController {

    @Autowired
    private CourseSessionService courseSessionService = null;

    private boolean openLaunchUrlInNewWindow = false;

    @Override
    protected String getLaunchUrl(Map<String, String> launchParams) {
        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);
        return "/app/loading/" + courseId;

    }

    @Override
    protected Map<String, String> getParametersForLaunch(Map<String, String> payload, Claims claims) {
        Map<String, String> paramMap = new HashMap<String, String>(1);

        paramMap.put(CUSTOM_CANVAS_COURSE_ID, payload.get(CUSTOM_CANVAS_COURSE_ID));
        paramMap.put(BasicLTIConstants.ROLES, payload.get(BasicLTIConstants.ROLES));
        paramMap.put(CUSTOM_CANVAS_USER_LOGIN_ID, payload.get(CUSTOM_CANVAS_USER_LOGIN_ID));
        paramMap.put(BasicLTIConstants.CONTEXT_TITLE, payload.get(BasicLTIConstants.CONTEXT_TITLE));
        paramMap.put(BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY, payload.get(BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY));
        paramMap.put(BasicLTIConstants.LIS_PERSON_SOURCEDID, payload.get(BasicLTIConstants.LIS_PERSON_SOURCEDID));

        openLaunchUrlInNewWindow = Boolean.valueOf(payload.get(CUSTOM_OPEN_IN_NEW_WINDOW));

        return paramMap;
    }

    @Override
    protected void preLaunchSetup(Map<String, String> launchParams, HttpServletRequest request, HttpServletResponse response) {
        String rolesString = launchParams.get(BasicLTIConstants.ROLES);
        String[] userRoles = rolesString.split(",");
        String authority = returnEquivalentAuthority(Arrays.asList(userRoles), getDefaultInstructorRoles());
        log.debug("LTI equivalent authority: " + authority);

        String userId = launchParams.get(CUSTOM_CANVAS_USER_LOGIN_ID);
        String systemId = launchParams.get(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID);
        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);

        LtiAuthenticationToken token = new LtiAuthenticationToken(userId,
                courseId, systemId, AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, authority), getToolContext());

        LtiAuthenticationToken sessionToken = courseSessionService.getAttributeFromSession(request.getSession(), courseId, LtiAuthenticationTokenAwareController.SESSION_TOKEN_KEY, LtiAuthenticationToken.class);

        if (sessionToken == null) {
            courseSessionService.addAttributeToSession(request.getSession(), courseId, LtiAuthenticationTokenAwareController.SESSION_TOKEN_KEY, token);
        }

        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Override
    protected String getToolContext() {
        return "lms_lti_crosslist";
    }

    @Override
    protected LAUNCH_MODE launchMode() {
        if (openLaunchUrlInNewWindow)
            return LAUNCH_MODE.WINDOW;

        return LAUNCH_MODE.FORWARD;
    }

    /**
     * Given a list of user roles, return the internal equivalent role
     * @param userRoles
     * @param instructorRoles
     * @return
     */
    @Override
    protected String returnEquivalentAuthority(List<String> userRoles, List<String> instructorRoles) {
        //Check for admins first
        if (userRoles.contains(ADMIN_ROLE)) {
            return LTIConstants.ADMIN_AUTHORITY;
        }

        //Then do normal stuff
        return super.returnEquivalentAuthority(userRoles, instructorRoles);
    }
}
