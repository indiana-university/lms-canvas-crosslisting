package edu.iu.uits.lms.crosslist.service;

import canvas.client.generated.api.AccountsApi;
import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.model.Account;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.Section;
import canvas.helpers.CanvasDateFormatUtil;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import iuonly.client.generated.api.FeatureAccessApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrosslistService {

   public final String ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING = "ALIEN_SECTION_BLOCKED";

   @Autowired
   private CoursesApi coursesApi = null;

   @Autowired
   private CourseSessionService courseSessionService = null;

   @Autowired
   private FeatureAccessApi featureAccessApi = null;

   @Autowired
   private AccountsApi accountsApi = null;

   // self reference so can use the cache for getCourseSections() from within this service
   @Autowired
   private CrosslistService self = null;

   public Map<CanvasTerm, List<SectionUIDisplay>> buildSectionsMap(List<Course> courses,
                                                                   Map<String,CanvasTerm> termMap,
                                                                   Comparator<CanvasTerm> termStartDateComparator,
                                                                   Course currentCourse,
                                                                   boolean includeNonSisSections,
                                                                   boolean includeSectionsCrosslistedElsewhere,
                                                                   boolean impersonationMode,
                                                                   boolean useCachedSections) {
      // This map will contain the CanvasTerm for the key and a List<SectionUIDisplay> for the value
      // The TreeMap with comparator will add new entries to the map in a sorted order
      Map<CanvasTerm,List<SectionUIDisplay>> sectionsMap = new TreeMap<>(termStartDateComparator);

      // Get all the course codes for all the courses we know about
      Map<String, String> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, Course::getCourseCode));

      // This List contains all courses and whether each section is in its natural course or is from another (alien)
      List<CourseSisNaturalAndAlien> sisNaturalAndAlienCourseList = new ArrayList<>();

      // Loop through this at a course level, because sections don't contain term information, else it'd be another lookup
      for (Course course : courses) {
         CourseSisNaturalAndAlien courseSisNaturalAndAlien = new CourseSisNaturalAndAlien(course.getId(), course.getSisCourseId());
         sisNaturalAndAlienCourseList.add(courseSisNaturalAndAlien);

         // get the sections to the course
         // TODO this makes page performance slow, especially with a lot of courses/sections
         List<Section> listOfSections = null;

         if (useCachedSections) {
            listOfSections = self.getCourseSections(course.getId());
         } else {
            listOfSections = this.getCourseSections(course.getId());
         }

         //Check to see if there are multiple sections, cause we might want to ignore the one that matches the original parent course
         boolean courseHasMultipleSections = listOfSections != null && listOfSections.size() > 1;

         // loop through the sections, although there will likely only be one per course
         for (Section section : listOfSections) {
            SectionUIDisplay sectionUIDisplayForCount = new SectionUIDisplay(termMap.get(course.getEnrollmentTermId()).getId(),
                    section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), impersonationMode), false, false, false);

            if (section.getSisSectionId() == null) {
               courseSisNaturalAndAlien.addAdHocSection(sectionUIDisplayForCount);
            }
            else {
               if (section.getSisSectionId().equals(course.getSisCourseId())) {
                  courseSisNaturalAndAlien.addNaturalSisSection(sectionUIDisplayForCount);
               } else {
                  courseSisNaturalAndAlien.addAlienSisSection(sectionUIDisplayForCount);
               }
            }

            // Filter out sections crosslisted with other courses and ad hoc sections
            boolean showNonSisSections = includeNonSisSections && (section.getSisSectionId() == null);
            boolean showEverythingButCoursesNativeSection = section.getSisSectionId() != null && !section.getSisSectionId().equals(currentCourse.getSisCourseId());

            if (showNonSisSections || showEverythingButCoursesNativeSection) {

               List<SectionUIDisplay> uiSection = new ArrayList<>();
               boolean newSectionMapEntry = true;

               // Use the course's enrollment termId, unless the section is crosslisted.
               // If the section is cross-listed, then look up the original parent course's termId
               // This keeps sections in their appropriate term when the page displays
               String termIdForCourseOrSection = course.getEnrollmentTermId();
               if (section.getNonxlistCourseId() != null) {
                  Course courseForTerm = coursesApi.getCourse(section.getNonxlistCourseId());
                  //Course might possibly be null here, under some strange and unlikely circumstances
                  if (courseForTerm != null) {
                     termIdForCourseOrSection = courseForTerm.getEnrollmentTermId();
                  }
               }

               // Look up if this term is in the sectionsMap
               if (sectionsMap.containsKey(termMap.get(termIdForCourseOrSection))) {
                  // This term is in our map, so use it
                  uiSection = sectionsMap.get(termMap.get(termIdForCourseOrSection));
                  newSectionMapEntry = false;
               }

               // Using this strictly to show that a new section was added to the array, but only matters if this
               // is a brand new map entry
               boolean addedSection = false;

               String courseCode = course.getCourseCode();
               if (section.getNonxlistCourseId() != null) {
                  courseMap.get(section.getNonxlistCourseId());
                  if (courseMap.containsKey(section.getCourseId())) {
                     courseCode = courseMap.get(section.getCourseId());
                  } else {
                     Course originalCourse = coursesApi.getCourse(section.getCourseId());
                     courseMap.put(section.getCourseId(), originalCourse.getCourseCode());
                     courseCode = originalCourse.getCourseCode();
                  }
               }

               if (section.getSisSectionId() == null) {
                  //Non-sis courses
                  if (section.getCourseId().equalsIgnoreCase(currentCourse.getId()) && section.getNonxlistCourseId() == null) {
                     log.debug(section.getName() + ": non-sis, not crosslisted, under current course - SKIP");
                  } else if (includeSectionsCrosslistedElsewhere && !section.getCourseId().equalsIgnoreCase(currentCourse.getId()) && section.getNonxlistCourseId() != null) {
                     log.debug(section.getName() + ": non-sis, crosslisted elsewhere");
                     uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), courseCode, impersonationMode), false, false, true));
                     addedSection = true;
                  } else if (section.getCourseId().equalsIgnoreCase(currentCourse.getId()) && section.getNonxlistCourseId() != null) {
                     log.debug(section.getName() + ": non-sis, already crosslisted to current course");
                     uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), section.getName(), true, true, false));
                     addedSection = true;
                  } else if (section.getNonxlistCourseId() == null) {
                     log.debug(section.getName() + ": non-sis, not crosslisted");
                     uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), courseCode, impersonationMode), false, false, false));
                     addedSection = true;
                  } else {
                     log.debug(section.getName() + ": non-sis, SKIP");
                  }
               } else {
                  //SIS Courses
                  if (section.getNonxlistCourseId() == null &&
                        !(courseHasMultipleSections && section.getSisSectionId().equals(section.getSisCourseId()))) {
                     //Not crosslisted, so should be included, unless it is the default section for this course and there are multiple sections
                     log.debug(section.getName() + ": sis, not crosslisted");
                     uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), courseCode, impersonationMode), false, false, false));
                     addedSection = true;
                  } else if (section.getNonxlistCourseId() != null && section.getCourseId().equals(currentCourse.getId())) {
                     log.debug(section.getName() + ": sis, crosslisted to me");
                     //Already crosslisted to this course
                     uiSection.add(new SectionUIDisplay(termIdForCourseOrSection, section.getId(), section.getName(), true, true, false));
                     addedSection = true;
                  } else if (includeSectionsCrosslistedElsewhere && section.getNonxlistCourseId() != null) {
                     log.debug(section.getName() + ": sis, crosslisted elsewhere");
                     // Section crosslisted to a DIFFERENT course
                     uiSection.add(new SectionUIDisplay(termIdForCourseOrSection, section.getId(), buildSectionDisplayName(section.getName(), courseCode, impersonationMode), false, false, true));
                     addedSection = true;
                  } else {
                     log.debug(section.getName() + ": sis, SKIP");
//                     log.debug("How did we get here?");
//                     log.debug("\tSis course id: " + section.getSis_course_id());
//                     log.debug("\tSis section id: " + section.getSis_section_id());
//                     log.debug("\tXlisted to: " + section.getNonxlist_course_id());
                  }
               }

               // if this is a brand new map entry AND something was actually added, then add the data to the map
               if (newSectionMapEntry && addedSection) {
                  sectionsMap.put(termMap.get(termIdForCourseOrSection), uiSection);
               }
            } else {
               log.debug(section.getName() + ": SKIP");
//               log.debug("Another else");
//               log.debug("\tSis Course Id: " + section.getSis_course_id());
//               log.debug("\tSis section id: " + section.getSis_section_id());
            }
         }
      }

      // See if any sections were excluded from being crosslisted because the only reason was that they had an
      // alien section in addition to their normal course section. If so, add them to the blockedList
      // so we can display them as unavilable.
      // ALSO....If Adhoc was the only reason a natural section wasn't added as available, and we are
      // includeNonSisSections then add the natural selection as available
      List <SectionUIDisplay> alienSectionBlockedList = new ArrayList<>();
      for(CourseSisNaturalAndAlien courseSisNaturalAndAlien : sisNaturalAndAlienCourseList) {
         if (courseSisNaturalAndAlien.hasAlienSection()) {
            log.debug("*** CourseId " + courseSisNaturalAndAlien.courseId + " has sis sections that can't be crosslisted because of alien sections:");

            for(SectionUIDisplay sectionUIDisplay : courseSisNaturalAndAlien.getNaturalSisSectionUiDisplays()) {
               log.debug("   ** sectionId = " + sectionUIDisplay.getSectionId() + ", " + sectionUIDisplay.getSectionName());

               alienSectionBlockedList.add(sectionUIDisplay);
            }
         } else {
            if (includeNonSisSections && includeSectionsCrosslistedElsewhere && courseSisNaturalAndAlien.isOnlyNaturalWithAdhocs()) {
               // should only be one natural
               SectionUIDisplay naturalSection = courseSisNaturalAndAlien.naturalSisSectionUiDisplays.get(0);

               CanvasTerm canvasTerm = termMap.get(naturalSection.getTermId());

               if (canvasTerm != null) {
                  List<SectionUIDisplay> availableList = sectionsMap.get(canvasTerm);

                  if (availableList == null) {
                     availableList = new ArrayList<>();
                     sectionsMap.put(canvasTerm, availableList);
                  }

                  availableList.add(naturalSection);
               }
            }
         }
      }

      // if any sections are eligible to be crosslisted if one removes the alien section blocker, add them to
      // the sectionMap w/ a fake term (that is later used in the template on UI render)
      if (! alienSectionBlockedList.isEmpty()) {
         CanvasTerm alienSectionBlockedFakeCanvasTerm = getAlienBlockedCanvasTerm();
         sectionsMap.put(alienSectionBlockedFakeCanvasTerm, alienSectionBlockedList);
      }

      // Sort the individual sections in each list
      Comparator<SectionUIDisplay> nameComparator = Comparator.comparing(SectionUIDisplay::getSectionName, Comparator.nullsFirst(Comparator.naturalOrder()));
      sectionsMap.values().forEach(sectionUIDisplays -> sectionUIDisplays.sort(nameComparator));

      return sectionsMap;
   }

   public Comparator<CanvasTerm> getTermStartDateComparator() {
      return new CanvasTermComparator();
   }

   // Don't change this cache key unless you also change how evict works in the CrosslistController
   // TODO - Commented out until a cluster cache solution can be found
   // @Cacheable(value = CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME, key = "#IUNetworkId + '-' + #excludeBlueprint")
   public List<Course> getCoursesTaughtBy(String IUNetworkId, boolean excludeBlueprint) {
      return coursesApi.getCoursesTaughtBy(IUNetworkId, excludeBlueprint, false, false);
   }

   public String buildSectionDisplayName(String sectionName, String courseCode, boolean impersonationMode) {
      if (!impersonationMode) {
         return sectionName;
      }
      return MessageFormat.format("{0} ({1})", sectionName, courseCode);
   }

   public boolean checkForFeature(HttpSession session, Course currentCourse, String feature) {
      Boolean fromSession = courseSessionService.getAttributeFromSession(session, currentCourse.getId(), feature, Boolean.class);
      if (fromSession == null) {
         List<Account> parentAccounts = accountsApi.getParentAccounts(currentCourse.getAccountId());
         List<String> parentAccountIds = parentAccounts.stream().map(Account::getId).collect(Collectors.toList());
         final Boolean featureEnabledForAccount = featureAccessApi.isFeatureEnabledForAccount(feature, currentCourse.getAccountId(), parentAccountIds);
         courseSessionService.addAttributeToSession(session, currentCourse.getId(), feature, featureEnabledForAccount.booleanValue());
         return featureEnabledForAccount;
      } else {
         return fromSession;
      }

   }

   /**
    * Gets dummy term for terms crosslisted into a course that aren't their natural course
    * @return
    */
   public CanvasTerm getAlienBlockedCanvasTerm() {
      CanvasTerm alienSectionBlockedFakeCanvasTerm = new CanvasTerm();
      alienSectionBlockedFakeCanvasTerm.setId(ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING);
      alienSectionBlockedFakeCanvasTerm.setName(ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING);

      Date date = new Date();
      SimpleDateFormat canvasDateFormat = new SimpleDateFormat(CanvasDateFormatUtil.CANVAS_DATE_FORMAT);
      alienSectionBlockedFakeCanvasTerm.setStartAt(canvasDateFormat.format(date));

      return alienSectionBlockedFakeCanvasTerm;
   }

//   @Cacheable(value = CrosslistConstants.COURSE_SECTIONS_CACHE_NAME)
   public List<Section> getCourseSections(String courseId) {
      return coursesApi.getCourseSections(courseId);
   }

   @Data
   private class CourseSisNaturalAndAlien {
      String courseId;
      String sisId;
      List<SectionUIDisplay> naturalSisSectionUiDisplays;
      List<SectionUIDisplay> adHocSectionUiDisplays;
      List<SectionUIDisplay> alienSisSectionUiDisplays;

      public CourseSisNaturalAndAlien(String courseId, String sisId) {
         this.courseId = courseId;
         this.sisId = sisId;
         this.naturalSisSectionUiDisplays = new ArrayList<>();
         this.adHocSectionUiDisplays = new ArrayList<>();
         this.alienSisSectionUiDisplays = new ArrayList<>();
      }

      public void addNaturalSisSection(SectionUIDisplay sectionUIDisplay) {
         naturalSisSectionUiDisplays.add(sectionUIDisplay);
      }

      public void addAdHocSection(SectionUIDisplay sectionUIDisplay) {
         adHocSectionUiDisplays.add(sectionUIDisplay);
      }

      public void addAlienSisSection(SectionUIDisplay sectionUIDisplay) {
         alienSisSectionUiDisplays.add(sectionUIDisplay);
      }

      public boolean hasAlienSection() {
         boolean hasAlienSection = false;

         if (naturalSisSectionUiDisplays.size() > 0 && alienSisSectionUiDisplays.size() > 0) {
            hasAlienSection = true;
         }

         return hasAlienSection;
      }

      public boolean isOnlyNaturalWithAdhocs() {
         return (naturalSisSectionUiDisplays.size() == 1 && alienSisSectionUiDisplays.size() == 0 &&
                 adHocSectionUiDisplays.size() > 0);
      }
   }
}
