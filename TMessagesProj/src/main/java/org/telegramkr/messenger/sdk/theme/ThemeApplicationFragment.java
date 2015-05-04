package org.telegramkr.messenger.sdk.theme;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.telegram.android.LocaleController;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.MainActivity;
import org.telegram.messenger.R;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;

public class ThemeApplicationFragment extends DialogFragment {

    private static final String ARG_POSITION = "position";
    private int position;
    View view;
    private ListView listView;
    private ListAdapter listAdapter;
    private boolean mBlackThemeEnable = false;

    private int themeRow;
    private int themeDefaultRow;
    private int themeBlackRow;

    public static ThemeApplicationFragment newInstance(int position) {
        ThemeApplicationFragment f = new ThemeApplicationFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        position = getArguments().getInt(ARG_POSITION);

        themeRow = 0;
        themeDefaultRow = themeRow++;
        themeBlackRow = themeRow++;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mBlackThemeEnable == false){
            updateThemePublish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.settings_theme_main_layout, container, false);
        //setRemoteTheme("background_main");
        listAdapter = new ListAdapter(ApplicationLoader.applicationContext);
        listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setVisibility(View.VISIBLE);
/*
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == themeColorRow) {
                    themeFragment.presentFragment(new SettingsThemeChooserActivity());
                }
            }
        });
*/
        updateThemePublish();
        return view;
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        public View getView(final int i, View view, ViewGroup viewGroup) {

            int type = getItemViewType(i);
            if (type == 0) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) ApplicationLoader.applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.settings_theme_row_button_layout, viewGroup, false);
                    TextView themeTextTitle = (TextView) view.findViewById(R.id.settings_row_text_title);
                    TextView themeTextSubTitle = (TextView) view.findViewById(R.id.settings_row_text_subtitle);
                    ImageView themeThumb = (ImageView) view.findViewById(R.id.theme_thumb);
                    final Button themeDelete = (Button) view.findViewById(R.id.btn_theme_delete);
                    themeDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (i == themeDefaultRow) {
                            } else if (i == themeBlackRow) {
                                ServiceAgent.getInstance().logEvent("Theme", "Delete", "BlackTheme");
                                Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                                intent.setData(Uri.parse("package:" + ThemeManager.TELEGRAM_THEME_BLACK));
                                startActivity(intent);
                            }
                        }
                    });
                    final Button themeApply = (Button) view.findViewById(R.id.btn_theme_apply);
                    themeApply.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (i == themeDefaultRow) {
                                ServiceAgent.getInstance().logEvent("Theme", "Apply", "DefaultTheme");
                                if (themeApply.isEnabled()) {
                                    String alertTitle = LocaleController.getString("theme_basic", R.string.theme_basic);
                                    String buttonMessage = LocaleController.getString("theme_apply_text", R.string.theme_apply_text);
                                    String themePackageName = "";
                                    ThemeManager.applyThemePackage(ApplicationLoader.fragmentActivity, MainActivity.class, alertTitle, buttonMessage, themePackageName);
                                }
                            } else if (i == themeBlackRow) {
                                if (themeApply.isEnabled()) {
                                    if (ThemeManager.getInstance().isExistApp(ThemeManager.TELEGRAM_THEME_BLACK)) {
                                        ServiceAgent.getInstance().logEvent("Theme", "Apply", "BlackTheme");
                                        String alertTitle = LocaleController.getString("theme_black", R.string.theme_black);
                                        String buttonMessage = LocaleController.getString("theme_apply_text", R.string.theme_apply_text);
                                        ThemeManager.applyThemePackage(ApplicationLoader.fragmentActivity, MainActivity.class, alertTitle, buttonMessage, ThemeManager.TELEGRAM_THEME_BLACK);
                                    } else {
                                        if(mBlackThemeEnable == true){
                                            ServiceAgent.getInstance().logEvent("Theme", "Search", "BlackTheme");
                                            String alertTitle = LocaleController.getString("theme_black", R.string.theme_black);
                                            String buttonMessage = LocaleController.getString("theme_play_text", R.string.theme_play_text);
                                            ThemeManager.searchOnMarket(ApplicationLoader.fragmentActivity, alertTitle, buttonMessage, ThemeManager.TELEGRAM_THEME_BLACK);
                                        }else {
                                            Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("theme_store_toast", R.string.theme_store_toast), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        }
                    });
                    View divider = view.findViewById(R.id.settings_row_divider);
                    if (i == themeDefaultRow) {
                        themeThumb.setImageResource(R.drawable.background_intro);
                        themeTextTitle.setText(R.string.theme_default);
                        themeTextSubTitle.setText(LocaleController.getString("theme_unlimit", R.string.theme_unlimit));
                        divider.setVisibility(View.VISIBLE);
                        if (ThemeManager.getInstance().getThemePackageName().equals("")) {
                            themeApply.setText(LocaleController.getString("theme_applyed", R.string.theme_applyed));
                            themeApply.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.black));
                            themeApply.setBackgroundResource(R.drawable.theme_button_applyed_background);
                            themeApply.setEnabled(false);
                        }

                        themeDelete.setVisibility(View.GONE);
                    } else if (i == themeBlackRow) {
                        themeThumb.setImageResource(R.drawable.theme_black_thumb);
                        themeTextTitle.setText(R.string.theme_black);
                        themeTextSubTitle.setText(LocaleController.getString("theme_unlimit", R.string.theme_unlimit));
                        divider.setVisibility(View.VISIBLE);

                        if (ThemeManager.getInstance().isExistApp(ThemeManager.TELEGRAM_THEME_BLACK)) {
                            if (ThemeManager.getInstance().getThemePackageName().equals(ThemeManager.TELEGRAM_THEME_BLACK)) {
                                themeApply.setText(LocaleController.getString("theme_applyed", R.string.theme_applyed));
                                themeApply.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.black));
                                themeApply.setBackgroundResource(R.drawable.theme_button_applyed_background);
                                themeApply.setEnabled(false);
                            }
                        }else {
                            themeApply.setText(LocaleController.getString("theme_download", R.string.theme_download));
                            themeApply.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.white));
                            themeApply.setBackgroundResource(R.drawable.theme_button_background);
                            themeDelete.setVisibility(View.GONE);
                        }
                    }
                    return view;
                }
            }

            return view;
        }
        @Override
        public int getCount() {
            return themeRow;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == themeDefaultRow || i == themeBlackRow) {
                return 0;
            } else{
                return 1;
            }
        }
        @Override
        public boolean isEmpty() {
            return false;
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

    private void updateThemePublish() {
        Thread background = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpClient Client = new DefaultHttpClient();
                    String URL = "http://devconcert.github.io/TelegramTheme/publish/black-theme.html";
                    try {
                        String data = "";

                        HttpGet httpget = new HttpGet(URL);
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        data = Client.execute(httpget, responseHandler);
                        if(data.length() > 0) {
                            String[] result = data.trim().split(":");
                            if (result.length > 0) {
                                if (result[1].equals("1")) {
                                    mBlackThemeEnable = true;
                                } else {
                                    mBlackThemeEnable = false;
                                }
                            }
                        }
                    }
                    catch(Exception ex) {
                    }
                }catch(Exception ex){
                }
            }
        });
        background.start();
    }
}
