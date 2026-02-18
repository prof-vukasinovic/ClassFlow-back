FROM tomcat:10.1-jdk21-temurin

# Clean default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy your custom WAR into Tomcat
# Adjust the path/name if your WAR file is different.
COPY libs/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
