FROM tomcat:10.1-jre17

ARG WAR_FILE=target/classflow-back-1.1.0.war

RUN rm -rf C:/tomcat/webapps/*

COPY ${WAR_FILE} C:/tomcat/webapps/ROOT.war
EXPOSE 8080 
CMD ["catalina.sh", "run"]