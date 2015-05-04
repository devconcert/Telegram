/*
 * This is the source code of Telegram for Android v. 1.7.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.telegram.android.AndroidUtilities;
import org.telegram.android.ContactsController;
import org.telegram.android.LocaleController;
import org.telegram.android.NotificationCenter;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.messenger.R;
import me.ttalk.sdk.theme.ThemeManager;

public class LabActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;

    private int itemStoreSectionRow;
    private int itemStoreRow;
    private int labDetailRow;
    private int rowCount;
    private BackupImageView avatarImage;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        ContactsController.getInstance().loadPrivacySettings();

        rowCount = 0;
        itemStoreSectionRow = rowCount++;
        itemStoreRow = rowCount++;
        labDetailRow = rowCount++;

//        NotificationCenter.getInstance().addObserver(this, NotificationCenter.privacyRulesUpdated);

        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
//        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.privacyRulesUpdated);
    }

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        if (fragmentView == null) {
            Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_ab_back");
            if (drawable != null){
                actionBar.setBackButtonDrawable(drawable);
            }else{
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            }

            actionBar.setAllowOverlayTitle(true);
            actionBar.setTitle(LocaleController.getString("LabTitle", R.string.LabTitle));
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            listAdapter = new ListAdapter(getParentActivity());

//            fragmentView = new FrameLayout(getParentActivity());
//            FrameLayout frameLayout = (FrameLayout) fragmentView;
//            frameLayout.setBackgroundColor(0xfff0f0f0);

            fragmentView = new LinearLayout(getParentActivity());
            LinearLayout linearLayout = (LinearLayout) fragmentView;
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setBackgroundResource(R.drawable.greydivider);

            FrameLayout frameLayout = new FrameLayout(getParentActivity());
            frameLayout.setBackgroundColor(0xffffffff);
            linearLayout.addView(frameLayout);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
            layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.TOP;
            frameLayout.setLayoutParams(layoutParams);

            avatarImage = new BackupImageView(getParentActivity());
            avatarImage.setImageResource(R.drawable.sticker_type_b_06);
            frameLayout.addView(avatarImage);
            FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) avatarImage.getLayoutParams();
            layoutParams2.width = AndroidUtilities.dp(140);
            layoutParams2.height = AndroidUtilities.dp(140);
            layoutParams2.topMargin = AndroidUtilities.dp(10);
            layoutParams2.bottomMargin = AndroidUtilities.dp(5);
            layoutParams2.gravity = Gravity.CENTER;
            avatarImage.setLayoutParams(layoutParams2);

            LinearLayout textLayout = new LinearLayout(getParentActivity());
            textLayout.setBackgroundColor(0xffffffff);
            textLayout.setVisibility(View.VISIBLE);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.addView(textLayout);
            layoutParams = (LinearLayout.LayoutParams) textLayout.getLayoutParams();
            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(50);
            textLayout.setLayoutParams(layoutParams);

            TextView emptyTextView = new TextView(getParentActivity());
            emptyTextView.setTextColor(0xff424242);
            emptyTextView.setTextSize(17);
            emptyTextView.setGravity(Gravity.CENTER);
            emptyTextView.setText(LocaleController.getString("LabWelcome", R.string.LabWelcome));
            textLayout.addView(emptyTextView);
            layoutParams = (LinearLayout.LayoutParams) emptyTextView.getLayoutParams();
            layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            layoutParams.weight = 0.5f;
            emptyTextView.setLayoutParams(layoutParams);

            LinearLayout divideLayer = new LinearLayout(getParentActivity());
            divideLayer.setBackgroundColor(0xffeeeeee);
            divideLayer.setVisibility(View.VISIBLE);
            divideLayer.setOrientation(LinearLayout.VERTICAL);
            linearLayout.addView(divideLayer);
            layoutParams = (LinearLayout.LayoutParams) divideLayer.getLayoutParams();
            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = AndroidUtilities.dp(0.5f);

            // ListView
            ListView listView = new ListView(getParentActivity());
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setVerticalScrollBarEnabled(false);
            listView.setDrawSelectorOnTop(true);
            linearLayout.addView(listView);
            layoutParams = (LinearLayout.LayoutParams) listView.getLayoutParams();
            layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            layoutParams.gravity = Gravity.TOP;
            listView.setLayoutParams(layoutParams);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == itemStoreRow) {
                        //ServiceAgent.getInstance().logEvent("Settings", "ShowSecretMessageRow");
                        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        boolean show = preferences.getBoolean("item_store", true);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("item_store", !show);
                        editor.commit();
                        if (view instanceof TextCheckCell) {
                            ((TextCheckCell) view).setChecked(!show);
                        }
                    }
                }
            });
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
//        if (id == NotificationCenter.privacyRulesUpdated) {
//            if (listAdapter != null) {
//                listAdapter.notifyDataSetChanged();
//            }
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return i == itemStoreRow;
        }

        @Override
        public int getCount() {
            return rowCount;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int type = getItemViewType(i);
            if (type == 0) {
                if (view == null) {
                    if (view == null) {
                        view = new TextCheckCell(mContext);
                        view.setBackgroundColor(0xffffffff);
                    }
                }
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                if (i == itemStoreRow) {
                    ((TextCheckCell) view).setTextAndCheck(LocaleController.getString("item_store_title", R.string.item_store_title), preferences.getBoolean("item_store", true), false);
                }
            } else if (type == 1) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                }
                if (i == labDetailRow) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("LabDetail", R.string.LabDetail));
                    view.setBackgroundResource(R.drawable.greydivider);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == itemStoreSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("LabChatsNDisplay", R.string.LabChatsNDisplay));
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == itemStoreRow) {
                return 0;
            } else if (i == labDetailRow) {
                return 1;
            } else if (i == itemStoreSectionRow) {
                return 2;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
