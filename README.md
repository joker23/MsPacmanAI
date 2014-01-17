Running
===
Compiling and running the program can be done in these ways:
For windows and iOS:
	
	javac -cp ./game/Exec.java #compiling
	java -cp ./game/Exec #running

For linux:
	
	make #compiling
	make run #running
	make clean #cleaning

Usage
===
Human takes in an input as well to flip on debugging for a proximity drawing 
Pacman takes in an input for debugging lines

you can also turn specific debug lines on and off by manipulating the Debug[] in the MyGhost.java file

Implementation Details
===

Each of the ghost will have a decision tree to choose which state that they are going to be in

instead of going to a corner...I decided that it was more strategic and challenging for the player if the ghost were to patrol a power pill

Clyde and Inky decided that they did not like their colors...therefore Clyde painted himself blue, and Inky pianted himself orange...and so begins Ms.Pacman

Blinky did not like the letter L so therefore, in this revamp, he wanted to be called 'Binky'

Pinky decided that he does not want to cause an overflow error so now he tries to guess which tile pacman will go to that is 4 tiles away from pacman.
Additional API
===
Game.java:
added some global variables on the top of the file for later use

G.java :
added functionality to convert x y coordinates to original indexed Nodes
[int] getIndex(int x, int y) : returns the position index corresponding to the xy coordinate

I also added a Pacman proximity highlighting to the Pacman class

I added a Strategy interface to link up all of my Strategies

to find the code for the AI for each look at the strategy section
Interesting Finds
===
Game field dimensions 108 x 116
Each tile is around 8
there are always 4 power pallets

Architectural Stuff
===

All code is located in the ./game folder

Exec.java is the driver class and in it you will find two inner classes (pacman and ghost) both of which are parent classes for pacman and the ghosts.

To change the mode of the game...you can run different types of games depending on what you uncomment.

Game Modes:
1) runExperiment
This will run a gui less game for n number of trials

2)runGame
can specify whether we want a display and also a delay (the delay could be set to 0 in order for a very fast game)

3)runGameTimed
can specify if we want a visual

4)runGameTimedAndRecorded
Takes in a filename and outputs the progression of the game in that file name

Example files in the examples folder
The GhostController.java and PacmanController.java in the controller folder are interfaces for your implementations of the controllers (ie MyPacman.java and MyGhost.java)

