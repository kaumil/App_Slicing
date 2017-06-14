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
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.android.de_app_slicing.propeditor.util.Utilities;

/**
 * A thread used to write logs to a private file.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class LogThread implements Runnable, Closeable {

	private List<String> logs;
	private File logFile;
	private boolean closing;
	private boolean closed;

	public LogThread(File file) {
		logs = new ArrayList<String>();
		this.logFile = file;
		closing = false;
		closed = false;
	}

	/**
	 * Add a log string to logs collection
	 * 
	 * @param log
	 *            Log to be added.
	 */
	public void addLog(String log) {
		if (!closing) {
			synchronized (logs) {
				logs.add(log);
				logs.notifyAll();
			}
		}
	}

	/**
	 * Close root shell
	 */
	@Override
	public void close() throws IOException {
		synchronized (logs) {
			closing = true;
			logs.notifyAll();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void run() {
		BufferedWriter bufferedWriter = null;
		try {
			createLogFile();
			bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
			writeLogs(bufferedWriter);
		} catch (IOException e) {
			closing = true;
		} finally {
			Utilities.doClose(bufferedWriter);
		}
		closed = true;
	}

	/**
	 * Write available logs to log file.
	 * 
	 * @throws IOException
	 */
	private void writeLogs(BufferedWriter bufferedWriter) throws IOException {
		while (!closing) {
			synchronized (logs) {
				try {
					logs.wait();
					for (String log : logs) {
						bufferedWriter.append(log);
						bufferedWriter.newLine();
					}
					bufferedWriter.flush();
					logs.clear();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Create the log file.
	 * 
	 * @throws IOException
	 */
	private void createLogFile() throws IOException {
		if (!logFile.exists()) {
			logFile.createNewFile();
		}
	}
}
