package org.telegramkr.messenger.sdk.store;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobeta.android.dslv.DragSortController;

import net.makeday.emoticonsdk.fragment.DSLVStickersFragment;

import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.PagerSlidingTabStrip;
import org.telegram.messenger.R;
import me.ttalk.sdk.ServiceAgent;
import org.telegramkr.messenger.sdk.ui.CustomViewPager;

public class ItemStoreSettingStickersFragment extends BaseFragment {

    private final static int emoticon_settings = 0;
    PagerSlidingTabStrip tabs;
    CustomViewPager pager;
    MyPagerAdapter adapter;

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        if (fragmentView == null) {

            ServiceAgent.getInstance().logEvent("ItemStore", "setting", "sticker");
            swipeBackEnabled = false;
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);

//            ActionBarMenu menu = actionBar.createMenu();
//            menu.addItem(emoticon_settings, R.drawable.ic_ab_other);

            actionBar.setAllowOverlayTitle(true);
            actionBar.setTitle(LocaleController.getString("item_store_sticker", R.string.item_store_stickers));

            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
//                    else if (id == emoticon_settings) {
//                        presentFragment(new ItemStoreSettingFragment());
//                    }
                }
            });

            // FIXME:
            fragmentView = inflater.inflate(R.layout.store_main_layout, null, false);
            pager = (CustomViewPager) fragmentView.findViewById(R.id.pager);

            adapter = new MyPagerAdapter(ApplicationLoader.fragmentActivity);
            pager.setAdapter(adapter);
            pager.setPagingEnabled(true);

            tabs = (PagerSlidingTabStrip) fragmentView.findViewById(R.id.tabs);
            tabs.setViewPager(pager);
            tabs.setShouldExpand(true);
            tabs.setUnderlineHeight(3);
            tabs.setIndicatorHeight(3);

            // FIXME
//            tabs.setTextSize(AndroidUtilities.dp(14));
//            tabs.setTextColor(0xFF000000);
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
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    private int mNumHeaders = 0;
    private int mNumFooters = 0;

    private int mDragStartMode = DragSortController.ON_DRAG;
    private boolean mRemoveEnabled = false;
    private int mRemoveMode = DragSortController.FLING_REMOVE;
    private boolean mSortEnabled = true;
    private boolean mDragEnabled = true;

    private class MyPagerAdapter extends FragmentStatePagerAdapter implements CustomViewPager.OnPageChangeListener {
        private final String[] TITLES = { "Change Items order" };
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
            // TODO: Add Flurry
            return getNewDslvFragment();
        }

        private Fragment getNewDslvFragment() {
            DSLVStickersFragment f = DSLVStickersFragment.newInstance(mNumHeaders, mNumFooters);
            f.removeMode = mRemoveMode;
            f.removeEnabled = mRemoveEnabled;
            f.dragStartMode = mDragStartMode;
            f.sortEnabled = mSortEnabled;
            f.dragEnabled = mDragEnabled;
            return f;
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }
}
