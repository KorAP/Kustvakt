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

RUN rm -r core && \
    rm -r full && \
    rm -r lite && \
    rm -r sample-index

RUN apk del git \
            maven

RUN cd ${M2_HOME} && rm -r .m2

FROM openjdk:8-alpine

RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    mkdir kustvakt && \
    chown -R kustvakt.korap /kustvakt

WORKDIR /kustvakt

COPY --from=builder /kustvakt/built/* /kustvakt/

USER kustvakt

EXPOSE 8089

ENTRYPOINT [ "java", "-jar" ]

CMD [ "Kustvakt-lite.jar" ]

# TODO:
#   - support lite build
#   - support full build
