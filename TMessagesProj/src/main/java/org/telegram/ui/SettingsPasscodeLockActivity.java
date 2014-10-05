/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 * Copyright DEVCONCERT, 2014.
 */

package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.android.LocaleController;
import org.telegram.android.NotificationCenter;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Views.ActionBar.ActionBarLayer;
import org.telegram.ui.Views.ActionBar.BaseFragment;
import org.telegramkr.messenger.R;
import org.telegramkr.passcodelock.core.PasscodeLock;
import org.telegramkr.passcodelock.core.PasscodeLockActivity;
import org.telegramkr.passcodelock.core.LockManager;

public class SettingsPasscodeLockActivity extends BaseFragment{
    private ListView listView;
    private TextView changePasswordTextView;

    private int passcodeLockServiceRow;
    private int passcodeLockChangePasswordRow;
    private int rowCount = 0;

    @Override
    public boolean onFragmentCreate() {
        passcodeLockServiceRow = rowCount++;
        passcodeLockChangePasswordRow = rowCount++;
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container) {
        if (fragmentView == null) {
            actionBarLayer.setDisplayHomeAsUpEnabled(true, R.drawable.ic_ab_back);
            actionBarLayer.setBackOverlay(R.layout.updating_state_layout);
            actionBarLayer.setTitle(LocaleController.getString("PasscodeLock", R.string.PasscodeLock));
            actionBarLayer.setActionBarMenuOnItemClick(new ActionBarLayer.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            fragmentView = inflater.inflate(R.layout.settings_layout, container, false);
            final ListAdapter listAdapter = new ListAdapter(getParentActivity());
            listView = (ListView)fragmentView.findViewById(R.id.listView);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == passcodeLockServiceRow) {
                        try {
                            ApplicationLoader.isPasscodeLockOn = true;
                            int type = LockManager.getInstance().getAppLock().isPasscodeSet() ? PasscodeLock.DISABLE_PASSLOCK
                                    : PasscodeLock.ENABLE_PASSLOCK;
                            Intent intent = new Intent(getParentActivity(), PasscodeLockActivity.class);
                            intent.putExtra(PasscodeLock.TYPE, type);
                            startActivityForResult(intent, type);
                        } catch(Exception e) {
                            finishActivity();
                        }
                    } else if(i == passcodeLockChangePasswordRow){
                        if(LockManager.getInstance().getAppLock().isPasscodeSet()) {
                            try {
                                Intent intent = new Intent(getParentActivity(), PasscodeLockActivity.class);
                                intent.putExtra(PasscodeLock.TYPE, PasscodeLock.CHANGE_PASSWORD);
                                intent.putExtra(PasscodeLock.MESSAGE, LocaleController.getString("PasscodeLock_enter_old", R.string.PasscodeLock_enter_old));
                                startActivityForResult(intent, PasscodeLock.CHANGE_PASSWORD);
                            }catch(Exception e) {
                                finishActivity();
                            }
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

    public void finishActivity() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        getParentActivity().startActivity(intent);
        getParentActivity().finish();
    }
    public void updateUI() {
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("PasscodeLock", Activity.MODE_PRIVATE);
        if (LockManager.getInstance().getAppLock().isPasscodeSet()) {
            // OFF -> ON
            if(changePasswordTextView != null){
                changePasswordTextView.setTextColor(Color.BLACK);
            }

            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("PasscodeLock", true);
            editor.commit();

        } else {
            if (getParentActivity() == null) {
                return;
            }
            // OFF -> ON
            if(changePasswordTextView != null){
                changePasswordTextView.setTextColor(Color.GRAY);
            }

            // ON -> OFF
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("PasscodeLock", false);
            editor.commit();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PasscodeLock.DISABLE_PASSLOCK:
                if (resultCode == Activity.RESULT_OK) {
                    ApplicationLoader.isPasscodeLockOn = false;
                    ApplicationLoader.isPasscodeChange = false;
                    Toast.makeText(getParentActivity(), LocaleController.getString("PasscodeLock_disable", R.string.PasscodeLock_disable),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case PasscodeLock.ENABLE_PASSLOCK:
            case PasscodeLock.CHANGE_PASSWORD:
                if (resultCode == Activity.RESULT_OK) {
                    ApplicationLoader.isPasscodeChange = true;
                    Toast.makeText(getParentActivity(), LocaleController.getString("PasscodeLock_setup", R.string.PasscodeLock_setup),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
        updateUI();
        listView.invalidateViews();
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
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_section_layout, viewGroup, false);
                }
            } else if (type == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_row_check_notify_layout, viewGroup, false);
                }
                TextView textView = (TextView)view.findViewById(R.id.settings_row_text);
                View divider = view.findViewById(R.id.settings_row_divider);

                ImageView checkButton = (ImageView)view.findViewById(R.id.settings_row_check_button);
                boolean enabled = false;

                final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("PasscodeLock", Activity.MODE_PRIVATE);

                // 암호설정된 녀석을 가지고 확인
                if (i == passcodeLockServiceRow) {
                    if (LockManager.getInstance().getAppLock().isPasscodeSet()) {
                        enabled = preferences.getBoolean("PasscodeLock", true);
                    } else {
                        enabled = preferences.getBoolean("PasscodeLock", false);
                    }
                    textView.setText(LocaleController.getString("PasscodeLock_manage", R.string.PasscodeLock_manage));
                    divider.setVisibility(View.INVISIBLE);
                }

                if (enabled) {
                    checkButton.setImageResource(R.drawable.btn_check_on);
                } else {
                    checkButton.setImageResource(R.drawable.btn_check_off);
                }
            } else if (type == 2) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_row_button_layout, viewGroup, false);
                }

                changePasswordTextView = (TextView)view.findViewById(R.id.settings_row_text);
                if (i == passcodeLockChangePasswordRow) {
                    changePasswordTextView.setText(LocaleController.getString("PasscodeLock_change", R.string.PasscodeLock_change));
                    if(LockManager.getInstance().getAppLock().isPasscodeSet()) {
                        changePasswordTextView.setTextColor(Color.BLACK);
                        ApplicationLoader.isPasscodeLockOn = false;
                    } else {
                        changePasswordTextView.setTextColor(Color.GRAY);
                    }
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == passcodeLockServiceRow ) {
                return 1;
            } else if(i == passcodeLockChangePasswordRow){
                return 2;
            } else {
                return 3;
            }
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
