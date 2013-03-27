To start JConsole with this protocol available run (assuming JAVA_HOME is set):

    jconsole -J-Djava.class.path=simple-jmx-connector.jar:$JAVA_HOME/lib/jconsole.jar:$JAVA_HOME/lib/tools.jar

or for Windows:

    jconsole -J"-Djava.class.path=target\simple-jmx-connector-1.0-SNAPSHOT.jar;%JAVA_HOME%\lib\jconsole.jar;%JAVA_HOME%\lib\tools.jar"

Then simply enter the url which should look like:

    service:jmx:simple://localhost:1234
