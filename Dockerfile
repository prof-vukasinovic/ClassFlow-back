FROM tomcat:10.1-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/* \
 && apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Télécharge le .war de la dernière release GitHub (quel que soit le numéro)

RUN set -e; \
  WAR_URL="$(curl -fsSL https://api.github.com/repos/prof-vukasinovic/ClassFlow-back/releases/latest \
    | sed -n 's/.*"browser_download_url":[ ]*"\([^"]*\.war\)".*/\1/p' \
    | head -n 1)"; \
  echo "Downloading: $WAR_URL"; \
  curl -fL -o /usr/local/tomcat/webapps/ROOT.war "$WAR_URL"

# Render impose un port via $PORT
CMD ["sh","-lc","sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" /usr/local/tomcat/conf/server.xml && catalina.sh run"]
