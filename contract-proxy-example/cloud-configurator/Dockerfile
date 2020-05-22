FROM openjdk:11-jre-slim

WORKDIR /opt

COPY target/*-jar-with-dependencies.jar ./cloud-configurator.jar

CMD ["/bin/sh", "-c", "java -jar cloud-configurator.jar"]