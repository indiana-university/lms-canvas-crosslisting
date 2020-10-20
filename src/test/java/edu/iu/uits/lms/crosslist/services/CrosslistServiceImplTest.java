package edu.iu.uits.lms.crosslist.services;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.Section;
import edu.iu.uits.lms.crosslist.controller.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
//@Ignore
public class CrosslistServiceImplTest {
   @Autowired
   @InjectMocks
   private CrosslistService crosslistService = null;

   @Autowired
   @Mock
   private CoursesApi coursesApi = null;

   @Autowired
   @Mock
   private CrosslistService self;

   List<Course> courseList1;
   List<Course> unavailableList;
   Map<String, CanvasTerm> termMap;

   @Before
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

      Mockito.when(coursesApi.getCourse(course1.getId())).thenReturn(course1);
      Mockito.when(coursesApi.getCourse(course2.getId())).thenReturn(course2);
      Mockito.when(coursesApi.getCourse(course3.getId())).thenReturn(course3);
      Mockito.when(coursesApi.getCourse(course4.getId())).thenReturn(course4);
      Mockito.when(coursesApi.getCourse(course1_extra.getId())).thenReturn(course1_extra);
      Mockito.when(coursesApi.getCourse(course2_extra.getId())).thenReturn(course2_extra);
      Mockito.when(coursesApi.getCourse(course3_extra.getId())).thenReturn(course3_extra);
      Mockito.when(coursesApi.getCourse(course4_extra.getId())).thenReturn(course4_extra);
      Mockito.when(coursesApi.getCourse(course5_extra.getId())).thenReturn(course5_extra);
      Mockito.when(coursesApi.getCourse(unavailableAlienCrosslistCourse.getId())).thenReturn(unavailableAlienCrosslistCourse);

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
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_false_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, false, false);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M413-21361", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), true, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse1_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(0), false, true, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(6, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assert.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assert.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assert.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assert.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse2_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), false, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay2.getSectionName());
      Assert.assertFalse(sectionUIDisplay2.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay3.getSectionName());
      Assert.assertTrue(sectionUIDisplay3.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay4.getSectionName());
      Assert.assertTrue(sectionUIDisplay4.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay5.getSectionName());
      Assert.assertTrue(sectionUIDisplay5.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay6.getSectionName());
      Assert.assertTrue(sectionUIDisplay6.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay7 = sectionUIDisplays.get(6);
      Assert.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay7.getSectionName());
      Assert.assertTrue(sectionUIDisplay7.isCurrentlyChecked());
   }

   @Test
   public void testCourse2_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), true, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(8, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay2.getSectionName());
      Assert.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay3.getSectionName());
      Assert.assertTrue(sectionUIDisplay3.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay4.getSectionName());
      Assert.assertTrue(sectionUIDisplay4.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay5.getSectionName());
      Assert.assertTrue(sectionUIDisplay5.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assert.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay6.getSectionName());
      Assert.assertTrue(sectionUIDisplay6.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay7 = sectionUIDisplays.get(7);
      Assert.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay7.getSectionName());
      Assert.assertFalse(sectionUIDisplay7.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay7.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse2_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(1), false, true, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-EA-MATH-M413-21396", sectionUIDisplay2.getSectionName());
      Assert.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-KO-MATH-M413-35882", sectionUIDisplay3.getSectionName());
      Assert.assertTrue(sectionUIDisplay3.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-NW-MATH-M413-35883", sectionUIDisplay4.getSectionName());
      Assert.assertTrue(sectionUIDisplay4.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SB-MATH-M413-35884", sectionUIDisplay5.getSectionName());
      Assert.assertTrue(sectionUIDisplay5.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assert.assertEquals("FA19-SE-MATH-M413-35885", sectionUIDisplay6.getSectionName());
      Assert.assertTrue(sectionUIDisplay6.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse3_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), false, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(1, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());
   }

   @Test
   public void testCourse3_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), true, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("NON-SIS-SECTION (1234567_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse3_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(2), false, true, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(6, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay1.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assert.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assert.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assert.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assert.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse4_false_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), false, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(2, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay2.getSectionName());
      Assert.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
   }

   @Test
   public void testCourse4_true_false() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), true, false, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(3, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("SECOND-NON-SIS-SECTION", sectionUIDisplay2.getSectionName());
      Assert.assertTrue(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertFalse(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void testCourse4_false_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(courseList1, termMap,
            crosslistService.getTermStartDateComparator(), courseList1.get(3), false, true, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(7, sectionUIDisplays.size());

      SectionUIDisplay sectionUIDisplay = sectionUIDisplays.get(0);
      Assert.assertEquals("FA19-EA-MATH-M366-21092 (1832782_code)", sectionUIDisplay.getSectionName());
      Assert.assertFalse(sectionUIDisplay.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay1 = sectionUIDisplays.get(1);
      Assert.assertEquals("FA19-EA-MATH-M413-21361 (1832823_code)", sectionUIDisplay1.getSectionName());
      Assert.assertFalse(sectionUIDisplay1.isCurrentlyChecked());

      SectionUIDisplay sectionUIDisplay2 = sectionUIDisplays.get(2);
      Assert.assertEquals("FA19-EA-MATH-M413-21396 (1832818_code)", sectionUIDisplay2.getSectionName());
      Assert.assertFalse(sectionUIDisplay2.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay2.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay3 = sectionUIDisplays.get(3);
      Assert.assertEquals("FA19-KO-MATH-M413-35882 (1832818_code)", sectionUIDisplay3.getSectionName());
      Assert.assertFalse(sectionUIDisplay3.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay3.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay4 = sectionUIDisplays.get(4);
      Assert.assertEquals("FA19-NW-MATH-M413-35883 (1832818_code)", sectionUIDisplay4.getSectionName());
      Assert.assertFalse(sectionUIDisplay4.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay4.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay5 = sectionUIDisplays.get(5);
      Assert.assertEquals("FA19-SB-MATH-M413-35884 (1832818_code)", sectionUIDisplay5.getSectionName());
      Assert.assertFalse(sectionUIDisplay5.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay5.isDisplayCrosslistedElsewhereWarning());

      SectionUIDisplay sectionUIDisplay6 = sectionUIDisplays.get(6);
      Assert.assertEquals("FA19-SE-MATH-M413-35885 (1832818_code)", sectionUIDisplay6.getSectionName());
      Assert.assertFalse(sectionUIDisplay6.isCurrentlyChecked());
      Assert.assertTrue(sectionUIDisplay6.isDisplayCrosslistedElsewhereWarning());
   }

   @Test
   public void unavailable_true_true_true() {
      Map<CanvasTerm, List<SectionUIDisplay>> sectionMap = crosslistService.buildSectionsMap(unavailableList, termMap,
              crosslistService.getTermStartDateComparator(), unavailableList.get(0), true, true, true);

      Assert.assertNotNull(sectionMap);
      log.debug("sectionMap: ", sectionMap);

      List<SectionUIDisplay> sectionUIDisplays = sectionMap.get(termMap.get("6462"));
      Assert.assertNotNull("results should not be null", sectionUIDisplays);
      Assert.assertEquals(1, sectionUIDisplays.size());

      List<SectionUIDisplay> unavailableSectionUIDisplays = sectionMap.get(termMap.get(crosslistService.ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING));
      Assert.assertNotNull("results should not be null", unavailableSectionUIDisplays);
      Assert.assertEquals(1, unavailableSectionUIDisplays.size());

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
      section.setSisCourseId(sisCourseId);
      section.setSisSectionId(sisSectionId);
      section.setCourseId(courseId);
      section.setNonxlistCourseId(nonXlistCourseId);
      section.setName(sectionName);

      return section;
   }

}
