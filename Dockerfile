FROM tomcat:10.1-jdk21-temurin

# Nettoie les webapps + installe curl
RUN rm -rf /usr/local/tomcat/webapps/* \
 && apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# WAR fixé sur 1.6.0
ENV WAR_URL="https://github.com/prof-vukasinovic/ClassFlow-back/releases/download/1.6.0/classflow-back-1.6.0.war"

# 1) télécharge le .war 1.6.0 en ROOT.war
# 2) adapte Tomcat au port Render ($PORT)
# 3) démarre Tomcat
CMD ["sh","-lc", "\
set -e; \
echo \"Downloading WAR: $WAR_URL\"; \
curl -fL --retry 3 --retry-delay 2 -o /usr/local/tomcat/webapps/ROOT.war \"$WAR_URL\"; \
sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml; \
catalina.sh run \
"]