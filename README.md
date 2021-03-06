# AGORA-PrioritizeSN

AGORA-PrioritizeSN is an application for prioritizing social network messages based on sensor data stream for flood risk management. 

## Pre-requisites:

- <a href="http://git-scm.com/">Git</a>.

- <a href="http://www.oracle.com/technetwork/pt/java/javase/downloads/index.html">Java JDK</a>.

- <a href="https://eclipse.org/">Eclipse</a>.

- <a href="http://www.apache.org/">Apache</a>.

- <a href="http://www.postgresql.org/">PostgreSQL and PGAdmin3</a>.

## Build Instructions

- Clone the project: <code>git clone https://github.com/agora-research-group/AGORA-DSM</code>.

- Open Eclipse, go to File - New Project and choose Java Project Options. Uncheck 'Use default location' and insert the location of the git project.

- Export the Java project into a .war file (need to install Maven Integration for Eclipse) inside the webapps folder of the Apache Tomcat.

- Use the CREATE_DATABASE.sql to create the tables.

- After that, import the shapefiles from São Paulo e Germany to their respective databases.

- When the Tomcat Apache start running again, <a href="https://twitter.com">tweets</a> will be prioritized based on sensor data from <a href="http://www.cemaden.gov.br/">CEMADEN</a>, <a href="https://www.pegelonline.wsv.de/gast/start">PEGELONLINE</a>.

## Reporting Bugs

Any problem should be reported to group-agora@googlegroups.com.

For more information on AGORA-DSM, please, visit its main web page at: http://www.agora.icmc.usp.br/site/, or read the following references <a href="http://www.agora.icmc.usp.br/site/wp-content/uploads/2015/10/Assis-GeoInfo.pdf">paperGEOINFO-2015</a> and <a href="http://www.agora.icmc.usp.br/site/wp-content/uploads/2015/10/Assis-GeoInfo.pdf">paperBRASNAM-2015</a>.
