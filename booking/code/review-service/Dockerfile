FROM amazoncorretto:21-alpine
WORKDIR /
COPY target/app.jar /app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app.jar"]
