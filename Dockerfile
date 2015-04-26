FROM dockerfile/ubuntu

RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

ENV WD /hoecoga-bot

RUN mkdir $WD
COPY application.conf $WD/
COPY logback.xml $WD/
COPY hoecoga-bot-assembly-1.2.jar $WD/

WORKDIR $WD

CMD ["java", "-Dconfig.file=application.conf", "-Dlogback.configurationFile=logback.xml", "-jar", "hoecoga-bot-assembly-1.2.jar"]
