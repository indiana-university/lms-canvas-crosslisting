package edu.iu.uits.lms.crosslist.services;

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

import edu.iu.uits.lms.canvas.config.CanvasConfiguration;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.model.FindParentResult;
import edu.iu.uits.lms.crosslist.model.ImpersonationModel;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.model.SisCourse;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CrosslistServiceImpl2Test {
   @Autowired
   @InjectMocks
   private CrosslistService crosslistService = null;

   @Autowired
   @Mock
   private CanvasConfiguration canvasConfiguration = null;

   @Autowired
   @Mock
   private CourseService courseService = null;

   @Autowired
   @Mock
   private SectionService sectionService = null;

   @Autowired
   @Mock
   private SisServiceImpl sisService = null;

   @Autowired
   @Mock
   private CrosslistService self;

   @BeforeEach
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
   }

   // The course the tool launched in (currentCourse) needs to be an SIS course.  SIS course is defined as
   // sisService.isLegitSisCourse(currentCourse.getSisCourseId())) returns back true.
   //

   // Test 1 - current course is an SIS course but no candidates to crosslist
   @Test
   public void currentCourseIsAnSisCourseButNoCandiatesToCrosslist() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      Course currentCourse = new Course() {{
         setSisCourseId("0001");
      }};

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};


      List<Course> courses = new ArrayList<>();

      Mockito.when(sisService.isLegitSisCourse(currentCourse.getSisCourseId())).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertTrue(sectionsMap.isEmpty());
   }

   // Test 2 - current course is an SIS course but only ad hoc sections exist, so no candidates to crosslist
   @Test
   public void currentCourseIsAnSisCourseButOnlyAdhocToCrosslist() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      Course currentCourse = new Course() {{
         setSisCourseId("0001");
      }};

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};


      List<Course> courses = new ArrayList<>() {{
         add(new Course() {{
            setId("AId");
            setCourseCode("codeA");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setId("BId");
            setCourseCode("codeB");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      for (Course course : courses) {
         Mockito.when(self.getCourseSections(course.getId())).thenReturn(
                 new ArrayList<>() {{
                    add(new Section() {{
                       setId(course.getId() + "Section1Id");
                       setSis_section_id(course.getSisCourseId());
                    }});
                    add(new Section() {{
                       setId(course.getId() + "Section2Id");
                    }});
                 }});
      }


      Mockito.when(sisService.isLegitSisCourse(currentCourse.getSisCourseId())).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertTrue(sectionsMap.isEmpty());
   }


   // Test 3 - current course is an SIS course and it has 2 candidates to crosslist
   @Test
   public void currentCourseIsAnSisCourseAndItHasCandidatesToCrosslist() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      Course currentCourse = new Course() {{
         setSisCourseId("0001");
      }};

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};


      List<Course> courses = new ArrayList<>() {{
         add(new Course() {{
            setId("AId");
            setCourseCode("codeA");
            setSisCourseId("sisId1");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setId("BId");
            setCourseCode("codeB");
            setSisCourseId("sisId2");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      for (Course course : courses) {
         Mockito.when(self.getCourseSections(course.getId())).thenReturn(
                 new ArrayList<>() {{
                    add(new Section() {{
                       setId(course.getId() + "Section1Id");
                       setSis_section_id(course.getSisCourseId());
                    }});
                    add(new Section() {{
                       setId(course.getId() + "Section2Id");
                    }});
                 }});
      }


      Mockito.when(sisService.isLegitSisCourse(currentCourse.getSisCourseId())).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertEquals(2, sectionsMap.get(new CanvasTerm() {{
         setId(term1.getId());
         setName(term1.getName());
      }}).size());
   }

   // Test 4 - current course is an SIS course but it cannot crosslist a candidate because
   //          current course has an etext and the candidate does not
   @Test
   public void currentCourseIsAnSisCourseAndHasAnEtextButCandidateDoesNotHaveEtext() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertFalse(result);
   }

   // Test 5 - current course is an SIS course but it cannot crosslist a candidate because
   //          current course has an etext and the candidate has an etext as well but it
   //          does not match the current course's
   @Test
   public void currentCourseIsAnSisCourseAndHasAnEtextCandidateDoesTooButNotTheSameEtext() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
         setEtextIsbns("etextIsbn2");
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertFalse(result);
   }

   @Test
   public void currentCourseIsAnSisCourseAndHasAnEtextCandidateDoesHaveTheSameButAlsoHasAnotherEtext() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
         setEtextIsbns("etextIsbn1,etextIsbn2");
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertFalse(result);
   }

   @Test
   public void currentCourseIsAnSisCourseAndHasTwoEtextsCandidateOnlyHasOneOfTheEtexts() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1,etextIsbn2");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertFalse(result);
   }

   @Test
   public void currentCourseIsAnSisCourseAndHasAnEtextCandidateHasTheSameEtext() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
         setEtextIsbns("etextIsbn1");
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertTrue(result);
   }

   @Test
   public void currentCourseIsAnSisCourseAndHas2EtextsCandidateHasTheSameEtexts() {
      final String currentSisCourseSiteId = "1234";
      final String candidateSisCourseSiteId = "5678";

      final SisCourse currentSisCourse = new SisCourse() {{
         setIuSiteId(currentSisCourseSiteId);
         setEtextIsbns("etextIsbn1,etextIsbn2");
      }};

      final SisCourse candiadateSisCourse = new SisCourse() {{
         setIuSiteId(candidateSisCourseSiteId);
         setEtextIsbns("etextIsbn1,etextIsbn2");
      }};

      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourseSiteId)).thenReturn(currentSisCourse);
      Mockito.when(sisService.getSisCourseBySiteId(candidateSisCourseSiteId)).thenReturn(candiadateSisCourse);

      boolean result = crosslistService.canCoursesBeCrosslistedBasedOnEtexts(currentSisCourseSiteId, candidateSisCourseSiteId);

      Assertions.assertTrue(result);
   }

   @Test
   public void noImpersonation() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      Course currentCourse = new Course() {{
         setSisCourseId("0001");
      }};

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};
      CanvasTerm term2 = new CanvasTerm() {{
         setId("TermId2");
         setName("TermName2");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
         put(term2.getId(), term2);
      }};


      List<Course> courses = new ArrayList<>() {{
         add(new Course() {{
            setId("AId");
            setCourseCode("codeA");
            setSisCourseId("sisId1");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setId("BId");
            setCourseCode("codeB");
            setSisCourseId("sisId2");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setId("CId");
            setCourseCode("codeC");
            setEnrollmentTermId(term2.getId());
         }});
      }};

      for (Course course : courses) {
         Mockito.when(self.getCourseSections(course.getId())).thenReturn(
                 new ArrayList<>() {{
                    add(new Section() {{
                       setId(course.getId() + "Section1Id");
                       setSis_section_id(course.getSisCourseId());
                    }});
                    add(new Section() {{
                       setId(course.getId() + "Section2Id");
                    }});
                 }});
      }


      Mockito.when(sisService.isLegitSisCourse(currentCourse.getSisCourseId())).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);
   }

}
