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
   public void currentCourseHasAnEtextButCandidateCourseDoesNotHaveEtext() {
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
   public void currentCourseHasAnEtextButCandidateCourseHasADifferentEtext() {
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
   public void currentCourseHasAnEtextButCandidateCourseHasBothTheSameEtextAndAnotherEtext() {
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
   public void currentCourseHasTwoEtextsButCandidateCourseOnlyHasOneOfTheEtexts() {
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
   public void currentCourseHasAnEtextAndCandidateCourseHasTheSameEtext() {
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
   public void currentCourseHas2EtextsAndCandidateCourseHasTheSameEtexts() {
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
         setId("CurrentSisCourseId");
         setCourseCode("CurrentSisCourseCode");
         setSisCourseId("CurrentSisCourseSisId");
         setEnrollmentTermId(term1.getId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

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
                 // original section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // non-SIS section, won't be included
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section-Adhoc1");
                    setId(courses.get(1).getId() + "-Section-Adhoc1");
                    setCourse_id(courses.get(1).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
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
                 // original section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Section1");
                    setId(courses.get(3).getId() + "-Section1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
              }});

      // verify the sections are legit SIS sections
      Mockito.when(sisService.isLegitSisCourse("OtherSisCourse1SisId")).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse("OtherSisCourse2SisId")).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse("OtherSisCourse3SisId")).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, false);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(3, sectionsMap.get(term1).size());
   }

   @Test
   public void properListofPotentialSectionsToCrosslistWithNothingPreviouslyCrosslistedShowNonSisSections() {
      ImpersonationModel impersonationModel = new ImpersonationModel();
      impersonationModel.setIncludeNonSisSections(true);
      // non-SIS needs to be coupled with impersonation mode
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
                 // original section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

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
                 // original section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section-Adhoc1");
                    setId(courses.get(1).getId() + "-Section-Adhoc1");
                    setCourse_id(courses.get(1).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1");
                    setId(courses.get(2).getId() + "-Section1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1-Adhoc2");
                    setId(courses.get(2).getId() + "-Section-Adhoc2");
                    setCourse_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
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
                      true, false);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(5, sectionsMap.get(term1).size());
   }

   @Test
   public void sectionCannotBeCrosslistedIfCourseHasOtherCrosslistedSectionInIt() {
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
                 // original section
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
                 // original section
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

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, true);

      final CanvasTerm unavailableCanvasTerm = new CanvasTerm() {{
         setId(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setName(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setStartAt("3000-01-01T12:00:00Z");
      }};

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(unavailableCanvasTerm));
      Assertions.assertEquals(1, sectionsMap.get(unavailableCanvasTerm).size());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap.get(unavailableCanvasTerm).get(0).getSectionId());
   }

   @Test
   public void sectionCannotBeCrosslistedIfCourseHasOtherCrosslistedSectionInItAndShowAlreadyCrosslistedSliderOn() {
      ImpersonationModel impersonationModel = new ImpersonationModel();
      impersonationModel.setIncludeCrosslistedSections(true);

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
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
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
                    setSis_course_id("Some made up course id");
                    setSis_section_id("Some sis section id not the same as sis_course_id");
                    setNonxlist_course_id("Some made up course id");
                 }});
              }});

      Mockito.when(sisService.isLegitSisCourse(courses.get(1).getSisCourseId())).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse("Some sis section id not the same as sis_course_id")).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, false);

      final CanvasTerm unavailableCanvasTerm = new CanvasTerm() {{
         setId(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setName(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setStartAt("3000-01-01T12:00:00Z");
      }};

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(1, sectionsMap.get(term1).size());
      Assertions.assertTrue(sectionsMap.get(term1).get(0).isDisplayCrosslistedElsewhereWarning());
      Assertions.assertEquals("OtherSisCourse1Id-Section2", sectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Section2 (OtherSisCourse1Code)", sectionsMap.get(term1).get(0).getSectionName());

      // 2nd call for Unavailable Section, since that's how it works on the actual page
      Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap2wow =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, true);

      Assertions.assertTrue(sectionsMap2wow.containsKey(unavailableCanvasTerm));
      Assertions.assertEquals(1, sectionsMap2wow.get(unavailableCanvasTerm).size());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap2wow.get(unavailableCanvasTerm).get(0).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap2wow.get(unavailableCanvasTerm).get(0).getSectionName());
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
                 // original section
                 add(new Section() {{
                    setName("currentSisCourseSection1");
                    setId("currentSisCourseSectionId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
              }});

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
                 // original section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Section1");
                    setId(courses.get(1).getId() + "-Section1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Adhoc-Section2");
                    setId(courses.get(1).getId() + "-Adhoc-Section2");
                    setCourse_id(courses.get(1).getId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(1).getId() + "-Adhoc-Section3");
                    setId(courses.get(1).getId() + "-Adhoc-Section3");
                    setCourse_id(courses.get(1).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Section1");
                    setId(courses.get(2).getId() + "-Section1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(2).getId() + "-Adhoc-Section2");
                    setId(courses.get(2).getId() + "-Adhoc-Section2");
                    setCourse_id(courses.get(2).getId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-Section1");
                    setId(courses.get(3).getId() + "-Section1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
                 // non-SIS section
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
                      true, false);

      Assertions.assertNotNull(sectionsMap);
      Assertions.assertEquals(1, sectionsMap.size());
      Assertions.assertTrue(sectionsMap.containsKey(term1));
      Assertions.assertEquals(7, sectionsMap.get(term1).size());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section2", sectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section2 (OtherSisCourse1Code)", sectionsMap.get(term1).get(0).getSectionName());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section3", sectionsMap.get(term1).get(1).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Adhoc-Section3 (OtherSisCourse1Code)", sectionsMap.get(term1).get(1).getSectionName());
      Assertions.assertEquals("OtherSisCourse1Id-Section1", sectionsMap.get(term1).get(2).getSectionId());
      Assertions.assertEquals("OtherSisCourse1Id-Section1 (OtherSisCourse1Code)", sectionsMap.get(term1).get(2).getSectionName());
      Assertions.assertEquals("OtherSisCourse2Id-Adhoc-Section2", sectionsMap.get(term1).get(3).getSectionId());
      Assertions.assertEquals("OtherSisCourse2Id-Adhoc-Section2 (OtherSisCourse2Code)", sectionsMap.get(term1).get(3).getSectionName());
      Assertions.assertEquals("OtherSisCourse2Id-Section1", sectionsMap.get(term1).get(4).getSectionId());
      Assertions.assertEquals("OtherSisCourse2Id-Section1 (OtherSisCourse2Code)", sectionsMap.get(term1).get(4).getSectionName());
      Assertions.assertEquals("OtherSisCourse3Id-Adhoc-Section2", sectionsMap.get(term1).get(5).getSectionId());
      Assertions.assertEquals("OtherSisCourse3Id-Adhoc-Section2 (OtherSisCourse3Code)", sectionsMap.get(term1).get(5).getSectionName());
      Assertions.assertEquals("OtherSisCourse3Id-Section1", sectionsMap.get(term1).get(6).getSectionId());
      Assertions.assertEquals("OtherSisCourse3Id-Section1 (OtherSisCourse3Code)", sectionsMap.get(term1).get(6).getSectionName());
   }

   @Test
   public void ultraTestForAllFlagsAndPermutations() {
      // first pass, do as a regular user and no flags
      ImpersonationModel impersonationModel = new ImpersonationModel();

      CanvasTerm term1 = new CanvasTerm() {{
         setId("TermId1");
         setName("TermName1");
      }};

      Map<String, CanvasTerm> termMap = new HashMap<>() {{
         put(term1.getId(), term1);
      }};

      Course currentSisCourse = new Course() {{
         setName("c0-CurrentCourseName");
         setId("c0-CurrentCourseId");
         setCourseCode("c0-CurrentCourseCode");
         setSisCourseId("c0-CurrentSisCourseId");
         setEnrollmentTermId(term1.getId());
      }};

      final SisCourse currentSisCourseForEtextLookup = new SisCourse() {{
         setIuSiteId(currentSisCourse.getSisCourseId());
      }};

      Mockito.when(self.getCourseSections(currentSisCourse.getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(currentSisCourse.getName());
                    setId("c0-CurrentCourseOriginalSectionId");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id(currentSisCourse.getSisCourseId());
                 }});
                 // some other section already crosslisted into current course
                 add(new Section() {{
                    setName("c0-crosslistedSectionInCurrentCourseName1");
                    setId("c0-crosslistedSectionInCurrentCourseId1");
                    setSis_course_id(currentSisCourse.getSisCourseId());
                    setSis_section_id("c0-crosslistedSectionInCurrentCourseName1");
                    setCourse_id(currentSisCourse.getId());
                    setNonxlist_course_id("c0-oldSisCourseId");
                 }});
              }});

      List<Course> courses = new ArrayList<>() {{
         add(currentSisCourse);
         add(new Course() {{
            setName("c1-EtextMismatchCourse");
            setId("c1-EtextMismatchCourseId");
            setCourseCode("c1-EtextMismatchCourseCode");
            setSisCourseId("c1-EtextMismatchSisCourseId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourse");
            setId("c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourseId");
            setCourseCode("c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourseCode");
            setSisCourseId("c2-MultipleSectionsAndAdditionalCrosslistedSectionsSisCourseId");
            setEnrollmentTermId(term1.getId());
         }});
         add(new Course() {{
            setName("c3-CourseWithSisAndNonSisSectionsCourse");
            setId("c3-CourseWithSisAndNonSisSectionsCourseId");
            setCourseCode("c3-CourseWithSisAndNonSisSectionsCourseCode");
            setSisCourseId("c3-CourseWithSisAndNonSisSectionsSisCourseId");
            setEnrollmentTermId(term1.getId());
         }});
      }};

      final SisCourse etextMismatchSisCourseForEtextLookup = new SisCourse() {{
         setIuSiteId(courses.get(1).getSisCourseId());
         setEtextIsbns("etextIsbn1");
      }};

      Mockito.when(self.getCourseSections(courses.get(1).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(1).getName());
                    setId(courses.get(1).getId() + "-SectionId1");
                    setCourse_id(courses.get(1).getId());
                    setSis_course_id(courses.get(1).getSisCourseId());
                    setSis_section_id(courses.get(1).getSisCourseId());
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(2).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(2).getName());
                    setId(courses.get(2).getId() + "-SectionId1");
                    setCourse_id(courses.get(2).getId());
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id(courses.get(2).getSisCourseId());
                 }});
                 // another section crosslisted into this course
                 add(new Section() {{
                    setName("c2-someOtherCourseName");
                    setId("c2-someOtherSectionId");
                    setSis_course_id(courses.get(2).getSisCourseId());
                    setSis_section_id("c2-someOtherCourseName");
                    setCourse_id(courses.get(2).getId());
                    setNonxlist_course_id("c2-someOtherCourseId");
                 }});
              }});

      Mockito.when(self.getCourseSections(courses.get(3).getId())).thenReturn(
              new ArrayList<>() {{
                 // original section
                 add(new Section() {{
                    setName(courses.get(3).getName());
                    setId(courses.get(3).getId() + "-SectionId1");
                    setCourse_id(courses.get(3).getId());
                    setSis_course_id(courses.get(3).getSisCourseId());
                    setSis_section_id(courses.get(3).getSisCourseId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-nonSis-Section1");
                    setId(courses.get(3).getId() + "-nonSis-Section1");
                    setCourse_id(courses.get(3).getId());
                 }});
                 // non-SIS section
                 add(new Section() {{
                    setName(courses.get(3).getId() + "-nonSis-Section2");
                    setId(courses.get(3).getId() + "-nonSis-Section2");
                    setCourse_id(courses.get(3).getId());
                 }});
              }});

      // mock current course and course1 for etext rule
      Mockito.when(sisService.getSisCourseBySiteId(currentSisCourse.getSisCourseId())).thenReturn(currentSisCourseForEtextLookup);
      Mockito.when(sisService.getSisCourseBySiteId(courses.get(1).getSisCourseId())).thenReturn(etextMismatchSisCourseForEtextLookup);

      // These Ids should match for original sections in each course and mocks that they're legit SIS, plus the "already crosslisted" section
      Mockito.when(sisService.isLegitSisCourse(courses.get(1).getSisCourseId())).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse(courses.get(2).getSisCourseId())).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse(courses.get(3).getSisCourseId())).thenReturn(true);
      Mockito.when(sisService.isLegitSisCourse("c2-someOtherCourseName")).thenReturn(true);

      Map<CanvasTerm, List<SectionUIDisplay>> regularUserNoFlagSectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, false);

      // general check for expected size results
      Assertions.assertNotNull(regularUserNoFlagSectionsMap);
      Assertions.assertEquals(1, regularUserNoFlagSectionsMap.size());
      Assertions.assertTrue(regularUserNoFlagSectionsMap.containsKey(term1));
      Assertions.assertEquals(2, regularUserNoFlagSectionsMap.get(term1).size());

      // make sure the current course's original section is not included
      SectionUIDisplay originalCourseSection = new SectionUIDisplay("TermId1", "c0-CurrentCourseOriginalSectionId", "c0-CurrentCourseName (c0-CurrentCourseCode)", false, false, false);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(originalCourseSection)));

      // make sure a section from an etext mismatch did not make it in
      SectionUIDisplay etextCourseSection = new SectionUIDisplay("TermId1", "c1-EtextMismatchCourseId-SectionId1", "c1-EtextMismatchCourse (c1-EtextMismatchCourseCode)", false, false, false);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(etextCourseSection)));

      // check if existing crosslisted section is in the map and is already checked
      Assertions.assertEquals("c0-crosslistedSectionInCurrentCourseId1", regularUserNoFlagSectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertTrue(regularUserNoFlagSectionsMap.get(term1).get(0).isOriginallyChecked());
      Assertions.assertTrue(regularUserNoFlagSectionsMap.get(term1).get(0).isCurrentlyChecked());

      // check if sections are thrown out from the 'contains other crosslisted section' rule
      SectionUIDisplay anotherCrosslistedRuleCourseOriginalSection = new SectionUIDisplay("TermId1", "c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourseId-SectionId1", "c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourse (c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourseCode)", false, false, false);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseOriginalSection)));

      SectionUIDisplay anotherCrosslistedRuleCourseCrosslistedSection = new SectionUIDisplay("TermId1", "c2-someOtherSectionId", "c2-someOtherCourseName (c2-MultipleSectionsAndAdditionalCrosslistedSectionsCourseCode)", false, false, true);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseCrosslistedSection)));

      // check if SIS section from "courses.get(3)" made it in
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourseId-SectionId1", regularUserNoFlagSectionsMap.get(term1).get(1).getSectionId());
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourse", regularUserNoFlagSectionsMap.get(term1).get(1).getSectionName());

      // make sure non-SIS are not included
      SectionUIDisplay nonSis1 = new SectionUIDisplay("TermId1", "c3-CourseWithSisAndNonSisSectionsCourseId-nonSis-Section1", "c3-CourseWithSisAndNonSisSectionsCourseId-nonSis-Section1 (c3-CourseWithSisAndNonSisSectionsCourseCode)", false, false, false);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis1)));

      SectionUIDisplay nonSis2 = new SectionUIDisplay("TermId1", "c3-CourseWithSisAndNonSisSectionsCourseId-nonSis-Section2", "c3-CourseWithSisAndNonSisSectionsCourseId-nonSis-Section2 (c3-CourseWithSisAndNonSisSectionsCourseCode)", false, false, false);
      Assertions.assertFalse(regularUserNoFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis2)));

      // 2nd pass, turn on the includeCrosslistedSections flag, same data
      impersonationModel.setIncludeCrosslistedSections(true);

      Map<CanvasTerm, List<SectionUIDisplay>> regularUserIncludeCrosslistedSectionsFlagSectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, false);

      // general check for expected size results
      Assertions.assertNotNull(regularUserIncludeCrosslistedSectionsFlagSectionsMap);
      Assertions.assertEquals(1, regularUserIncludeCrosslistedSectionsFlagSectionsMap.size());
      Assertions.assertTrue(regularUserIncludeCrosslistedSectionsFlagSectionsMap.containsKey(term1));
      Assertions.assertEquals(3, regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).size());

      // make sure the current course's original section is not included
      Assertions.assertFalse(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(originalCourseSection)));

      // make sure a section from an etext mismatch did not make it in
      Assertions.assertFalse(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(etextCourseSection)));

      // check if existing crosslisted section is in the map and is already checked
      Assertions.assertEquals("c0-crosslistedSectionInCurrentCourseId1", regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertTrue(regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).get(0).isOriginallyChecked());
      Assertions.assertTrue(regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).get(0).isCurrentlyChecked());

      // check if sections are thrown out from the 'contains other crosslisted section' rule
      Assertions.assertFalse(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseOriginalSection)));
      // this one should be true this time
      Assertions.assertTrue(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseCrosslistedSection)));

      // check if SIS section from "courses.get(3)" made it in, bump the get to get(2)
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourseId-SectionId1", regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).get(2).getSectionId());
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourse", regularUserIncludeCrosslistedSectionsFlagSectionsMap.get(term1).get(2).getSectionName());

      // make sure non-SIS are not included
      Assertions.assertFalse(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis1)));
      Assertions.assertFalse(regularUserIncludeCrosslistedSectionsFlagSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis2)));

      // 3rd pass, enter impersonation mode and turn every flag on except for loadUnavailable
      impersonationModel.setUsername("me");
      impersonationModel.setIncludeNonSisSections(true);
      impersonationModel.setIncludeSisSectionsInParentWithCrosslistSections(true);

      Map<CanvasTerm, List<SectionUIDisplay>> impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, false);

      // general check for expected size results
      Assertions.assertNotNull(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap);
      Assertions.assertEquals(1, impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.size());
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.containsKey(term1));
      Assertions.assertEquals(5, impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).size());

      // make sure the current course's original section is not included
      Assertions.assertFalse(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(originalCourseSection)));

      // make sure a section from an etext mismatch did not make it in
      Assertions.assertFalse(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(etextCourseSection)));

      // check if existing crosslisted section is in the map and is already checked
      Assertions.assertEquals("c0-crosslistedSectionInCurrentCourseId1", impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).get(0).getSectionId());
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).get(0).isOriginallyChecked());
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).get(0).isCurrentlyChecked());

      // check if sections are thrown out from the 'contains other crosslisted section' rule
      Assertions.assertFalse(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseOriginalSection)));
      // this one should be true this time
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseCrosslistedSection)));

      // check if SIS section from "courses.get(3)" made it in
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourseId-SectionId1", impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).get(2).getSectionId());
      Assertions.assertEquals("c3-CourseWithSisAndNonSisSectionsCourse (c3-CourseWithSisAndNonSisSectionsCourseCode)", impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.get(term1).get(2).getSectionName());

      // make sure non-SIS are included
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis1)));
      Assertions.assertTrue(impersonateUserAllFlagOnExceptLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(nonSis2)));

      // 4th pass, impersonation and all flags on, including loading the unavailable term
      Map<CanvasTerm, List<SectionUIDisplay>> impersonateUserAllFlagOnAndLoadUnavailableSectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, currentSisCourse,
                      impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                      impersonationModel.getUsername() != null || impersonationModel.isSelfMode(),
                      true, true);

      // loading unavailable will be smaller and this test should just have the 1 value in the Unavailable Term
      final CanvasTerm unavailableCanvasTerm = new CanvasTerm() {{
         setId(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setName(crosslistService.UNAVAILABLE_SECTION_TERM_STRING);
         setStartAt("3000-01-01T12:00:00Z");
      }};

      Assertions.assertNotNull(impersonateUserAllFlagOnAndLoadUnavailableSectionsMap);
      Assertions.assertEquals(1, impersonateUserAllFlagOnAndLoadUnavailableSectionsMap.size());
      Assertions.assertTrue(impersonateUserAllFlagOnAndLoadUnavailableSectionsMap.containsKey(unavailableCanvasTerm));
      Assertions.assertEquals(1, impersonateUserAllFlagOnAndLoadUnavailableSectionsMap.get(unavailableCanvasTerm).size());

      Assertions.assertTrue(impersonateUserAllFlagOnAndLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseOriginalSection)));
      Assertions.assertFalse(impersonateUserAllFlagOnAndLoadUnavailableSectionsMap.values().stream().anyMatch(sectionUI -> sectionUI.contains(anotherCrosslistedRuleCourseCrosslistedSection)));
   }
}