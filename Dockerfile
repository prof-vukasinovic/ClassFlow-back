# A mettre dans test/ClassFlowApp/ClassFlow-back/Dockerfile pour telecharger et lancer la derniere release du projet sur GitHub

FROM tomcat:10.1-jdk21-temurin

# Nettoie les webapps + installe curl
RUN rm -rf /usr/local/tomcat/webapps/* \
 && apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# URL API GitHub (latest release)
ENV GH_API="https://api.github.com/repos/prof-vukasinovic/ClassFlow-back/releases/latest"

# 1) récupère l’URL du .war latest
# 2) télécharge en ROOT.war
# 3) adapte Tomcat au port Render ($PORT)
# 4) démarre Tomcat
CMD ["sh","-lc", "\
set -e; \
WAR_URL=$(curl -fsSL \"$GH_API\" | sed -n 's/.*\"browser_download_url\":[ ]*\"\\([^\"]*\\.war\\)\".*/\\1/p' | head -n 1); \
echo \"Downloading WAR: $WAR_URL\"; \
test -n \"$WAR_URL\"; \
curl -fL --retry 3 --retry-delay 2 -o /usr/local/tomcat/webapps/ROOT.war \"$WAR_URL\"; \
sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml; \
catalina.sh run \
"]