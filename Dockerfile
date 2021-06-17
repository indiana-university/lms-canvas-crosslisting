FROM registry.docker.iu.edu/lms/microservices_base:1.0.0
MAINTAINER LMS Development Team <iu-uits-lms-dev-l@list.iu.edu>

CMD exec java $JAVA_OPTS -jar /usr/src/app/lms-lti-crosslist.jar

EXPOSE 5005

COPY --chown=lms:root target/lms-lti-crosslist.jar /usr/src/app/