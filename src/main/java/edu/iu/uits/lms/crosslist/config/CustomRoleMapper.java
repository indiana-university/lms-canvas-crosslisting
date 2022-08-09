package edu.iu.uits.lms.crosslist.config;

import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {

   private ToolConfig toolConfig;

   public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository, ToolConfig toolConfig) {
      super(defaultInstructorRoleRepository);
      this.toolConfig = toolConfig;
   }

   @Override
   protected String returnEquivalentAuthority(String[] userRoles, List<String> instructorRoles) {
      List<String> userRoleList = Arrays.asList(userRoles);
      List<String> adminRoleList = toolConfig.getAdminRoles();

      // check for admins first
      for (String adminRole : adminRoleList) {
         if (userRoleList.contains(adminRole)) {
            return LTIConstants.ADMIN_AUTHORITY;
         }
      }

      //Then do normal stuff
      return super.returnEquivalentAuthority(userRoles, instructorRoles);
   }
}
