To start JConsole with this protocol available run:

    jconsole -J-Djava.class.path=simple-jmx-connector.jar:$JAVA_HOME/lib/jconsole.jar:$JAVA_HOME/lib/tools.jar

Then simply enter the url which should look like:

    service:jmx:simple://localhost:1234
