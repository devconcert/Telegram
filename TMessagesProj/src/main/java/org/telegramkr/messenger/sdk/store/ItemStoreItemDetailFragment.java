package org.telegramkr.messenger.sdk.store;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.Gson;

import net.makeday.emoticonsdk.ItemManager;
import net.makeday.emoticonsdk.EmoticonTabOrder;
import net.makeday.emoticonsdk.VolleySingleton;
import net.makeday.emoticonsdk.core.ZipDecompress;
import net.makeday.emoticonsdk.fragment.ProgressDialogFragment;
import net.makeday.emoticonsdk.model.EmoticonItemModel;
import net.makeday.emoticonsdk.model.EmoticonModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;
import org.telegram.messenger.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.os.Build.*;

public class ItemStoreItemDetailFragment extends BaseFragment {
    private EmoticonItemModel mItemModel = null;
    private Button btnPresentRecomm;
    private Button btnDownload;
    public boolean presentFragment(BaseFragment fragment) {
        return parentLayout != null && parentLayout.presentFragment(fragment);
    }

    @Override
    public View createView(Context context, LayoutInflater inflater) {
        swipeBackEnabled = false;

        Drawable drawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_ab_back");
        if (drawable != null) {
            actionBar.setBackButtonDrawable(drawable);
        } else {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        }
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("item_store_sticker_info", R.string.item_store_sticker_info));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
// FIXME
        fragmentView = inflater.inflate(R.layout.store_list_detail_layout, null, false);
        final EmoticonModel obj = ApplicationLoader.emoticonItemObject;

//        if(obj.EVENT == EmoticonItemObject.ITEM_EVENT.APPLICATION_NEW){
//            LinearLayout installInfo = (LinearLayout)fragmentView.findViewById(R.id.emoticon_install_info);
//            installInfo.setVisibility(View.VISIBLE);
//            TextView emoticon_text_need_app_install = (TextView)fragmentView.findViewById(R.id.emoticon_text_need_app_install);
//            emoticon_text_need_app_install.setText(obj.event_text);
//            Button btn_need_app_install = (Button)fragmentView.findViewById(R.id.btn_need_app_install);
//
//            // TODO [설치하기] 버튼 Disable
//            if (ThemeManager.isExistApp(obj.event_url)) {
//                btn_need_app_install.setVisibility(View.GONE);
//            }else {
//                btn_need_app_install.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Intent intent = new Intent(Intent.ACTION_VIEW, obj.getEventAppPackageNameToURI());
//                        ApplicationLoader.applicationContext.startActivity(intent);
//                    }
//                });
//            }
//        }
        // TODO: Emoticon Thumb
        NetworkImageView emoticonThumb = (NetworkImageView)fragmentView.findViewById(R.id.emoticon_thumb);
        emoticonThumb.setImageUrl(obj.getThumb_small_file(), VolleySingleton.getInstance(ApplicationLoader.applicationContext).getImageLoader());

        TextView textCompanyName = (TextView)fragmentView.findViewById(R.id.emoticon_text_companyname);
        textCompanyName.setText(obj.getCompany_name());
        TextView textEmoticonTitle = (TextView)fragmentView.findViewById(R.id.emoticon_text_title);
        // TODO: ko or en
        textEmoticonTitle.setText(obj.getTitle(LocaleController.getCurrentLanguageCode()));
        TextView textDuration = (TextView)fragmentView.findViewById(R.id.emoticon_text_duration);
        textDuration.setText(obj.getDuration(LocaleController.getCurrentLanguageCode()));
        TextView textPrice = (TextView)fragmentView.findViewById(R.id.emoticon_text_price);
        textPrice.setText(obj.getPrice(LocaleController.getCurrentLanguageCode()));

        // TODO: [PRESENT]
        btnPresentRecomm = (Button)fragmentView.findViewById(R.id.btn_theme_present_recomm);
        btnPresentRecomm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildVars.DEBUG_VERSION) {
                    // TODO: PRESENT 로직?
                    // TODO: SERVER, Send Present

                    ServiceAgent.getInstance().logEvent("ItemStore_Sticker", "present", String.valueOf(obj.getId()));
                    Toast.makeText(ApplicationLoader.applicationContext, "PRESENT", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnPresentRecomm.setVisibility(Button.GONE);

        // TODO: [DOWNLOAD]
        btnDownload = (Button)fragmentView.findViewById(R.id.btn_theme_buy_and_down);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServiceAgent.getInstance().logEvent("ItemStore_Sticker", "download", String.valueOf(obj.getId()));

                // TODO: Add Tracking
                // TODO: 저장된 이모티콘 PREFERENCE에도 저장, 저장형식은 어떻게 하지?? 따로 SQLITE를 만들어야할까~????
                // TODO: AsyncTask를 이용해서 거기에서 데이터를 받고??
                if (mItemModel != null) {
                    downloadTask();
                } else {
                    fetch(ItemManager.getInstance().getMyItemsUrl(String.valueOf(obj.getId())), String.valueOf(obj.getId()));
                }

                ////////////////////////////////////
                // TODO: Directory의 이름은 렌덤하게
                // TODO: 이모티콘 파일들의 이름도 랜덤하게
                // TODO: 확장자 제거.

                ////////////////////////////////////
                // TODO: Download이 완료되면,
                // TODO: 이모티콘 순서 PREFERENCE의 마지막에 추가를 한다.?
                // TODO: PREFERENCE의 처음에 추가를 한다.
                // TODO: 나의 이모티콘에서 삭제/순서변경 가능하게 수정.
            }
        });

        if(EmoticonTabOrder.getInstance(ApplicationLoader.fragmentActivity).isDownloaded(String.valueOf(obj.getId()))) {
            disableDownloadButton();
        }
        TextView textDescription = (TextView)fragmentView.findViewById(R.id.emoticon_text_info);
        textDescription.setTextSize(AndroidUtilities.dp(5.0f));
        textDescription.setText(obj.getDescription(LocaleController.getCurrentLanguageCode()));

        NetworkImageView thumbBig = (NetworkImageView)fragmentView.findViewById(R.id.emoticon_thumb_big);
        thumbBig.setImageUrl(obj.getThumb_big_file(), VolleySingleton.getInstance(ApplicationLoader.applicationContext).getImageLoader());

        TextView textCopyright = (TextView)fragmentView.findViewById(R.id.emoticon_text_copyright);
        textCopyright.setText(obj.getCopyright());

        ServiceAgent.getInstance().logEvent("ItemStore", "load_detail", String.valueOf(obj.getId()));
        return fragmentView;
    }

    private void disableDownloadButton() {
        btnPresentRecomm.setVisibility(Button.GONE);
        btnDownload.setClickable(false);
        btnDownload.setBackgroundResource(R.drawable.theme_button_pressed);
    }

    public void downloadTask() {
        NoCancelTask task = new NoCancelTask();
        task.setActivity(ApplicationLoader.fragmentActivity);
        if (VERSION.SDK_INT>= VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mItemModel);
        } else {
            task.execute(mItemModel);
        }
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

    private void fetch(String url, final String parent_id) {
        JsonObjectRequest request = new JsonObjectRequest(
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Pasrsing
                            mItemModel = parse(response, parent_id);

                            // Download Task.
                            downloadTask();
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

        VolleySingleton.getInstance(ApplicationLoader.applicationContext).getRequestQueue().add(request);
    }

    private EmoticonItemModel parse(JSONObject json, String parent_id) throws JSONException {
        EmoticonItemModel itemModel = null;
        Gson gson = new Gson();
        try {
            try {
                String convertString = new String(json.toString().getBytes("ISO-8859-1"), "UTF-8");
                itemModel = gson.fromJson(convertString, EmoticonItemModel.class);
                itemModel.setParentId(parent_id);
            } catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        }catch(Exception e){ }

        return itemModel;
    }

    private class NoCancelTask extends AsyncTask<EmoticonItemModel, String, String> {
        private Activity activity;
        private ProgressDialogFragment progressFragment;
        public void setActivity(Activity activity){
            this.activity = activity;
        }

        @Override
        protected void onPreExecute(){
            // TODO: replace strings
            progressFragment = new ProgressDialogFragment.ProgressDialogFragmentBuilder(ApplicationLoader.fragmentActivity)
                    .setMessage("Loading Data ...")
                    .setCancelable(false).show();

//            c = ProgressDialogFragment2.newInstance("Title", "Loading Data ...");
//            progressFragment.show(ApplicationLoader.fragmentActivity.getSupportFragmentManager(), "progress");
        }

        @Override
        protected String doInBackground(EmoticonItemModel... itemModel) {
            try {
                EmoticonItemModel item = itemModel[0];
                File emoticonDir = new File(activity.getExternalFilesDir(null), ItemManager.ITEM_STORE_ROOT_DIRECTORY);
                // TODO: test
//            EmoticonTabOrder.getInstance(this.activity).resetTab();
//            emoticonDir.delete();
                if(!emoticonDir.exists())
                    emoticonDir.mkdir();

                File zipDir = new File(emoticonDir, "zip");
                if(!zipDir.exists())
                    zipDir.mkdir();

                URL url = new URL(item.getItemsUrl());
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.connect();

                int length = conn.getContentLength();
                InputStream is = new BufferedInputStream(url.openStream(),10*1024);

                String[] urls = item.getItemsUrl().split("/");
                String filename = urls[urls.length-1];
                File zipFile = new File(activity.getExternalFilesDir(null) + File.separator
                        + ItemManager.getItemStoreZipPath() + filename);

                if(!zipFile.exists()) {
                    zipFile.createNewFile();
                }

                OutputStream os = new FileOutputStream(zipFile);
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = is.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / length));
                    os.write(data, 0, count);
                }
                os.flush();
                os.close();
                is.close();

                // TODO: ZIP 파일 압축해제 ??
                File itemDir = new File(emoticonDir, ItemManager.ITEM_STORE_ITEMS);
                if(!itemDir.exists())
                    itemDir.mkdir();

                File outDir = new File(itemDir, item.getFileName());
                if(!outDir.exists())
                    outDir.mkdir();

                new ZipDecompress(zipFile, outDir).UnZip();

                // TODO: 다운 받은 이모티콘 PREF에 저장
                // TODO: 저장 순서는??
                EmoticonTabOrder.getInstance(this.activity).generateTabOrder(item.getFileName());
                EmoticonTabOrder.getInstance(this.activity).saveRefresh(true);
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressFragment.updateProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String result){
            progressFragment.getDialog().dismiss();
            if(EmoticonTabOrder.getInstance(this.activity).isRefresh()) {
                if(BuildVars.DEBUG_VERSION) {
                    // TODO: Remove toast
                    Toast.makeText(ApplicationLoader.applicationContext, "SUCCESS", Toast.LENGTH_SHORT).show();
                }
                disableDownloadButton();
            }
        }
    }
}
