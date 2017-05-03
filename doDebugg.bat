: skript zum Remote Debuggen von eclipse's Debugger aus.

set OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
set JAR=-jar target/Kustvakt-core-0.59.7.jar

java %OPTS% %JAR%



