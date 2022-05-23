# Use alpine linux as base image
FROM openjdk:8-alpine as builder

# Copy repository respecting .dockerignore
COPY . /kustvakt

WORKDIR /kustvakt

RUN apk update && \
    apk add --no-cache git \
            maven

RUN git config --global user.email "korap+docker@ids-mannheim.de" && \
    git config --global user.name "Docker"

# Install Koral
RUN mkdir Koral && git clone https://github.com/KorAP/Koral.git Koral && \
    cd Koral && \
    git checkout master && \
    mvn clean install

RUN rm -r Koral

# Install Krill
RUN mkdir built && \
    git clone https://github.com/KorAP/Krill.git Krill && \
    cd Krill && \
    git checkout master && \
    mvn clean install && \
    mvn -Dmaven.test.skip=true package && \
    mv target/Krill-Indexer.jar /kustvakt/built/Krill-Indexer.jar

RUN rm -r Krill

# Install Kustvakt core
RUN cd core && \
    mvn clean install

# Package lite
RUN cd lite && \
    mvn clean package && \
    find target/Kustvakt-lite-*.jar -exec mv {} ../built/Kustvakt-lite.jar ';'

RUN sed 's!\(krill\.indexDir\s*=\).\+!\1\/kustvakt\/index!' lite/src/main/resources/kustvakt-lite.conf \
    > built/kustvakt-lite.conf

# Package full
RUN cd full && \
    mvn clean package && \
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
RUN rm -r core && \
    rm -r full && \
    rm -r lite && \
    rm -r sample-index

RUN apk del git \
            maven

RUN cd ${M2_HOME} && rm -r .m2

FROM openjdk:8-alpine AS kustvakt-lite

RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    mkdir kustvakt && \
    chown -R kustvakt.korap /kustvakt

WORKDIR /kustvakt

COPY --from=builder /kustvakt/built/Kustvakt-lite.* /kustvakt/

USER kustvakt

EXPOSE 8089

ENTRYPOINT [ "java", "-jar" ]

CMD [ "Kustvakt-lite.jar" ]


FROM openjdk:8-alpine AS kustvakt-full

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

USER kustvakt

EXPOSE 8089

ENTRYPOINT [ "java", "-jar" ]

CMD [ "Kustvakt-full.jar" ]

# docker build -f Dockerfile -t korap/kustvakt:{nr}-full --target kustvakt-full .
# docker build -f Dockerfile -t korap/kustvakt:{nr} --target kustvakt-lite .
