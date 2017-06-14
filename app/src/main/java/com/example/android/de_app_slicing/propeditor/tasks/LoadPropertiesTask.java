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
package com.example.android.de_app_slicing.propeditor.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.example.android.de_app_slicing.propeditor.PropEditorApplication;
import com.example.android.de_app_slicing.propeditor.R;
import com.example.android.de_app_slicing.propeditor.models.Constants;
import com.example.android.de_app_slicing.propeditor.properties.Entities;
import android.app.Application;
import android.os.AsyncTask;

/**
 * An asynchronous task to load the properties.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class LoadPropertiesTask extends
		AsyncTask<Void, Void, DefaultAsyncTaskResult> {
	private static final String TAG = LoadPropertiesTask.class.getName();

	/**
	 * Responder used on loading process.
	 */
	public interface Responder {
		Application getApplication();

		void startLoadProperties();

		void endLoadProperties(DefaultAsyncTaskResult result);
	}

	private Responder responder;
	private PropEditorApplication application;
	private DefaultAsyncTaskResult defaultResult;
	private String privateDir;
	private String fileName;
	private Entities properties;

	/**
	 * Constructor of this async task
	 * 
	 * @param responder
	 *            The process responder provided to get some application info
	 * @param fileName
	 *            The full path for file name of properties
	 * @param properties
	 *            The propertied to be loaded
	 */
	public LoadPropertiesTask(Responder responder, String fileName,
			Entities properties) {
		this.responder = responder;
		this.fileName = fileName;
		this.properties = properties;
		application = (PropEditorApplication) responder.getApplication();
		privateDir = application.getFilesDir() != null ?
				application.getFilesDir().getAbsolutePath() : null;
	}

	/**
	 * Method invoked on the background thread.
	 */
	@Override
	protected DefaultAsyncTaskResult doInBackground(Void... params) {
		defaultResult = new DefaultAsyncTaskResult();
		defaultResult.resultId = Constants.OK;
		loadTheProperties();
		return defaultResult;
	}

	/**
	 * Method invoked on the UI thread before the task is executed.
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		responder.startLoadProperties();
	}

	/**
	 * Method invoked on the UI thread after the background computation
	 * finishes.
	 */
	@Override
	protected void onPostExecute(DefaultAsyncTaskResult result) {
		super.onPostExecute(result);
		responder.endLoadProperties(result);
	}

	/**
	 * Open and load properties file.
	 */
	private void loadTheProperties() {
		File f = new File(fileName);
		if (f.exists() && f.isFile()) {
			if (!f.canRead()) {
				if (application.getUnixShell().hasRootAccess() && privateDir != null) {
					f = prepareOriginalFile();
				}
			}
			if (f != null && f.canRead()) {
				InputStream inputStream = null;
				try {
					inputStream = new FileInputStream(f);
					properties.load(inputStream);
					defaultResult.resultMessage = getStringMessage(R.string.properties_loaded,
									properties.size());
				} catch (IllegalArgumentException e) {
					defaultResult.resultId = Constants.ERROR_REPORT;
					defaultResult.resultMessage = getStringMessage(R.string.loading_exception_report,
							fileName, "IllegalArgumentException: ", e.getMessage());
					application.logE(TAG, defaultResult.resultMessage, e);
				} catch (FileNotFoundException e) {
					defaultResult.resultId = Constants.ERROR_REPORT;
					defaultResult.resultMessage = getStringMessage(R.string.loading_exception_report,
							fileName, "FileNotFoundException", e.getMessage());
					application.logE(TAG, defaultResult.resultMessage, e);
				} catch (IOException e) {
					defaultResult.resultId = Constants.ERROR_REPORT;
					defaultResult.resultMessage = getStringMessage(R.string.loading_exception_report,
							fileName, "IOException", e.getMessage());
					application.logE(TAG, defaultResult.resultMessage, e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
						}
					}
				}
			} else {
				if (application.getUnixShell().hasRootAccess()) {
					defaultResult.resultId = Constants.ERROR;
					defaultResult.resultMessage = getStringMessage(R.string.unable_to_read, fileName);
					application.logE(TAG, defaultResult.resultMessage);
				} else {
					defaultResult.resultId = Constants.ERROR;
					defaultResult.resultMessage = getStringMessage(R.string.no_root_privileges);
					application.logE(TAG, defaultResult.resultMessage);
				}
			}
		} else {
			defaultResult.resultId = Constants.ERROR;
			defaultResult.resultMessage = getStringMessage(
					R.string.file_not_exist, fileName);
			application.logE(TAG, defaultResult.resultMessage);
		}
	}

	/**
	 * Prepare the message string based on the resource id and parameters.
	 *
	 * @param resId      The resource ID of the message template.
	 * @param formatArgs The message parameters.
	 * @return The formatted message.
	 */
	private String getStringMessage(int resId, Object... formatArgs) {
		return responder.getApplication().getString(resId, formatArgs);
	}

	/**
	 * Create a copy of original file on the private data folder to be read.
	 * 
	 * @return The readable file or null if is not available.
	 */
	private File prepareOriginalFile() {
		File destFile = new File(privateDir + File.separator + "tmp"
				+ File.separator + PropEditorApplication.BUILD_PROP);
		if (destFile.exists()) {
			destFile.delete();
		} else {
			if (!destFile.getParentFile().exists()) {
				destFile.getParentFile().getAbsoluteFile().mkdirs();
			}
		}
		if (application.getUnixShell().runUnixCommand(
				"cat " + fileName + " > " + destFile.getAbsolutePath())) {
			application.getUnixShell().runUnixCommand(
					"chmod 644 " + destFile.getAbsolutePath());
			application.getUnixShell().runUnixCommand(
					"chcon u:object_r:app_data_file:s0:c512,c768 " + destFile.getAbsolutePath());
		} else {
			destFile = null;
		}
		return destFile;
	}

}
