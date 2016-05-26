/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cyanogenmod.theme.chooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Toolbar;

import cyanogenmod.providers.ThemesContract.ThemesColumns;

public class ChooserActivity extends FragmentActivity implements DrawerAdapter.DrawerClickListener {
    public static final String TAG = ChooserActivity.class.getName();
    public static final String EXTRA_COMPONENT_FILTER = "component_filter";
    public static final String EXTRA_PKGNAME = "pkgName";
    public static final String EXTRA_TITLE = "title";

    private static final HashMap<String, Integer> sTitles = new HashMap<String, Integer>();
    static {
        sTitles.put(ThemesColumns.MODIFIES_LOCKSCREEN, R.string.lock_screen);
        sTitles.put(ThemesColumns.MODIFIES_ICONS, R.string.icons);
        sTitles.put(ThemesColumns.MODIFIES_FONTS, R.string.fonts);
        sTitles.put(ThemesColumns.MODIFIES_LAUNCHER, R.string.wallpapers);
        sTitles.put(ThemesColumns.MODIFIES_BOOT_ANIM, R.string.boot_anims);
        sTitles.put(ThemesColumns.MODIFIES_ALARMS, R.string.sounds);
        sTitles.put(ThemesColumns.MODIFIES_NOTIFICATIONS, R.string.sounds);
        sTitles.put(ThemesColumns.MODIFIES_RINGTONES, R.string.sounds);
        sTitles.put(ThemesColumns.MODIFIES_OVERLAYS, R.string.style);
        sTitles.put(ThemesColumns.MODIFIES_STATUS_BAR, R.string.style);
        sTitles.put(ThemesColumns.MODIFIES_NAVIGATION_BAR, R.string.style);
        sTitles.put(ThemesColumns.MODIFIES_STATUSBAR_HEADERS, R.string.status_bar_headers);
    }

    public static Map<String, Integer> getTitleMap() {
        return Collections.unmodifiableMap(sTitles);
    }

    public static String getTitleFromFilterList(Context context, ArrayList<String> filter) {
        if (filter == null || filter.isEmpty() || !getTitleMap().containsKey(filter.get(0))) {
            return context.getResources().getString(R.string.app_name);
        }
        int res = getTitleMap().get(filter.get(0));
        String title = null;
        try {
            title = context.getResources().getString(res);
        } catch (Exception e) {
            title = context.getResources().getString(R.string.app_name);
        }
        return title;
    }

    private DrawerAdapter mDrawerAdapter;

    private DrawerLayout mDrawerLayout;
    private ViewGroup mDrawerContainer;
    private ListView mDrawerList;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDrawer();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);
        if (mDrawerLayout != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_menu);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mDrawerLayout.isDrawerOpen(mDrawerContainer)) {
                        mDrawerLayout.closeDrawer(mDrawerContainer);
                    } else {
                        mDrawerLayout.openDrawer(mDrawerContainer);
                    }
                }
            });
        }

        NotificationHijackingService.ensureEnabled(this);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    public void setToolbarColor(int color) {
        mToolbar.setBackgroundColor(color);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerContainer = (ViewGroup) findViewById(R.id.left_drawer_container);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerAdapter = new DrawerAdapter(this, this);
        mDrawerList.setAdapter(mDrawerAdapter);
    }

    private void handleIntent(final Intent intent) {
        //Determine if there we need to filter by component (ex icon sets only)
        Bundle extras = intent.getExtras();
        String filter = (extras == null) ? null : extras.getString(EXTRA_COMPONENT_FILTER);

        // If activity started by wallpaper chooser then filter on wallpapers
        if (Intent.ACTION_SET_WALLPAPER.equals(intent.getAction())) {
            filter = "mods_homescreen";
        }

        // Support filters passed in as csv. Since XML prefs do not support
        // passing extras in as arrays.
        ArrayList<String> filtersList = new ArrayList<String>();
        if (filter != null) {
            String[] filters = filter.split(",");
            filtersList.addAll(Arrays.asList(filters));
        }

        Fragment fragment = null;
        String title = getTitleFromFilterList(this, filtersList);
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasExtra(EXTRA_PKGNAME)) {
            String pkgName = intent.getStringExtra(EXTRA_PKGNAME);
            fragment = ChooserDetailFragment.newInstance(pkgName, filtersList);
            // Handle case where Theme Store or some other app wishes to open
            // a detailed theme view for a given package
            try {
                final PackageManager pm = getPackageManager();
                if (pm.getPackageInfo(pkgName, 0) == null) {
                    fragment = ChooserBrowseFragment.newInstance(filtersList, title);
                }
            } catch (PackageManager.NameNotFoundException e) {
                fragment = ChooserBrowseFragment.newInstance(filtersList, title);
            }
        } else {
            fragment = ChooserBrowseFragment.newInstance(filtersList, title);
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment,
                "ChooserBrowseFragment").commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onNavItemSelected(DrawerAdapter.DrawerItem item) {
            Fragment fragment = null;
            ArrayList<String> filtersList = new ArrayList<String>();
            if (item.components != null) {
                String[] filters = item.components.split(",");
                filtersList.addAll(Arrays.asList(filters));
                String title = getTitleFromFilterList(this, filtersList);
                fragment = ChooserBrowseFragment.newInstance(filtersList, title);
            } else if (item.id == R.id.theme_packs) {
                fragment = ChooserBrowseFragment.newInstance(filtersList, getString(R.string.app_name));
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction().replace(R.id.content, fragment,
                        "ChooserBrowseFragment").commit();
            }

            if (mDrawerLayout != null) mDrawerLayout.closeDrawer(mDrawerContainer);
    }
}
