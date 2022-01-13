.PHONY: jade

DIRSRC  	:= src/chatbot/
DIRJADE 	:= lib/jade.jar
DIRJSOUP 	:= lib/jsoup-1.14.3.jar
 
JVC := javac
JVM := java

all: compile run

compile:
	@ echo ">>>>>>> Compilando..."
	$(JVC) -classpath $(DIRJADE):$(DIRJSOUP) -d classes $(DIRSRC)*.java
	@ echo "----------------------------------------------------------" 
    
run:
	@ echo ">>>>>>> Ejecutando..."   
	$(JVM) jade.Boot -agents "emisor:chatbot.Emisor;receptor:chatbot.Receptor"

