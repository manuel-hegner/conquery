FROM gcr.io/google-appengine/openjdk:11

COPY executable/target/executable*.jar executable.jar
COPY config.yml config.yml
CMD [ "java", "-jar","executable.jar", "server", "config.yml"]