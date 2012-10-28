///*
// * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.thialfihar.android.apg.ui;
//
//import java.util.Vector;
//
//import org.thialfihar.android.apg.Constants;
//import org.thialfihar.android.apg.R;
//import org.thialfihar.android.apg.Id;
//import org.thialfihar.android.apg.ui.widget.SelectPublicKeyListAdapter;
//
//import com.actionbarsherlock.app.ActionBar;
//import com.actionbarsherlock.app.SherlockFragmentActivity;
//import com.actionbarsherlock.view.Menu;
//import com.actionbarsherlock.view.MenuItem;
//
//import android.app.SearchManager;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.Button;
//import android.widget.ListView;
//import android.widget.TextView;
//
//public class SelectPublicKeyListActivityOld extends SherlockFragmentActivity {
//
//    // Not used in sourcode, but listed in AndroidManifest!
//    public static final String ACTION_SELECT_PUBLIC_KEYS = Constants.INTENT_PREFIX
//            + "SELECT_PUBLIC_KEYS";
//
//    public static final String RESULT_EXTRA_SELECTION = "selection";
//    public static final String RESULT_EXTRA_USER_IDS = "userIds";
//
//    protected ListView mList;
//    protected SelectPublicKeyListAdapter mListAdapter;
//    protected View mFilterLayout;
//    protected Button mClearFilterButton;
//    protected TextView mFilterInfo;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        final ActionBar actionBar = getSupportActionBar();
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setDisplayHomeAsUpEnabled(false);
//        actionBar.setHomeButtonEnabled(false);
//
//        setContentView(R.layout.select_public_key);
//
//        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
//
//        mList = (ListView) findViewById(R.id.list);
//        // needed in Android 1.5, where the XML attribute gets ignored
//        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
//
//        mFilterLayout = findViewById(R.id.layout_filter);
//        mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
//        mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);
//
//        mClearFilterButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                handleIntent(new Intent());
//            }
//        });
//
//        handleIntent(getIntent());
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        handleIntent(intent);
//    }
//
//    private void handleIntent(Intent intent) {
//        String searchString = null;
//        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//            searchString = intent.getStringExtra(SearchManager.QUERY);
//            if (searchString != null && searchString.trim().length() == 0) {
//                searchString = null;
//            }
//        }
//
//        long selectedKeyIds[] = null;
//        selectedKeyIds = intent.getLongArrayExtra(RESULT_EXTRA_SELECTION);
//
//        if (selectedKeyIds == null) {
//            Vector<Long> vector = new Vector<Long>();
//            for (int i = 0; i < mList.getCount(); ++i) {
//                if (mList.isItemChecked(i)) {
//                    vector.add(mList.getItemIdAtPosition(i));
//                }
//            }
//            selectedKeyIds = new long[vector.size()];
//            for (int i = 0; i < vector.size(); ++i) {
//                selectedKeyIds[i] = vector.get(i);
//            }
//        }
//
//        if (searchString == null) {
//            mFilterLayout.setVisibility(View.GONE);
//        } else {
//            mFilterLayout.setVisibility(View.VISIBLE);
//            mFilterInfo.setText(getString(R.string.filterInfo, searchString));
//        }
//
//        if (mListAdapter != null) {
//            mListAdapter.cleanup();
//        }
//
//        mListAdapter = new SelectPublicKeyListAdapter(this, mList, searchString, selectedKeyIds);
//        mList.setAdapter(mListAdapter);
//
//        if (selectedKeyIds != null) {
//            for (int i = 0; i < mListAdapter.getCount(); ++i) {
//                long keyId = mListAdapter.getItemId(i);
//                for (int j = 0; j < selectedKeyIds.length; ++j) {
//                    if (keyId == selectedKeyIds[j]) {
//                        mList.setItemChecked(i, true);
//                        break;
//                    }
//                }
//            }
//        }
//    }
//
//    private void cancelClicked() {
//        setResult(RESULT_CANCELED, null);
//        finish();
//    }
//
//    private void okClicked() {
//        Intent data = new Intent();
//        Vector<Long> keys = new Vector<Long>();
//        Vector<String> userIds = new Vector<String>();
//        for (int i = 0; i < mList.getCount(); ++i) {
//            if (mList.isItemChecked(i)) {
//                keys.add(mList.getItemIdAtPosition(i));
//                userIds.add((String) mList.getItemAtPosition(i));
//            }
//        }
//        long selectedKeyIds[] = new long[keys.size()];
//        for (int i = 0; i < keys.size(); ++i) {
//            selectedKeyIds[i] = keys.get(i);
//        }
//        String userIdArray[] = new String[0];
//        data.putExtra(RESULT_EXTRA_SELECTION, selectedKeyIds);
//        data.putExtra(RESULT_EXTRA_USER_IDS, userIds.toArray(userIdArray));
//        setResult(RESULT_OK, data);
//        finish();
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, Id.menu.option.search, 0, R.string.menu_search).setIcon(
//                android.R.drawable.ic_menu_search);
//        menu.add(1, Id.menu.option.cancel, 0, R.string.btn_doNotSave).setShowAsAction(
//                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//        menu.add(1, Id.menu.option.okay, 1, R.string.btn_okay).setShowAsAction(
//                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//
//        case Id.menu.option.okay:
//            okClicked();
//            return true;
//
//        case Id.menu.option.cancel:
//            cancelClicked();
//            return true;
//
//        default:
//            break;
//
//        }
//        return false;
//    }
//}
