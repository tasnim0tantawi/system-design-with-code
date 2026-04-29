FROM amazoncorretto:21-alpine
WORKDIR /
COPY target/app.jar /app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app.jar"]
