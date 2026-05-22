#!/usr/bin/env bash
# shell script to run vep-pipeline
. /etc/profile

APPNAME="vep-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu
fi

# environment: 'prod' on the reed pipeline server, 'dev' elsewhere
ENV=dev
if [[ "$SERVER" == REED* ]]; then
  ENV=prod
fi

# assembly map_key (required)
MAP_KEY=380
# optional chromosome filter (if unset, all chromosomes are processed)
# CHR=20
# optional output directory (if unset, the file is written to /tmp)
# OUT_DIR=/home/rgddata/pipelines/$APPNAME/data

cd $APPDIR
java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/${APPNAME}.jar --mapKey $MAP_KEY --env $ENV ${CHR:+--chr $CHR} ${OUT_DIR:+--outDir $OUT_DIR} 2>&1 > $APPDIR/run.log

mailx -s "[$SERVER] vep-pipeline run" $EMAIL_LIST < $APPDIR/logs/summary.log
