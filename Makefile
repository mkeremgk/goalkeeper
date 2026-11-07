JAVAC ?= javac
JAVA ?= java
SOURCES = AI.java Game.java Main.java Evaluation.java GoalkeeperTests.java

.PHONY: build run test evaluate train
build:
	mkdir -p build
	$(JAVAC) -d build $(SOURCES)
run: build
	$(JAVA) -cp build Main play
test: build
	$(JAVA) -Djava.awt.headless=true -cp build GoalkeeperTests
evaluate: build
	$(JAVA) -Djava.awt.headless=true -cp build Main evaluate
train: build
	$(JAVA) -Djava.awt.headless=true -cp build Main train
