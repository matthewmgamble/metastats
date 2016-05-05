
For those who are not familiar with Prometheus  (https://prometheus.io/) , it is an open-source systems monitoring and alerting toolkit that does an amazing job of aggregating data from various sources into a single time-series database and the providing a highly flexible query interface for visualizing and working with data.  Internally, I had been using Prometheus to graph some internal application metrics (response time, load average, transactions per second, etc) and was very pleased with the dashboards it allowed me to create. Grafana (Grafana - Beautiful Metrics, Analytics, dashboards and monitoring!) is a metrics and analytics dashboard that plugs into  Prometheus (and other back-ends) to produce the graphs.  It also allows you to create various dashboard views quickly and easily and share them with other users, which is great since you can create different dashboards for different systems and switch between them quickly and easily.  I'm not going to get into how to setup Prometheus and Grafana here since there are plenty of great tutorials online.

So I wondered to myself - how can I integrate our Metaswitch trunk and PBX statistics into Prometheus?  After reading various metaswitch guides, I figured the fastest way to do this would be to get the data from the ems_stats_db.  The challenge, however, is that Prometheus expects to poll an HTTP endpoint to get its metrics, so the integration required something to bridge the gap between the ems_stats_db in postgres to an HTTP interface.  To accomplish this, I wrote a small agent in Java that connects to the ems_stats_db and then exposes the SIP trunk and PBX stats tables as metrics that Prometheus can understand.  To help others get this setup I've open sourced the code for the connector, which can be found on github @ https://github.com/matthewmgamble/metastats

Setting up the Java agent is pretty straight forward.  Clone the github repo, then build the application using Maven.  To configure the application, it uses the config.json file to store the application configuration. The format is pretty easy to understand:

{
  "dbServer": "META_CFS_IP",
  "dbUser": "statsread",
  "dbPass": "STASREADPASSWORD"
  "dbName": "ems_stats_db",
  "logDir": "/tmp",
  "logFileName": "metastats.log",
  "dbSSL": false,
  "debug": true,
  "listenPort": 8080
}

Change "dbServer" to the MetaCFS IP, and add the database password that you got from Metaswitch support.  The listenPort is also needed as that's the port you will use for Prometheus to connect to the application so you need to ensure any firewall rules on the machine are updated to allow the Prometheus host to connect to the machine running this application on that port.

You can then start the application by running "java -jar target/metastats-1.0-SNAPSHOT.jar -config config.json"

The last step is to let Promethus know about the new datasource - in your prometheus.yml configuration file add the new job, replacing "MACHINE-RUNNING-AGENT" with the IP/Hostname of the machine running the Java application.

  - job_name: 'metaswitch'
    target_groups:
      - targets: ['MACHINE-RUNNING-AGENT:8080']

And that's it - you should now be able to go into Grafana and start adding graphs for your Metaswitch.
