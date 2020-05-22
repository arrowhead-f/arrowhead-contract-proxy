FROM openjdk:11-jre-slim

WORKDIR /opt

COPY target/*-jar-with-dependencies.jar ./arrowhead-contract-proxy.jar

CMD ["/bin/sh", "-c", "java -jar arrowhead-contract-proxy.jar"]