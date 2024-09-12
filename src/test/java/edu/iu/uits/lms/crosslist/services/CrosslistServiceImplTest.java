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
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.crosslist.model.ImpersonationModel;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.iuonly.model.SisCourse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CrosslistServiceImplTest {
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
   public void properListofPotentialSectionsToCrosslistWithNothingPreviouslyCrosslisted() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("CurrentSisCourse");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(currentSisCourse.getSisCourseId())).thenReturn(true);

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("OtherSisCourse1");
            setId("OtherSisCourse1Id");
            setCourseCode("OtherSisCourse1Code");
            setSisCourseId("OtherSisCourse1SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse2");
            setId("OtherSisCourse2Id");
            setCourseCode("OtherSisCourse2Code");
            setSisCourseId("OtherSisCourse2SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse3");
            setId("OtherSisCourse3Id");
            setCourseCode("OtherSisCourse3Code");
            setSisCourseId("OtherSisCourse3SisId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1");
                    setId(courses.get(2).getId() + "-Section1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Section1");
                    setId(courses.get(3).getId() + "-Section1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
              }});

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(3, sectionsMap.get(term1).size());
   }

   @Test
   public void properListofPotentialSectionsToCrosslistWithNothingPreviouslyCrosslistedShowAdhoc() {
      ImpersonationModel impersonationModel = new ImpersonationModel();
      impersonationModel.setIncludeNonSisSections(true);

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("CurrentSisCourse");
         setId("CurrentSisCourseId");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(currentSisCourse.getSisCourseId())).thenReturn(true);

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("OtherSisCourse1");
            setId("OtherSisCourse1Id");
            setCourseCode("OtherSisCourse1Code");
            setSisCourseId("OtherSisCourse1SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse2");
            setId("OtherSisCourse2Id");
            setCourseCode("OtherSisCourse2Code");
            setSisCourseId("OtherSisCourse2SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse3");
            setId("OtherSisCourse3Id");
            setCourseCode("OtherSisCourse3Code");
            setSisCourseId("OtherSisCourse3SisId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // ad hoc section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section-Adhoc1");
                    setId(courses.get(1).getId() + "-Section-Adhoc1");
                    setCourse_id(courses.get(1).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1");
                    setId(courses.get(2).getId() + "-Section1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
                 // ad hoc section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1-Adhoc2");
                    setId(courses.get(2).getId() + "-Section-Adhoc2");
                    setCourse_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Section1");
                    setId(courses.get(3).getId() + "-Section1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
              }});

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(3, sectionsMap.get(term1).size());
   }

   @Test
   public void naturalCandidateSectionCannotBeCrosslistedIfCandidateSectionsCourseHasOtherCrosslistedSectionInIt() {
      ImpersonationModel impersonationModel = new ImpersonationModel();

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("CurrentSisCourse");
         setId("CurrentSisCourseId");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(currentSisCourse.getSisCourseId())).thenReturn(true);

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("OtherSisCourse1");
            setId("OtherSisCourse1Id");
            setCourseCode("OtherSisCourse1Code");
            setSisCourseId("OtherSisCourse1SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse2");
            setId("OtherSisCourse2Id");
            setCourseCode("OtherSisCourse2Code");
            setSisCourseId("OtherSisCourse2SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse3");
            setId("OtherSisCourse3Id");
            setCourseCode("OtherSisCourse3Code");
            setSisCourseId("OtherSisCourse3SisId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // crosslisted from elsewhere
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section2");
                    setId(courses.get(1).getId() + "-Section2");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id("Some sis section id not the same as sis_course_id");
                    setNonxlist_course_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(courseService.getCourse(courses.get(2).getSisCourseId())).thenReturn(courses.get(3));

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      final CanvasTerm alienSectionBlockedCanvasTerm = new CanvasTerm() {{
         setId("ALIEN_SECTION_BLOCKED");
         setName("ALIEN_SECTION_BLOCKED");
         setStartAt("3000-01-01T12:00:00Z");
      }};

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(alienSectionBlockedCanvasTerm));
      Assertions.assertEquals(1, sectionsMap.get(alienSectionBlockedCanvasTerm).size());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap.get(alienSectionBlockedCanvasTerm).get(0).getSectionId());
   }

   @Test
   public void naturalCandidateSectionCannotBeCrosslistedIfCandidateSectionsCourseHasOtherCrosslistedSectionInItShowAlreadyCrosslistedSliderOn() {
      ImpersonationModel impersonationModel = new ImpersonationModel();
      impersonationModel.setIncludeCrosslistedSections(true);
      impersonationModel.setSelfMode(true);

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("CurrentSisCourse");
         setId("CurrentSisCourseId");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(currentSisCourse.getSisCourseId())).thenReturn(true);

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("OtherSisCourse1");
            setId("OtherSisCourse1Id");
            setCourseCode("OtherSisCourse1Code");
            setSisCourseId("OtherSisCourse1SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse2");
            setId("OtherSisCourse2Id");
            setCourseCode("OtherSisCourse2Code");
            setSisCourseId("OtherSisCourse2SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse3");
            setId("OtherSisCourse3Id");
            setCourseCode("OtherSisCourse3Code");
            setSisCourseId("OtherSisCourse3SisId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // crosslisted from elsewhere
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section2");
                    setId(courses.get(1).getId() + "-Section2");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id("Some sis section id not the same as sis_course_id");
                    setNonxlist_course_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(courseService.getCourse(courses.get(2).getSisCourseId())).thenReturn(courses.get(3));

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      final CanvasTerm alienSectionBlockedCanvasTerm = new CanvasTerm() {{
         setId("ALIEN_SECTION_BLOCKED");
         setName("ALIEN_SECTION_BLOCKED");
         setStartAt("3000-01-01T12:00:00Z");
      }};

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(2, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(alienSectionBlockedCanvasTerm));
      Assertions.assertEquals(1, sectionsMap.get(alienSectionBlockedCanvasTerm).size());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap.get(alienSectionBlockedCanvasTerm).get(0).getSectionId());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(1, sectionsMap.get(term1).size());
      Assertions.assertEquals("OtherSisCourse1Id-Section2", sectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Section2 (OtherSisCourse1Code)", sectionsMap.get(term1).get(0).getSectionName());
      Assertions.assertTrue(sectionsMap.get(term1).get(0).isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void impersonationShowAdhocSections() {
      ImpersonationModel impersonationModel = new ImpersonationModel();
      impersonationModel.setUsername("me");
      impersonationModel.setIncludeNonSisSections(true);

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("CurrentSisCourse");
         setId("CurrentSisCourseId");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(currentSisCourse.getSisCourseId())).thenReturn(true);

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("OtherSisCourse1");
            setId("OtherSisCourse1Id");
            setCourseCode("OtherSisCourse1Code");
            setSisCourseId("OtherSisCourse1SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse2");
            setId("OtherSisCourse2Id");
            setCourseCode("OtherSisCourse2Code");
            setSisCourseId("OtherSisCourse2SisId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("OtherSisCourse3");
            setId("OtherSisCourse3Id");
            setCourseCode("OtherSisCourse3Code");
            setSisCourseId("OtherSisCourse3SisId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // ad hoc
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Adhoc-Section2");
                    setId(courses.get(1).getId() + "-Adhoc-Section2");
                    setCourse_id(courses.get(1).getId());
                 }});
                 // ad hoc
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Adhoc-Section3");
                    setId(courses.get(1).getId() + "-Adhoc-Section3");
                    setCourse_id(courses.get(1).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1");
                    setId(courses.get(2).getId() + "-Section1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
                 // ad hoc
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Adhoc-Section2");
                    setId(courses.get(2).getId() + "-Adhoc-Section2");
                    setCourse_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // natural section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Section1");
                    setId(courses.get(3).getId() + "-Section1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
                 // ad hoc
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Adhoc-Section2");
                    setId(courses.get(3).getId() + "-Adhoc-Section2");
                    setCourse_id(courses.get(3).getId());
                 }});
              }});

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(4, sectionsMap.get(term1).size());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section2", sectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section3", sectionsMap.get(term1).get(1).getSectionId());
      Assertions.assertEquals("OtherSisCourse2Id-Adhoc-Section2", sectionsMap.get(term1).get(2).getSectionId());
      Assertions.assertEquals("OtherSisCourse3Id-Adhoc-Section2", sectionsMap.get(term1).get(3).getSectionId());
   }
}