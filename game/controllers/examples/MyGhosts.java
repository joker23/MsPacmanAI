package game.controllers.examples;

import 	game.controllers.GhostController;
import 	game.core.Game.DM;
import 	game.core.Game;
import 	game.core.GameView;
import	java.awt.Color;
import	java.util.*;
import	java.awt.Point;
import 	java.io.*;

/**
 * MyGhost
 *	This is the file that has all of the logic behind the ghost actions. It handles assigning different
 *	actions that each ghost takes at anytime...
 *	It will also hold the decision tree that governs the action of each ghost given their state (which is
 *	a list of attributes)
 *
 *	This file holds a lot of information to help you find sections of the code:
 *	
 *		Strategy Section .......................................157
 *			this section is where all of the AI code is stored
 *		
 *		Decision Tree Section ..................................504
 *
 *	@Author Steven Zhang
 */
public class MyGhosts implements GhostController{

	//Game engine constants
	private static final int 	TILE_WIDTH 	= 3;				//the approx width of a tile
	private static final int 	SECOND 		= 20; 				//the counter for 1 second;
	private static final int 	MAXX 		= 108;				//maximum x coordinate value
	private static final int 	MAXY 		= 116;				//maximum y coordinate value
	//path to where we store our decision trees
	private static final String PATH_DT		="game/controllers/examples/DecisionTrees/";
	//Colors that corrospond to the ghost index
	public static final Color[] PALLET 		= { Color.RED, Color.PINK, Color.ORANGE, Color.BLUE};

	//AI constants
	private static final int 	ATTR_LEN 	= 7;				//length of our attributes
	private static final int 	SCAT_TIME	= 7*SECOND; 		//seconds of Scattering
	private static final int 	CHASE_TIME 	= 25*SECOND;		//seconds of chasing
	private static final int 	CRIT_DIST 	= 8*TILE_WIDTH;		//the distance that clyde starts to retarget
	private static final int 	CRIT_NUM 	= 30;				//this is when Binky will always chase

	//Debuging Options
	private static boolean 		Debugging 	= false;			//is debugging turned on
	private static boolean[] 	DebugGhost 	= new boolean[4]; 	//this is an array so we can specify which ghost we want to debug  	

	//Decision Tree structures
	private DecisionTree 		tree;							//our decision tree	
	private static int[][] 		attr;							//our attribute array

	/**
	 * Main constructor for MyGhost initializes everything
	 *
	 * @param debugging boolean		: is the debugging flag on true
	 * @param debug		boolean[]	: debugging flag for each ghost
	 * @param dt		String		: selects a specific decision tree to run
	 */
	public MyGhosts(boolean debugging, boolean[] debug, String dt){
		this.Debugging = debugging;
		
		for(int i=0; i<4; i++){
			this.DebugGhost[i] = debug[i];
		}

		initAttr();
		tree = new DecisionTree(dt);
	}

	// chooses the default decision tree
	public MyGhosts(boolean debugging, boolean[] debug){
		this(debugging, debug, "Default.txt");
	}

	//sets debugging for all
	public MyGhosts(boolean debugging){
		this(debugging, new boolean[]{debugging, debugging, debugging, debugging});
	}
	
	//runs the program without debug
	public MyGhosts(){
		this(false);
	}

	//fires the action to move the ghost...indirectly
	public int[] getActions(Game game,long timeDue) {
		int[] directions=new int[Game.NUM_GHOSTS];
		
		//updates each attribute
		updateAttr(game);
		
		//highlights the critical distance (when clyde will start running away)
		if(Debugging && DebugGhost[3]){
			highlightCritDistance(game, game.getCurPacManLoc(), new HashSet<Integer>());
		}
		
		//checks the decision tree for action
		for(int i=0; i<4; i++){
			directions[i] = tree.whatDo(attr[i]).run(game, i);
		}
		
		return directions;
	}

	/**
	 * initializes the attribute arrays
	 * here is the list of attributes by index
	 *
	 * 0 [int] 	: ghost id
	 * 1 [bool] : ghost edible?
	 * 2 [bool] : is ghost in the chase phase? false if it is in the scatter phase
	 * 4 [int]  : dummy state to hold ghost scatter timer
	 * 5 [bool] : distance is under a certain value?
	 * 6 [bool] : is number of pills under a certain value?
	 */
	public void initAttr(){
		attr = new int[4][ATTR_LEN];
		for(int i=0; i<4; i++){
			attr[i][0] = i;	
			attr[i][2] = 1;	// all ghosts start in the chase phase...
			//take a random start timer so that the ghosts won't all
			//transition at the same time.
			attr[i][3] = (int)Math.ceil(Math.random() * CHASE_TIME); 
		}
	}

	/**
	 * updates the attributes of each ghost;
	 * based on the mapping of varibles
	 */
	public void updateAttr(Game game){
		for(int i=0; i<4; i++){
			attr[i][1] = game.isEdible(i) ? 1 : 0; 	//checks to see if the ghost is edible
			attr[i][3] --; //down tick the timer...
			if(attr[i][3] == 0){
				if(attr[i][2] == 1){ // if it is at chase then go to scatter
					attr[i][2] = 0;
					attr[i][3] = SCAT_TIME;
				} else {			//else if you are in scatter..chase
					attr[i][2] = 1;
					attr[i][3] = CHASE_TIME;
				}
			}
			if(game.getNumActivePills() <= CRIT_NUM)	{ //not enough pills?
				attr[i][6] = 1;
			}

			//checks to see if pacman is at the threshold distance
			if(game.getPathDistance(game.getCurPacManLoc(), game.getCurGhostLoc(i)) <= CRIT_DIST){
				attr[i][5] = 1;
			}else{
				attr[i][5] = 0;
			}
		}
	}

	/**
	 * hightlights the 8 closest squares in front of pacman in each direction
	 * this is used for debugging Clyde who should run if he is within that distance
	 */
	private void highlightCritDistance(Game game, int loc, HashSet<Integer> vis){
		if(game.getPathDistance(loc, game.getCurPacManLoc())>=CRIT_DIST){
			return;
		}
		vis.add(loc);
		for(int i=0; i<4; i++){
			int next = game.getNeighbour(loc, i);
			if(next != -1 && !vis.contains(next)){
				GameView.addPoints(game, Color.GRAY, next);
				highlightCritDistance(game, next, vis);
			}
		}
	}

	/************************************************************************************************
	 *  Strategy section
	 *  this section will hold all of the AI code (strategies for the ghosts) that are available
	 *  for the ghosts
	 * *********************************************************************************************/

	// strategy interface	
	public interface Strategy {
		//runs the strategy
		public int run(Game game, int ghostIndex);
	}

	/**
	 * This action should trigger when a ghost is edible...
	 * the ghost should then wonder aimlessly (randomizing its next
	 * move)
	 */
	private class Frightened implements Strategy{
		
		public int run(Game game, int ghost){
			if(game.ghostRequiresAction(ghost)){
				int[] possible = game.getPossibleGhostDirs(ghost);
				int index = (int)(Math.random()*possible.length); //randomly chooses a place to go...
				return game.getNeighbour(game.getCurGhostLoc(ghost), possible[index]);
			} return -1;
		}
	}

	/**
	 *	This is the red ghost's chase mode
	 *	In this mode, the ghost will stalk pacman...choosing the path
	 *	that will direct take it to pacman
	 */ 
	private class BinkyChase implements Strategy {
		
		public int run(Game game, int ghost) {
			int res = game.getNextGhostDir(ghost, game.getCurPacManLoc(), true, Game.DM.PATH); 
			if(Debugging && DebugGhost[ghost])
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), game.getCurPacManLoc());
			return res;
		}
	}

	/**
	 * Red ghost's scatter target
	 * which targets the upper right pill
	 */
	private class BinkyScatter implements Strategy {

		public int run(Game game, int ghost) {
			int dest = findDest(game, ghost);
			int res = game.getNextGhostDir(ghost, dest, true, Game.DM.PATH);
			if(Debugging && DebugGhost[ghost])
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), dest);
			return res;
		}

		// finds the upper right pallet on the map
		private int findDest(Game game, int ghost) {
			int[] possible = game.getPowerPillIndices();
			int dest = possible[0]; 
			int currx = game.getX(possible[0]);
			int curry = game.getY(possible[0]);
			for(int i =1; i<possible.length; i++){
				int x = game.getX(possible[i]);
				int y = game.getY(possible[i]);
				if(x>currx || y<curry){
					currx = x;
					curry = y;
					dest = possible[i];
				}
			}
			return dest;
		}
	}

	/**
	 * This is the AI for the pink ghost's chase state
	 * this function will try to find the 4th tile in front of Pacman
	 * and targetting that in the hopes of ambushing her.
	 */
	private class PinkyChase implements Strategy {

		public int run(Game game, int ghost){
			int next = 0;
			int res = -1;

			Set<Integer> dest = findDest(game, ghost);
			int[] arr = new int[dest.size()];
			int counter = 0;
			for(int i : dest){
				arr[counter++] = i;
			}

			res = arr[(int)(Math.random()*dest.size())];
			if(game.ghostRequiresAction(ghost)){	
				next = game.getNextGhostDir(ghost, res, true, Game.DM.PATH);
			}

			if(Debugging && res!=-1 && DebugGhost[ghost]){
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), res);
			}
			return next;

		}

		//private helper function to find 4 spaces ahead of pacman
		private Set<Integer> findDest(Game game, int ghost){
			int pacLoc = game.getCurPacManLoc();
			int pacDir = game.getCurPacManDir();
			HashSet<Integer> res = new HashSet<Integer>();
			HashSet<Integer> approx = new HashSet<Integer>();

			int counter = 4*TILE_WIDTH;
			int nextLoc;
			int prevLoc = pacLoc;
	
			while((nextLoc = game.getNeighbour(prevLoc, pacDir)) != -1 && counter > 0){
				approx.add(nextLoc);
				prevLoc = nextLoc;
				counter --;
			}
			
			if(counter > 0){
				//begin dfs to find the rest of the blocks
				//we do this to avoid an overflow and still find 4 tiles from pacman
				dfs(game, prevLoc, counter, res, approx);
			}
			else {
				res.add(nextLoc);
			}
			
			if(Debugging && DebugGhost[ghost]){
				GameView.addPoints(game, PALLET[ghost], res);
			}

			return res;
		}

		//helper dfs just so we can find which squares are probably in front of pacman
		private void dfs(Game game, int curr, int counter,  Set<Integer> res, Set<Integer> approx){
			if(counter <= 0) {
				res.add(curr);
				return;
			}
			for(int i = 0; i<4; i++){
				int next = game.getNeighbour(curr, i);
				if(next!=-1 && !approx.contains(next)){
					approx.add(curr);
					dfs(game, next, counter-1, res, approx);
				}
			}
		}
	}
	
	//AI to scatter to the top left
	private class PinkyScatter implements Strategy {

		public int run(Game game, int ghost) {
			int dest = findDest(game, ghost);
			int res = game.getNextGhostDir(ghost, dest, true, Game.DM.PATH);
			if(Debugging && DebugGhost[ghost])
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), dest);
			return res;
		}

		// finds the upper right pallet on the map
		private int findDest(Game game, int ghost) {
			int[] possible = game.getPowerPillIndices();
			int dest = possible[0];
			int currx = game.getX(possible[0]);
			int curry = game.getY(possible[0]);
			for(int i =1; i<possible.length; i++){
				int x = game.getX(possible[i]);
				int y = game.getY(possible[i]);
				if(x<currx || y<curry){
					currx = x;
					curry = y;
					dest = possible[i];
				}
			}
			return dest;
		}
	}

	//AI that find 2 times the transposition between pacman and binky
	//see the pacman documentation for more details...it's hard to explain
	private class InkyChase implements Strategy{
		
		private int[] d1 = {1, -1, 0, 0};
		private int[] d2 = {0, 0, -1, 1};

		public int run(Game game, int ghost){
			int point1 = findInterestPoint1(game, ghost);		//2 tiles in front of pacman
			int point2 = game.getCurGhostLoc(0);				//binky's location

			int dx = game.getX(point1) - game.getX(point2);
			int dy = game.getY(point1) - game.getY(point2);

			int finalx = game.getX(point2) + (2*dx);
			int finaly = game.getY(point2) + (2*dy);
			
			double slope = ((double) dy) / ((double) dx);

			//makes sure that values are in range...
			//so if a vlaue is out of range we simply truncate the 
			//magnitude of the vector...
			if(finalx>MAXX){
				int diff = finalx - MAXX;
				finalx -= diff;
				finaly -= (int) Math.ceil(slope*diff);
			} else if(finalx<1){
				int diff = 1 - finalx;
				finalx += diff;
				finaly += (int) Math.ceil(slope*diff);
			}

			//truncate it to within the maze boundaries
			slope = ((double) dx) / ((double) dy);
			if(finaly>MAXY){
				int diff = finaly - MAXY;
				finaly -= diff;
				finalx -= (int) Math.ceil(slope*diff);
			} else if(finaly < 1){
				int diff = 1 - finaly;
				finaly += diff;
				finalx += (int) Math.ceil(slope*diff);
			}
			
			int ret = game.getIndex(finalx, finaly);
			//start bfs to find the closest legit square for 
			//the ghost to target
			if(ret == -1){
				ret = bfs(game, finalx, finaly);
			}		

			if(Debugging && DebugGhost[ghost]){
				GameView.addPoints(game, PALLET[ghost], point1);
				GameView.addPoints(game, PALLET[ghost], point2);
				GameView.addPoints(game, PALLET[ghost], ret);
				GameView.addLines(game, PALLET[ghost], point2, ret);
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), ret);
			}
			return ret;
		}

		//finds the point that is 2 tiles in front of pacman
		//if there isn't a point that is 2 tiles in front
		//then the algorithm will select tiles closer to pacman
		private int findInterestPoint1(Game game, int ghost){
			int pacLoc = game.getCurPacManLoc();
			int pacDir = game.getCurPacManDir();
			int counter  = 2*TILE_WIDTH;
			
			int next = pacLoc;
			int prev = pacLoc;

			while(counter-->0 && game.getNeighbour(prev, pacDir) != -1){
				prev = next;
				next = game.getNeighbour(prev, pacDir);
			}
			return prev;
		}

		//helper bfs method that will find the closest legitimate 
		//space for the ghost to target
		private int bfs(Game game, int x, int y){
			LinkedList<Point> q = new LinkedList<Point>();
			HashSet<Point> vis = new HashSet<Point>();

			Point start = new Point(x,y);

			q.add(start);
			vis.add(start);
			
			while(!q.isEmpty()){
				Point tmp = q.poll();
				int check = game.getIndex(tmp.x, tmp.y);
				
				if(check != -1){
					return check;
				}
				
				for(int i=0; i<d1.length; i++){
					int nx = tmp.x + d1[i];
					int ny = tmp.y + d2[i];
					
					boolean inBound = nx>0 && nx<= MAXX && ny>0 && ny<= MAXY;
					Point next = new Point(nx, ny);
					boolean visited = vis.contains(next);
					if(!visited && inBound){
						vis.add(next);
						q.add(next);
					}
				}
			}
			return -1; //hopefully we will never return this...
		}
	}

	//AI to scatter to the bottom left
	private class InkyScatter implements Strategy {

		public int run(Game game, int ghost) {
			int dest = findDest(game, ghost);
			int res = game.getNextGhostDir(ghost, dest, true, Game.DM.PATH);
			if(Debugging && DebugGhost[ghost])
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), dest);
			return res;
		}

		// finds the bottom left pallet on the map
		private int findDest(Game game, int ghost) {
			int[] possible = game.getPowerPillIndices();
			int dest = possible[0];
			int currx = game.getX(possible[0]);
			int curry = game.getY(possible[0]);
			for(int i =1; i<possible.length; i++){
				int x = game.getX(possible[i]);
				int y = game.getY(possible[i]);
				if(x<currx || y>curry){
					currx = x;
					curry = y;
					dest = possible[i];
				}
			}
			return dest;
		}
	}

	//AI to scatter to the bottom right
	private class ClydeScatter implements Strategy {

		public int run(Game game, int ghost) {
			int dest = findDest(game, ghost);
			int res = game.getNextGhostDir(ghost, dest, true, Game.DM.PATH);
			if(Debugging && DebugGhost[ghost])
				GameView.addLines(game, PALLET[ghost], game.getCurGhostLoc(ghost), dest);
			return res;
		}

		// finds the upper right pallet on the map
		private int findDest(Game game, int ghost) {
			int[] possible = game.getPowerPillIndices();
			int dest = possible[0];
			int currx = game.getX(possible[0]);
			int curry = game.getY(possible[0]);
			for(int i =1; i<possible.length; i++){
				int x = game.getX(possible[i]);
				int y = game.getY(possible[i]);
				if(x>currx || y>curry){
					currx = x;
					curry = y;
					dest = possible[i];
				}
			}
			return dest;
		}
	}


	/************************************************************************************************
	*  Decision Tree
	*  This is my decision tree implementation
	*
	* This is a Decision Tree for the ghosts in Ms.Pacman
	* This Tree will be generating the different strategies that 
	* the ghosts will use to pursue pacman
	*	
	*	Instance attributes (in order):
	*		ghostid		(int)		: id of the ghost
	*		edible	 	(boolean) 	: whether ghost is edible
	*		chase?		(boolean)	: is it chasing or scattering
	*		timer [dummy](int)		: holds the time
	*		dist		(boolean)	: is pacman too close
	*		pill		(boolean)	: is there not enough pills left?
	*/
	private class DecisionTree{

		private final int 			NUM_STRATS = 8;	//number of strategies

		protected  int				root; 			//int of the root of the Tree
		protected  Strategy[] 		strats; 		//an array that holds all of the stratgies
		protected  Node[]			tree;			//an array representation of the tree;

		public DecisionTree(String dt){
			initStrats();
			initTree(dt);
		}

		public DecisionTree(){
			this("Default.txt");
		}
		//initializes the strategies
		// use this as a strategy look up..
		public void initStrats(){
			strats = new Strategy[NUM_STRATS];
			strats[0] = new BinkyScatter(); 
			strats[1] = new PinkyScatter(); 
			strats[2] = new InkyScatter(); 	
			strats[3] = new ClydeScatter();
			strats[4] = new BinkyChase(); 
			strats[5] = new PinkyChase(); 
			strats[6] = new InkyChase(); 
			strats[7] = new Frightened();
		}

		//makes the tree from a text doc
		// input format
		// first line : n m r
		// where 	n = number of nodes
		//			m = number of leaves
		//			r = root index
		// next n lines : [index] [attr] [#children] [chilren indices]
		// next m lines : [index] [strategy index]
		// refer to pdf for more details
		// note -1 is the dummy attribute for leaves
		public void initTree(String filePath){
			try {
				BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(PATH_DT + filePath)));
				StringTokenizer st = new StringTokenizer(in.readLine());
				int n = Integer.parseInt(st.nextToken());
				int m = Integer.parseInt(st.nextToken());
				this.root = Integer.parseInt(st.nextToken());

				this.tree = new Node[n];

				while(n-->0){
					st = new StringTokenizer(in.readLine());
					int index = Integer.parseInt(st.nextToken());
					Node node = new Node(Integer.parseInt(st.nextToken()));

					int num = Integer.parseInt(st.nextToken());
					tree[index] = node;

					if(num == -1){
						continue;
					}
					int[] tmp = new int[num];
					for(int i=0; i<num; i++){
						tmp[i] = Integer.parseInt(st.nextToken());
					}

					tree[index].children = tmp;	
				}

				while(m-->0){
					st = new StringTokenizer(in.readLine());
					int index = Integer.parseInt(st.nextToken());
					tree[index].strat = strats[Integer.parseInt(st.nextToken())];
				}
			} catch (IOException e){	
				e.printStackTrace();
				System.out.println("Input reading error switching to initializing default tree...");
				getDefaultTree();
			} finally{
				printTree();
			}
		}

		/**
		* method to find out what a ghost should do
		* and returns a strategy for it to take
		*/
		public Strategy whatDo(int[] attributes){
			return whatDo(root, attributes);
		}

		/**
		* private helper method to traverse the decision tree
		*/
		private Strategy whatDo(int curr, int[] attributes){
			Node node = tree[curr];
			
			if(node.isLeaf()){
				return node.strat;
			}
			return whatDo(node.children[attributes[node.attribute]], attributes);
		}

		/**
		 * if all else fails and we can't the tree from a buffer...then we will hard
		 * code in our default tree which is the same thing as the tree made by Default.txt
		 */
		private void getDefaultTree(){
			this.tree = new Node[17];
			this.root = 0;

			tree[0] = new Node(1);
			tree[0].children = new int[]{1, 2};
			tree[1] = new Node(2);
			tree[1].children = new int[]{3, 4};
			tree[2] = new Node(strats[7]);
			tree[3] = new Node(0);
			tree[3].children = new int[]{5, 8, 9, 10};
			tree[4] = new Node(0);
			tree[4].children = new int[]{11, 12, 13, 14};
			tree[5] = new Node(6);
			tree[5].children = new int[]{6, 7};
			tree[6] = new Node(strats[0]);
			tree[7] = new Node(strats[4]);
			tree[8] = new Node(strats[1]);
			tree[9] = new Node(strats[2]);
			tree[10] = new Node(strats[3]);
			tree[11] = new Node(strats[4]);
			tree[12] = new Node(strats[5]);
			tree[13] = new Node(strats[6]);
			tree[14] = new Node(5);
			tree[14].children = new int[]{15, 16};
			tree[15] = new Node(strats[4]);
			tree[16] = new Node(strats[3]);
		}
		
		/**
		 * prints out the decision tree that
		 * we generated
		 */
		public void printTree(){
			PrintWriter out = new PrintWriter(System.out);
			printTree(root, 0, out);
			out.flush();
		}

		/**
		 * private helper method that will
		 * recursively go down the generated tree
		 * printing out the nodes
		 */
		private void printTree(int curr, int h, PrintWriter out){
			for(int i=0; i<h; i++){
				out.print("  |  ");
			}
			Node n = tree[curr];
			out.print("[id:"+curr+"]");
			if(n.isLeaf()){
				out.print("-->" + n.strat.getClass().getSimpleName());
				out.println();
				return;
			} out.println();
			for(int i: n.children){
				printTree(i, h+1, out);
			}
		}



		/**
		* This class is a single node in the decision tree that will test specific
		* attributes 
		*/
		private class Node{
			Integer attribute;	//index of the attribute being tested
			int[] children;		//index of the children of the Node
			Strategy strat;		//the strategy being employed by the ghost

			/**
			* constructs a normal node
			*/
			public Node(Integer attribute) {
				this.attribute = attribute;
			}

			/**
			* constructs a leaf node
			*/
			public Node(Strategy strat){
				this.strat = strat;
			}

			/**
			* @return : whether the node is a leaf
			*/
			public boolean isLeaf(){
				return strat != null;
			}
		}
	}
}
