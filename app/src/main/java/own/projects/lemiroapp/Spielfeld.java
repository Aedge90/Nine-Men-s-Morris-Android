package own.projects.lemiroapp;

import java.util.LinkedList;

import android.graphics.Path;
import android.util.Log;

public abstract class Spielfeld {

	protected final int LENGTH = 7;
	Options.Color[][] field;
	final Options.Color O = Options.Color.NOTHING;
	final Options.Color N = Options.Color.INVALID;
	Options.MillMode millMode;

	private LinkedList<Position> positionsWhite = new LinkedList<Position>();
	private LinkedList<Position> positionsBlack = new LinkedList<Position>();

    void assertValidandNotNull(Position p) {
        if (p == null) {
            throw new IllegalArgumentException("getPossibleMillX: p is null");
        }
        if (field[p.getY()][p.getX()] == N) {
            throw new IllegalArgumentException("getPossibleMillX: p is not valid");
        }
    }
	
	LinkedList<Position> getPositions(Options.Color color) {
		if(color.equals(Options.Color.WHITE)){
			return positionsWhite;
		}else if(color.equals(Options.Color.BLACK)){
			return positionsBlack;
		}else{
			throw new IllegalArgumentException("unknown Color in get Positions");
		}
	}
	
	Options.MillMode getMillVar(){
		return millMode;
	}
	
	Options.Color getPos(int x, int y) {
		return field[y][x];
	}

	Options.Color getPos(Position pos) {
		if (pos == null){
			Log.e("Spielfeld", "Error: getPos: Position was null!");
		}
		return getPos(pos.getX(), pos.getY());
	}

	int getStoneCount(){
		return positionsWhite.size() + positionsBlack.size();
	}

	//return false if the position wasnt found
	private boolean removePos(LinkedList<Position> positions, Position del){
		for (Position pos : positions){
			if (pos.getX() == del.getX() && pos.getY() == del.getY()) {
				positions.remove(pos);
				return true;
			}
		}
		return false;
	}
	
	private void setPos(Position pos, Options.Color color) {
		//kill
		if(color.equals(Options.Color.NOTHING)){
			Options.Color killcolor = field[pos.getY()][pos.getX()];
			if(false == removePos(getPositions(killcolor), pos)){
				throw new IllegalArgumentException("trying to delete Set Position thats is not found!");
			}
			field[pos.getY()][pos.getX()] = Options.Color.NOTHING;
		}else{	//set
			getPositions(color).add(pos);
			field[pos.getY()][pos.getX()] = color;
		}
	}
	
	private void makeMove(Position src, Position dest, Options.Color color) {
		if(false == removePos(getPositions(color), src)){
			throw new IllegalArgumentException("trying to delete " + color + " Src Position thats is not found!");
		}
		getPositions(color).add(dest);
		field[src.getY()][src.getX()] = Options.Color.NOTHING;
		field[dest.getY()][dest.getX()] = color;
	}

    //this executes the complete turn of a player, including setting or moving and killing
	void makeWholeMove(Zug move, Player player){
		if(move.getSet() != null){
            if(!getPos(move.getSet()).equals(Options.Color.NOTHING)){
                throw new IllegalArgumentException("Player " + player.getColor() + " is trying to set to an occupied field by: " + getPos(move.getSet()));
            }
			setPos(move.getSet(), player.getColor());
            player.setSetCount(player.getSetCount() - 1);
		}
		if(move.getDest() != null){
            if(!getPos(move.getDest()).equals(Options.Color.NOTHING)){
                throw new IllegalArgumentException("Player " + player.getColor() + " is trying to move to an occupied field by: " + getPos(move.getDest()));
            }
			makeMove(move.getSrc(), move.getDest(), player.getColor());
		}
		if(move.getKill() != null){
            if(getPos(move.getKill()).equals(player.getColor())){
                throw new IllegalArgumentException("Trying to kill own piece of color: " + player.getColor());
            }
            if(getPos(move.getKill()).equals(Options.Color.NOTHING)){
                throw new IllegalArgumentException("Player " + player.getColor() + " is trying to kill an empty field");
            }
			setPos(move.getKill(), Options.Color.NOTHING);
		}
	}

    //undoes a complete turn of a player, including setting or moving and killing
	public void reverseWholeMove(Zug move, Player player) {
		if(move.getSet() != null){
			setPos(move.getSet(), Options.Color.NOTHING);
            player.setSetCount(player.getSetCount() + 1);
		}
		if(move.getDest() != null){
			makeMove(move.getDest(), move.getSrc(), player.getColor());
		}
		if(move.getKill() != null){
			setPos(move.getKill(), player.getOtherPlayer().getColor());
		}	
	}

    //returns true if two pieces of same color are found, that form a mill together with position
    //assumes that position is of the same color that is passed here !
	boolean inMill(Position p, Options.Color player) {
        if(null != getMill(p, player)){
            //mill was found
            return true;
        }else{
            return false;
        }
    }

	Position[] getMill(Position p, Options.Color player) {
		
		Position[] millX = getPossibleMillX(p);
		
		if(millX != null){ //its null here if there is no possible mill in x direction
			int count = 0;
			for(int i = 0; i<3; i++){
				if(!millX[i].equals(p)){
					if(getPos(millX[i]).equals(player)){
						count ++;
					}
				}
				if(count == 2){
					return millX;
				}
			}
		}
		
		Position[] millY = getPossibleMillY(p);
		if(millY != null){ //its null here if there is no possible mill in y direction
			int count = 0;
			for(int i = 0; i<3; i++){
				if(!millY[i].equals(p)){
					if(getPos(millY[i]).equals(player)){
						count ++;
					}
				}
				if(count == 2){
					return millY;
				}
			}
		}
		
		if(millMode.equals(Options.MillMode.MILL7)){
			return getMill7(p, player);
		}
		
		return null;
	}
	
	

	//is any move possible?
	boolean movesPossible(Options.Color player, int setCount) {
		if(setCount > 0){
			return true;
		}
		boolean jump = false;
		if (getPositions(player).size() == 3){
			jump = true;
		}

		if (!jump){
			for (Position p : getPositions(player)) {
				if (moveUp(p).isValid())
					return true;
				if (moveDown(p).isValid())
					return true;
				if (moveRight(p).isValid())
					return true;
				if (moveLeft(p).isValid())
					return true;
			}
		} else {
			return true;
		}
		return false;

	}
	
	//is move dest possible?
	boolean movePossible(Position src, Position dest){
		if(!getPos(dest).equals(Options.Color.NOTHING)){
			return false;
		}
		if(getPositions(getPos(src)).size() == 3){
			return true;
		}
		if(dest.equals(moveUp(src).getDest()) ||dest.equals(moveDown(src).getDest())
				||dest.equals(moveRight(src).getDest()) ||dest.equals(moveLeft(src).getDest())){
			return true;
		}
		return false;
	}
	
	public String toString() {
		String print = "";
		print += "    0 1 2 3 4 5 6\n------------------\n";
		for (int y = 0; y < field.length; y++) {
			print += y + " | ";
			for (int x = 0; x < field[y].length; x++) {
				print += !getPos(x, y).equals(Options.Color.INVALID) ? getPos(x, y) + " " : "  ";

			}
			print += '\n';
		}
		return print;

	}
	
	//the following methods are implemented by subclasses
	Position[] getPossibleMillX(Position p){
		return null;
	};
	Position[] getPossibleMillY(Position p){
		return null;
	};
	Zug moveUp(Position p) {
		return null;
	}
	Zug moveDown(Position p) {
		return null;
	}
	Zug moveLeft(Position p) {
		return null;
	}
	Zug moveRight(Position p) {
		return null;
	}
	
	Position[] getMill7(Position p, Options.Color player) {
		return null;
	}
	
	boolean inMill7(Position p, Options.Color player) {
		return false;
	}
}
