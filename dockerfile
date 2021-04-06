FROM adoptopenjdk/openjdk14:jdk-14.0.2_12-ubuntu-slim

RUN mkdir /flimey
COPY . /flimey
WORKDIR /flimey

RUN apt-get update
RUN apt-get -y install gnupg2
RUN apt-get -y install curl
RUN apt-get -y install unzip

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add

RUN apt-get update

RUN apt-get -y install sbt
RUN sbt dist

RUN unzip "./target/universal/*.zip"
RUN find . -name "*.zip" -type f -delete
RUN rm -r "./target/universal/scripts"



FROM ubuntu:20.04

EXPOSE 9080
EXPOSE 9443

RUN mkdir /flimey
RUN mkdir /flimeydata

WORKDIR /flimey
RUN apt-get update
RUN apt-get -y install openjdk-14-jre
RUN apt-get -y install postgresql
RUN apt-get -y install sudo; adduser root sudo

COPY --from=0 /flimey/target/universal .
COPY --from=0 /flimey/run.sh .

RUN FLIMEY_BIN=$(find . -name "flimey-core" -print); chmod 777 "$FLIMEY_BIN"; echo "${FLIMEY_BIN} -Dplay.evolutions.db.flimey_session.autoApply=true -Dplay.evolutions.db.flimey_data.autoApply=true -Dconfig.file=/flimey/runtime.conf">> run.sh
RUN chmod 777 run.sh

CMD ./run.sh
