FROM openjdk:8
ENV workdir=/var/lib/jenkins/workspace/fish-island-backend/target
COPY . ${workdir}
WORKDIR ${workdir}
EXPOSE 8123
CMD ["java","-jar","-Duser.timezone=GMT+08","fish-island-backend-0.0.1-SNAPSHOT.jar"]
