package com.stackunderflow.ops;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

public class StackOps extends Activity {

	EditText userField;
	EditText passField;
	TextView statusField;
	SharedPreferences settings;
	SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stack_ops);

		userField = (EditText) findViewById(R.id.userField);
		passField = (EditText) findViewById(R.id.passField);
		statusField = (TextView) findViewById(R.id.statusView);

		Context ctx = getApplicationContext();
		settings = PreferenceManager.getDefaultSharedPreferences(ctx);

		editor = settings.edit();
		userField.setText(settings.getString("username", ""));
		passField.setText(settings.getString("password", ""));
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stack_ops, menu);
		return true;
	}

	public void doLogin(View view )  {

		String username = userField.getText().toString();
		String password = passField.getText().toString();

		editor.putString("username", username);
		editor.putString("password", password);
		editor.apply();
		
		startService(new Intent(this, StackOpsService.class));
	}

}


