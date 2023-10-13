# Use alpine linux as base image
FROM openjdk:19-alpine as builder

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
    mvn clean install

RUN rm -r Koral-* v*.zip

RUN mkdir built

# Install Krill
RUN curl -I https://github.com/KorAP/Krill/releases/latest | \
      grep location | \
      perl -e '$|++; <> =~ m/tag\/(v[\d\.]+(?:-release)?)/; print "https://github.com/KorAP/Krill/archive/refs/tags/${1}\.zip\n"' |\
      wget -i - && \
    unzip *.zip && \
    cd Krill-* && \
    mvn clean install && \
    mvn -Dmaven.test.skip=true package && \
    mv target/Krill-Indexer.jar /kustvakt/built/Krill-Indexer.jar

RUN rm -r Krill-* v*.zip

# Package lite
RUN cd full && \
    mvn clean package -P lite && \
    find target/Kustvakt-lite-*.jar -exec mv {} ../built/Kustvakt-lite.jar ';'

RUN sed 's!\(krill\.indexDir\s*=\).\+!\1\/kustvakt\/index!' full/src/main/resources/kustvakt-lite.conf \
    > built/kustvakt-lite.conf

# Package full
RUN cd full && \
    mvn clean package -DskipTests=true && \
    find target/Kustvakt-full-*.jar -exec mv {} ../built/Kustvakt-full.jar ';'

RUN cat full/src/main/resources/kustvakt.conf | \
    sed 's!\(krill\.indexDir\s*=\).\+!\1\/kustvakt\/index!' | \
    sed 's!\(ldap\.config\s*=\).\+!\1\/kustvakt\/ldap\/ldap\.conf!' | \
    sed 's!\(oauth2\.initial\.super\.client\s*=\).\+!\1\/true!' | \
    sed '$ a oauth2.initial.super.client = true' \
    > built/kustvakt.conf

RUN sed  's!\(ldifFile\s*=\).\+!\1\/kustvakt\/ldap\/ldap.ldif!' \
    full/src/main/resources/embedded-ldap-example.conf \
    > built/ldap.conf

RUN cat full/src/main/resources/example-users.ldif \
    > built/ldap.ldif

# Cleanup
RUN rm -r full && \
    rm -r sample-index && \
    rm -r wiki-index

RUN apk del git \
            perl \
            curl \
            wget \
            maven

RUN cd ${M2_HOME} && rm -r .m2

FROM openjdk:19-alpine AS kustvakt-lite

RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    mkdir kustvakt && \
    chown -R kustvakt.korap /kustvakt

WORKDIR /kustvakt

COPY --from=builder /kustvakt/built/Kustvakt-lite.jar /kustvakt/
COPY --from=builder /kustvakt/built/kustvakt-lite.conf /kustvakt/
COPY --from=builder /kustvakt/built/Krill-Indexer.jar /kustvakt/

USER kustvakt

EXPOSE 8089

ENTRYPOINT [ "java", "-jar" ]

CMD [ "Kustvakt-lite.jar" ]

FROM openjdk:19-alpine AS kustvakt-full

RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    mkdir kustvakt && \
    chown -R kustvakt.korap /kustvakt

WORKDIR /kustvakt

RUN mkdir ./ldap \
    mkdir ./client

COPY --from=builder /kustvakt/built/Kustvakt-full.jar /kustvakt/
COPY --from=builder /kustvakt/built/kustvakt.conf /kustvakt/
COPY --from=builder /kustvakt/built/ldap.* /kustvakt/ldap/
COPY --from=builder /kustvakt/built/Krill-Indexer.jar /kustvakt/

USER kustvakt

EXPOSE 8089

ENTRYPOINT [ "java", "-jar" ]

CMD [ "Kustvakt-full.jar" ]

# docker build -f Dockerfile -t korap/kustvakt:{nr}-full --target kustvakt-full .
# docker build -f Dockerfile -t korap/kustvakt:{nr} --target kustvakt-lite .
