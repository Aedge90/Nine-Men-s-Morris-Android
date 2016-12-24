package own.projects.lemiroapp;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;

public class MainActivity extends Activity {

	private final static int RUN_GAME = 67;
	private final static int SET_OPTIONS = 100;
	
	private int screenWidth;
	private final MainActivity THIS = this;
	
	private Options options;

	private LayoutInflater vi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		options = new Options();

		vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		screenWidth = size.x;

		setContentView(R.layout.activity_main);
		
		Intent setOptionsIntent = new Intent(this, OptionsActivity.class);
		startActivityForResult(setOptionsIntent, SET_OPTIONS);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == SET_OPTIONS) {
			if (resultCode == RESULT_OK) {
				
				options = data.getParcelableExtra("own.projects.lemiroapp.Options");
				
				Log.i("MainActivity", "Starting new Game with Options:\n" + options);

				if(options.gameMode.equals(Options.GameMode.HUMANBOT)){
					Intent appvsbotIntent = new Intent(THIS, HumanVsBot.class);
					appvsbotIntent.setExtrasClassLoader(Options.class.getClassLoader());
					appvsbotIntent.putExtra("own.projects.lemiroapp.Options", options);
					startActivityForResult(appvsbotIntent, RUN_GAME);
				}else if(options.gameMode.equals(Options.GameMode.BOTBOT)){
					Intent botvsbotIntent = new Intent(THIS, BotVsBot.class);
					botvsbotIntent.setExtrasClassLoader(Options.class.getClassLoader());
					botvsbotIntent.putExtra("own.projects.lemiroapp.Options", options);
					startActivityForResult(botvsbotIntent, RUN_GAME);
				}else{
					Intent humanvshumanIntent = new Intent(THIS, HumanVsHuman.class);
					humanvshumanIntent.setExtrasClassLoader(Options.class.getClassLoader());
					humanvshumanIntent.putExtra("own.projects.lemiroapp.Options", options);
					startActivityForResult(humanvshumanIntent, RUN_GAME);
				}
				
			}else {
				finish();
			}
		}else if(requestCode == RUN_GAME) {
			if(resultCode == RESULT_CANCELED){
				finish();
			}else{
				Intent setOptionsIntent = new Intent(THIS, OptionsActivity.class);
				startActivityForResult(setOptionsIntent, SET_OPTIONS);
			}
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void quit() {
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			new AlertDialog.Builder(THIS)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle("Quit")
					.setMessage("Do you really want to quit?")
					.setPositiveButton("YES",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									quit();
								}
							}).setNegativeButton("NO", null).show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}