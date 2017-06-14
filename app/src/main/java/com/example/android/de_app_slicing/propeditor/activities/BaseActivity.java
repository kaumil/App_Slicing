/**
 * This file is part of PropEditor application.
 * 
 * Copyright (C) 2016 Claudiu Ciobotariu
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.example.android.de_app_slicing.propeditor.activities;

import com.example.android.de_app_slicing.propeditor.PropEditorApplication;
import com.example.android.de_app_slicing.propeditor.R;
import com.example.android.de_app_slicing.propeditor.tasks.DefaultAsyncTaskResult;
import com.example.android.de_app_slicing.propeditor.tasks.DelayAsyncTask;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Here are defined default Activity behavior.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class BaseActivity extends AppCompatActivity implements DelayAsyncTask.Responder {
	protected PropEditorApplication mApplication;
	protected TextView mTitle;
	protected ImageView icon;
	protected int menuId;
	private boolean showMenu;
	protected AlertDialog mAlertDialog;

	/**
	 * The method invoked when the activity is creating
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mApplication = (PropEditorApplication) getApplication();
		applyApplicationTheme();
		super.onCreate(savedInstanceState);
	}

	/**
	 * Apply application theme.
	 */
	protected void applyApplicationTheme() {
		this.setTheme(mApplication.getApplicationTheme());
	}

	/**
	 * Define the layout for this activity
	 * 
	 * @param layoutResID
	 *            Layout resource ID
	 */
	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		applyCustomTitle();
	}

	/**
	 * Set the content view for this activity
	 * 
	 * @param view
	 *            The view to be set
	 */
	@Override
	public void setContentView(View view) {
		super.setContentView(view);
		applyCustomTitle();
	}

	/**
	 * Override default menu, with another menu
	 * 
	 * @param menuId
	 *            Menu resource ID
	 */
	protected void setMenuId(int menuId) {
		this.menuId = menuId;
		showMenu = true;
	}

	/**
	 * Create option menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean processed = false;
		if (showMenu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(menuId, menu);
			if (mApplication.isProPresent()) {
				//MenuItem item_donate = menu.findItem(R.id.item_donate);
				//item_donate.setVisible(false);
			}
			super.onCreateOptionsMenu(menu);
			processed = true;
		}
		return processed;
	}

	/**
	 * Invoked when an item from option menu is selected Send the menu ID to be
	 * processed.
	 * 
	 * @param item
	 *            The selected menu item
	 * @return A boolean value. True if the item is processed by this activity
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onMenuItemSelected(item.getItemId());
	}

	/**
	 * A default menu option consumer.
	 * 
	 * @param menuItemId
	 *            The menu item ID to be processed
	 * @return A boolean value. True if the item is processed by this activity
	 */
	protected boolean onMenuItemSelected(int menuItemId) {
		return false;
	}

	/**
	 * Invoked when the activity is put on pause
	 */
	@Override
	protected void onPause() {
		super.onPause();
		prepareForClose();
	}

	/**
	 * Prepare the activity to be closed.
	 */
	protected void prepareForClose() {
		mApplication.destroyAlertDialog(mAlertDialog);
		mApplication.onClose();
	}

	/**
	 * Method invoked on back
	 */
	protected void goBack() {
		finish();
	}

	/**
	 * Method invoked on back
	 */
	protected void sendFeedback() {
		finish();
	}

	/**
	 * Apply a custom title layout on top of this activity
	 */
	private void applyCustomTitle() {
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.title_layout);
		mTitle = (TextView) findViewById(R.id.title);
		icon = (ImageView) findViewById(R.id.icon);
	}

	/**
	 * On this method is a confirmation dialog.
	 * 
	 * @param titleStringId
	 *            The resource string id used for the confirmation dialog title.
	 * @param message
	 *            The message used for the confirmation dialog text.
	 * @param confirmationId
	 *            The id used to be identified the confirmed case.
	 * @param anObject
	 *            This could be used to send from the object needed on the
	 *            confirmation.
	 */
	protected void showConfirmationDialog(int titleStringId, String message,
			final int confirmationId, final Object anObject) {
		mAlertDialog = new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(titleStringId)
				.setMessage(message)
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								onConfirmation(confirmationId, anObject);
							}

						}).setNegativeButton(R.string.no, null).show();
	}

	/**
	 * This method should overwrite on each activity to handle confirmations
	 * cases.
	 * 
	 * @param confirmationId
	 *            The confirmation ID to identify the case.
	 * @param anObject
	 *            An object send by the caller method.
	 */
	protected void onConfirmation(int confirmationId, Object anObject) {

	}

	@Override
	public void startDelayPeriod() {
		mApplication.showProgressDialog(this, R.string.exiting);
	}

	@Override
	public void endDelayPeriod(DefaultAsyncTaskResult result) {
		prepareForClose();
		finish();
	}
}
