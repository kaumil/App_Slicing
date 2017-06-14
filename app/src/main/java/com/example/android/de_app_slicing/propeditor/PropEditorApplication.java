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
package com.example.android.de_app_slicing.propeditor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.example.android.de_app_slicing.propeditor.properties.Entities;
import com.example.android.de_app_slicing.propeditor.provider.CachedFileProvider;
import com.example.android.de_app_slicing.propeditor.tasks.LogThread;
import com.example.android.de_app_slicing.propeditor.util.DevicesUtils;
import com.example.android.de_app_slicing.propeditor.util.Utilities;
import com.example.android.de_app_slicing.propeditor.shell.UnixCommands;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * This is main application class. Here are defined the progress dialog and
 * information popup.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class PropEditorApplication extends Application {
	private final String TAG = getClass().getName();
	private ProgressDialog mProgressDialog;
	private Entities mProperties;
	private String mWaitString;
	private Locale mDefaultLocale;
	private UnixCommands mUnixShell;
	private SharedPreferences mSharedPreferences;
	private boolean mMustRestart;

	public static final String BUILD_PROP = "build.prop";
	public static final String BUILD_PROP_PATH = "/system/build.prop";

	private static int mSdkInt = 8;
	private static int mVersionCode = -1;
	private static String mVersionName = null;

	public static final String LOGS_FOLDER_NAME = "logs";
	public static final String LOG_FILE_NAME = "PropEditor_logs.log";
	private File mLogsFolder;
	private static File mLogFile;
	private static LogThread mLogFileThread;
	private static SimpleDateFormat mSimpleDateFormat;

	public static final String KEY_APP_THEME = "appTheme";
	public static final String KEY_SU_PATH = "suPath";
	private static final int BUFFER = 1024;

	private AlertDialog mAlertDialog;

	/**
	 * This method is invoked when the application is created.
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mSdkInt = android.os.Build.VERSION.SDK_INT;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mProperties = new Entities();
		mWaitString = getString(R.string.please_wait);
		mDefaultLocale = Locale.getDefault();
	}

	/**
	 * The user-visible SDK version of the framework.
	 *
	 * @return The user-visible SDK version of the framework
	 */
	public int getSdkInt() {
		return mSdkInt;
	}

	/**
	 * Obtain the current unix shell.
	 * 
	 * @return Unix shell.
	 */
	public UnixCommands getUnixShell() {
		if (mUnixShell == null) {
			mUnixShell = new UnixCommands(getSuPath());
		}
		return mUnixShell;
	}

	/**
	 * Get the super user binary path.
	 *
	 * @return The super user binary path.
	 */
	public String getSuPath() {
		return mSharedPreferences.getString(KEY_SU_PATH, "");
	}

	/**
	 * Method used when the application should be closed.
	 */
	public void onClose() {
		saveSuPath();
		hideProgressDialog();
		destroyAlertDialog(mAlertDialog);
		if (mUnixShell != null) {
			mUnixShell.closeShell();
		}
	}

	/**
	 * Save SU path, if any.
	 */
	private void saveSuPath() {
		if (mUnixShell != null) {
			String suPath = mUnixShell.getSuPath();
			saveStringValue(KEY_SU_PATH, suPath);
		}
	}

	/**
	 * This will show a progress dialog using a context and a message ID from
	 * application string resources.
	 * 
	 * @param context
	 *            The context where should be displayed the progress dialog.
	 * @param messageId
	 *            The string resource id.
	 */
	public void showProgressDialog(Context context, int messageId) {
		showProgressDialog(context, getString(messageId));
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 * 
	 * @param context
	 *            The context where should be displayed the progress dialog.
	 * @param message
	 *            The message displayed inside of progress dialog.
	 */
	public void showProgressDialog(Context context, String message) {
		hideProgressDialog();
		mProgressDialog = ProgressDialog.show(context, mWaitString, message);
	}

	/**
	 * Method used to hide the progress dialog.
	 */
	public void hideProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
	}

	/**
	 * Destroy alert dialog.
	 */
	public void destroyAlertDialog(AlertDialog alertDialog) {
		if (alertDialog != null) {
			try {
				if (alertDialog.isShowing()) {
					alertDialog.dismiss();
				}
			} catch (Exception e) {
			}
			alertDialog = null;
		}
	}

	/**
	 * Method used to show the informations.
	 * 
	 * @param context
	 *            The context where should be displayed the message.
	 * @param resourceMessageId
	 *            The string resource id.
	 * @param arguments
	 *            The message arguments.
	 */
	public void showMessageInfo(Context context, int resourceMessageId,
			Object... arguments) {
		String message = getString(resourceMessageId, arguments);
		showMessageInfo(context, message);
	}

	/**
	 * Method used to show the informations.
	 * 
	 * @param context
	 *            The context where should be displayed the message.
	 * @param resourceMessageId
	 *            The string resource id.
	 */
	public void showMessageInfo(Context context, int resourceMessageId) {
		String message = getString(resourceMessageId);
		showMessageInfo(context, message);
	}

	/**
	 * This method is used to show on front of a context a toast message.
	 * 
	 * @param context
	 *            The context where should be showed the message.
	 * @param message
	 *            The message used to be displayed on the information box.
	 */
	public void showMessageInfo(Context context, String message) {
		if (message != null && message.length() > 0) {
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Method used to show the errors.
	 * 
	 * @param context
	 *            The context where should be displayed the message.
	 * @param resourceMessageId
	 *            The string resource id.
	 */
	public void showMessageError(Context context, int resourceMessageId) {
		String message = getString(resourceMessageId);
		showMessageError(context, message);
	}

	/**
	 * Method used to show the errors.
	 * 
	 * @param context
	 *            The context where should be displayed the message.
	 * @param resourceMessageId
	 *            The string resource id.
	 * @param arguments
	 *            The message arguments.
	 */
	public void showMessageError(Context context, int resourceMessageId,
			Object... arguments) {
		String message = getString(resourceMessageId, arguments);
		showMessageError(context, message);
	}

	/**
	 * This method is used to show on front of a context a toast message
	 * containing applications errors.
	 * 
	 * @param context
	 *            The context where should be showed the message.
	 * @param message
	 *            The error message used to be displayed on the information box.
	 */
	public void showMessageError(Context context, String message) {
		if (message != null && message.length() > 0) {
			try {
				mAlertDialog = new AlertDialog.Builder(context)
						.setTitle(R.string.error_occurred)
						.setMessage(message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setNeutralButton(R.string.ok,
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int whichButton) {
										return;
									}
								}).show();
			} catch (Exception e) {
				Log.e(TAG,
						"message:" + message + " Exception:" + e.getMessage(),
						e);
			}
		}
	}

	/**
	 * Retrieve default application locale
	 * 
	 * @return Default locale used on application
	 */
	public Locale getDefaultLocale() {
		return mDefaultLocale;
	}

	/**
	 * Retrieve available properties.
	 * 
	 * @return Loaded properties.
	 */
	public Entities getEntities() {
		return mProperties;
	}

	/**
	 * Check for pro version.
	 * 
	 * @return True if pro version exist.
	 */
	public boolean isProPresent() {
		PackageManager pm = getPackageManager();
		boolean success = false;
		try {
			success = (PackageManager.SIGNATURE_MATCH == pm.checkSignatures(
					this.getPackageName(), "com.example.android.de_app_slicing.propeditorpro"));
			Log.d(TAG, "isProPresent: " + success);
		} catch (Exception e) {
			Log.e(TAG, "isProPresent: " + e.getMessage(), e);
		}
		return success;
	}

	/**
	 * Get the application theme.
	 *
	 * @return The application theme.
	 */
	public int getApplicationTheme() {
		String theme = mSharedPreferences.getString(KEY_APP_THEME, "dark");
		if ("dark".equals(theme)) {
			return R.style.AppThemeLight;
		}
		return R.style.AppThemeLight;
	}

	/**
	 * Set the must restart flag.
	 *
	 * @param mustRestart The value to be set.
	 */
	public void setMustRestart(boolean mustRestart) {
		this.mMustRestart = mustRestart;
	}

	/**
	 * Check the must restart flag state.
	 *
	 * @return The must restart flag state.
	 */
	public boolean isMustRestart() {
		return mMustRestart;
	}

	/**
	 * Get logs folder. If is not defined then is initialized and created.
	 *
	 * @return Logs folder.
	 */
	public File getLogsFolder() {
		if (mLogsFolder == null) {
			mLogsFolder = new File(getCacheDir() + File.separator + PropEditorApplication.LOGS_FOLDER_NAME);
			if (!mLogsFolder.exists()) {
				mLogsFolder.mkdirs();
			}
		}
		return mLogsFolder;
	}

	/**
	 * Send a log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logE(String tag, String msg) {
		Log.e(tag, msg);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg);
	}

	/**
	 * Send a log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr  An exception to log
	 */
	public void logE(String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg
				+ "\t" + Log.getStackTraceString(tr));
	}

	/**
	 * Send a log message.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logD(String tag, String msg) {
		Log.d(tag, msg);
		writeLogFile(System.currentTimeMillis(), "DEBUG\t" + tag + "\t" + msg);
	}

	/**
	 * Write the log message to the app log file.
	 *
	 * @param logmessage The log message.
	 */
	private void writeLogFile(long milliseconds, String logmessage) {
		if (checkLogFileThread()) {
			mLogFileThread.addLog(mSimpleDateFormat.format(new Date(milliseconds))
					+ "\t" + logmessage);
		}
	}

	/**
	 * Check if log file thread exist and create it if not.
	 */
	private boolean checkLogFileThread() {
		if (mLogFileThread == null) {
			try {
				mLogFile = new File(getLogsFolder(), PropEditorApplication.LOG_FILE_NAME);
				mLogFileThread = new LogThread(mLogFile);
				mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
				mSimpleDateFormat.setTimeZone(TimeZone.getDefault());
				new Thread(mLogFileThread).start();
			} catch (Exception e) {
				logE(TAG, "Exception: " + e.getMessage(), e);
			}
		}
		return mLogFileThread != null;
	}

	/**
	 * Obtain the log file.
	 *
	 * @return The log file.
	 */
	public File getLogFile() {
		return mLogFile;
	}

	/**
	 * Remove log file from disk.
	 */
	public void deleteLogFile() {
		if (mLogFile != null && mLogFile.exists()) {
			try {
				mLogFileThread.close();
				while (!mLogFileThread.isClosed()) {
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			} catch (InterruptedException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			}
			mLogFileThread = null;
			mLogFile.delete();
		}
	}



	/**
	 * Retrieve the application version code.
	 *
	 * @return The application version code.
	 */
	public int getVersionCode() {
		if (mVersionCode == -1) {
			try {
				mVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
			}
		}
		return mVersionCode;
	}

	/**
	 * Retrieve the application version name.
	 *
	 * @return The application version name.
	 */
	public String getVersionName() {
		if (mVersionName == null) {
			try {
				mVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (PackageManager.NameNotFoundException e) {
			}
		}
		return mVersionName;
	}

	/**
	 * User just confirmed to send a report.
	 */
	public void doSendReport(FragmentActivity activity, int requestCode,
									 String emailTitle) {
		showProgressDialog(activity, R.string.send_report);
		String message = getApplicationContext().getString(R.string.report_body);
		File logsFolder = getLogsFolder();
		File archive = getLogArchive(logsFolder);
		String[] TO = {"ciubex@yahoo.com"};

		Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);

		ArrayList<Uri> uris = new ArrayList<>();
		if (archive != null && archive.exists() && archive.length() > 0) {
			uris.add(Uri.parse("content://" + CachedFileProvider.AUTHORITY
					+ "/" + archive.getName()));
		}
		if (!uris.isEmpty()) {
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		hideProgressDialog();
		try {
			activity.startActivityForResult(Intent.createChooser(emailIntent,
					getApplicationContext().getString(R.string.send_report)), requestCode);
		} catch (ActivityNotFoundException ex) {
			logE(TAG,
					"confirmedSendReport Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Build the logs and call the archive creator.
	 *
	 * @param logsFolder The logs folder.
	 * @return The archive file which should contain the logs.
	 */
	private File getLogArchive(File logsFolder) {
		File logFile = getLogFile();
		File logcatFile = getLogcatFile(logsFolder);
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
		String fileName = "propeditor_" + format.format(now) + ".zip";
		return getArchives(new File[]{logFile, logcatFile, new File(PropEditorApplication.BUILD_PROP_PATH)},
				logsFolder, fileName);
	}

	/**
	 * Method used to build a ZIP archive with log files.
	 *
	 * @param files       The log files to be added.
	 * @param logsFolder  The logs folder where should be added the archive name.
	 * @param archiveName The archive file name.
	 * @return The archive file.
	 */
	private File getArchives(File[] files, File logsFolder, String archiveName) {
		File archive = new File(logsFolder, archiveName);
		try {
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(archive);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte data[] = new byte[BUFFER];
			File file;
			FileInputStream fi;
			ZipEntry entry;
			int count;
			for (int i = 0; i < files.length; i++) {
				file = files[i];
				if (file.exists() && file.length() > 0) {
					logD(TAG, "Adding to archive: " + file.getName());
					fi = new FileInputStream(file);
					origin = new BufferedInputStream(fi, BUFFER);
					entry = new ZipEntry(file.getName());
					out.putNextEntry(entry);
					while ((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
					}
					Utilities.doClose(entry);
					Utilities.doClose(origin);
				}
			}
			Utilities.doClose(out);
		} catch (FileNotFoundException e) {
			logE(TAG, "getArchives failed: FileNotFoundException", e);
		} catch (IOException e) {
			logE(TAG, "getArchives failed: IOException", e);
		}
		return archive;
	}

	/**
	 * Generate logs file on cache directory.
	 *
	 * @param cacheFolder Cache directory where are the logs.
	 * @return File with the logs.
	 */
	private File getLogcatFile(File cacheFolder) {
		File logFile = new File(cacheFolder, "PropEditor_logcat.log");
		Process shell = null;
		InputStreamReader reader = null;
		FileWriter writer = null;
		char LS = '\n';
		char[] buffer = new char[BUFFER];
		String model = Build.MODEL;
		if (!model.startsWith(Build.MANUFACTURER)) {
			model = Build.MANUFACTURER + " " + model;
		}
		logD(TAG, "Prepare Logs to be send via e-mail.");
		String oldCmd = "logcat -d -v threadtime com.example.android.de_app_slicing.propeditor:v dalvikvm:v System.err:v *:s";
		String newCmd = "logcat -d -v threadtime";
		String command = newCmd;
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			if (getSdkInt() <= 15) {
				command = oldCmd;
			}
			shell = Runtime.getRuntime().exec(command);
			reader = new InputStreamReader(shell.getInputStream());
			writer = new FileWriter(logFile);
			writer.write("Android version: " + Build.VERSION.SDK_INT +
					" (" + Build.VERSION.CODENAME + ")" + LS);
			writer.write("Device: " + model + LS);
			writer.write("Device name: " + DevicesUtils.getDeviceName(getAssets()) + LS);
			writer.write("App version: " + getVersionName() +
					" (" + getVersionCode() + ")" + LS);
			int n;
			do {
				n = reader.read(buffer, 0, BUFFER);
				if (n == -1) {
					break;
				}
				writer.write(buffer, 0, n);
			} while (true);
			shell.waitFor();
		} catch (IOException e) {
			logE(TAG, "getLogcatFile failed: IOException", e);
		} catch (InterruptedException e) {
			logE(TAG, "getLogcatFile failed: InterruptedException", e);
		} catch (Exception e) {
			logE(TAG, "getLogcatFile failed: Exception", e);
		} finally {
			Utilities.doClose(writer);
			Utilities.doClose(reader);
			if (shell != null) {
				shell.destroy();
			}
		}
		return logFile;
	}

	/**
	 * Store a string value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The string value to be saved.
	 */
	private void saveStringValue(String key, String value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}
}
