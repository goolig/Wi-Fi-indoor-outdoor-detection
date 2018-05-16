echo set PATH=%PATH%;C:\Program Files\R\R-3.1.2\bin\;C:\Program Files\R\R-3.1.2\lib\;C:\guy\ThesisFinal\JRI\rJava_0.9-6\rJava\jri;C:\Program Files\R\R-3.1.2\bin\x64

java -cp .;examples;JRI.jar;src/JRI.jar rtest $*

pause