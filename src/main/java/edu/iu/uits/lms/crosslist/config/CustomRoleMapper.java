package edu.iu.uits.lms.crosslist.config;

import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import static edu.iu.uits.lms.canvas.helpers.CanvasConstants.ADMIN_ROLE;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {

   public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository) {
      super(defaultInstructorRoleRepository);
   }

   @Override
   protected String returnEquivalentAuthority(String[] userRoles, List<String> instructorRoles) {
      List<String> userRoleList = Arrays.asList(userRoles);

      //Check for admins first
      if (userRoleList.contains(ADMIN_ROLE)) {
         return LTIConstants.ADMIN_AUTHORITY;
      }

      //Then do normal stuff
      return super.returnEquivalentAuthority(userRoles, instructorRoles);
   }
}
