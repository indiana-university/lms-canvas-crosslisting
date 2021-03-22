package edu.iu.uits.lms.crosslist.controller;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.SectionsApi;
import canvas.client.generated.api.TermsApi;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.Section;
import canvas.client.generated.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.crosslist.CrosslistConstants;
import edu.iu.uits.lms.crosslist.model.ImpersonationModel;
import edu.iu.uits.lms.crosslist.model.SectionUIDisplay;
import edu.iu.uits.lms.crosslist.model.SectionWrapper;
import edu.iu.uits.lms.crosslist.model.SubmissionStatus;
import edu.iu.uits.lms.crosslist.security.CrosslistAuthenticationToken;
import edu.iu.uits.lms.crosslist.service.CrosslistService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import iuonly.client.generated.api.FeatureAccessApi;
import iuonly.client.generated.api.SudsApi;
import iuonly.client.generated.model.SudsCourse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequestMapping("/app")
public class CrosslistController extends LtiAuthenticationTokenAwareController {

    private static final String FEATURE_MULTITERM_CROSSLISTING = "multiterm.crosslisting";

    @Autowired
    @Qualifier("CrosslistCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private CoursesApi coursesApi = null;

    @Autowired
    private FeatureAccessApi featureAccessApi = null;

    @Autowired
    private SectionsApi sectionsApi = null;

    @Autowired
    private TermsApi termsApi = null;

    @Autowired
    private ObjectMapper objectMapper = null;

    @Autowired
    private ResourceBundleMessageSource messageSource = null;

    @Autowired
    private CrosslistService crosslistService = null;

    @Autowired
    private SudsApi sudsApi = null;

    @Autowired
    private CourseSessionService courseSessionService;

    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }

    private Course getValidatedCourse(LtiAuthenticationToken token, HttpSession session) {
        String courseId = token.getContext();
        Course currentCourse = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.COURSE_KEY, Course.class);

        if (currentCourse == null) {
            currentCourse = coursesApi.getCourse(courseId);
            courseSessionService.addAttributeToSession(session, courseId, CrosslistAuthenticationToken.COURSE_KEY, currentCourse);

            List<User> courseInstructors = coursesApi.getInstructorsForCourse(courseId);
            //Filter out users with no loginid before sorting
            List<User> filteredSortedInstructors = courseInstructors.stream()
                  .filter(u -> u.getLoginId() != null)
                  .sorted(Comparator.comparing(User::getLoginId))
                  .collect(Collectors.toList());
            courseSessionService.addAttributeToSession(session, courseId, CrosslistAuthenticationToken.INSTRUCTORS_KEY, filteredSortedInstructors);
        }
        return currentCourse;
    }

    @RequestMapping("/loading/{courseId}")
    public String loading(@PathVariable("courseId") String courseId, Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("hideFooter", true);
        return "loading";
    }

    private String showMainPage(LtiAuthenticationToken token, Map<CanvasTerm,List<SectionUIDisplay>> sectionsMap,
                                List<CanvasTerm> selectableTerms, Model model, HttpSession session) {
        Course currentCourse = getValidatedCourse(token, session);

        model.addAttribute("activeCourseSections", sectionsMap.get(currentCourse.getTerm()));
        model.addAttribute("sectionsMap", sectionsMap);
        model.addAttribute("courseTitle", currentCourse.getName());
        model.addAttribute("courseId", currentCourse.getId());
        model.addAttribute("activeTerm", currentCourse.getTerm());
        model.addAttribute("selectableTerms", selectableTerms);
        model.addAttribute("multiTermEnabled", crosslistService.checkForFeature(session, currentCourse, FEATURE_MULTITERM_CROSSLISTING));

        // setting this so doEditConfirmation can use this later and we don't need to look it up again
//        token.setData("selectableTerms", selectableTerms);
        courseSessionService.addAttributeToSession(session, currentCourse.getId(), "selectableTerms", selectableTerms);

        // sets the count for the number of courses currently checked. This is used in the html logic
        int count = 0;
        for (List<SectionUIDisplay> listForCounter : sectionsMap.values()) {
            count = count + listForCounter.stream().filter(i -> (i.isCurrentlyChecked())).collect(Collectors.toList()).size();
        }

        model.addAttribute("checkedSectionCount", count);

        model.addAttribute("instructors", courseSessionService.getAttributeFromSession(session, currentCourse.getId(), CrosslistAuthenticationToken.INSTRUCTORS_KEY, List.class));

        SudsCourse sudsCurrentCourse = sudsApi.getSudsCourseBySiteId(currentCourse.getSisCourseId());

        // display etext ordered warning
        if (sudsCurrentCourse != null && sudsCurrentCourse.getIuCourseLoadStatus() != null
                && sudsCurrentCourse.getIuCourseLoadStatus().toUpperCase().equals("Y")) {
            model.addAttribute("etextMessage",  messageSource.getMessage("etext.message", null, Locale.getDefault()));
        }

        return "index";
    }

    @RequestMapping("/{courseId}/main")
    @Secured({LTIConstants.ADMIN_AUTHORITY, LTIConstants.INSTRUCTOR_AUTHORITY})
    public String main(@PathVariable("courseId") String courseId, Model model, HttpSession session) {
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);

        ImpersonationModel impersonationModel = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, ImpersonationModel.class);

        if (impersonationModel == null) {
            impersonationModel = new ImpersonationModel();
        }
        model.addAttribute("impersonationModel", impersonationModel);

        String currentUserId = impersonationModel.getUsername() == null ? (String)token.getPrincipal(): impersonationModel.getUsername();

        Comparator<CanvasTerm> termStartDateComparator = crosslistService.getTermStartDateComparator();

        Course currentCourse = getValidatedCourse(token, session);

        CanvasTerm currentTerm = currentCourse.getTerm();

        List<Section> currentCourseSections = coursesApi.getCourseSections(currentCourse.getId());

        // Use this list to filter out terms from the dropdown
        List<String> termFilterList = new ArrayList<>();
        // add the current/active term to the filter list since it will always be valid
        termFilterList.add(currentTerm.getId());

        // filter through the rest of the sections to see if any of the cross-listed sections belong to a different term
        for (Section currentSections : currentCourseSections) {
            if (currentSections.getNonxlistCourseId() != null) {
                Course course = coursesApi.getCourse(currentSections.getNonxlistCourseId());
                //Course might possibly be null here, under some strange and unlikely circumstances
                if (course != null) {
                    CanvasTerm term = course.getTerm();
                    if (!termFilterList.contains(term) && !term.equals(currentTerm)) {
                        // not the same term as the current one and does not exist in the list yet
                        termFilterList.add(term.getId());
                    }
                }
            }
        }

        // Get all courses for the user
        // Setting the variable to true does bring back some section information on a course, but it is incomplete and not helpful for what we need
        List<Course> courses = crosslistService.getCoursesTaughtBy(currentUserId, false);

        // get the list of terms in Canvas
        List<CanvasTerm> terms = termsApi.getEnrollmentTerms();

        // convert to a map for easier lookup later
        Map<String,CanvasTerm> termMap = terms.stream().collect(Collectors.toMap(CanvasTerm::getId,Function.identity()));

        // this list will be used for the options in the dropdown
        List<CanvasTerm> selectableTerms = new ArrayList<>();

        if (crosslistService.checkForFeature(session, currentCourse, FEATURE_MULTITERM_CROSSLISTING)) {
            // fill in the selectableTerms list and filter out terms that will be displayed on the screen
            for (Course course : courses) {
                String courseTermId = course.getEnrollmentTermId();
                if (termMap.get(courseTermId) != null && !selectableTerms.contains(termMap.get(courseTermId)) && !termFilterList.contains(courseTermId)) {
                    // if term doesn't exist in the map and isn't a term that's will be loaded because of other cross-listed sections
                    selectableTerms.add(termMap.get(courseTermId));
                }
            }

            // sort it!
            selectableTerms.sort(termStartDateComparator);

// =============================================================================
            // thread start

            Runnable termLoadRunnable = () ->
            {
                // preload cache for all future terms
                final long threadId = Thread.currentThread().getId();

                final int MAX_BACKGROUND_LOADS = 1;
                int count = 0;
                for (CanvasTerm canvasTerm : selectableTerms) {
                    if (++count <= MAX_BACKGROUND_LOADS &&
                            termStartDateComparator.compare(canvasTerm, currentTerm) < 1) {
                        log.debug("***** thread(" + threadId + ") for termId = " + canvasTerm.getId() + " " + canvasTerm.getName());
                        List<Course> threadUserCourses = crosslistService.getCoursesTaughtBy(currentUserId, false);
                        threadUserCourses = threadUserCourses.stream().filter(c -> c.getEnrollmentTermId() != null && c.getEnrollmentTermId().equals(canvasTerm.getId())).collect(Collectors.toList());

                        for (Course course : threadUserCourses) {
                            // don't store the return value because we don't care.
                            // We just want the cache to fill up
                            crosslistService.getCourseSections(course.getId());
                        }
                    } else {
                        break;
                    }
                }
                log.debug("*** thread(" + threadId + ") work done");
            };

            new Thread(termLoadRunnable).start();

            // thread end
// =============================================================================

        }

        // filter the active list down to a smaller set
        courses = courses.stream().filter(c -> termFilterList.contains(c.getEnrollmentTermId())).collect(Collectors.toList());


        // Page title
        if (!model.containsAttribute("pageTitle")) {
            model.addAttribute("pageTitle", "Cross-listing Assistant");
        }

        Map<CanvasTerm, List<SectionUIDisplay>> sectionsMap =
              crosslistService.buildSectionsMap(courses, termMap, termStartDateComparator, currentCourse,
                    impersonationModel.isIncludeNonSisSections(), impersonationModel.isIncludeCrosslistedSections(),
                    impersonationModel.getUsername() != null);

        for (CanvasTerm canvasTermKey : sectionsMap.keySet()) {
            if (canvasTermKey.getName().equals(crosslistService.ALIEN_SECTION_BLOCKED_FAKE_CANVAS_TERM_STRING)) {
                model.addAttribute("hasAlienBlocked", true);
                break;
            }
        }

        return showMainPage(token, sectionsMap, selectableTerms, model, session);
    }

    @RequestMapping("/{courseId}/continue")
    @Secured({LTIConstants.ADMIN_AUTHORITY, LTIConstants.INSTRUCTOR_AUTHORITY})
    public String doContinue(@PathVariable("courseId") String courseId, @RequestParam("sectionList") String sectionListJson,
                               Model model, HttpSession session) {
        Comparator<CanvasTerm> termStartDateComparator = crosslistService.getTermStartDateComparator();

        List<SectionUIDisplay> sectionList = null;
        try {
            sectionList = Arrays.asList(objectMapper.readValue(sectionListJson, SectionUIDisplay[].class));
        } catch (IOException e) {
            log.error("unable to parse json into object", e);
        }

        // Rebuild the map in case the user clicks Edit
        Map<CanvasTerm,List<SectionUIDisplay>> rebuiltTermMap = new TreeMap<>(termStartDateComparator);

        // get the list of terms in Canvas
        List<CanvasTerm> terms = termsApi.getEnrollmentTerms();

        // convert to a map for easier lookup later
        Map<String,CanvasTerm> termMap = terms.stream().collect(Collectors.toMap(CanvasTerm::getId,Function.identity()));

        // add fake canvas term for unavailable list in the UI
        CanvasTerm alienSectionBlockedFakeCanvasTerm = crosslistService.getAlienBlockedCanvasTerm();
        termMap.put(alienSectionBlockedFakeCanvasTerm.getId(), alienSectionBlockedFakeCanvasTerm);

        // Rebuild the map. This is less complex compared to the main()
        for (SectionUIDisplay sectionUI : sectionList) {
            List<SectionUIDisplay> uiSection = new ArrayList<>();
            boolean newEntry = true;

            // Look up if this term is in the otherSectionsMap
            if (rebuiltTermMap.containsKey(termMap.get(sectionUI.getTermId()))) {
                // This term is in our map, so use it
                uiSection = rebuiltTermMap.get(termMap.get(sectionUI.getTermId()));
                newEntry = false;
            }

            uiSection.add(sectionUI);
            if (newEntry) {
                rebuiltTermMap.put(termMap.get(sectionUI.getTermId()), uiSection);
            }
        }

        // TODO theoretically, we don't need to sort individual sections, because they were already added in order. Uncomment if we change our minds
        // Comparator<SectionUIDisplay> nameComparator = Comparator.comparing(SectionUIDisplay::getSectionName, Comparator.nullsFirst(Comparator.naturalOrder()));
        // rebuiltTermMap.values().forEach(sectionUIDisplays -> sectionUIDisplays.sort(nameComparator));

        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);

        Course currentCourse = getValidatedCourse(token, session);
        // set the List<SectionUIDisplay> in the token to potentially be used later for submitting
        courseSessionService.addAttributeToSession(session, courseId, CrosslistAuthenticationToken.SECTION_LIST_KEY, sectionList);
        // set the Map<CanvasTerm,List<SectionUIDisplay>> in the token to potentially be used later for displaying the Edit
        courseSessionService.addAttributeToSession(session, courseId, CrosslistAuthenticationToken.SECTION_MAP_KEY, rebuiltTermMap);

        SectionWrapper sectionWrapper = processSections(sectionList);

        model.addAttribute("courseTitle", currentCourse.getName());
        model.addAttribute("removeListSections", sectionWrapper.getRemoveList());

        SudsCourse sudsCurrentCourse = sudsApi.getSudsCourseBySiteId(currentCourse.getSisCourseId());

        // if course has etexts, check to see if the crosslisted sections have the same etexts
        if (sudsCurrentCourse != null && sudsCurrentCourse.getEtextIsbns() != null) {
            List<String> missingEtextSections = new ArrayList<String>();

            String[] currentCourseEtextIsbns = sudsCurrentCourse.getEtextIsbns().split(",");

            for (SectionUIDisplay sectionUIDisplay : sectionWrapper.getAddList()) {
                String sectionUIDisplaySectionName = sectionUIDisplay.getSectionName();

                // Needed in case of in impersonation mode the section name will read
                // FA20-blah-blah-blah-1234 (FA20-blah-blah-blah-1234)
                int indexOfParenthesis = sectionUIDisplaySectionName.indexOf("(");

                if (indexOfParenthesis != -1) {
                    sectionUIDisplaySectionName = sectionUIDisplaySectionName.substring(0, indexOfParenthesis).trim();
                }

                SudsCourse sudsCrosslistCourse = sudsApi.getSudsCourseBySiteId(sectionUIDisplaySectionName);

                if (sudsCrosslistCourse == null || sudsCrosslistCourse.getEtextIsbns() == null) {
                    String sectionName = sectionUIDisplay.getSectionName();
                    missingEtextSections.add(sectionName);

                    List<SectionUIDisplay> addList = sectionWrapper.getAddList();
                    List<SectionUIDisplay> finalList = sectionWrapper.getFinalList();

                    sectionWrapper.setAddList(removeSectionUiDisplayBySectionName(addList, sectionName));
                    sectionWrapper.setFinalList(removeSectionUiDisplayBySectionName(finalList, sectionName));
                    uncheckSectionUiDisplayBySectionId(sectionUIDisplay.getSectionId(), sectionList);
                } else {
                    List<String> crosslistCourseEtextIsbns = Arrays.asList(sudsCrosslistCourse.getEtextIsbns().split(","));

                    for (String currentCourseEtextIsbn : currentCourseEtextIsbns) {
                        boolean exists = crosslistCourseEtextIsbns.contains(currentCourseEtextIsbn);

                        if (! exists) {
                            String sectionName = sectionUIDisplay.getSectionName();
                            missingEtextSections.add(sectionName);

                            List<SectionUIDisplay> addList = sectionWrapper.getAddList();
                            List<SectionUIDisplay> finalList = sectionWrapper.getFinalList();

                            sectionWrapper.setAddList(removeSectionUiDisplayBySectionName(addList, sectionName));
                            sectionWrapper.setFinalList(removeSectionUiDisplayBySectionName(finalList, sectionName));
                            uncheckSectionUiDisplayBySectionId(sectionUIDisplay.getSectionId(), sectionList);
                        }
                    }
                }
            }

            if (missingEtextSections.size() > 0) {
                model.addAttribute("missingEtextSections", missingEtextSections);
            }
        } // end etexts

        model.addAttribute("summaryListSections", sectionWrapper.getFinalList());
        model.addAttribute("addListSections", sectionWrapper.getAddList());

        ImpersonationModel impersonationModel = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, ImpersonationModel.class);

        if (impersonationModel == null) {
            impersonationModel = new ImpersonationModel();
        }
        model.addAttribute("impersonationModel", impersonationModel);

        return "confirmation";
    }

    @RequestMapping(value = {"/{courseId}/confirm", "/{courseId}/continue"}, method = RequestMethod.POST, params="action=" + CrosslistConstants.ACTION_CANCEL)
    @Secured({LTIConstants.ADMIN_AUTHORITY, LTIConstants.INSTRUCTOR_AUTHORITY})
    public String doCancel(@PathVariable("courseId") String courseId, Model model, HttpSession session) {
        log.debug("doCancel");
        return main(courseId, model, session);
    }

    @RequestMapping(value = "/{courseId}/confirm", method = RequestMethod.POST, params="action=" + CrosslistConstants.ACTION_EDIT)
    @Secured({LTIConstants.ADMIN_AUTHORITY, LTIConstants.INSTRUCTOR_AUTHORITY})
    public String doEditConfirmation(@PathVariable("courseId") String courseId, Model model, HttpSession session) {
        log.debug("doEdit");
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);

        ImpersonationModel impersonationModel = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, ImpersonationModel.class);

        if (impersonationModel == null) {
            impersonationModel = new ImpersonationModel();
        }
        model.addAttribute("impersonationModel", impersonationModel);

        // grab these objects from the token
        Map<CanvasTerm,List<SectionUIDisplay>> sectionMap = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.SECTION_MAP_KEY, Map.class);
        List<CanvasTerm> selectableTerms = courseSessionService.getAttributeFromSession(session, courseId,
              "selectableTerms", List.class);

        // remove terms already in the display from selectableTerms so they're not in the dropdown
        for (CanvasTerm sectionTerm : sectionMap.keySet()) {
            if (selectableTerms.contains(sectionTerm)) {
                selectableTerms.remove(sectionTerm);
            }
        }

        model.addAttribute(CrosslistConstants.MODE_EDIT, true);
        return showMainPage(token, sectionMap, selectableTerms, model, session);
    }

    @RequestMapping(value = "/{courseId}/confirm", method = RequestMethod.POST, params="action=" + CrosslistConstants.ACTION_SUBMIT)
    @Secured({LTIConstants.ADMIN_AUTHORITY, LTIConstants.INSTRUCTOR_AUTHORITY})
    public String doSubmitConfirmation(@PathVariable("courseId") String courseId, Model model, HttpSession session) {
        log.debug("doSubmit");
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);

        ImpersonationModel impersonationModel = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, ImpersonationModel.class);

        if (impersonationModel == null) {
            impersonationModel = new ImpersonationModel();
        }
        model.addAttribute("impersonationModel", impersonationModel);

        String currentUserId = impersonationModel.getUsername() == null ? (String)token.getPrincipal(): impersonationModel.getUsername();

        List<SectionUIDisplay> sectionList = courseSessionService.getAttributeFromSession(session, courseId,
              CrosslistAuthenticationToken.SECTION_LIST_KEY, List.class);
        SectionWrapper sectionWrapper = processSections(sectionList);
        boolean hasSuccesses = false;
        boolean hasErrors = false;

        Set<String> courses2Evict = new HashSet<>();
        courses2Evict.add(courseId);

        for (SectionUIDisplay sectionUi : sectionWrapper.getAddList()) {
            if (sectionUi.isDisplayCrosslistedElsewhereWarning()) {
                log.debug("Need to uncrosslist " + sectionUi.getSectionId() + " first...");
                //Look up the section, so we can get the course it is in
                Section section = sectionsApi.getSection(sectionUi.getSectionId());
                //Get it's course id, so we can clear the section cache from the previous course
                courses2Evict.add(section.getCourseId());
            }

            log.debug("Crosslisting " + sectionUi.getSectionId() + " into course " + courseId);

            Section section = sectionsApi.crossList(sectionUi.getSectionId(), courseId);
            if (section != null) {
                log.debug("Crosslisted Section: " + section);
                hasSuccesses = true;
            } else {
                hasErrors = true;
            }
        }

        for (SectionUIDisplay sectionUi : sectionWrapper.getRemoveList()) {
            log.debug("Decrosslisting " + sectionUi.getSectionId() + " from course " + courseId);

            Section section = sectionsApi.decrossList(sectionUi.getSectionId());
            if (section != null) {
                log.debug("Decrosslisted Section: " + section);
                hasSuccesses = true;
            } else {
                hasErrors = true;
            }
        }

        if (hasErrors || hasSuccesses) {
            SubmissionStatus status = new SubmissionStatus();
            String messageKey = null;
            String pageTitle = "";

            if (hasSuccesses && !hasErrors) {
                status.setStatusClass(CrosslistConstants.STATUS_SUCCESS);
                messageKey = "status.success";
                pageTitle = "Success - Cross-listing Assistant";
            } else if (hasSuccesses) {
                status.setStatusClass(CrosslistConstants.STATUS_PARTIAL);
                messageKey = "status.partial";
                pageTitle = "Some sites cross-listed - Cross-listing Assistant";
            } else {
                status.setStatusClass(CrosslistConstants.STATUS_FAILED);
                messageKey = "status.error";
                pageTitle = "Error has occurred - Cross-listing Assistant";
            }

            String statusMessage = messageSource.getMessage(messageKey, null, Locale.getDefault());
            status.setStatusMessage(statusMessage);
            model.addAttribute("submissionStatus", status);

            // Page title
            model.addAttribute("pageTitle", pageTitle);
            evictCourseIdAndSectionsFromCache(courses2Evict, sectionWrapper, currentUserId);
        }

        return main(courseId, model, session);
    }

    /**
     *
     * @param courseId CourseId for the current course
     * @param termId The id of the term that's being asked to load into the page
     * @param sectionListJson The json of the objects on the page, used to retain the Map
     * @param model the model!
     * @return returns the fragment for termData
     */
    @RequestMapping(value = "/{courseId}/loadTerm/{termId}", method = RequestMethod.POST)
    public String doTermLoad(@PathVariable("courseId") String courseId, @PathVariable("termId") String termId,
                             @RequestParam("sectionList") String sectionListJson, @RequestParam("collapsedTerms") String collapsedTerms,
                             Model model, HttpSession session) {
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);
        Course currentCourse = getValidatedCourse(token, session);
        Comparator<CanvasTerm> termStartDateComparator = crosslistService.getTermStartDateComparator();

        boolean featureEnabled = crosslistService.checkForFeature(session, currentCourse, FEATURE_MULTITERM_CROSSLISTING);
        if (featureEnabled) {
            ImpersonationModel impersonationModel = courseSessionService.getAttributeFromSession(session, courseId,
                  CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, ImpersonationModel.class);

            if (impersonationModel == null) {
                impersonationModel = new ImpersonationModel();
            }
            model.addAttribute("impersonationModel", impersonationModel);

            String currentUserId = impersonationModel.getUsername() == null ? (String)token.getPrincipal(): impersonationModel.getUsername();

            List<SectionUIDisplay> sectionList = null;
            try {
                sectionList = Arrays.asList(objectMapper.readValue(sectionListJson, SectionUIDisplay[].class));
            } catch (IOException e) {
                log.error("unable to parse json into object", e);
            }

            List<String> collapsedTermsList = new ArrayList<>();
            if (collapsedTerms.length() > 0) {
                collapsedTermsList = Arrays.asList(collapsedTerms.split(","));
            }

            Map<CanvasTerm,List<SectionUIDisplay>> rebuiltTermMap = new TreeMap<>(termStartDateComparator);

            // get the list of terms in Canvas
            List<CanvasTerm> terms = termsApi.getEnrollmentTerms();

            // convert to a map for easier lookup later
            Map<String,CanvasTerm> termMap = terms.stream().collect(Collectors.toMap(CanvasTerm::getId,Function.identity()));

            // add fake canvas term for unavailable list in the UI
            CanvasTerm alienSectionBlockedFakeCanvasTerm = crosslistService.getAlienBlockedCanvasTerm();
            termMap.put(alienSectionBlockedFakeCanvasTerm.getId(), alienSectionBlockedFakeCanvasTerm);

            // rebuild the json feed into the Map
            for (SectionUIDisplay sectionUI : sectionList) {
                List<SectionUIDisplay> uiSection = new ArrayList<>();
                boolean newEntry = true;

                // Look up if this term is in the otherSectionsMap
                if (rebuiltTermMap.containsKey(termMap.get(sectionUI.getTermId()))) {
                    // This term is in our map, so use it
                    uiSection = rebuiltTermMap.get(termMap.get(sectionUI.getTermId()));
                    newEntry = false;
                }

                //If a term has been selected that has no sections, we don't want to add an actual object for it
                if (sectionUI.getSectionId() != null) {
                    uiSection.add(sectionUI);
                }
                if (newEntry) {
                    rebuiltTermMap.put(termMap.get(sectionUI.getTermId()), uiSection);
                }
            }

            // Look up the new course/section information for the requested term
            List<Course> courses = crosslistService.getCoursesTaughtBy(currentUserId, false);
            courses = courses.stream().filter(c -> c.getEnrollmentTermId() != null && c.getEnrollmentTermId().equals(termId)).collect(Collectors.toList());
            List<SectionUIDisplay> uiSection = new ArrayList<>();
            // get the CanvasTerm object for use later for the map
            CanvasTerm termForModel = terms.stream().filter(term -> term.getId().equals(termId)).findFirst().orElse(null);

            // add the new term's sections to the map
            for (Course course : courses) {
                List<Section> listOfSections = coursesApi.getCourseSections(course.getId());

                for (Section section : listOfSections) {
                    final String impersonationUsername = impersonationModel.getUsername();
                    final String courseCode = course.getCourseCode();

                    // As the instructor (NOT using the tool's impersonation)
                    if ((impersonationUsername == null && section.getSisCourseId() != null && section.getSisSectionId() != null)
                            ||
                    // using the tool's impersonation
                        (impersonationUsername != null && courseCode != null)) {
                        uiSection.add(new SectionUIDisplay(termId, section.getId(),
                              crosslistService.buildSectionDisplayName(section.getName(), courseCode, impersonationModel.getUsername() != null),
                              false, false, false));
                    }
                }

                rebuiltTermMap.put(termForModel, uiSection);
            }

            // Make sure the sections are still sorted
            Comparator<SectionUIDisplay> nameComparator = Comparator.comparing(SectionUIDisplay::getSectionName, Comparator.nullsFirst(Comparator.naturalOrder()));
            rebuiltTermMap.values().forEach(sectionUIDisplays -> sectionUIDisplays.sort(nameComparator));

            model.addAttribute("activeCourseSections", rebuiltTermMap.get(currentCourse.getTerm()));
            model.addAttribute("sectionsMap", rebuiltTermMap);
            model.addAttribute("courseTitle", currentCourse.getName());
            model.addAttribute("courseId", currentCourse.getId());
            model.addAttribute("activeTerm", currentCourse.getTerm());
            model.addAttribute("multiTermEnabled", featureEnabled);
            model.addAttribute("collapsedTerms", collapsedTermsList);
        }

        return "fragments/termData :: termData";
    }

    private SectionWrapper processSections(List<SectionUIDisplay> sectionList) {
        Map<String, List<SectionUIDisplay>> resultMap =
                sectionList.stream().collect(Collectors.groupingBy(SectionUIDisplay::getResultType));

        //Get the existing saved ones, as well as the newly added ones
        List<SectionUIDisplay> finalList = nullSafeList(resultMap.get(SectionUIDisplay.TYPE.SAVED.name()));
        finalList.addAll(nullSafeList(resultMap.get(SectionUIDisplay.TYPE.ADDED.name())));

        SectionWrapper sectionWrapper = new SectionWrapper();
        sectionWrapper.setFinalList(finalList);
        sectionWrapper.setAddList(nullSafeList(resultMap.get(SectionUIDisplay.TYPE.ADDED.name())));
        sectionWrapper.setRemoveList(nullSafeList(resultMap.get(SectionUIDisplay.TYPE.REMOVED.name())));

        return sectionWrapper;
    }

    /**
     * Returns an empty list instead of null
     * @param sectionList
     * @return
     */
    private List<SectionUIDisplay> nullSafeList(List<SectionUIDisplay> sectionList) {
        if (sectionList == null) {
            return new ArrayList<>();
        }
        return sectionList;
    }

    /**
     * Updates various caches based on the cross-listing or de-cross-listing that has occurred
     * This method assumes that some activity has happened
     * @param courses2Evict
     * @param sectionWrapper
     */
    private void evictCourseIdAndSectionsFromCache(Set<String> courses2Evict, @NonNull SectionWrapper sectionWrapper,
                                                   @NonNull String currentUserId) {
        Cache courseSectionsCache = cacheManager.getCache(CrosslistConstants.COURSE_SECTIONS_CACHE_NAME);
        Set<String> courseIds = new HashSet<>();
        if (courseSectionsCache != null) {
            courseIds.addAll(courses2Evict);

            for (SectionUIDisplay sectionUIDisplay : sectionWrapper.getAddList()) {
                // evict the old parent from the cache
                Section section = sectionsApi.getSection(sectionUIDisplay.getSectionId());
                if (section != null && section.getNonxlistCourseId() != null) {
                    courseIds.add(section.getNonxlistCourseId());
                }
            }

            for (SectionUIDisplay sectionUIDisplay : sectionWrapper.getRemoveList()) {
                // evict the old section's parent from the cache, although it may not exist
                Section section = sectionsApi.getSection(sectionUIDisplay.getSectionId());
                if (section != null && section.getCourseId() != null) {
                    courseIds.add(section.getCourseId());
                }
            }

            // Evict all courseIds from the cache
            for (String courseId2Evict : courseIds) {
                courseSectionsCache.evict(courseId2Evict);
            }

            // if there're items in here, clear the coursesTaughtBy cache to get updated data
            if (!sectionWrapper.getRemoveList().isEmpty()) {
                evictCoursesTaughtByCache(currentUserId);
            }
        }
    }

    /**
     * Evicts values from the CoursesTaughtBy cache
     * @param currentUserId User values that we want removed from the cache
     */
    private void evictCoursesTaughtByCache(String currentUserId) {
        Cache coursesTaughtByCache = cacheManager.getCache(CrosslistConstants.COURSES_TAUGHT_BY_CACHE_NAME);
        // this false currently works since that's exclusively true in CrosslistController
        coursesTaughtByCache.evict(currentUserId + "-" + false);
    }

    @PostMapping(value = "/{courseId}/impersonate", params="action=" + CrosslistConstants.ACTION_IMPERSONATE)
    @Secured({LTIConstants.ADMIN_AUTHORITY})
    public String beginImpersonation(@PathVariable("courseId") String courseId, @ModelAttribute ImpersonationModel impersonationModel, Model model, HttpSession session) {
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);
        courseSessionService.addAttributeToSession(session, courseId, CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY, impersonationModel);
        return main(courseId, model, session);
    }

    @PostMapping(value = "/{courseId}/impersonate", params="action=" + CrosslistConstants.ACTION_END_IMPERSONATE)
    @Secured({LTIConstants.ADMIN_AUTHORITY})
    public String endImpersonation(@PathVariable("courseId") String courseId, @ModelAttribute ImpersonationModel impersonationModel, Model model, HttpSession session) {
        LtiAuthenticationToken token = getValidatedToken(courseId, courseSessionService);
        courseSessionService.removeAttributeFromSession(session, courseId, CrosslistAuthenticationToken.IMPERSONATION_DATA_KEY);
        return main(courseId, model, session);
    }

    private List<SectionUIDisplay> removeSectionUiDisplayBySectionName(@NonNull List<SectionUIDisplay> oldList, @NonNull String toRemoveSectionName) {
        List<SectionUIDisplay> newList = new ArrayList<SectionUIDisplay>();

        for (SectionUIDisplay sectionUIDisplay : oldList) {
            String sectionName = sectionUIDisplay.getSectionName();

            if (! toRemoveSectionName.equals(sectionName)) {
                newList.add(sectionUIDisplay);
            }
        }

        return newList;
    }

    private void uncheckSectionUiDisplayBySectionId(@NonNull String sectionId, @NonNull List<SectionUIDisplay> courseList) {
        for (SectionUIDisplay sectionUIDisplay : courseList) {
            if (sectionId.equals(sectionUIDisplay.getSectionId())) {
                sectionUIDisplay.setCurrentlyChecked(false);

                return;
            }
        }
    }
}
