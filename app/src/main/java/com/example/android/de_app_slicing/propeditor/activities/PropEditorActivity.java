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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.example.android.de_app_slicing.propeditor.ManualEdit;
import com.example.android.de_app_slicing.propeditor.PropEditorApplication;
import com.example.android.de_app_slicing.propeditor.R;
import com.example.android.de_app_slicing.propeditor.TweaksListActivity;
import com.example.android.de_app_slicing.propeditor.dialogs.EditorDialog;
import com.example.android.de_app_slicing.propeditor.list.PropertiesListAdapter;
import com.example.android.de_app_slicing.propeditor.models.Constants;
import com.example.android.de_app_slicing.propeditor.properties.Entity;
import com.example.android.de_app_slicing.propeditor.tasks.DefaultAsyncTaskResult;
import com.example.android.de_app_slicing.propeditor.tasks.LoadPropertiesTask;
import com.example.android.de_app_slicing.propeditor.tasks.RestorePropertiesTask;
import com.example.android.de_app_slicing.propeditor.tasks.SavePropertiesTask;
import com.example.android.de_app_slicing.propeditor.util.Utilities;

/**
 * Main activity class.
 */
public class PropEditorActivity extends BaseActivity implements
        LoadPropertiesTask.Responder, SavePropertiesTask.Responder,
        RestorePropertiesTask.Responder {

    private PropertiesListAdapter adapter;
    private EditText filterBox;
    private ListView propertiesList = null;

    private static final int CONFIRM_ID_DELETE = 0;
    private static final int CONFIRM_ID_RESTORE = 1;
    private static final int CONFIRM_ID_RELOAD = 2;
    private static final int CONFIRM_ID_REBOOT = 4;
    private static final int CONFIRM_ID_ERROR_REPORT = 5;
    private static final int REQUEST_CODE_SETTINGS = 0;
    private static final int REQUEST_SEND_REPORT = 1;

    private EditorDialog mEditorDialog;

    /**
     * The method invoked when the activity is creating
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prop_editor);
        setMenuId(R.menu.activity_prop_editor);
        prepareFilterBox();
        prepareMainListView();
    }

    /**
     * Prepare filter editor box
     */
    private void prepareFilterBox() {
        filterBox = (EditText) findViewById(R.id.filter_box);
        filterBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                applyFilter(s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * This method is invoked when the filter is edited
     *
     * @param charSequence The char sequence from the filter
     */
    private void applyFilter(CharSequence charSequence) {
        if (adapter != null) {
            mApplication.showProgressDialog(this, R.string.filtering);
            adapter.getFilter().filter(charSequence);
            mApplication.hideProgressDialog();
        }
    }

    /**
     * Prepare main list view with all controls
     */
    private void prepareMainListView() {
        propertiesList = (ListView) findViewById(R.id.properties_list);
        propertiesList.setEmptyView(findViewById(R.id.empty_list_view));
        propertiesList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (position > -1 && position < adapter.getCount()) {
                    showItemDialogMenu(position);
                }
            }
        });
        if (mApplication.getEntities().isEmpty()) {
            loadPropertiesList();
        } else {
            reloadAdapter();
        }
    }

    /**
     * Load properties list, invoke the thread.
     */
    private void loadPropertiesList() {
        new LoadPropertiesTask(this, PropEditorApplication.BUILD_PROP_PATH,
                mApplication.getEntities()).execute();
    }

    @Override
    protected void onPause() {
        destroyEditorDialog();
        super.onPause();
    }

    /**
     * Destroy the editor dialog.
     */
    private void destroyEditorDialog() {
        if (mEditorDialog != null && mEditorDialog.isShowing()) {
            mEditorDialog.dismiss();
        }
    }

    /**
     * This method is invoked when is selected a menu item from the option menu
     *
     * @param menuItemId The selected menu item
     */
    @Override
    protected boolean onMenuItemSelected(int menuItemId) {
        boolean processed = false;
        switch (menuItemId) {
            case R.id.menu_tweaks:
                processed=true;
                onMenuTweaks();
                break;
            case R.id.item_reboot:
                processed = true;
                onMenuItemReboot();
                break;
            case R.id.item_add:
                processed = true;
                onMenuItemAdd();
                break;
            case R.id.item_reload:
                processed = true;
                onMenuItemReload();
                break;
            case R.id.item_restore:
                processed = true;
                onMenuItemRestore();
                break;
            case R.id.item_manual_edit:
                processed=true;
                onMenuManualEdit();
            case R.id.item_save:
                processed = true;
                onMenuItemSave();
                break;
            case R.id.item_help:
                processed=true;
                onMenuItemHelp();
                break;
            case R.id.item_feedback:
                processed=true;
                onMenuItemFeedback();
                break;
            case R.id.item_back:
                processed = true;
                goBack();
                break;
        }
        return processed;
    }

    /**
     * Method invoked when the load properties task is started
     */
    @Override
    public void startLoadProperties() {
        mApplication.showProgressDialog(this, R.string.loading_properties);
    }

    /**
     * Method invoked when the load properties task is finished
     */
    @Override
    public void endLoadProperties(DefaultAsyncTaskResult result) {
        propertiesList.removeAllViewsInLayout();
        reloadAdapter();
        mApplication.getEntities().setModified(false);
        mApplication.hideProgressDialog();
        if (Constants.OK == result.resultId) {
            mApplication.showMessageInfo(this, result.resultMessage);
        } else if (Constants.ERROR_REPORT == result.resultId) {
            showConfirmationDialog(
                    R.string.send_error_report_title,
                    result.resultMessage, CONFIRM_ID_ERROR_REPORT, null);
        } else {
            mApplication.showMessageError(this, result.resultMessage);
        }
    }

    /**
     * Reload adapter and properties list based on the provided properties.
     */
    public void reloadAdapter() {
        adapter = new PropertiesListAdapter(this, mApplication, mApplication.getEntities());
        propertiesList.setAdapter(adapter);
        propertiesList.setFastScrollEnabled(mApplication.getEntities().size() > 50);
    }

    /**
     * This method show the popup menu when the user do a long click on a list
     * item
     *
     * @param position The contact position where was made the long click
     */
    private void showItemDialogMenu(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Entity entity = adapter.getItem(position);
        builder.setTitle(getString(R.string.item_edit, entity.getKey()));
        builder.setItems(R.array.menu_list,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                onMenuItemEdit(position);
                                break;
                            case 1:
                                onMenuItemDelete(position);
                                break;
                            case 2:
                                onMenuItemAdd();
                                break;
                        }
                    }
                });
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    /**
     * Invoked when is chose the Add from menus. Should open the Editor to add a
     * new property.
     */
    private void onMenuItemAdd() {
        mEditorDialog = new EditorDialog(this, mApplication.getEntities(), null, R.string.add_property);
        mEditorDialog.show();
    }

    /**
     * Invoked when is chose the Edit from menus.
     *
     * @param position The position of selected element to be edited
     */
    private void onMenuItemEdit(int position) {
        Entity entity = adapter.getItem(position);
        mEditorDialog = new EditorDialog(this, mApplication.getEntities(), entity,
                R.string.edit_property);
        mEditorDialog.show();
    }

    /**
     * Invoked when is chose the Delete from menus.
     *
     * @param position The position of selected element to be deleted
     */
    private void onMenuItemDelete(int position) {
        final Entity entity = adapter.getItem(position);
        if (entity != null) {
            showConfirmationDialog(
                    R.string.remove_property,
                    mApplication.getString(R.string.remove_property_question,
                            entity.getKey()), CONFIRM_ID_DELETE, entity);
        }
    }


    /**
     * This method is invoked by the each time when is accepted a confirmation
     * dialog.
     *
     * @param confirmationId The confirmation ID to identify the case.
     * @param anObject       An object send by the caller method.
     */
    @Override
    protected void onConfirmation(int confirmationId, Object anObject) {
        switch (confirmationId) {
            case CONFIRM_ID_DELETE:
                doDeleteEntity(anObject);
                break;
            case CONFIRM_ID_RESTORE:
                new RestorePropertiesTask(this, PropEditorApplication.BUILD_PROP_PATH).execute();
                break;
            case CONFIRM_ID_RELOAD:
                doListReload();
                break;
            case CONFIRM_ID_REBOOT:
                doReboot();
                break;
            case CONFIRM_ID_ERROR_REPORT:
                doErrorReport();
                break;
        }
    }

    /**
     * Method used to delete an entity from the list.
     *
     * @param anEntity Entity to be deleted.
     */
    private void doDeleteEntity(Object anEntity) {
        if (anEntity instanceof Entity) {
            Entity entity = (Entity) anEntity;
            mApplication.getEntities().remove(entity);
            reloadAdapter();
            mApplication.getEntities().setModified(true);
        }
    }

    /**
     * Launch the default browser with a specified URL page.
     *
     * @param urlResourceId The URL resource id.
     */
    private void startBrowserWithPage(int urlResourceId) {
        String url = mApplication.getString(urlResourceId);
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException exception) {
        }
    }

    /**
     * Method invoked when is clicked the reboot menu item.
     */
    private void onMenuItemReboot() {
        if (mApplication.getUnixShell().hasRootAccess()) {
            showConfirmationDialog(R.string.reboot,
                    mApplication.getString(R.string.reboot_confirmation),
                    CONFIRM_ID_REBOOT, null);
        } else {
            mApplication.showMessageError(this, R.string.no_root_privileges);
        }
    }

    /**
     * Invoked when is chose the Reload menu item.
     */
    private void onMenuItemReload() {
        if (mApplication.getEntities().isModified()) {
            showConfirmationDialog(R.string.reload,
                    mApplication.getString(R.string.reload_confirmation),
                    CONFIRM_ID_RELOAD, null);
        } else {
            doListReload();
        }
    }

    /**
     * Method used to invoke the list reloading.
     */
    private void doListReload() {
        loadPropertiesList();
    }

    /**
     * Method used to reboot the device.
     */
    private void doReboot() {
        mApplication.showProgressDialog(this, R.string.rebooting);
        Utilities.reboot(mApplication);
    }

    /**
     * This is invoked when the user chose the restore menu item.
     */
    private void onMenuItemRestore() {
        showConfirmationDialog(R.string.restore,
                mApplication.getString(R.string.restore_confirmation),
                CONFIRM_ID_RESTORE, null);
    }

    /**
     * Invoked when is chose the Save menu item.
     */
    private void onMenuItemSave() {
        new SavePropertiesTask(this, PropEditorApplication.BUILD_PROP_PATH, mApplication.getEntities()).execute();
    }

    /**
     * The saving process is started.
     */
    @Override
    public void startSaveProperties() {
        mApplication.showProgressDialog(this, R.string.saving_properties);
    }

    /**
     * The saving process is ended.
     */
    @Override
    public void endSaveProperties(DefaultAsyncTaskResult result) {
        mApplication.hideProgressDialog();
        if (Constants.OK == result.resultId) {
            mApplication.showMessageInfo(this, result.resultMessage);
        } else {
            mApplication.showMessageError(this, result.resultMessage);
        }
    }
    /**
     * Show the tweaks activity
     */
    private void onMenuTweaks() {
        Intent intent = new Intent(getBaseContext(), TweaksListActivity.class);
        startActivityForResult(intent, 1);
    }
    /**
     * Show the manual edit activity
     */
    private void onMenuManualEdit() {
        Intent intent = new Intent(getBaseContext(), ManualEdit.class);
        startActivityForResult(intent, 1);
    }

    /**
     * Show the help activity
     */
    private void onMenuItemHelp() {
        Intent intent = new Intent(getBaseContext(), HelpActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * Show the feedback activity
     */
    private void onMenuItemFeedback() {
        Intent intent = new Intent(getBaseContext(), SendFeedbackActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * This method is invoked when is started the restore process.
     */
    @Override
    public void startRestoreProperties() {
        mApplication.showProgressDialog(this, R.string.restoring);
    }

    /**
     * This method is invoked when the restore process is finished.
     */
    @Override
    public void endRestoreProperties(DefaultAsyncTaskResult result) {
        mApplication.hideProgressDialog();
        if (Constants.OK == result.resultId) {
            mApplication.showMessageInfo(this, result.resultMessage);
            loadPropertiesList();
        } else {
            mApplication.showMessageError(this, result.resultMessage);
        }
    }

    /**
     * This method is invoked when a child activity is finished and this
     * activity is showed again
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (mApplication.isMustRestart()) {
                mApplication.setMustRestart(false);
                restartActivity();
            }
        }
    }

    /**
     * Restart this activity.
     */
    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Send email
     */
    private void doErrorReport() {
        mApplication.doSendReport(this, REQUEST_SEND_REPORT,
                mApplication.getApplicationContext().getString(R.string.send_error_report_title));
    }
}
