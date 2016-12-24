/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package own.projects.lemiroapp;

import android.view.View;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class GameModeActivity extends android.support.v4.app.FragmentActivity{

	protected final static int COMPUTE_DONE = 20;
	protected final static int READ_READY_DONE_HUMAN_SET = 74;
	protected final static int READ_READY_DONE_HUMAN_MOVE = 75;
	protected final static int RESULT_RESTART = Activity.RESULT_FIRST_USER + 1;
	
	protected final int LENGTH = 7;
    protected Lock lock = new ReentrantLock();
    protected Condition selection = lock.newCondition();
    protected volatile boolean selected;

    volatile Move currMove;
	volatile int remiCount;
	Thread gameThread;
	Options options;
	GridLayout fieldLayout;
	GameBoardView fieldView;
	TextView progressText;
	ProgressBar progressBar;
	ProgressUpdater progressUpdater;
	GameBoard field;
	Handler handler;
	int screenWidth;
	ImageView redSector;
	ImageView[] millSectors;
	final GameModeActivity THIS = this;

    volatile State state;
    protected enum State {
        SET, MOVEFROM, MOVETO, IGNORE, KILL, GAMEOVER
    }

    Player currPlayer;

	private void setDefaultUncaughtExceptionHandler() {
	    try {
	        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

	            @Override
	            public void uncaughtException(Thread t, Throwable e) {
	            	StackTraceElement[] trace = e.getStackTrace();
	            	String tracem = "";
	            	for(int i=0; i<trace.length; i++){
	            		tracem += trace[i] + "\n";
	            	}
	                Log.e("GameModeActivity", "Uncaught Exception detected in thread {}" + t + e + "\n" + tracem);

                    final String message = e.getMessage();

                    //display message so that the user sees that something went wrong
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                            new AlertDialog.Builder(THIS)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle("Error")
                            .setMessage(message)
                            .setCancelable(false)
                            .setNeutralButton("Quit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            })
                            .show();
                        }
                    });

	            }
	        });
	    } catch (SecurityException e) {
	        Log.e("GameModeActivity", "Could not set the Default Uncaught Exception Handler" + e);
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setDefaultUncaughtExceptionHandler();

        options = getIntent().getParcelableExtra("own.projects.lemiroapp.Options");

        millSectors = new ImageView[3];

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		screenWidth = size.x;

		setContentView(R.layout.activity_main);
		fieldLayout = (GridLayout) findViewById(R.id.field);
		progressText = (TextView) findViewById(R.id.progressText);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		((LinearLayout) fieldLayout.getParent()).updateViewLayout(fieldLayout,
				new LinearLayout.LayoutParams(screenWidth, screenWidth));

		progressUpdater = new ProgressUpdater(progressBar, this);
		
		currMove = null;
		remiCount = 20;
		fieldView = new GameBoardView(THIS, fieldLayout);
		
		init();

		gameThread.start();
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			new AlertDialog.Builder(THIS)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle("Options")
			.setMessage("What do you want to do?")
			.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int id) {
					new AlertDialog.Builder(THIS)
					.setCancelable(false)
					.setTitle("Quit?")
					.setMessage("Do you really want to Quit?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							setResult(RESULT_CANCELED);
							gameThread.interrupt();
							finish();
						}})
					.setNegativeButton("No", null)
					.show();
				}
			})
			.setNegativeButton("Cancel", null)
			.setNeutralButton("Restart", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
						int id) {
					new AlertDialog.Builder(THIS)
					.setTitle("Restart?")
					.setMessage("Do you want to restart?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							setResult(RESULT_RESTART);
							gameThread.interrupt();
							finish();
						}})
					.setNegativeButton("No", null)
					.show();
				}
			})
			.show();
			return true;
		}else{
			return super.onKeyDown(keyCode, event);
		}
	}
	
	protected abstract void init();

    private void signalSelection(){
        lock.lock();
        selected = true;
        selection.signal();
        lock.unlock();
    }

    private void waitforSelection() throws InterruptedException{
        lock.lock();
        try {
            while(!selected) { //necessary to avoid lost wakeup
                selection.await();
            }
        } finally {
            selected = false;
            lock.unlock();
        }
    }

    void setSectorListeners() {

        for (int y = 0; y < LENGTH; y++) {
            for (int x = 0; x < LENGTH; x++) {
                if (!field.getColorAt(x, y).equals(Options.Color.INVALID)) {
                    fieldView.getPos(new Position(x, y)).setOnClickListener(
                            new  OnFieldClickListener(x,y));
                }
            }
        }
    }

    void humanTurn(Player human) throws InterruptedException{

        currMove = null;
        Position newPosition = null;
        if(human.getSetCount() <= 0){
            state = State.MOVEFROM;
            // wait until a source piece and its destination position is chosen
            waitforSelection();
            fieldView.makeMove(currMove, human.getColor());
            fieldView.getPos(currMove.getSrc()).setOnClickListener(new OnFieldClickListener(currMove.getSrc()));
            fieldView.getPos(currMove.getDest()).setOnClickListener(new OnFieldClickListener(currMove.getDest()));
            newPosition = currMove.getDest();
        }else{
            state = State.SET;
            // wait for human to set
            waitforSelection();
            fieldView.setPos(currMove.getDest(), human.getColor());
            fieldView.getPos(currMove.getDest()).setOnClickListener(new OnFieldClickListener(currMove.getDest()));
            newPosition = currMove.getDest();
        }

        field.executeSetOrMovePhase(currMove, human);

        if (field.inMill(newPosition, human.getColor())) {
            state = State.KILL;
            Position[] mill = field.getMill(newPosition, human.getColor());
            fieldView.paintMill(mill, millSectors);
            //wait until kill is chosen
            waitforSelection();
            fieldView.setPos(currMove.getKill(), Options.Color.NOTHING);
            fieldView.getPos(currMove.getKill()).setOnClickListener(new OnFieldClickListener(currMove.getKill()));
            fieldView.unpaintMill(millSectors);

            field.executeKillPhase(currMove, human);
        }

        if(!field.equals(GameBoard.GameState.RUNNING)) {
            state = State.GAMEOVER;
        }
    }

    void botTurn(Player bot, Strategy brain) throws InterruptedException{
        Position newPosition = null;
        if(bot.getSetCount() <= 0){
            currMove = brain.computeMove();

            setTextinUIThread(progressText, "Bot is moving!");

            fieldView.makeMove(currMove, bot.getColor());
            fieldView.getPos(currMove.getSrc()).setOnClickListener(
                    new OnFieldClickListener(currMove.getSrc()));
            fieldView.getPos(currMove.getDest()).setOnClickListener(
                    new OnFieldClickListener(currMove.getDest()));
            newPosition = currMove.getDest();
        }else{

            //TODO animate setting moves

            currMove = brain.computeMove();

            setTextinUIThread(progressText, "Bot is moving!");

            fieldView.setPos(currMove.getDest(), bot.getColor());
            fieldView.getPos(currMove.getDest()).setOnClickListener(
                    new OnFieldClickListener(currMove.getDest()));
            newPosition = currMove.getDest();
        }

        field.executeSetOrMovePhase(currMove, bot);

        if (currMove.getKill() != null) {
            Position[] mill = field.getMill(newPosition, bot.getColor());

            fieldView.paintMill(mill, millSectors);

            fieldView.setPos(currMove.getKill(), Options.Color.NOTHING);
            fieldView.getPos(currMove.getKill()).setOnClickListener(
                    new OnFieldClickListener(currMove.getKill()));
            Thread.sleep(1500);
            fieldView.unpaintMill(millSectors);

            field.executeKillPhase(currMove, bot);
        }

    }

    //TODO hide previous toast when showing a new one so they wont stack

	protected void showToast(String text){
		Toast toast = Toast.makeText(this,text ,Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM,0,0);
		toast.show();
	}
	
	void setTextinUIThread(final TextView view, final String text){
		runOnUiThread(new Runnable() {
			public void run() {
				view.setText(text);
			}
		});
	}
	
	void setTextinUIThread(final TextView view, final int stringID){
		runOnUiThread(new Runnable() {
			public void run() {
				view.setText(getString(stringID));
			}
		});
	}

    boolean ShowGameOverMessageIfWon() {

        //TODO show remiCount somewhere

        if(field.getState(currPlayer).equals(GameBoard.GameState.DRAW)){
            showGameOverMsg("Draw!", "Nobody wins.");
            setTextinUIThread(progressText, "Draw! Nobody wins");
            return true;
        }

        String winningColor = currPlayer.getColor().toString();
        winningColor = winningColor.toUpperCase().charAt(0) + winningColor.substring(1).toLowerCase();
        String message;
        if(currPlayer.getDifficulty() == null) {
            //the player is a human
            message = "You have won!";
        }else{

            message = winningColor + " has won!";
        }

        if(field.getState(currPlayer).equals(GameBoard.GameState.WON_NO_MOVES)){
            showGameOverMsg(message, "The opponent could not make any further move.");
            setTextinUIThread(progressText, "Game Over! " + winningColor + " has won");
            return true;
        }else if (field.getState(currPlayer).equals(GameBoard.GameState.WON_KILLED_ALL)) {
            showGameOverMsg(message, "The opponent has lost all of his pieces.");
            setTextinUIThread(progressText, "Game Over! " + winningColor + " has won");
            return true;
        }
        return false;
    }
	
    private void showGameOverMsg(final String title, final String message){
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			runOnUiThread(new Runnable(){

				@Override
				public void run() {

					new AlertDialog.Builder(THIS)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle(title)
					.setMessage(message)
					.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            new AlertDialog.Builder(THIS)
                                    .setCancelable(false)
                                    .setTitle("Quit?")
                                    .setMessage("Do you really want to Quit?")
                                    .setPositiveButton("Yes",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    setResult(RESULT_CANCELED);
                                                    finish();
                                                }})
                                    .setNegativeButton("No", null)
                                    .show();
                        }
					})
                   .setPositiveButton("New Game", new DialogInterface.OnClickListener(){
                       @Override
                       public void onClick(DialogInterface dialogInterface, int id) {
                           setResult(RESULT_OK);
                           finish();
                       }
                   })
                    .setNeutralButton("Show Gameboard", null)
                    .show();
				}
			});
	 }

    protected class OnFieldClickListener implements View.OnClickListener {

        final int x;
        final int y;

        OnFieldClickListener(int x ,int y){
            this.x = x;
            this.y = y;
        }

        OnFieldClickListener(Position pos){
            this.x = pos.getX();
            this.y = pos.getY();
        }

        @Override
        public void onClick(View arg0) {

            if (state == State.SET) {
                if(field.getColorAt(new Position(x,y)).equals(currPlayer.getColor())
                        || field.getColorAt(new Position(x,y)).equals((currPlayer.getOtherPlayer().getColor()))){
                    showToast("You can not set to this Position!");
                }else{
                    Position pos = new Position(x, y);
                    currMove = new Move(pos, null, null);
                    state = State.IGNORE;
                    signalSelection();
                }
            } else if (state == State.MOVEFROM) {
                if(!(field.getColorAt(new Position(x,y)).equals(currPlayer.getColor()))){
                    showToast("Nothing to move here!");
                }else{
                    redSector = fieldView.createSector(Options.Color.RED);
                    redSector.setLayoutParams(new GridLayout.LayoutParams(
                            GridLayout.spec(y, 1), GridLayout.spec(x, 1)));
                    fieldLayout.addView(redSector);
                    //set invalid position for now so that constructor doesnt throw IllegalArgumentException
                    currMove = new Move(new Position(-1,-1), new Position(x,y), null);
                    state = State.MOVETO;
                }
            } else if (state == State.MOVETO) {
                if(!field.movePossible(currMove.getSrc(), new Position(x,y))){
                    state = State.MOVEFROM;
                    //signal that currMove could not be set
                    currMove = null;
                    fieldLayout.removeView(redSector);
                    showToast("You can not move to this Position!");
                }else{
                    fieldLayout.removeView(redSector);
                    currMove = new Move(new Position(x,y), currMove.getSrc(), null);
                    state = State.IGNORE;
                    signalSelection();
                }
            } else if (state == State.IGNORE) {
                showToast("It is not your turn!");
            }else if (state == State.KILL) {
                if(!(field.getColorAt(new Position(x,y)).equals(currPlayer.getOtherPlayer().getColor()))){
                    showToast("Nothing to kill here!");
                }else if(field.inMill(new Position(x,y), currPlayer.getOtherPlayer().getColor())){
                    //if every single stone of enemy is part of a mill we are allowed to kill
                    LinkedList<Position> enemypos = field.getPositions(currPlayer.getOtherPlayer().getColor());
                    boolean allInMill = true;
                    for(int i = 0; i<enemypos.size(); i++){
                        if(!field.inMill(enemypos.get(i), currPlayer.getOtherPlayer().getColor())){
                            allInMill = false;
                            break;
                        }
                    }
                    if(allInMill){
                        Position killPos = new Position(x, y);
                        currMove = new Move(currMove.getDest(), currMove.getSrc(), killPos);
                        state = State.IGNORE;
                        signalSelection();
                    }else{
                        showToast("You can not kill a mill! Choose another target!");
                    }
                }else{
                    Position killPos = new Position(x, y);
                    currMove = new Move(currMove.getDest(), currMove.getSrc(), killPos);
                    state = State.IGNORE;
                    signalSelection();
                }
            }

        }
    }
}
