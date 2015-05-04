package org.telegramkr.messenger.sdk.store;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.PagerSlidingTabStrip;
import me.ttalk.sdk.theme.ThemeManager;
import org.telegram.messenger.R;
import org.telegramkr.messenger.sdk.ui.CustomViewPager;

public class ItemStoreMainFragment extends BaseFragment {
    private final static int emoticon_settings = 0;
    private PagerSlidingTabStrip tabs;
    private CustomViewPager pager;
    private MyPagerAdapter adapter;

    public boolean presentFragment(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragment(fragment);
    }

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        if (fragmentView == null) {
            swipeBackEnabled = false;
            Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_ab_back");
            if (drawable != null) {
                actionBar.setBackButtonDrawable(drawable);
            } else {
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            }
// FIXME:
//            ActionBarMenu menu = actionBar.createMenu();
//            Drawable otherDrawable = ThemeManager.getRemoteResourceDrawable("ic_ab_other");
//            if (otherDrawable != null) {
//                menu.addItem(emoticon_settings, otherDrawable);
//            } else {
//                menu.addItem(emoticon_settings, R.drawable.ic_ab_other);
//            }

            actionBar.setAllowOverlayTitle(true);
            actionBar.setTitle(LocaleController.getString("item_store_title", R.string.item_store_title));
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == emoticon_settings) {
                        presentFragment(new ItemStoreSettingFragment());
                    }
                }
            });

            // FIXME
            fragmentView = inflater.inflate(R.layout.store_main_layout, null);
            pager = (CustomViewPager) fragmentView.findViewById(R.id.pager);

            adapter = new MyPagerAdapter(ApplicationLoader.fragmentActivity);
            pager.setAdapter(adapter);
            pager.setPagingEnabled(true);

            tabs = (PagerSlidingTabStrip) fragmentView.findViewById(R.id.tabs);
            tabs.setViewPager(pager);
            tabs.setShouldExpand(true);
            tabs.setUnderlineHeight(3);
            tabs.setIndicatorHeight(3);

            tabs.setTextSize(AndroidUtilities.dp(14));
            tabs.setTextColor(0xFF000000);
            tabs.setIndicatorColor(0xFF3F9FE0); // 0xFFF4842D
        } else {
            ViewGroup parent = (ViewGroup)fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        ApplicationLoader.baseFragment = this;
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter implements CustomViewPager.OnPageChangeListener {
        private final String[] TITLES = {
                LocaleController.getString("item_store_tab_new", R.string.item_store_tab_new),
//                LocaleController.getString("item_store_tab_top", R.string.item_store_tab_top),
//                LocaleController.getString("item_store_tab_free", R.string.item_store_tab_free)
        };
        public MyPagerAdapter(FragmentActivity activity) {
            super(activity.getSupportFragmentManager());
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
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
            // TODO: Add ServiceAgent
            if (position == 0) {
                return new ItemStoreItemFragment().newInstance(position);
            } else if (position == 1) {
                return new ItemStoreItemFragment().newInstance(position);
            } else
                return new ItemStoreItemFragment().newInstance(position);
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }
}
