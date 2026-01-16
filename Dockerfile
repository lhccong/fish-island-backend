FROM eclipse-temurin:17

ENV workdir=/cong/fish
COPY target/fish-island-backend-0.0.1-SNAPSHOT.jar ${workdir}/fish-island-backend-0.0.1-SNAPSHOT.jar
WORKDIR ${workdir}
RUN chmod +x fish-island-backend-0.0.1-SNAPSHOT.jar
EXPOSE 8123
CMD ["java", "-jar", "-Duser.timezone=GMT+08", "fish-island-backend-0.0.1-SNAPSHOT.jar"]
