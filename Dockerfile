# Use alpine linux as base image
FROM openjdk:8-alpine

# Copy repository respecting .dockerignore
COPY . /kustvakt

WORKDIR /kustvakt

RUN addgroup -S korap && \
    adduser -S kustvakt -G korap && \
    chown -R kustvakt.korap /kustvakt

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
RUN git clone https://github.com/KorAP/Krill.git Krill && \
    cd Krill && \
    git checkout master && \
    mvn clean install

RUN rm -r Krill

# Install Kustvakt core
RUN cd core && \
    mvn clean install

# Package lite
RUN mkdir built && \
    cd lite && \
    mvn clean package && \
    find target/Kustvakt-lite-*.jar -exec mv {} ../built/Kustvakt-lite.jar ';'

RUN mv lite/src/main/resources/kustvakt-lite.conf built/

RUN rm -r core && \
    rm -r full && \
    rm -r lite

RUN apk del git \
            maven

RUN cd ${M2_HOME} && rm -r .m2

USER kustvakt

CMD ["java", "-jar", "built/kustvakt-lite.jar"]

# TODO:
#   - support lite build
#   - support full build
