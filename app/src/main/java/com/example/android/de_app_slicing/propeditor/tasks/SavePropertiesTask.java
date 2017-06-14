/**
 * This file is part of PropEditor application.
 * 
 * Copyright (C) 2013 Claudiu Ciobotariu
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.example.android.de_app_slicing.propeditor.PropEditorApplication;
import com.example.android.de_app_slicing.propeditor.R;
import com.example.android.de_app_slicing.propeditor.models.Constants;
import com.example.android.de_app_slicing.propeditor.properties.Entities;
import com.example.android.de_app_slicing.propeditor.util.Utilities;
import android.app.Application;
import android.os.AsyncTask;

/**
 * An asynchronous task used to save the properties.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class SavePropertiesTask extends
		AsyncTask<Void, Void, DefaultAsyncTaskResult> {
	private static final String TAG = LoadPropertiesTask.class.getName();

	/**
	 * Responder used on save process.
	 */
	public interface Responder {
		Application getApplication();

		void startSaveProperties();

		void endSaveProperties(DefaultAsyncTaskResult result);
	}

	private Responder responder;
	private PropEditorApplication application;
	private DefaultAsyncTaskResult defaultResult;
	private String privateDir;
	private String fileName;
	private File destinationFile;
	private Entities properties;

	public SavePropertiesTask(Responder responder, String fileName,
			Entities properties) {
		this.responder = responder;
		this.fileName = fileName;
		destinationFile = new File(fileName);
		this.properties = properties;
		privateDir = responder.getApplication().getFilesDir() != null ?
				responder.getApplication().getFilesDir().getAbsolutePath() : null;
		application = (PropEditorApplication) responder.getApplication();
	}

	/**
	 * Method invoked on the background thread.
	 */
	@Override
	protected DefaultAsyncTaskResult doInBackground(Void... params) {
		defaultResult = new DefaultAsyncTaskResult();
		defaultResult.resultId = Constants.OK;
		boolean isSystem = fileName.startsWith(Constants.SYSTEM_PARTITION);
		boolean continueSave = true;
		boolean shouldMountSystem = false;
		if (destinationFile.getParentFile() == null) {
			continueSave = false;
			defaultResult.resultId = Constants.ERROR;
			defaultResult.resultMessage = application
					.getString(R.string.destination_folder_null);
		}
		if (continueSave) {
			continueSave = application.getUnixShell().hasRootAccess();
			if (!continueSave) {
				defaultResult.resultId = Constants.ERROR;
				defaultResult.resultMessage = application
						.getString(R.string.no_root_privileges);
			}
		}
		if (continueSave) {
			if (isSystem) {
				shouldMountSystem = !application.getUnixShell()
						.checkPartitionMountFlags(Constants.SYSTEM_PARTITION,
				Constants.READ_WRITE);
			}
			if (shouldMountSystem) {
				continueSave = application.getUnixShell().mountPartition(
						Constants.SYSTEM_PARTITION, Constants.READ_WRITE);
			}
			if (!continueSave) {
				defaultResult.resultId = Constants.ERROR;
				defaultResult.resultMessage = application
						.getString(R.string.system_no_mount);
			}
		}
		if (continueSave) {
			if (privateDir != null) {
				saveTheProperties();
			}
			if (privateDir != null && backupOldFile()) {
				moveNewFile();
			} else {
				defaultResult.resultId = Constants.ERROR;
				defaultResult.resultMessage = application.getString(
						R.string.backup_failed);
			}
			if (isSystem && shouldMountSystem) {
				application.getUnixShell().mountPartition(
						Constants.SYSTEM_PARTITION, Constants.READ_ONLY);
			}
		}
		return defaultResult;
	}

	/**
	 * Method invoked on the UI thread before the task is executed.
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		responder.startSaveProperties();
	}

	/**
	 * Method invoked on the UI thread after the background computation
	 * finishes.
	 */
	@Override
	protected void onPostExecute(DefaultAsyncTaskResult result) {
		super.onPostExecute(result);
		responder.endSaveProperties(result);
	}

	/**
	 * Save the Properties
	 */
	private void saveTheProperties() {
		File file = new File(privateDir + File.separator
				+ destinationFile.getName());
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			properties.store(writer);
			defaultResult.resultMessage = application.getString(
					R.string.file_saved, fileName);
		} catch (IOException e) {
			defaultResult.resultId = Constants.ERROR;
			defaultResult.resultMessage = application.getString(
					R.string.saving_exception, fileName, "IOException",
					e.getMessage());
			application.logE(TAG, defaultResult.resultMessage, e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					/* ignored */
				}
			}
		}
		if (file.exists()) {
			application.getUnixShell().runUnixCommand(
					"chmod 644 " + file.getAbsoluteFile()
			);
		}
	}

	/**
	 * Backup old file.
	 */
	private boolean backupOldFile() {
		boolean result = true;
		String bkFileName = backupOriginalFile();
		if (bkFileName != null) {
			result = application.getUnixShell().runUnixCommand(
					"mv " + fileName + " " + bkFileName);
		}
		return result;
	}

	/**
	 * Move the file from the application private folder to the right place.
	 */
	private void moveNewFile() {
		String prvFile = privateDir + File.separator
				+ destinationFile.getName();
		if (existDestinationFolder()) {
			if (application.getUnixShell().runUnixCommand(
					"cat " + prvFile + " > " + fileName)) {
				application.getEntities().setModified(false);
			} else {
				defaultResult.resultId = Constants.ERROR;
				defaultResult.resultMessage = application.getString(
						R.string.new_file_failed);
			}
		} else {
			defaultResult.resultId = Constants.ERROR;
			defaultResult.resultMessage = application.getString(
					R.string.destination_folder_not_exist, destinationFile
							.getParentFile().getAbsolutePath());
		}
	}

	/**
	 * If destination folder does not exist it will be created.
	 */
	private boolean existDestinationFolder() {
		boolean exist = destinationFile.getParentFile().exists();
		if (!exist) {
			exist = application.getUnixShell()
					.runUnixCommand(
							"mkdir -p "
									+ destinationFile.getParentFile()
											.getAbsolutePath());
		}
		return exist;
	}

	/**
	 * Create backup the original file.
	 */
	private String backupOriginalFile() {
		String backFileName = null;
		if (Utilities.existFile(fileName)) {
			backFileName = fileName + ".bak";
			if (Utilities.existFile(backFileName)) {
				backFileName = null;
			}
		}
		return backFileName;
	}
}
