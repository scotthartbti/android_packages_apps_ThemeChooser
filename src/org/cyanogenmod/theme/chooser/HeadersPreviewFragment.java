/*
 * Copyright (C) 2016 The DirtyUnicorns Project
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

import org.cyanogenmod.theme.util.Utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import cyanogenmod.providers.ThemesContract;

public class HeadersPreviewFragment extends Fragment {
    private static final String PKG_EXTRA = "pkg_extra";
    private String mPkgName;
    private LruCache<String, Bitmap> mMemoryCache;

    private ImageView mHeader1;
    private ImageView mHeader2;
    private ImageView mHeader3;

    static HeadersPreviewFragment newInstance(String pkgName) {
        final HeadersPreviewFragment f = new HeadersPreviewFragment();
        final Bundle args = new Bundle();
        args.putString(PKG_EXTRA, pkgName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPkgName = getArguments().getString(PKG_EXTRA);

        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        final int memClass = ((ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE))
                .getMemoryClass();
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in bytes rather than number of items.
                return bitmap.getByteCount();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.headers_preview_item, container, false);
        mHeader1 = (ImageView) view.findViewById(R.id.header1);
        mHeader2 = (ImageView) view.findViewById(R.id.header2);
        mHeader3 = (ImageView) view.findViewById(R.id.header3);
        new AsyncHeaderLoaderTask().execute(mPkgName);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public class AsyncHeaderLoaderTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                String packageName = params[0];
                mMemoryCache.put(ThemesContract.PreviewColumns.HEADER_PREVIEW_1,
                        Utils.getPreviewBitmap(getContext(), packageName,
                                ThemesContract.PreviewColumns.HEADER_PREVIEW_1));
                mMemoryCache.put(ThemesContract.PreviewColumns.HEADER_PREVIEW_2,
                        Utils.getPreviewBitmap(getContext(), packageName,
                                ThemesContract.PreviewColumns.HEADER_PREVIEW_2));
                mMemoryCache.put(ThemesContract.PreviewColumns.HEADER_PREVIEW_3,
                        Utils.getPreviewBitmap(getContext(), packageName,
                                ThemesContract.PreviewColumns.HEADER_PREVIEW_3));
            } catch (Exception e) {
                // lazy handling until we get stronger loading/caching
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voidz) {
            try {
                if (mHeader1 != null)
                    mHeader1.setBackground(new BitmapDrawable(mMemoryCache
                            .get(ThemesContract.PreviewColumns.HEADER_PREVIEW_1)));
                if (mHeader2 != null)
                    mHeader2.setBackground(new BitmapDrawable(mMemoryCache
                            .get(ThemesContract.PreviewColumns.HEADER_PREVIEW_2)));
                if (mHeader3 != null)
                    mHeader3.setBackground(new BitmapDrawable(mMemoryCache
                            .get(ThemesContract.PreviewColumns.HEADER_PREVIEW_3)));
            } catch (Exception e) {
                // lazy handling until we get stronger loading/caching
            }
        }
    }
}
