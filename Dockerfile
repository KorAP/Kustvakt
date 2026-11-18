# *** WARNING *** This Dockerfile has been updated to build one image that can run both full and lite versions. The changes require updating existing docker compose files accordingly.
# Backward compatilibity is added to support older docker compose deployments

# Corretto 25.0.5.1.2 fixes CVE-2026-114938 (Java4Shell).
# Pin the patched builds: the upstream tag omits the security patch suffix.
# Keep the Java SE APIs and runtime providers used by authentication, TLS,
# JDBC, locales and diagnostics, without shipping the compiler or toolchain.
ARG JAVA_MODULES=java.se,jdk.charsets,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.localedata,jdk.naming.dns,jdk.naming.rmi,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.zipfs,jdk.management,jdk.management.agent,jdk.httpserver,jdk.attach,jdk.jcmd
FROM amazoncorretto:25.0.5-alpine3.23@sha256:6198a628ef255ef0804ce8240303968d417eb30d6286f58857fea9ece3af1963 AS java-runtime-alpine
ARG JAVA_MODULES
RUN apk add --no-cache binutils && \
    jlink --add-modules "$JAVA_MODULES" --strip-debug --compress=zip-6 \
    --no-header-files --no-man-pages --output /opt/java/runtime && \
    rm /opt/java/runtime/lib/security/cacerts && \
    cp -L "$JAVA_HOME/lib/security/cacerts" /opt/java/runtime/lib/security/cacerts

FROM java-runtime-alpine AS builder
# Copy repository respecting .dockerignore
COPY . /kustvakt
WORKDIR /kustvakt
RUN apk update && \
    apk add --no-cache git \
            curl \
            perl \
            wget \
            maven
RUN git config --global user.email "korap+docker@ids-mannheim.de" && \
    git config --global user.name "Docker"

# Install Koral
RUN curl -I https://github.com/KorAP/Koral/releases/latest | \
      grep location | \
      perl -e '$|++; <> =~ m/tag\/(v[\d\.]+(?:-release)?)/; print "https://github.com/KorAP/Koral/archive/refs/tags/${1}\.zip\n"' |\
      wget -i - && \
    unzip *.zip && \
    cd Koral-* && \
    mvn -Djvm=/opt/java/runtime/bin/java clean install
RUN rm -r Koral-* v*.zip
RUN mkdir built

# Install Krill
RUN curl -I https://github.com/KorAP/Krill/releases/latest | \
      grep location | \
      perl -e '$|++; <> =~ m/tag\/(v[\d\.]+(?:-release)?)/; print "https://github.com/KorAP/Krill/archive/refs/tags/${1}\.zip\n"' |\
      wget -i - && \
    unzip *.zip && \
    cd Krill-* && \
    mvn -Djvm=/opt/java/runtime/bin/java clean install && \
    mvn -Dmaven.test.skip=true package && \
    mv target/Krill-Indexer.jar /kustvakt/built/Krill-Indexer.jar
RUN rm -r Krill-* v*.zip

# Kustvakt
# --legacy-start
# RUN mvn clean package -P full && \
#    find target/Kustvakt-full-*.jar -exec mv {} /kustvakt/built/Kustvakt-full.jar ';'
# --legacy-end
RUN mvn -Djvm=/opt/java/runtime/bin/java clean package && \
    find target/Kustvakt-*.jar -exec mv {} /kustvakt/built/Kustvakt.jar ';'
RUN sed 's!\(krill\.indexDir\s*=\).\+!\1\/kustvakt\/index!' src/main/resources/kustvakt-lite.conf \
    > built/kustvakt-lite.conf
RUN cat src/main/resources/kustvakt.conf | \
    sed 's!\(krill\.indexDir\s*=\).\+!\1\/kustvakt\/index!' | \
    sed 's!\(ldap\.config\s*=\).\+!\1\/kustvakt\/ldap\/ldap\.conf!' | \
    sed 's!\(oauth2\.initial\.super\.client\s*=\).\+!\1\/true!' | \
    sed '$ a oauth2.initial.super.client = true' \
    > built/kustvakt.conf
RUN sed  's!\(ldifFile\s*=\).\+!\1\/kustvakt\/ldap\/ldap.ldif!' \
    src/main/resources/embedded-ldap-example.conf \
    > built/ldap.conf
RUN cat src/main/resources/example-users.ldif \
    > built/ldap.ldif
RUN apk del git \
            perl \
            curl \
            wget \
            maven
RUN cd ${M2_HOME} && rm -r .m2

# Cleanup
RUN rm -r src && \
    rm -r wiki-index


FROM busybox:latest AS example-index
WORKDIR /kustvakt
RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    mkdir kustvakt && \
    chown -R kustvakt:korap /kustvakt
COPY --from=builder /kustvakt/sample-index /kustvakt/index
USER kustvakt
CMD ["sh"]


# Not Alpine: musl >= 1.2.6 breaks SQLite writes under Docker on old
# hosts like CentOS 7 (pwritev2 gets EPERM instead of ENOSYS)
FROM amazoncorretto:25.0.5-al2023@sha256:8b82be5adeabd374f5438e90635a120868e07ea8e85be6b691eb1c994d53f1e2 AS java-runtime-glibc
ARG JAVA_MODULES
RUN yum install -y binutils && \
    jlink --add-modules "$JAVA_MODULES" --strip-debug --compress=zip-6 \
    --no-header-files --no-man-pages --output /opt/java/runtime && \
    rm /opt/java/runtime/lib/security/cacerts && \
    cp -L "$JAVA_HOME/lib/security/cacerts" /opt/java/runtime/lib/security/cacerts

FROM public.ecr.aws/amazonlinux/amazonlinux:2023-minimal@sha256:c64f3b683d01c1c874831adc6a7b5a2fd6f0f9affd58bc638939a9f46cc03543 AS kustvakt
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=java-runtime-glibc /opt/java/runtime /opt/java/openjdk
RUN microdnf install -y --setopt=install_weak_deps=0 shadow-utils libstdc++ zlib ca-certificates fontconfig freetype && \
    microdnf clean all && \
    groupadd --system korap && \
    useradd --system -g korap kustvakt && \
    mkdir kustvakt && \
    chown -R kustvakt:korap /kustvakt
WORKDIR /kustvakt
USER kustvakt
RUN mkdir ./ldap && \
    mkdir ./client && \
    mkdir ./data
COPY --from=builder /kustvakt/built/Kustvakt.jar /kustvakt
COPY --from=builder /kustvakt/built/kustvakt.conf /kustvakt/data/
COPY --from=builder /kustvakt/built/kustvakt-lite.conf /kustvakt/data/
COPY --from=builder /kustvakt/built/ldap.* /kustvakt/ldap/
COPY --from=builder /kustvakt/built/Krill-Indexer.jar /kustvakt/
EXPOSE 8089
ENTRYPOINT [ "java", "--enable-native-access=ALL-UNNAMED", "-jar"]
CMD ["Kustvakt.jar"]

# docker build -f Dockerfile -t korap/kustvakt:{nr} -t korap/kustvakt:{nr}-full .
# docker build -f Dockerfile -t korap/example-index:{nr} --target example-index .
# run lite version (default)
# docker run --rm --net host -v [absolute-path-sample-index]:/kustvakt/index korap/kustvakt:{nr}
# run full version 
# docker run --rm --net host -v [absolute-path-sample-index]:/kustvakt/index korap/kustvakt:{nr} Kustvakt.jar --full
