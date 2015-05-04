package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.PagerSlidingTabStrip;
import org.telegramkr.messenger.sdk.theme.ThemeApplicationFragment;
import org.telegramkr.messenger.sdk.theme.ThemeInstalledApplicationFragment;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;
import org.telegram.messenger.R;
import org.telegramkr.messenger.sdk.ui.CustomViewPager;

public class SettingsThemeFragment extends BaseFragment {

    PagerSlidingTabStrip tabs;
    CustomViewPager pager;
    MyPagerAdapter adapter;
    private boolean mThemeStoreEnable = false;

    public boolean presentFragment(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragment(fragment);
    }

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        swipeBackEnabled = false;

        Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_ab_back");
        if (drawable != null){
            actionBar.setBackButtonDrawable(drawable);
        }else{
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        }
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("themeChooser", R.string.themeChooser));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        // FIXME
        fragmentView = inflater.inflate(R.layout.settings_theme_layout, null, false);
        pager = (CustomViewPager) fragmentView.findViewById(R.id.pager);

        adapter = new MyPagerAdapter(ApplicationLoader.fragmentActivity);
        pager.setAdapter(adapter);

        final int pageMargin = (int)TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 20,
                                    ApplicationLoader.applicationContext.getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        pager.setPagingEnabled(true);

        tabs = (PagerSlidingTabStrip) fragmentView.findViewById(R.id.tabs);
        tabs.setViewPager(pager);
        tabs.setShouldExpand(true);
        tabs.setUnderlineHeight(3);
        tabs.setIndicatorHeight(3);
        tabs.setTextSize(AndroidUtilities.dp(14));
        tabs.setTextColor(0xFF000000);
        tabs.setIndicatorColor(0xFF3F9FE0); // 0xFFF4842D

        Button btn_theme_store = (Button) fragmentView.findViewById(R.id.btn_theme_store);
        btn_theme_store.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceAgent.getInstance().logEvent("Settings.Theme", "Disable");
                if(mThemeStoreEnable == true){
                    if (ThemeManager.getInstance().isExistApp(ThemeManager.TELEGRAM_THEME_STORE_DOODLE)) {
                        ServiceAgent.getInstance().logEvent("Settings.Theme.ThemeStore", "Start");
                        Uri uri = Uri.parse("doodledoodle://theme_messenger?category=telegram");
                        Intent i = new Intent(Intent.ACTION_VIEW, uri);
                        ApplicationLoader.fragmentActivity.startActivity(i);
                    } else {
                        ServiceAgent.getInstance().logEvent("Settings.Theme.ThemeStore", "SearchOnMarket");
                        String alertTitle = LocaleController.getString("theme_store_run", R.string.theme_store_run);
                        String buttonMessage = LocaleController.getString("theme_store_run_text", R.string.theme_store_run_text);
                        ThemeManager.searchOnMarket(ApplicationLoader.fragmentActivity, alertTitle, buttonMessage, "market://details?id=" + ThemeManager.TELEGRAM_THEME_STORE_DOODLE);
                    }
                }else {
                    Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("theme_store_toast", R.string.theme_store_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });
        return fragmentView;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateThemeStorePublish();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter implements CustomViewPager.OnPageChangeListener {
        private final String[] TITLES = {
                LocaleController.getString("theme_telegram_app", R.string.theme_telegram_app),
                LocaleController.getString("theme_installed_app", R.string.theme_installed_app)
        };

        public MyPagerAdapter(FragmentActivity activity) {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // TODO Auto-generated method stub

            FragmentManager manager = ((Fragment) object).getFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.remove((Fragment) object);
            trans.commit();

            super.destroyItem(container, position, object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0) {
                ServiceAgent.getInstance().logEvent("Settings.Theme.ThemeStore", "Application");
                return ThemeApplicationFragment.newInstance(position);
            }else {
                ServiceAgent.getInstance().logEvent("Settings.Theme", "Installed");
                return new ThemeInstalledApplicationFragment().newInstance(position);
            }
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    private void updateThemeStorePublish() {
        Thread background = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    HttpClient Client = new DefaultHttpClient();
                    String URL = "http://devconcert.github.io/TelegramTheme/publish/theme-store-doodle.html";
                    try {
                        String data = "";

                        HttpGet httpget = new HttpGet(URL);
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        data = Client.execute(httpget, responseHandler);
                        if(data.length() > 0) {
                            String[] result = data.trim().split(":");
                            if (result.length > 0) {
                                if (result[1].equals("1")) {
                                    mThemeStoreEnable = true;
                                } else {
                                    mThemeStoreEnable = false;
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
