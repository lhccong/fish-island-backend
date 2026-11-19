FROM eclipse-temurin:17 AS ocr-native

ARG JAR_FILE=fish-island-backend-0.0.1-SNAPSHOT.jar

RUN apt-get update \
    && apt-get install -y --no-install-recommends patchelf \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /ocr-build
COPY ${JAR_FILE} app.jar
# Recent glibc refuses to load this upstream library with an executable stack.
RUN set -eu; \
    native_jar="$(jar tf app.jar | sed -n '/^BOOT-INF\/lib\/rapidocr-onnx-linux-x86_64-.*\.jar$/p')"; \
    test -n "$native_jar" || { echo "RapidOCR Linux runtime missing from the JAR. Rebuild the backend with Maven." >&2; exit 1; }; \
    jar xf app.jar "$native_jar"; \
    jar xf "$native_jar" lib/libRapidOcr.so; \
    patchelf --clear-execstack lib/libRapidOcr.so

FROM eclipse-temurin:17

ENV workdir=/thome/1panel/cong/fish
WORKDIR ${workdir}
COPY --from=ocr-native /ocr-build/app.jar ./fish-island-backend-0.0.1-SNAPSHOT.jar
COPY --from=ocr-native /ocr-build/lib/libRapidOcr.so /tmp/ocrJava/onnx/libRapidOcr.so
RUN chmod +x fish-island-backend-0.0.1-SNAPSHOT.jar
EXPOSE 8123
CMD ["java", "-Duser.timezone=GMT+08", "-jar", "fish-island-backend-0.0.1-SNAPSHOT.jar"]
