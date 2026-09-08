FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:2f261adc67d3d0d14ce5106796ec4bf3bf2709b8eec5e1fb06b1e035bffaa56c
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar app.jar
# Custom OTEL Java agent extension that rewrites spurious ERROR statuses on the
# SSE notification endpoint span (caused by client disconnects) to OK.
# Loaded by the OTEL auto-instrumentation agent via OTEL_JAVAAGENT_EXTENSIONS.
COPY otel-extension/build/libs/otel-extension.jar otel-extension.jar
CMD ["-jar","app.jar"]