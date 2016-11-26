package own.projects.lemiroapp;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;


public class GameBoardTestNonParameterized {

    private GameBoard mGameBoard;
    private Player mPlayerWhite;
    private Player mPlayerBlack;

    @Before
    public void setUp(){
        mGameBoard = new Mill9();
        mPlayerBlack = new Player(Options.Color.BLACK);
        mPlayerWhite = new Player(Options.Color.WHITE);
        mPlayerBlack.setOtherPlayer(mPlayerWhite);
        mPlayerWhite.setOtherPlayer(mPlayerBlack);
        mPlayerBlack.setSetCount(5);
        mPlayerWhite.setSetCount(5);
    }


    @Test
    public void executeCompleteTurn_WithSetMoveOnNonEmptyPosShouldThrowException(){

        GameBoard mGameBoard = new Mill9();

        Position p = new Position(6,6);
        try {
            mPlayerBlack.setSetCount(5);
            mPlayerWhite.setSetCount(5);
            mGameBoard.executeCompleteTurn(new Move(p, null, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(p, null, null), mPlayerWhite);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessage = "is trying to set to an occupied field by";
            if(!e.getMessage().contains(expectedMessage)){
                fail("expected message to contain: " + expectedMessage + "\n" + "but was: " + e.getMessage());
            }
        }


    }

    @Test
    public void executeCompleteTurn_WithMoveMoveOnNonEmptyPosShouldThrowException(){

        Position src1 = new Position(6,0);
        Position src2 = new Position(6,6);
        Position dest = new Position(6,3);

        try {
            mPlayerBlack.setSetCount(1);
            mPlayerWhite.setSetCount(1);
            mGameBoard.executeCompleteTurn(new Move(src1, null, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(src2, null, null), mPlayerWhite);
            mGameBoard.executeCompleteTurn(new Move(dest, src1, null), mPlayerBlack);
            mGameBoard.executeCompleteTurn(new Move(dest, src2, null), mPlayerWhite);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessage = "is trying to move to an occupied field by";
            if(!e.getMessage().contains(expectedMessage)){
                fail("expected message to contain: " + expectedMessage + "\n" + "but was: " + e.getMessage());
            }
        }

    }

    @Test
    public void executeCompleteTurn_KillOwnPieceShouldThrowException(){

        Position p = new Position(6,6);
        try {
            mPlayerBlack.setSetCount(5);
            mGameBoard.executeCompleteTurn(new Move(p, null, p), mPlayerBlack);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessageStart = "Trying to kill own piece of color";
            if(!e.getMessage().startsWith(expectedMessageStart)){
                fail("expected message to start with: " + expectedMessageStart + "\n" + "but was: " + e.getMessage());
            }
        }

    }


    @Test
    public void executeCompleteTurn_KillNotExistingPieceShouldThrowException(){

        Position set = new Position(0,0);
        Position kill = new Position(6,3);

        try {
            GameBoard emptyGameBoard = mGameBoard.getCopy();
            mPlayerBlack.setSetCount(5);
            emptyGameBoard.executeCompleteTurn(new Move(set, null, kill), mPlayerBlack);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            String expectedMessageEnd = "is trying to kill an empty field";
            if(!e.getMessage().endsWith(expectedMessageEnd)){
                fail("expected message to end with: " + expectedMessageEnd + "\n" + "but was: " + e.getMessage());
            }
        }

    }

    @Test
    public void getMill_ShouldbeNullForMill7(){

        final Options.Color B = Options.Color.BLACK;
        final Options.Color W = Options.Color.WHITE;
        final Options.Color N = Options.Color.NOTHING;
        final Options.Color I = Options.Color.INVALID;

        Options.Color[][] mill7 =

                {{N, I, I, N, I, I, N},
                { I, N, I, W, I, B, I},
                { I, I, I, I, I, I, I},
                { N, W, I, W, I, W, N},
                { I, I, I, I, I, I, I},
                { I, B, I, W, I, B, I},
                { B, I, I, N, I, I, N}};

        GameBoard gameBoard = new Mill7(mill7);

        Position[] mill1 = gameBoard.getMill(new Position(1,3), W);
        assertNull(mill1);
        Position[] mill2 = gameBoard.getMill(new Position(3,3), W);
        assertNull(mill2);
        Position[] mill3 = gameBoard.getMill(new Position(5,3), W);
        assertNull(mill3);

        Position[] mill4 = gameBoard.getMill(new Position(3,1), W);
        assertNull(mill4);
        Position[] mill5 = gameBoard.getMill(new Position(3,5), W);
        assertNull(mill5);

    }

}
