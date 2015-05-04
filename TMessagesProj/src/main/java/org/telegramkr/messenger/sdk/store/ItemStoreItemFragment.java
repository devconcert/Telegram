package org.telegramkr.messenger.sdk.store;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.Gson;

import net.makeday.emoticonsdk.ItemManager;
import net.makeday.emoticonsdk.model.EmoticonModel;
import net.makeday.emoticonsdk.model.MetaModel;
import net.makeday.emoticonsdk.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.Adapters.BaseFragmentAdapter;
import org.telegram.messenger.R;
import me.ttalk.sdk.ServiceAgent;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ItemStoreItemFragment extends ListFragment {
    private static final String ARG_POSITION = "position";
    private int position;
    private int themeRow;
    View mHeaderView;
    private ArrayList<EmoticonModel> mStickerItemList = new ArrayList<>();
    ArrayList<EmoticonModel> mStickerAddLists = new ArrayList<>();

    private StoreItemListAdapter storeItemListAdapter;
    public static ItemStoreItemFragment newInstance(int position) {
        ItemStoreItemFragment f = new ItemStoreItemFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.position = getArguments().getInt(ARG_POSITION);
        this.themeRow = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.store_sticker_layout, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // FIXME: New, Free, Downloaded?
        ServiceAgent.getInstance().logEvent("ItemStore_Sticker", "load", "new");
        setListAdapter(null);
        if (getListView().getHeaderViewsCount() == 0) {
            // TODO: AD, NOTICE
            LayoutInflater inflater = (LayoutInflater)ApplicationLoader.fragmentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mHeaderView = inflater.inflate(R.layout.store_header_layout, null, false);
            getListView().addHeaderView(mHeaderView);
        }
        if (storeItemListAdapter == null) {
            storeItemListAdapter = new StoreItemListAdapter(ApplicationLoader.applicationContext, mStickerItemList);
        }
        fetch();

        setListAdapter(storeItemListAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setListAdapter(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Header View
        position -= l.getHeaderViewsCount();

        if(position >= 0) {
            // List View
            ApplicationLoader.emoticonItemObject = mStickerItemList.get(position);
            ApplicationLoader.baseFragment.presentFragment(new ItemStoreItemDetailFragment());
        }
    }

    private class StoreItemListAdapter extends BaseFragmentAdapter {
        private Context mContext;
        private ArrayList<EmoticonModel> _itemsList;

        public StoreItemListAdapter(Context context, ArrayList<EmoticonModel> itemsList) {
            this.mContext = context;
            this._itemsList = itemsList;
        }

        public View getView(final int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater li = (LayoutInflater)this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = li.inflate(R.layout.store_list_row_layout, viewGroup, false);
            }
            NetworkImageView imageThumbSmall = (NetworkImageView)view.findViewById(R.id.theme_thumb);
            TextView textCompanyName = (TextView) view.findViewById(R.id.emoticon_row_text_companyname);
            TextView textEmoticonName = (TextView) view.findViewById(R.id.emoticon_row_text_title);
            TextView textPrice = (TextView) view.findViewById(R.id.emoticon_row_text_price);
            View divider = view.findViewById(R.id.settings_row_divider);
            imageThumbSmall.setImageUrl(this._itemsList.get(i).getThumb_small_file(), VolleySingleton.getInstance(ApplicationLoader.applicationContext).getImageLoader());

            textCompanyName.setText(this._itemsList.get(i).getCompany_name());
            textEmoticonName.setText(this._itemsList.get(i).getTitle(LocaleController.getCurrentLanguageCode()));
            textPrice.setText(this._itemsList.get(i).getPrice(LocaleController.getCurrentLanguageCode()));
            divider.setVisibility(View.VISIBLE);
            return view;
        }
        @Override
        public int getCount() {
            return this._itemsList.size();
        }

        @Override
        public Object getItem(int arg0) {
            return this._itemsList.get(arg0);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        public void setData(List<EmoticonModel> data) {
            if (_itemsList != null) {
                _itemsList.clear();
            } else {
                _itemsList = new ArrayList<>();
            }
            if (data != null) {
                _itemsList.addAll(data);
            }
            notifyDataSetChanged();
        }
    }

    private void fetch() {
        // Create a new JsonObjectRequest.
        JsonObjectRequest request = new JsonObjectRequest(
                ItemManager.getInstance().getMyEmoticonUrl(),
                null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            mStickerItemList = parse(response);
                            storeItemListAdapter.setData(mStickerItemList);
                            AndroidUtilities.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                // Set Header ad view
                                if(mStickerAddLists.size() > 0) {
                                    final EmoticonModel model = mStickerAddLists.get(0);
                                    NetworkImageView adImage = (NetworkImageView) mHeaderView.findViewById(R.id.emoticon_ad);
                                    adImage.setImageUrl(model.getThumb_ad_file(), VolleySingleton.getInstance(ApplicationLoader.applicationContext).getImageLoader());
                                    mHeaderView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // TODO: Anlaysic
                                            ServiceAgent.getInstance().logEvent("ItemStore_Sticker", "click_ad", String.valueOf(model.getId()));

                                            ApplicationLoader.emoticonItemObject = model;
                                            ApplicationLoader.baseFragment.presentFragment(new ItemStoreItemDetailFragment(), false);
                                        }
                                    });
                                    View divider = mHeaderView.findViewById(R.id.settings_row_divider);
                                    divider.setVisibility(View.VISIBLE);
                                }
                                }
                            });

                            if(BuildVars.DEBUG_VERSION) {
                                // TODO: Remove toast
                                Toast.makeText(ApplicationLoader.applicationContext, "SUCCESS", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(BuildVars.DEBUG_VERSION) {
                            // TODO: Remove toast & ErrorHandlering
                            Toast.makeText(ApplicationLoader.applicationContext, "ERROR", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        VolleySingleton.getInstance(getActivity()).getRequestQueue().add(request);
    }

    private ArrayList<EmoticonModel> parse(JSONObject json) throws JSONException {
        ArrayList<EmoticonModel> records = new ArrayList<>();
        Gson gson = new Gson();
        try {
            JSONObject jsonMeta = json.getJSONObject("meta");
            try {
                String convertString = new String(jsonMeta.toString().getBytes("ISO-8859-1"), "UTF-8");
                MetaModel recipe = gson.fromJson(convertString, MetaModel.class);
            } catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        } catch(Exception e){ }

        // EmoticonObject, EmoticonItemsObject
        JSONArray jsonObjects = json.getJSONArray("objects");
        for(int i =0; i < jsonObjects.length(); i++) {
            JSONObject jsonEmoticon = jsonObjects.getJSONObject(i);
            try {
                String convertString = new String(jsonEmoticon.toString().getBytes("ISO-8859-1"), "UTF-8");
                EmoticonModel sticker = gson.fromJson(convertString, EmoticonModel.class);
                if(sticker.isAd()) {
                    mStickerAddLists.add(sticker);
                }
                records.add(gson.fromJson(convertString, EmoticonModel.class));
            } catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        }

        return records;
    }
}