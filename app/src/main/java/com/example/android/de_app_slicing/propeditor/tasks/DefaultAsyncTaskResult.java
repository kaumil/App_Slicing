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

/**
 * Default result model used on asynchronous tasks to store process results:
 * task ID, result ID (OK or ERROR) and result message string.
 * 
 * @author Kaumil Trivedi
 * 
 */
public class DefaultAsyncTaskResult {
	public int taskId;
	public int resultId;
	public String resultMessage;
}