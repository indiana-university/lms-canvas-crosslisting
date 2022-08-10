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

import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
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
import java.util.Arrays;
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
   private CourseService courseService = null;

   @Autowired
   @Mock
   private CrosslistService self;

   List<Course> courseList1;
   List<Course> unavailableList;
   Map<String, CanvasTerm> termMap;

   @BeforeEach
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);

      Course course1 = createCourse("1832782", "FA19-EA-MATH-M366-21092");
      Course course2 = createCourse("1832818", "FA19-EA-MATH-M413-21117");
      Course course3 = createCourse("1832823", "FA19-EA-MATH-M413-21361");
      Course course4 = createCourse("1234567", "FA19-EA-MATH-M123-12345");

      //Extra courses
      Course course1_extra = createCourse("1832829", "1832829_SIS");
      Course course2_extra = createCourse("1842038", "1842038_SIS");
      Course course3_extra = createCourse("1843948", "1843948_SIS");
      Course course4_extra = createCourse("1843324", "1843324_SIS");
      Course course5_extra = createCourse("1844424", "1844424_SIS");

      Course unavailableAlienCrosslistCourse = createCourse("1111111", "1111111_SIS");

      unavailableList = Arrays.asList(course1, unavailableAlienCrosslistCourse);

      courseList1 = Arrays.asList(course1, course2, course3, course4);

      Mockito.when(courseService.getCourse(course1.getId())).thenReturn(course1);
      Mockito.when(courseService.getCourse(course2.getId())).thenReturn(course2);
      Mockito.when(courseService.getCourse(course3.getId())).thenReturn(course3);
      Mockito.when(courseService.getCourse(course4.getId())).thenReturn(course4);
      Mockito.when(courseService.getCourse(course1_extra.getId())).thenReturn(course1_extra);
      Mockito.when(courseService.getCourse(course2_extra.getId())).thenReturn(course2_extra);
      Mockito.when(courseService.getCourse(course3_extra.getId())).thenReturn(course3_extra);
      Mockito.when(courseService.getCourse(course4_extra.getId())).thenReturn(course4_extra);
      Mockito.when(courseService.getCourse(course5_extra.getId())).thenReturn(course5_extra);
      Mockito.when(courseService.getCourse(unavailableAlienCrosslistCourse.getId())).thenReturn(unavailableAlienCrosslistCourse);

      List<Section> sectionList1 = new ArrayList<>();
      List<Section> sectionList2 = new ArrayList<>();
      List<Section> sectionList3 = new ArrayList<>();
      List<Section> sectionList4 = new ArrayList<>();
      List<Section> unavailableSectionList = new ArrayList<>();

      sectionList1.add(createSection(course1.getId(), course1.getSisCourseId(),
            "FA19-EA-MATH-M366-21092", null, "FA19-EA-MATH-M366-21092"));

      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-EA-MATH-M413-21117", null, "FA19-EA-MATH-M413-21117"));
      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-EA-MATH-M413-21396", "1832829", "FA19-EA-MATH-M413-21396"));
      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-KO-MATH-M413-35882", "1842038", "FA19-KO-MATH-M413-35882"));
      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-NW-MATH-M413-35883", "1843948", "FA19-NW-MATH-M413-35883"));
      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-SB-MATH-M413-35884", "1843324", "FA19-SB-MATH-M413-35884"));
      sectionList2.add(createSection(course2.getId(), course2.getSisCourseId(),
            "FA19-SE-MATH-M413-35885", "1844424", "FA19-SE-MATH-M413-35885"));

      sectionList3.add(createSection(course3.getId(), course3.getSisCourseId(),
            "FA19-EA-MATH-M413-21361", null, "FA19-EA-MATH-M413-21361"));
//      sectionList3.add(createSection(course3.getId(), course3.getSis_course_id(), null, "1832818", "THIRD-NON-SIS-SECTION"));

      sectionList4.add(createSection(course4.getId(), course4.getSisCourseId(),
            "FA19-EA-MATH-M123-12345", null, "FA19-EA-MATH-M123-12345"));
      sectionList4.add(createSection(course4.getId(), course4.getSisCourseId(), null, null, "NON-SIS-SECTION"));
      sectionList4.add(createSection(course4.getId(), course4.getSisCourseId(), null, "1832818", "SECOND-NON-SIS-SECTION"));

      unavailableSectionList.add(createSection(unavailableAlienCrosslistCourse.getId(), unavailableAlienCrosslistCourse.getSisCourseId(),
              unavailableAlienCrosslistCourse.getSisCourseId(), "0", "NATURAL-SIS-SECTION"));
      unavailableSectionList.add(createSection(unavailableAlienCrosslistCourse.getId(), course1.getId(),
              course1.getSisCourseId(), "0", "ALIEN-SIS-SECTION"));

      Mockito.when(self.getCourseSections(course1.getId())).thenReturn(sectionList1);
      Mockito.when(self.getCourseSections(course2.getId())).thenReturn(sectionList2);
      Mockito.when(self.getCourseSections(course3.getId())).thenReturn(sectionList3);
      Mockito.when(self.getCourseSections(course4.getId())).thenReturn(sectionList4);
      Mockito.when(self.getCourseSections(unavailableAlienCrosslistCourse.getId())).thenReturn(unavailableSectionList);

      termMap = new HashMap<>();
      CanvasTerm t1 = new CanvasTerm();
      t1.setName("Term 1");
      t1.setSisTermId("4198");
      t1.setId("6462");
      termMap.put("6462", t1);

      CanvasTerm unavailableCanvasTerm = crosslistService.getAlienBlockedCanvasTerm();
      termMap.put(crosslistService.ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING, unavailableCanvasTerm);
   }

   @Test
   public void testCourse1_false_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_false_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, false, false, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), true, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, true, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(6, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assertions.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assertions.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assertions.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assertions.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse2_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), false, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay2.getSectionName());
      Assertions.assertFalse(sectionUIDisplay2.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay3.getSectionName());
      Assertions.assertTrue(sectionUIDisplay3.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay4.getSectionName());
      Assertions.assertTrue(sectionUIDisplay4.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay5.getSectionName());
      Assertions.assertTrue(sectionUIDisplay5.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay6.getSectionName());
      Assertions.assertTrue(sectionUIDisplay6.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay7 = sectionUIDisplays.get(6);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay7.getSectionName());
      Assertions.assertTrue(sectionUIDisplay7.isCurrentlyChecked());
   }

   @Test
   public void testCourse2_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), true, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(8, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay2.getSectionName());
      Assertions.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay3.getSectionName());
      Assertions.assertTrue(sectionUIDisplay3.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay4.getSectionName());
      Assertions.assertTrue(sectionUIDisplay4.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay5.getSectionName());
      Assertions.assertTrue(sectionUIDisplay5.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay6.getSectionName());
      Assertions.assertTrue(sectionUIDisplay6.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay7 = sectionUIDisplays.get(7);
      Assertions.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay7.getSectionName());
      Assertions.assertFalse(sectionUIDisplay7.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay7.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse2_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), false, true, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay2.getSectionName());
      Assertions.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay3.getSectionName());
      Assertions.assertTrue(sectionUIDisplay3.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay4.getSectionName());
      Assertions.assertTrue(sectionUIDisplay4.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay5.getSectionName());
      Assertions.assertTrue(sectionUIDisplay5.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay6.getSectionName());
      Assertions.assertTrue(sectionUIDisplay6.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse3_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), false, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());
   }

   @Test
   public void testCourse3_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), true, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse3_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), false, true, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(6, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assertions.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assertions.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assertions.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assertions.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse4_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), false, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay2.getSectionName());
      Assertions.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
   }

   @Test
   public void testCourse4_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), true, false, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(3, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("SECOND-NON-SIS-SECTION", sectionUIDisplay2.getSectionName());
      Assertions.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse4_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), false, true, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assertions.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assertions.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assertions.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assertions.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assertions.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assertions.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assertions.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assertions.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assertions.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assertions.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assertions.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assertions.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assertions.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay6.getSectionName());
      Assertions.assertFalse(sectionUIDisplay6.isCurrentlyChecked());
      Assertions.assertTrue(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void unavailable_true_true_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(unavailableList, termMap,
              crosslistService.getTermStartDateComparator(), unavailableList.get(0), true, true, true, true);

      Assertions.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assertions.assertNotNull(sectionUIDisplays, "results should not be null");
      Assertions.assertEquals(1, sectionUIDisplays.size());

      List<SectionUIDisplay> unavailableSectionUIDisplays = sectionMap.get(termMap.get(crosslistService.ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING));
      Assertions.assertNotNull(unavailableSectionUIDisplays, "results should not be null");
      Assertions.assertEquals(1, unavailableSectionUIDisplays.size());

   }

   private Course createCourse(String courseId, String sisCourseId) {
      Course course = new Course();
      course.setId(courseId);
      course.setSisCourseId(sisCourseId);
      course.setCourseCode(courseId + "_code");
      course.setEnrollmentTermId("6462");

      return course;
   }

   private Section createSection(String courseId, String sisCourseId,
                                 String sisSectionId, String nonXlistCourseId, String sectionName) {
      Section section = new Section();
      section.setId(sisSectionId);
      section.setSis_course_id(sisCourseId);
      section.setSis_section_id(sisSectionId);
      section.setCourse_id(courseId);
      section.setNonxlist_course_id(nonXlistCourseId);
      section.setName(sectionName);

      return section;
   }

}
