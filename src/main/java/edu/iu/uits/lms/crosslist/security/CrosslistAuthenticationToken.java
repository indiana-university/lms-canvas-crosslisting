package edu.iu.uits.lms.crosslist.security;

public interface CrosslistAuthenticationToken {

    /**
     * Ley used to get the course specific session
     */
    String COURSE_SESSION_KEY = "crosslist_course_session";

    /**
     * Key used to get the course out of the data map
     */
    String COURSE_KEY = "course";

    /**
     * Key used to get the section list out of the data map
     */
    String SECTION_LIST_KEY = "section_list";

    /**
     * Key used to get the Map<CanvasTerm,List<SectionUIDisplay>> object out of the data map
     */
    String SECTION_MAP_KEY = "section_map";

    /**
     * Key used to get the list of course instructors out of the data map
     */
    String INSTRUCTORS_KEY = "course_instructors";

    /**
     * Key used to get the impersonation data out of the data map
     */
    String IMPERSONATION_DATA_KEY = "impersonation_data_key";

}
