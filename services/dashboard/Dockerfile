FROM eclipse-temurin:25-jre-noble AS layers
WORKDIR /workspace
COPY dashboard/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM gcr.io/distroless/java25-debian13
WORKDIR /app
USER nonroot

COPY --from=layers /workspace/extracted/dependencies/ ./
COPY --from=layers /workspace/extracted/spring-boot-loader/ ./
COPY --from=layers /workspace/extracted/snapshot-dependencies/ ./
COPY --from=layers /workspace/extracted/application/ ./

EXPOSE 8083
ENTRYPOINT ["org.springframework.boot.loader.launch.JarLauncher"]
