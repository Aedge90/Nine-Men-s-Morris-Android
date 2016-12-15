package own.projects.lemiroapp;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Strategy {

    private int nThreads;
    private Thread[] threads;
    private StrategyRunnable[] runnables;
    final ProgressUpdater up;

    private final GameBoard gameBoard;
    private final Player maxPlayer;

    static enum GameState {
        RUNNING, DRAW, WON_NO_MOVES, WON_KILLED_ALL
    };

    Strategy(final GameBoard field, final Player player, final ProgressUpdater up) {
        this(field, player, up, 8);
    }

    @VisibleForTesting
    Strategy(final GameBoard field, final Player player, final ProgressUpdater up, final int nThreads) {
        this.gameBoard = field;
        this.maxPlayer = player;
        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.runnables = new StrategyRunnable[nThreads];
        this.up = up;
        for (int i = 0; i < nThreads; i++){
            runnables[i] = new StrategyRunnable(gameBoard, maxPlayer, up, i);
        }
    }

    public Move computeMove() throws InterruptedException {

        // shuffle list, so we dont end up with the same moves every game
        LinkedList<Move> shuffle = shuffleListOfPossMoves();

        LinkedList<Move> kills = splitListByKillMoves(shuffle);
        LinkedList<Move> others = shuffle;

        up.setMax(shuffle.size());

        StrategyRunnable.maxWertKickoff = Integer.MIN_VALUE;
        StrategyRunnable.resultMove = null;
        //not StrategyRunnable.MIN as StrategyRunnable.MIN might be multiplied in evaluation and thus is not the minimal possible number
        StrategyRunnable.resultEvaluation = Integer.MIN_VALUE;

        StrategyRunnable.possibleMovesKickoff = kills;
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }
        for (int i = 0; i < nThreads; i++){
            threads[i].join();
        }

        // wait until completion, so other moves will know about maxWertKickoff and hopefully
        // run fast, as they should do a lot of cutoffs
        StrategyRunnable.possibleMovesKickoff = others;
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }
        for (int i = 0; i < nThreads; i++){
            threads[i].join();
        }

        for (int i = 0; i < nThreads; i++) {
           //runnables need to know which move was chosen
           runnables[i].setPreviousMove(StrategyRunnable.resultMove);
        }

        up.reset();

        return StrategyRunnable.resultMove;
    }

    @VisibleForTesting
    public LinkedList<Move> shuffleListOfPossMoves() {

        runnables[0].updateState();
        LinkedList<Move> shuffle = runnables[0].possibleMoves(maxPlayer);

        Collections.shuffle(shuffle);

        return shuffle;

    }

    @VisibleForTesting
    public LinkedList<Move> splitListByKillMoves (LinkedList<Move> moves) {

        //TODO sort opening a mill and preventing one moves to the beginning but after kill moves

        LinkedList<Move> kills = new LinkedList<Move>();
        // but make sure the kill moves are at the beginning again, to improve performance
        for(Move m : moves) {
            if (m.getKill() != null) {
                kills.add(m);
            }
        }
        moves.removeAll(kills);
        return kills;
    }

    @VisibleForTesting
    public int getResultEvaluation() {
        return StrategyRunnable.resultEvaluation;
    }

    GameState getState() {

        //TODO initialize remi count correctly and use whoWon in Activity
        int remiCount = 20;

        if(gameBoard.getPositions(maxPlayer.getColor()).size() == 3 && gameBoard.getPositions(maxPlayer.getOtherPlayer().getColor()).size() == 3){
            remiCount --;
            if(remiCount == 0){
                return GameState.DRAW;
            }
        }

        //only the other player can have lost as its impossible for maxPlayer to commit suicide
        if(!gameBoard.movesPossible(maxPlayer.getOtherPlayer().getColor(), maxPlayer.getOtherPlayer().getSetCount())){
            return GameState.WON_NO_MOVES;
        }else if ((gameBoard.getPositions(maxPlayer.getOtherPlayer().getColor()).size() < 3 && maxPlayer.getOtherPlayer().getSetCount() <= 0)) {
            return GameState.WON_KILLED_ALL;
        }

        return GameState.RUNNING;
    }
}
