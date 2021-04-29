#!/bin/sh

#该脚本与项目打包后的jar放置在同一目录下，然后执行即可。产生tpid记录运行项目后的进程id

JAR_NAME=simple-search-1.0-SNAPSHOT.jar

tpid=`ps -ef|grep $JAR_NAME|grep -v grep|grep -v kill|awk '{print $2}'`
if [ ${tpid} ]; then
echo 'Stop Process...'
fi
sleep 5
tpid=`ps -ef|grep $JAR_NAME|grep -v grep|grep -v kill|awk '{print $2}'`
if [ ${tpid} ]; then
echo 'Kill Process!'
kill -9 $tpid
else
echo 'Stop Success!'
fi

tpid=`ps -ef|grep $JAR_NAME|grep -v grep|grep -v kill|awk '{print $2}'`
if [ ${tpid} ]; then
        echo 'App is running.'
else
        echo 'App is NOT running.'
fi

rm -f tpid
nohup java -jar ./$JAR_NAME --spring.profiles.active=test >log.txt  2>&1 &
echo $! > tpid
echo 'Start Success!' 
