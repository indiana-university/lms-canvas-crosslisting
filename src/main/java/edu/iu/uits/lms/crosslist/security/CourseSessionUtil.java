package edu.iu.uits.lms.crosslist.security;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for interacting with the session so that we can easily get/set course-specific values
 */
public class CourseSessionUtil {

   /**
    * Add an attribute to the session for a course
    * @param session
    * @param courseId CourseId key to use since the session could be reused for a different course launch
    * @param key Key within the course context
    * @param value Value to set
    */
   public static void addAttributeToSession(HttpSession session, String courseId, String key, Object value)  {
      Map<String, Map<String, Object>> courseMap = (Map<String, Map<String, Object>>) session.getAttribute(CrosslistAuthenticationToken.COURSE_SESSION_KEY);
      if (courseMap == null) {
         courseMap = new HashMap<>();
      }
      Map<String, Object> courseData = courseMap.computeIfAbsent(courseId, k -> new HashMap<>());
      courseData.put(key, value);
      session.setAttribute(CrosslistAuthenticationToken.COURSE_SESSION_KEY, courseMap);
   }

   /**
    * Get an attribute from the session for a course
    * @param session
    * @param courseId CourseId key to use since the session could be reused for a different course launch
    * @param key Key within the course context
    * @param clazz Return type
    * @return The object from the session, or null if not found
    */
   public static <T> T getAttributeFromSession(HttpSession session, String courseId, String key, Class<T> clazz) {
      Map<String, Map<String, Object>> courseMap = (Map<String, Map<String, Object>>) session.getAttribute(CrosslistAuthenticationToken.COURSE_SESSION_KEY);
      if (courseMap != null) {
         Map<String, Object> courseData = courseMap.get(courseId);
         if (courseData != null) {
            return (T)courseData.get(key);
         }
      }
      return null;
   }

   /**
    * Remove an attribute from the session for a course
    * @param session
    * @param courseId CourseId key to use since the session could be reused for a different course launch
    * @param key Key within the course context
    * @return The object from the session, or null if not found
    */
   public static <T> T removeAttributeFromSession(HttpSession session, String courseId, String key) {
      Map<String, Map<String, Object>> courseMap = (Map<String, Map<String, Object>>) session.getAttribute(CrosslistAuthenticationToken.COURSE_SESSION_KEY);
      if (courseMap != null) {
         Map<String, Object> courseData = courseMap.get(courseId);
         if (courseData != null) {
            return (T)courseData.remove(key);
         }
      }
      return null;
   }
}
