FROM ubuntu:16.04
MAINTAINER oss@lena-brueder.de
RUN apt-get update && apt-get install -y apt-transport-https && apt-get -y dist-upgrade
RUN apt-get install -y openjdk-8-jre-headless git unzip wget
RUN echo "deb https://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823 && \
    apt-get update && \
    apt-get -y install sbt
RUN useradd -ms /bin/bash user
USER user
WORKDIR /home/user
RUN sbt exit
RUN wget https://downloads.typesafe.com/typesafe-activator/1.3.11/typesafe-activator-1.3.11-minimal.zip && \
	unzip typesafe-activator-1.3.11-minimal.zip && \
    chmod a+x activator-1.3.11-minimal/bin/activator
RUN git clone https://github.com/lenalebt/vacuum_cleaner.git
RUN cd vacuum_cleaner && ../activator-1.3.11-minimal/bin/activator compile
RUN cd vacuum_cleaner && timeout 240s ../activator-1.3.11-minimal/bin/activator ui || echo
RUN rm -f vacuum_cleaner/RUNNING_PID

EXPOSE 8080
EXPOSE 8888
ENTRYPOINT cd vacuum_cleaner && ../activator-1.3.11-minimal/bin/activator -Dhttp.address=0.0.0.0 ui
