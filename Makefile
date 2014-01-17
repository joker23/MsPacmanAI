all:
	javac -cp . game/Exec.java

run:
	java -cp . game/Exec

clean:
	find . -iname "*.class" -exec rm '{}' ';'
