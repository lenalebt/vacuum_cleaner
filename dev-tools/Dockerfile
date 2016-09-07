FROM ubuntu:16.04
MAINTAINER oss@lena-brueder.de
RUN apt-get update && apt-get install -y git apt-transport-https && apt-get -y dist-upgrade
RUN echo "deb https://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823 && \
    apt-get update && \
    apt-get -y install sbt && \
    sbt --version
