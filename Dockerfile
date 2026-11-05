FROM maven:4.0.0-rc-7-eclipse-temurin-25-noble AS base
WORKDIR /java/app/base

RUN apt-get update 
RUN apt-get install -y build-essential
RUN apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false
RUN rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:resolve -B

FROM maven:4.0.0-rc-7-eclipse-temurin-25-noble AS builder
WORKDIR /java/app/builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    fontconfig \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=base /root/.m2 /root/.m2
COPY . .
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java25-debian13:nonroot AS runtime
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
USER nobody
WORKDIR /app

COPY --from=builder /java/app/builder/docs/GeoLite2-ASN.mmdb /app/geoip/GeoLite2-ASN.mmdb
COPY --from=builder /java/app/builder/docs/GeoLite2-City.mmdb /app/geoip/GeoLite2-City.mmdb
COPY --from=builder /java/app/builder/docs/GeoLite2-Country.mmdb /app/geoip/GeoLite2-Country.mmdb

COPY --from=builder /java/app/builder/target/app.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar" ]

