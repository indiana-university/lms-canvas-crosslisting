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

/**
 * Created by chmaurer on 3/2/16.
 */
public interface CrosslistConstants {

    String EHCACHE_PROVIDER_TYPE = "org.ehcache.jsr107.EhcacheCachingProvider";

    String COURSE_SECTIONS_CACHE_NAME = "CourseSections";
    String COURSES_TAUGHT_BY_CACHE_NAME = "CoursesTaughtBy";

    String ACTION_SUBMIT = "submit";
    String ACTION_CONTINUE = "continue";
    String ACTION_CANCEL = "cancel";
    String ACTION_EDIT = "edit";
    String ACTION_IMPERSONATE = "impersonate";
    String ACTION_END_IMPERSONATE = "end_impersonate";

    String MODE_EDIT = "editMode";

    String STATUS_SUCCESS = "rvt-alert--success";
    String STATUS_FAILED = "rvt-alert--danger";
    String STATUS_PARTIAL = "rvt-alert--warning";
    String STATUS_NOOP = "noop";

}
