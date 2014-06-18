CP=$EDIREADER_HOME/edireader-4.7.3.jar
CP=$CP:$EDIREADER_HOME/lib/JQuantify-3.3.jar
java -cp $CP com.berryworks.edireader.benchmark.Benchmark $*

