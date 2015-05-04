package org.telegramkr.messenger.sdk.theme;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.android.LocaleController;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.MainActivity;
import org.telegram.messenger.R;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeAppInfo;
import me.ttalk.sdk.theme.ThemeManager;

import java.util.ArrayList;

public class ThemeInstalledApplicationFragment extends DialogFragment {

    private static final String ARG_POSITION = "position";
    private int position;
    private static final int REQUEST_UNINSTALL = 1;
    private static String mThemeUnstallPackageName = "";
    private static String mThemeTitle = "";
    private ArrayList<ThemeAppInfo> mItems = new ArrayList<ThemeAppInfo>();
    private View view = null;
    private ListView listView = null;
    private ListAdapter listAdapter = null;

    public static ThemeInstalledApplicationFragment newInstance(int position) {
        ThemeInstalledApplicationFragment f = new ThemeInstalledApplicationFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.settings_theme_main_layout, container, false);
        listView = (ListView)view.findViewById(R.id.listView);
        updateThemeApplication();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();

            ApplicationLoader.fragmentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(ThemeManager.getInstance().isEnableTheme(mThemeUnstallPackageName)){
                        Toast.makeText(ApplicationLoader.fragmentActivity, mThemeTitle + LocaleController.getString("theme_uninstall", R.string.theme_uninstall), Toast.LENGTH_SHORT).show();
                        mThemeUnstallPackageName = "";
                        ThemeManager.getInstance().setThemePackageName("");
                        ThemeManager.getInstance().restartApp(MainActivity.class);
                    }
                }
            });

            updateThemeApplication();
        }
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;
        private ArrayList<ThemeAppInfo> themeItems = new ArrayList<ThemeAppInfo>();

        public ListAdapter(Context context, ArrayList<ThemeAppInfo> items) {
            mContext = context;
            themeItems = items;
        }

        @Override
        public int getCount() {
            return themeItems.size();
        }

        @Override
        public Object getItem(int i) {
            return themeItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        public void update() {
            themeItems.clear();
            notifyDataSetChanged();
        }
        @Override
        public boolean isEmpty() {
            return false;
        }

        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            final ThemeListViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater li = (LayoutInflater) ApplicationLoader.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(R.layout.settings_theme_row_button_layout, null);
                viewHolder = new ThemeListViewHolder(convertView);
            } else {
                viewHolder = (ThemeListViewHolder) convertView.getTag();
            }
            convertView.setTag(viewHolder);

            final ThemeAppInfo item = themeItems.get(i);
            final String themePackageName = item.packageName;

            Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable(themePackageName, "background_intro");
            viewHolder.themePackageName = themePackageName;
            viewHolder.themeThumb.setImageDrawable(drawable);
            viewHolder.themeTextTitle.setText(item.themeName);
            viewHolder.themeTextSubTitle.setText(LocaleController.getString("theme_unlimit", R.string.theme_unlimit));
            viewHolder.themeApply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewHolder.themeApply.isEnabled()) {

                        ServiceAgent.getInstance().logEvent("Theme", "Apply", themePackageName);
                        String alertTitle = item.themeName;
                        String buttonMessage = LocaleController.getString("theme_apply_text", R.string.theme_apply_text);
                        ThemeManager.applyThemePackage(ApplicationLoader.fragmentActivity, MainActivity.class, alertTitle, buttonMessage, themePackageName);
                    }
                }
            });

            viewHolder.themeDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewHolder.themeDelete.isEnabled()) {
                        ServiceAgent.getInstance().logEvent("Theme", "Delete", themePackageName);
                        mThemeUnstallPackageName = themePackageName;
                        mThemeTitle = item.themeName;
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse("package:" + themePackageName));
                        startActivity(intent);
                    }
                }
            });

            if (ThemeManager.getInstance().getThemePackageName().equals(viewHolder.themePackageName)) {
                viewHolder.themeApply.setText(LocaleController.getString("theme_applyed", R.string.theme_applyed));
                viewHolder.themeApply.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.black));
                viewHolder.themeApply.setBackgroundResource(R.drawable.theme_button_applyed_background);
                viewHolder.themeApply.setEnabled(false);
            }

            return convertView;
        }
    }

    class ThemeListViewHolder {
        TextView themeTextTitle;
        TextView themeTextSubTitle;
        ImageView themeThumb;
        Button themeApply;
        Button themeDelete;
        String themePackageName;
        public ThemeListViewHolder(View base) {
            themeTextTitle = (TextView) base.findViewById(R.id.settings_row_text_title);
            themeTextSubTitle = (TextView) base.findViewById(R.id.settings_row_text_subtitle);
            themeThumb = (ImageView) base.findViewById(R.id.theme_thumb);
            themeApply = (Button) base.findViewById(R.id.btn_theme_apply);
            themeDelete = (Button) base.findViewById(R.id.btn_theme_delete);
        }
    }

    //@@ Custom Theme
    private void setRemoteTheme(String text){
        LinearLayout relativeLayout = (LinearLayout)view.findViewById(R.id.setting_theme_list);
        Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable(text);
        if (drawable != null){
            if (Build.VERSION.SDK_INT >= 16) {
                relativeLayout.setBackground(drawable);
            } else {
                relativeLayout.setBackgroundDrawable(drawable);
            }
        }
    }

    // For theme application
    private Thread mThread;
    private static final int SEND_THREAD_THEME_APPLICATION = 0;
    private static final int SEND_THREAD_THEME_STOP_MESSAGE = 1;

    // Theme Layout update
    private void updateThemeApplication() {
        Handler mHandler = new Handler(){
            public void handleMessage(Message msg) {
                if(msg.what == SEND_THREAD_THEME_APPLICATION){
                    mItems = (ArrayList<ThemeAppInfo>)msg.obj;

                    listAdapter = new ListAdapter(ApplicationLoader.applicationContext, mItems);
                    listView = (ListView)view.findViewById(R.id.listView);
                    listView.setAdapter(listAdapter);
                    listView.setVisibility(View.VISIBLE);

                    listAdapter.notifyDataSetChanged();
                }
            };
        };

        mThread = new BackThread(mHandler);
        mThread.setDaemon(true);
        mThread.start();
    }

    class BackThread extends Thread {
        Handler mHandler;
        public BackThread(Handler handler) {
            mHandler=handler;
        }

        @Override
        public void run() {
            ArrayList<ThemeAppInfo> themeAppList = ThemeManager.getInstance().getInstalledApps();
            if(themeAppList.size() != 0) {
                try {
                    Message msg = Message.obtain();
                    msg.what = SEND_THREAD_THEME_APPLICATION;
                    msg.obj = themeAppList;
                    mHandler.sendMessage(msg);
                }catch(Exception e){

                }
            }else{
                ApplicationLoader.fragmentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView = (ListView)view.findViewById(R.id.listView);
                        listView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }
}
