package edu.iu.uits.lms.crosslist.config;

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

import edu.iu.uits.lms.crosslist.model.DecrosslistUser;
import edu.iu.uits.lms.crosslist.repository.DecrosslistUserRepository;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {

   private ToolConfig toolConfig;

   private DecrosslistUserRepository decrosslistUserRepository;

   public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository, ToolConfig toolConfig, DecrosslistUserRepository decrosslistUserRepository) {
      super(defaultInstructorRoleRepository);
      this.toolConfig = toolConfig;
      this.decrosslistUserRepository = decrosslistUserRepository;
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

   @Override
   public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
      List<GrantedAuthority> remappedAuthorities = new ArrayList<>();
      remappedAuthorities.addAll(authorities);
      for (GrantedAuthority authority : authorities) {
         OidcUserAuthority userAuth = (OidcUserAuthority) authority;
         OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(userAuth.getAttributes());
         log.debug("LTI Claims: {}", userAuth.getAttributes());

         if (Boolean.parseBoolean(oidcTokenUtils.getCustomValue("is_crosslist_tool"))) {
            log.debug("CustomRoleMapper: Crosslist tool");
            // Use the legit roles for crosslisting, which is less restrictive than the decrosslist tool
            return super.mapAuthorities(authorities);
         } else {
            log.debug("CustomRoleMapper: Decrosslist tool");
            // decrosslist tool has a restriction of needing to be in a certain table to access the tool
            String userId = oidcTokenUtils.getUserLoginId();

            String rolesString = "NotAuthorized";

            DecrosslistUser user = decrosslistUserRepository.findByUsername(userId);

            if (user != null) {
               rolesString = LTIConstants.CANVAS_INSTRUCTOR_ROLE;
            }

            String[] userRoles = rolesString.split(",");

            String newAuthString = returnEquivalentAuthority(userRoles, getDefaultInstructorRoles());

            OidcUserAuthority newUserAuth = new OidcUserAuthority(newAuthString, userAuth.getIdToken(), userAuth.getUserInfo());
            remappedAuthorities.add(newUserAuth);
         }
      }

      return remappedAuthorities;
   }
}
