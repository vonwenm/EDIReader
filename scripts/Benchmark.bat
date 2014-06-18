set CP=%EDIREADER_HOME%/edireader-4.7.3.jar
set CP=%CP%;%EDIREADER_HOME%/lib/JQuantify-3.3.jar
java -cp "%CP%" com.berryworks.edireader.benchmark.Benchmark %1 %2 %3 %4

