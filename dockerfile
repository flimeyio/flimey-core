FROM adoptopenjdk/openjdk14:jdk-14.0.2_12-ubuntu-slim

RUN mkdir /flimeytmp
COPY . /flimeytmp
WORKDIR /flimeytmp

RUN apt-get update
RUN apt-get -y install gnupg2
RUN apt-get -y install curl

RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add

RUN apt-get update
RUN apt-get -y install sbt

RUN sbt dist;



FROM ubuntu:20.04

EXPOSE 9080
EXPOSE 9443

RUN mkdir /flimeytmp
RUN mkdir /flimey
RUN mkdir /flimeydata

WORKDIR /flimey

COPY --from=0 /flimeytmp /flimeytmp

RUN apt-get update
RUN apt-get -y install unzip
RUN apt-get -y install openjdk-14-jre
RUN apt-get -y install postgresql
RUN apt-get -y install sudo; adduser root sudo

RUN mv -v /flimeytmp/run.sh /flimey/run.sh;mv -v /flimeytmp/target/universal/* /flimey
RUN rm -r "./scripts"
RUN unzip *.zip && rm *.zip
RUN rm -r "/flimeytmp"

RUN FLIMEYBIN=$(find . -name "flimey-core" -print); echo $FLIMEYBIN; chmod 777 "$FLIMEYBIN"; echo "${FLIMEYBIN} -Dplay.evolutions.db.flimey_session.autoApply=true -Dplay.evolutions.db.flimey_data.autoApply=true -Dconfig.file=/flimey/runtime.conf" >> run.sh
RUN chmod 777 run.sh

CMD ./run.sh
