package edu.iu.uits.lms.crosslist;

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

import edu.iu.uits.lms.canvas.config.EnableCanvasClient;
import edu.iu.uits.lms.common.samesite.EnableCookieFilter;
import edu.iu.uits.lms.common.server.GitRepositoryState;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.server.ServerUtils;
import edu.iu.uits.lms.lti.config.EnableGlobalErrorHandler;
import edu.iu.uits.lms.common.session.EnableCourseSessionService;
import edu.iu.uits.lms.crosslist.config.ToolConfig;
import edu.iu.uits.lms.iuonly.config.EnableIuOnlyClient;
import edu.iu.uits.lms.lti.config.EnableLtiClient;
import edu.iu.uits.lms.redis.config.EnableRedisConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Date;

@SpringBootApplication
@EnableGlobalErrorHandler
@Slf4j
@EnableCookieFilter(ignoredRequestPatterns = "/rest/**")
@EnableRedisConfiguration
@EnableLtiClient(toolKeys = {"lms_lti_crosslisting", "lms_lti_decrosslisting"})
@EnableCanvasClient
@EnableIuOnlyClient
@EnableConfigurationProperties(GitRepositoryState.class)
@EnableCourseSessionService(sessionKey = "crosslist_course_session")
public class WebApplication {

    @Autowired
    private ToolConfig toolConfig;

    private final static int STEP_CAPACITY = 2048;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WebApplication.class);
        BufferingApplicationStartup startup = new BufferingApplicationStartup(STEP_CAPACITY);
        startup.addFilter(startupStep -> startupStep.getName().matches("spring.boot.application.ready"));
        app.setApplicationStartup(startup);
        app.run(args);
    }

    @Autowired
    private GitRepositoryState gitRepositoryState;

    @Bean(name = ServerInfo.BEAN_NAME)
    ServerInfo serverInfo() {
        return ServerInfo.builder()
              .serverName(ServerUtils.getServerHostName())
              .environment(toolConfig.getEnv())
              .buildDate(new Date())
              .gitInfo(gitRepositoryState.getBranch() + "@" + gitRepositoryState.getCommitIdAbbrev())
              .artifactVersion(toolConfig.getVersion()).build();
    }

}
