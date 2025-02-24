package edu.iu.uits.lms.crosslist.service;

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
import edu.iu.uits.lms.canvas.helpers.CanvasDateFormatUtil;
import edu.iu.uits.lms.canvas.model.Account;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.model.FindParentResult;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.iuonly.model.SisCourse;
import edu.iu.uits.lms.iuonly.services.FeatureAccessServiceImpl;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrosslistService {

   public final static String UNAVAILABLE_SECTION_TERM_STRING = "UNAVAILABLE_SECTION";

   @Autowired
   private CourseService courseService = null;

   @Autowired
   private CourseSessionService courseSessionService = null;

   @Autowired
   private FeatureAccessServiceImpl featureAccessService = null;

   @Autowired
   private AccountService accountService = null;

   @Autowired
   private SisServiceImpl sisService;

   @Autowired
   private SectionService sectionService;

   @Autowired
   private CanvasConfiguration canvasConfiguration;


   // self reference so can use the cache for getCourseSections() from within this service
   @Lazy
   @Autowired
   private CrosslistService self = null;

   // this method is assuming proper authentication has happened
   public Map<CanvasTerm, List<SectionUIDisplay>> buildSectionsMap(List<Course> courses,
                                                                   Map<String,CanvasTerm> termMap,
                                                                   Course currentCourse,
                                                                   boolean includeNonSisSections,
                                                                   boolean includeSectionsCrosslistedElsewhere,
                                                                   boolean impersonationMode,
                                                                   boolean useCachedSections,
                                                                   boolean loadUnavailable) {
      // This map will contain the CanvasTerm for the key and a List<SectionUIDisplay> for the value
      // The TreeMap with comparator will add new entries to the map in a sorted order
      Map<CanvasTerm,List<SectionUIDisplay>> sectionsMap = new TreeMap<>();

      // only need to get this once for the Unavailable Sections
      CanvasTerm unavailableCanvasTerm = getUnavailableCanvasTerm();

      for (Course course : courses) {
         List<Section> listOfSectionsCache = null;
         List<SectionUIDisplay> uiSection = new ArrayList<>();

         // Using this strictly to show that a new section was added to the array, but only matters if this
         // is a brand new map entry
         boolean newSectionMapEntry = true;

         if (useCachedSections) {
            listOfSectionsCache = self.getCourseSections(course.getId());
         } else {
            listOfSectionsCache = this.getCourseSections(course.getId());
         }

         // since we potentially remove sections in this method, assign the cache to a new variable that won't stay cached and cause problems on subsequent calls
         List<Section> listOfSections = new ArrayList<>(listOfSectionsCache);

         // Regardless of any flags or admin status, if etext/ISBN values don't match exactly between the courses,
         // none of the sections are eligible for display. Do this upfront to save time by skipping courses
         if (!canCoursesBeCrosslistedBasedOnEtexts(currentCourse.getSisCourseId(), course.getSisCourseId())) {
            log.debug("SKIP: etext mismatch between courses: " + course.getSisCourseId());
            continue;
         }

         boolean courseHasMultipleSections = listOfSections != null && listOfSections.size() > 1;

         List<SectionUIDisplay> unavailableSectionList = new ArrayList<>();

         // A section can not be cross-listed into the current course unless its original parent contains 0 cross-listed
         // sections. Also, make sure this course isn't the same as the current course
         if (courseHasMultipleSections && !course.getId().equals(currentCourse.getId())) {
            // if any of the sections are crosslisted, we can potentially skip it unless includeSectionsCrosslistedElsewhere is true
            boolean anyCrosslistedSections = listOfSections.stream().anyMatch(section -> section.getNonxlist_course_id() != null);
            if (anyCrosslistedSections) {
               List<SectionUIDisplay> unavailableUiSection = new ArrayList<>();
               List<Section> removeSectionList = new ArrayList<>();
               // likely skipping this course overall, but capture the sections for the Unavailable Sections via includeSisSectionsInParentWithCrosslistSections and loadUnavailable flags
               for (Section section : listOfSections) {
                  if (section.getNonxlist_course_id() == null) {
                     if (loadUnavailable) {
                        // this stuff is only necessary if we're displaying unavailable
                        boolean isNewUnavailableSection = true;
                        // Look up if this term is in the sectionsMap
                        if (sectionsMap.containsKey(unavailableCanvasTerm)) {
                           // This term is in our map, so use it
                           unavailableUiSection = sectionsMap.get(unavailableCanvasTerm);
                           isNewUnavailableSection = false;
                        }

                        SectionUIDisplay sectionUIDisplayForCount = new SectionUIDisplay(termMap.get(course.getEnrollmentTermId()).getId(),
                                section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), impersonationMode), false, false, false);
                        unavailableUiSection.add(sectionUIDisplayForCount);

                        if (isNewUnavailableSection) {
                           unavailableSectionList.add(sectionUIDisplayForCount);
                           sectionsMap.put(unavailableCanvasTerm, unavailableSectionList);
                        }
                        log.debug("ADD: added into the Unavailable Section: " + section.getSis_section_id());
                     } else {
                        log.debug("SKIP: section thrown out from the 'contains other crosslisted section' rule, but included in Unavailable Section if flag is on: " + section.getSis_section_id());
                     }
                     // remove this section since we don't want it to potentially display anywhere else later
                     removeSectionList.add(section);
                  } else {
                     log.debug("SKIP: section thrown out from the 'contains other crosslisted section' rule, but NOT included in Unavailable Section: " + section.getSis_section_id());
                  }
               }

               if (!includeSectionsCrosslistedElsewhere) {
                  // if this flag is false, then we can skip the rest of the checks and move forward. Otherwise, the sections
                  // not included in the Unavailable group could be displayed and still need to pass the checks if they will be there or not
                  continue;
               }

               // remove the unavailable sections so they are not potentially included as a section eligible for crosslisting
               listOfSections.removeAll(removeSectionList);

               log.debug("includeSectionsCrosslistedElsewhere is true, so sections from this block COULD still be displayed...");
            }
         }

         // if the loadUnavailable flag is true, we don't care about any of this
         if (!loadUnavailable) {
            // made it through course level things, let's check sections if they're available to display
            for (Section section : listOfSections) {
               // term collecting stuff
               String termIdForCourseOrSection = course.getEnrollmentTermId();
               if (section.getNonxlist_course_id() != null) {
                  Course courseForTerm = courseService.getCourse(section.getNonxlist_course_id());
                  // Course might possibly be null here, under some strange and unlikely circumstances
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

               // let's do some more checks!
               boolean addedSection = false;

               // if this is the current course's original section, skip it
               if (currentCourse.getSisCourseId().equals(section.getSis_section_id()) && section.getNonxlist_course_id() == null) {
                  log.debug("SKIP: section is the current course's original section: " + section.getSis_section_id());
                  continue;
               }

               if (currentCourse.getId().equals(section.getCourse_id())) {
                  // if the section is already in the current course, add it for display (and checked) regardless of other criteria
                  uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), section.getName(), true, true, false));
                  addedSection = true;
                  log.debug("ADD and CHECKED: added because already in current course: " + section.getSis_section_id());
               } else if (includeNonSisSections && impersonationMode) {
                  // not trusting the includeNonSisSections flag on its own. Confirm with impersonationMode being true, too
                  if (includeSectionsCrosslistedElsewhere) {
                     if (section.getNonxlist_course_id() == null) {
                        // not crosslisted elsewhere, add it. This is valid for display regardless of the includeSectionsCrosslistedElsewhere flag
                        uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), impersonationMode), false, false, false));
                        addedSection = true;
                        log.debug("ADD: not crosslisted, but in includeSectionsCrosslistedElsewhere block. non-SIS block. SIS ID: " + section.getSis_section_id() + " SIS Course ID: " + section.getSis_course_id() + " Course ID: " + section.getCourse_id() + " Section ID: " + section.getId());
                     } else {
                        // crosslisted elsewhere
                        uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), impersonationMode), false, false, true));
                        addedSection = true;
                        log.debug("ADD: crosslisted elsewhere, but includeSectionsCrosslistedElsewhere flag is true. non-SIS block. SIS ID: " + section.getSis_section_id() + " SIS Course ID: " + section.getSis_course_id() + " Course ID: " + section.getCourse_id() + " Section ID: " + section.getId());
                     }
                  } else if (section.getNonxlist_course_id() == null) {
                     // not crosslisted elsewhere, add it
                     uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), impersonationMode), false, false, false));
                     addedSection = true;
                     log.debug("ADD: not crosslisted. non-SIS block. SIS ID: " + section.getSis_section_id() + " SIS Course ID: " + section.getSis_course_id() + " Course ID: " + section.getCourse_id() + " Section ID: " + section.getId());
                  }
               } else {
                  // assuming regular user here or impersonation mode, so needs to pass the SIS validations. Even though
                  // the query assumes a course lookup, the section in this case for proper SIS provisioning should still work fine
                  if (sisService.isLegitSisCourse(section.getSis_section_id())) {
                     // legit SIS course
                     if (section.getNonxlist_course_id() == null) {
                        // section is not crosslisted, so let's add it
                        uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), section.getName(), false, false, false));
                        addedSection = true;
                        log.debug("ADD: not crosslisted anywhere and is SIS. SIS ID: " + section.getSis_section_id());
                     } else if (includeSectionsCrosslistedElsewhere) {
                        // section IS crosslisted, but since this flag is on and confirmed it's SIS, add it
                        uiSection.add(new SectionUIDisplay(termMap.get(termIdForCourseOrSection).getId(), section.getId(), buildSectionDisplayName(section.getName(), course.getCourseCode(), true), false, false, true));
                        addedSection = true;
                        log.debug("ADD: crosslisted elsewhere, is SIS, and includeSectionsCrosslistedElsewhere is true. SIS ID: " + section.getSis_section_id());
                     }
                  } else {
                     log.debug("SKIP: Not legit sis section: " + section.getId());
                  }
               }

               // if this is a brand new map entry AND something was actually added, then add the data to the map
               if (newSectionMapEntry && addedSection) {
                  sectionsMap.put(termMap.get(termIdForCourseOrSection), uiSection);
               }
            }
         }
      }

      // Sort the individual sections in each list
      Comparator<SectionUIDisplay> nameComparator = Comparator.comparing(SectionUIDisplay::getSectionName, Comparator.nullsFirst(Comparator.naturalOrder()));
      sectionsMap.values().forEach(sectionUIDisplays -> sectionUIDisplays.sort(nameComparator));

      return sectionsMap;
   }

   // Don't change this cache key unless you also change how evict works in the CrosslistController
   @Cacheable(value = CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME, key = "#IUNetworkId + '-' + #excludeBlueprint")
   public List<Course> getCoursesTaughtBy(String IUNetworkId, boolean excludeBlueprint) {
      log.debug("cache miss for {} - getCoursesTaughtBy({}, {})", CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME, IUNetworkId, excludeBlueprint);
      return courseService.getCoursesTaughtBy(IUNetworkId, excludeBlueprint, false, false);
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
         List<Account> parentAccounts = accountService.getParentAccounts(currentCourse.getAccountId());
         List<String> parentAccountIds = parentAccounts.stream().map(Account::getId).collect(Collectors.toList());
         final Boolean featureEnabledForAccount = featureAccessService.isFeatureEnabledForAccount(feature, currentCourse.getAccountId(), parentAccountIds);
         courseSessionService.addAttributeToSession(session, currentCourse.getId(), feature, featureEnabledForAccount.booleanValue());
         return featureEnabledForAccount;
      } else {
         return fromSession;
      }

   }

   /**
    * Gets dummy term for sections unable to be crosslisted because there are other sections crosslisted into their course
    * @return The CanvasTerm
    */
   public static CanvasTerm getUnavailableCanvasTerm() {
      CanvasTerm unavailableSectionCanvasTerm = new CanvasTerm();
      unavailableSectionCanvasTerm.setId(UNAVAILABLE_SECTION_TERM_STRING);
      unavailableSectionCanvasTerm.setName(UNAVAILABLE_SECTION_TERM_STRING);

      Date date = Date.from(LocalDate.of(3000, 01, 01)
              .atStartOfDay(ZoneId.systemDefault()).toInstant());
      SimpleDateFormat canvasDateFormat = new SimpleDateFormat(CanvasDateFormatUtil.CANVAS_DATE_FORMAT);
      unavailableSectionCanvasTerm.setStartAt(canvasDateFormat.format(date));

      return unavailableSectionCanvasTerm;
   }

   @Cacheable(value = CrosslistConstants.COURSE_SECTIONS_CACHE_NAME)
   public List<Section> getCourseSections(String courseId) {
      log.debug("cache miss for {} - getCourseSections({})", CrosslistConstants.COURSE_SECTIONS_CACHE_NAME, courseId);
      return courseService.getCourseSections(courseId);
   }

   public boolean canCoursesBeCrosslistedBasedOnEtexts(String sourceSisCourseSiteId, String destinationSisCourseSiteId) {
      SisCourse sourceSisCourse = sisService.getSisCourseBySiteId(sourceSisCourseSiteId);
      sourceSisCourse = sourceSisCourse == null ? new SisCourse() : sourceSisCourse;

      SisCourse destinationSisCourse = sisService.getSisCourseBySiteId(destinationSisCourseSiteId);
      destinationSisCourse = destinationSisCourse == null ? new SisCourse() : destinationSisCourse;

      if (sourceSisCourse.getEtextIsbns() == null && destinationSisCourse.getEtextIsbns() != null) {
         return false;
      }

      if (sourceSisCourse.getEtextIsbns() != null && destinationSisCourse.getEtextIsbns() == null) {
         return false;
      }

      List<String> sourceCourseEtextIsbns =  sourceSisCourse.getEtextIsbns() == null
              ? new ArrayList<>() : new ArrayList<>(List.of(sourceSisCourse.getEtextIsbns().split(",")));

      List<String> destinationCourseEtextIsbns =  destinationSisCourse.getEtextIsbns() == null
              ? new ArrayList<>() : new ArrayList<>(List.of(destinationSisCourse.getEtextIsbns().split(",")));

      Collections.sort(sourceCourseEtextIsbns);
      Collections.sort(destinationCourseEtextIsbns);

      return sourceCourseEtextIsbns.equals(destinationCourseEtextIsbns);
   }

   // this method is only used in the decrosslist tool
   public FindParentResult processSisLookup(String sisSectionId) {
      FindParentResult findParentResult = new FindParentResult();

      if (sisSectionId == null || sisSectionId.isEmpty()) {
         findParentResult.setShowCourseInfo(false);
         findParentResult.setStatusMessage(CrosslistConstants.LOOKUP_FAILURE_MISSING_SIS_ID);
         return findParentResult;
      }

      // this style of Canvas search is case-sensitive, so let's force the upper case that will be accurate for SIS
      Section section = sectionService.getSection(String.format("sis_section_id:%s", sisSectionId.toUpperCase()));

      if (section == null) {
         findParentResult.setShowCourseInfo(false);
         findParentResult.setStatusMessage(CrosslistConstants.LOOKUP_FAILURE_NOT_FOUND_IN_CANVAS_MESSAGE);
         return findParentResult;
      }

      Course course = courseService.getCourse(section.getCourse_id());

      if (course == null) {
         findParentResult.setShowCourseInfo(false);
         findParentResult.setStatusMessage(CrosslistConstants.LOOKUP_FAILURE_NOT_FOUND_IN_CANVAS_MESSAGE);
         return findParentResult;
      }

      // separate sections call since sections are null in a course lookup to the Canvas API
      // fine with this being empty, even though that is unlikely
      // null is not ok
      List<Section> sectionsList = courseService.getCourseSections(section.getCourse_id());

      if (sectionsList == null) {
         findParentResult.setShowCourseInfo(false);
         findParentResult.setStatusMessage(CrosslistConstants.LOOKUP_FAILURE_NOT_FOUND_IN_CANVAS_MESSAGE);
         return findParentResult;
      }

      findParentResult.setShowCourseInfo(true);
      findParentResult.setStatusMessage(CrosslistConstants.LOOKUP_SUCCESS_FOUND_MESSAGE);
      findParentResult.setStatusIconCssClasses(CrosslistConstants.LOOKUP_SUCCESS_CSS);
      findParentResult.setStatusIconName(CrosslistConstants.LOOKUP_SUCCESS_ICON_NAME);
      findParentResult.setName(course.getName());
      findParentResult.setSisCourseId(course.getSisCourseId());
      findParentResult.setUrl(String.format("%s/courses/%s",
              canvasConfiguration.getBaseUrl(), section.getCourse_id()));
      findParentResult.setCanvasCourseId(course.getId());
      findParentResult.setSectionList(sectionsList);

      return findParentResult;
   }
}
