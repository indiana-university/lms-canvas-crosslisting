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

import edu.iu.uits.lms.common.it12logging.LmsFilterSecurityInterceptorObjectPostProcessor;
import edu.iu.uits.lms.common.it12logging.RestSecurityLoggingConfig;
import edu.iu.uits.lms.common.oauth.CustomJwtAuthenticationConverter;
import edu.iu.uits.lms.crosslist.repository.DecrosslistUserRepository;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

import static edu.iu.uits.lms.lti.LTIConstants.BASE_USER_AUTHORITY;
import static edu.iu.uits.lms.lti.LTIConstants.WELL_KNOWN_ALL;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

    @Autowired
    private ToolConfig toolConfig;

    @Autowired
    private DecrosslistUserRepository decrosslistUserRepository;

    @Order(5)
    @Bean
    public SecurityFilterChain restFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .securityMatcher("/rest/**", "/api/**")
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers("/rest/**")
                        .access(new WebExpressionAuthorizationManager("hasAuthority('SCOPE_lms:rest') and hasAuthority('ROLE_LMS_REST_ADMINS')"))
                        .requestMatchers("/api/**").permitAll()
                )
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt ->
                                jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter())))
                .with(new RestSecurityLoggingConfig(), log -> {});
        return http.build();
    }

    @Order(6)
    @Bean
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(WELL_KNOWN_ALL, "/error", "/app/**")
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers(WELL_KNOWN_ALL, "/error").permitAll()
                        .requestMatchers("/**").hasAuthority(BASE_USER_AUTHORITY)
                        .withObjectPostProcessor(new LmsFilterSecurityInterceptorObjectPostProcessor())
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self' https://*.instructure.com"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                );

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/app/jsrivet/**", "/app/webjars/**", "/app/css/**", "/app/js/**", "/favicon.ico");
    }

    @Bean
    public SecurityFilterChain catchallFilterChain(HttpSecurity http) throws Exception {
        //Setup the LTI handshake
        http.with(new Lti13Configurer(), lti ->
                lti.setSecurityContextRepository(new HttpSessionSecurityContextRepository())
                        .grantedAuthoritiesMapper(new CustomRoleMapper(defaultInstructorRoleRepository, toolConfig, decrosslistUserRepository)));

        http.securityMatcher("/**")
                .authorizeHttpRequests((authz) -> authz.anyRequest().authenticated()
                        .withObjectPostProcessor(new LmsFilterSecurityInterceptorObjectPostProcessor()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self' https://*.instructure.com"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                );

        return http.build();
    }
}
