FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:c98e1c3f3d4a1db634ef62f8c564e6541325b9635abeeb1cbe8cf50c7db6fdd1
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar app.jar
# Custom OTEL Java agent extension that rewrites spurious ERROR statuses on the
# SSE notification endpoint span (caused by client disconnects) to OK.
# Loaded by the OTEL auto-instrumentation agent via OTEL_JAVAAGENT_EXTENSIONS.
COPY otel-extension/build/libs/otel-extension.jar otel-extension.jar
CMD ["-jar","app.jar"]