package edu.iu.uits.lms.crosslist;

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
