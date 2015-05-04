package org.telegramkr.messenger.sdk.store;

import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.messenger.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;

public class ItemStoreSettingFragment extends BaseFragment {

    private ListAdapter listAdapter;

    private int stickerSectionRow;
    private int myStickerRow;
    private int purchaseRow;
    private int giftBoxRow;
    private int transactionsSectionRow;
    private int shopHelpSectionRow;
    private int shopHelpDetailRow;
    private int itemShopHelpRow;
    private int emoticonDividerRow;
    private int emoticonBuyDividerRow;
    private int rowCount;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        rowCount = 0;
        stickerSectionRow = rowCount++;
        myStickerRow = rowCount++;
        emoticonBuyDividerRow = rowCount++;
        transactionsSectionRow = rowCount++;
        purchaseRow = rowCount++;
        giftBoxRow = rowCount++;
        emoticonDividerRow = rowCount++;
        shopHelpSectionRow = rowCount++;
        itemShopHelpRow = rowCount++;
        shopHelpDetailRow = rowCount++;
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        if (fragmentView == null) {
            ServiceAgent.getInstance().logEvent("ItemStore", "setting", "main");

            // TODO 아이콘 변경???? X 로
            Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_ab_back");
            if (drawable != null){
                actionBar.setBackButtonDrawable(drawable);
            }else{
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            }

            actionBar.setAllowOverlayTitle(true);
            actionBar.setTitle(LocaleController.getString("item_store_setting", R.string.item_store_setting));

            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    }
                }
            });

            listAdapter = new ListAdapter(getParentActivity());

            fragmentView = new FrameLayout(getParentActivity());
            FrameLayout frameLayout = (FrameLayout) fragmentView;
            frameLayout.setBackgroundColor(0xfff0f0f0);

            ListView listView = new ListView(getParentActivity());
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setVerticalScrollBarEnabled(false);
            listView.setDrawSelectorOnTop(true);
            frameLayout.addView(listView);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
            layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT;
            layoutParams.gravity = Gravity.TOP;
            listView.setLayoutParams(layoutParams);
            listView.setAdapter(listAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    if (i == myStickerRow) {
                        // TODO:
                        presentFragment(new ItemStoreSettingStickersFragment());
                    } else if (i == purchaseRow) {
                        // TODO:
                    } else if (i == itemShopHelpRow) {
                        // TODO:
                    } else if (i == itemShopHelpRow) {
                        // TODO:
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
            return i == myStickerRow || i == purchaseRow || i == giftBoxRow || i == itemShopHelpRow;
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
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                TextSettingsCell textCell = (TextSettingsCell) view;
                if (i == myStickerRow) {
                    textCell.setText(LocaleController.getString("item_store_sticker", R.string.item_store_stickers), true);
                } else if (i == purchaseRow) {
                    textCell.setText(LocaleController.getString("item_store_purchase_list", R.string.item_store_purchase_list), true);
                } else if (i == giftBoxRow) {
                    textCell.setText(LocaleController.getString("item_store_gift_list", R.string.item_store_gift_list), true);
                } else if (i == itemShopHelpRow){
                    textCell.setText(LocaleController.getString("item_store_help", R.string.item_store_help), true);
                }
            } else if (type == 1) {
                if (view == null) {
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }
                if (i == shopHelpDetailRow) {
                    ((TextInfoPrivacyCell) view).setText(LocaleController.getString("item_store_help_detail", R.string.item_store_help_detail));
                    view.setBackgroundResource(R.drawable.greydivider);
                }
            } else if (type == 2) {
                if (view == null) {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(0xffffffff);
                }

                if (i == stickerSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("item_store_head_items", R.string.item_store_head_items));
                } else if (i == transactionsSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("item_store_head_transactions", R.string.item_store_head_transactions));
                } else if (i == shopHelpSectionRow) {
                    ((HeaderCell) view).setText(LocaleController.getString("item_store_head_support", R.string.item_store_head_support));
                }
            } else if (type == 3) {
                if (view == null) {
                    view = new ShadowSectionCell(mContext);
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == myStickerRow || i == purchaseRow || i == giftBoxRow || i == itemShopHelpRow) {
                return 0;
            } else if (i == shopHelpDetailRow) {
                return 1;
            } else if (i == stickerSectionRow || i == shopHelpSectionRow || i == transactionsSectionRow) {
                return 2;
            } else if (i == emoticonDividerRow || i == emoticonBuyDividerRow){
                return 3;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
