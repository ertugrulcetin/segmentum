FROM openjdk:8-alpine

COPY target/uberjar/segmentum.jar /segmentum/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/segmentum/app.jar"]
