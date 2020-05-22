FROM openjdk:11-jre-slim

WORKDIR /opt

COPY target/*-jar-with-dependencies.jar ./contract-initiator.jar

CMD ["/bin/sh", "-c", "java -jar contract-initiator.jar"]