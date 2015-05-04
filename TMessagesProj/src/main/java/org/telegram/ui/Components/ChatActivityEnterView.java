/*
 * This is the source code of Telegram for Android v. 1.4.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.makeday.emoticonsdk.EmojiconEditText;
import net.makeday.emoticonsdk.EmojiconsPopup;
import net.makeday.emoticonsdk.StickerGridView;
import net.makeday.emoticonsdk.StickerUsersGridView;
import net.makeday.emoticonsdk.component.StickersPreviewPanel;
import net.makeday.emoticonsdk.core.CacheStorage;
import net.makeday.emoticonsdk.core.StickerUtils;
import net.makeday.emoticonsdk.sticker.Sticker;

import org.telegram.android.query.StickersQuery;
import org.telegram.messenger.Utilities;
import org.telegram.android.AndroidUtilities;
import org.telegram.android.Emoji;
import org.telegram.android.LocaleController;
import org.telegram.android.MediaController;
import org.telegram.android.MessageObject;
import org.telegram.android.MessagesController;
import org.telegram.android.SendMessagesHelper;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.FileLog;
import org.telegram.android.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TLRPC;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.android.AnimationCompat.AnimatorListenerAdapterProxy;
import org.telegram.android.AnimationCompat.AnimatorSetProxy;
import org.telegram.android.AnimationCompat.ObjectAnimatorProxy;
import org.telegram.android.AnimationCompat.ViewProxy;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.Cells.StickerEmojiCell;

import me.ttalk.sdk.ServiceAgent;
import me.ttalk.sdk.theme.ThemeManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ChatActivityEnterView extends FrameLayoutFixed implements NotificationCenter.NotificationCenterDelegate, SizeNotifierRelativeLayout.SizeNotifierRelativeLayoutDelegate {

    public interface ChatActivityEnterViewDelegate {
        void onMessageSend(String message);
        void needSendTyping();
        void onTextChanged(CharSequence text, boolean bigChange);
        void onAttachButtonHidden();
        void onAttachButtonShow();
        void onWindowSizeChanged(int size);
        void onItemStoreStart();
    }

    public interface Listener {
        void onStickerSelected(TLRPC.Document sticker);
    }

    private EditText messageEditText;
    private ImageView sendButton;
    //private PopupWindow emojiPopup;
    private EmojiconsPopup emojiPopup;
    private ImageView emojiButton;
    //private EmojiView emojiView;
    private TextView recordTimeText;
    private ImageView audioSendButton;
    private FrameLayout recordPanel;
    private LinearLayout slideText;
    private View sizeNotifierLayout;
    private FrameLayout attachButton;
    private LinearLayout textFieldContainer;
    private View topView;

    private int framesDroped;

    private int keyboardTransitionState;
    private boolean showKeyboardOnEmojiButton;
    private ViewTreeObserver.OnPreDrawListener onPreDrawListener;

    private PowerManager.WakeLock mWakeLock;
    private AnimatorSetProxy runningAnimation;
    private AnimatorSetProxy runningAnimation2;
    private ObjectAnimatorProxy runningAnimationAudio;
    private int runningAnimationType;
    private int audioInterfaceState;

    private int keyboardHeight;
    private int keyboardHeightLand;
    private boolean keyboardVisible;
    private boolean sendByEnter;
    private long lastTypingTimeSend;
    private String lastTimeString;
    private float startedDraggingX = -1;
    private float distCanMove = AndroidUtilities.dp(80);
    private boolean recordingAudio;
    private boolean forceShowSendButton;
    private boolean allowStickers;

    private Activity parentActivity;
    private BaseFragment parentFragment;
    private long dialog_id;
    private boolean ignoreTextChange;
    private MessageObject replyingMessageObject;
    private TLRPC.WebPage messageWebPage;
    private boolean messageWebPageSearch = true;
    private ChatActivityEnterViewDelegate delegate;
    private float topViewAnimation;
    private boolean topViewShowed;
    private boolean needShowTopView;
    private boolean allowShowTopView;
    private StickersPreviewPanel stickersPreviewPanel;
    private AnimatorSetProxy currentTopViewAnimation;
    private ArrayList<TLRPC.Document> stickers;
    private StickersGridAdapter stickersGridAdapter;
    private HashMap<Long, Integer> stickersUseHistory = new HashMap<>();
    private Listener listener;

    public ChatActivityEnterView(Activity context, View parent, BaseFragment fragment, boolean isChat, boolean isEncrypt)  {
        super(context);
        boolean isCustomTheme = false;
        Drawable composeDrawable = ThemeManager.getInstance().getRemoteResourceDrawable("compose_panel");
        if (composeDrawable != null){
            isCustomTheme = true;
            if (Build.VERSION.SDK_INT >= 16) setBackground(composeDrawable);
            else setBackgroundDrawable(composeDrawable);
        }else{
            setBackgroundResource(R.drawable.compose_panel);
        }

        setFocusable(true);
        setFocusableInTouchMode(true);

        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStartError);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordStopped);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recordProgressChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidSent);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.hideEmojiKeyboard);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioRouteChanged);
        parentActivity = context;
        parentFragment = fragment;
        sizeNotifierLayout = parent;
        if (sizeNotifierLayout instanceof SizeNotifierRelativeLayout) {
            ((SizeNotifierRelativeLayout) sizeNotifierLayout).setDelegate(this);
        } else if (sizeNotifierLayout instanceof SizeNotifierFrameLayout) {
            ((SizeNotifierFrameLayout) sizeNotifierLayout).setDelegate(this);
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        sendByEnter = preferences.getBoolean("send_by_enter", false);

        parent.getViewTreeObserver().addOnPreDrawListener(onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (keyboardTransitionState == 1) {
                    if (keyboardVisible || framesDroped >= 60) {
                        showEmojiPopup(false, false);
                        keyboardTransitionState = 0;
                    } else {
                        openKeyboard();
                    }
                    framesDroped++;
                    return false;
                } else if (keyboardTransitionState == 2) {
                    if (!keyboardVisible || framesDroped >= 60) {
                        int currentHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
                        sizeNotifierLayout.setPadding(0, 0, 0, currentHeight);
                        keyboardTransitionState = 0;
                    }
                    framesDroped++;
                    return false;
                }
                return true;
            }
        });

        textFieldContainer = new LinearLayout(context);
        if(isCustomTheme) textFieldContainer.setBackgroundResource(R.drawable.transparent);
        else textFieldContainer.setBackgroundColor(0xffffffff);
        textFieldContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(textFieldContainer);
        LayoutParams layoutParams2 = (LayoutParams) textFieldContainer.getLayoutParams();
        layoutParams2.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams2.width = LayoutHelper.MATCH_PARENT;
        layoutParams2.height = LayoutHelper.WRAP_CONTENT;
        layoutParams2.topMargin = AndroidUtilities.dp(2);
        textFieldContainer.setLayoutParams(layoutParams2);

        FrameLayoutFixed frameLayout = new FrameLayoutFixed(context);
        textFieldContainer.addView(frameLayout);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        layoutParams.width = 0;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.weight = 1;
        frameLayout.setLayoutParams(layoutParams);

        if (isChat) {
            attachButton = new FrameLayout(context);
            attachButton.setEnabled(false);
    //            ViewProxy.setPivotX(attachButton, AndroidUtilities.dp(48));
            frameLayout.addView(attachButton);
            FrameLayout.LayoutParams layoutParamsAttach = (FrameLayout.LayoutParams) attachButton.getLayoutParams();
            layoutParamsAttach.width = AndroidUtilities.dp(48);
            layoutParamsAttach.height = AndroidUtilities.dp(48);
            layoutParamsAttach.gravity = Gravity.BOTTOM;
            layoutParamsAttach.topMargin = AndroidUtilities.dp(2);
            attachButton.setLayoutParams(layoutParamsAttach);
        }

        messageEditText = new EmojiconEditText(context);
        messageEditText.setHint(LocaleController.getString("TypeMessage", R.string.TypeMessage));
        messageEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        messageEditText.setInputType(messageEditText.getInputType() | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        messageEditText.setSingleLine(false);
        messageEditText.setMaxLines(4);
        messageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, preferences.getInt("fons_size", 18));
        messageEditText.setGravity(Gravity.BOTTOM);
        messageEditText.setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(12));
        messageEditText.setBackgroundDrawable(null);
        AndroidUtilities.clearCursorDrawable(messageEditText);
        messageEditText.setTextColor(0xff000000);
        messageEditText.setHintTextColor(0xffb2b2b2);
        frameLayout.addView(messageEditText);

        FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = LayoutHelper.WRAP_CONTENT;
        layoutParams1.gravity = Gravity.BOTTOM;
        layoutParams1.leftMargin = AndroidUtilities.dp(isChat ? 48 : 10);
        layoutParams1.rightMargin = AndroidUtilities.dp(48);
        messageEditText.setLayoutParams(layoutParams1);
        messageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i == 4 && !keyboardVisible && emojiPopup != null && emojiPopup.isShowing()) {
                    if (keyEvent.getAction() == 1) {
                        showEmojiPopup(false, false);
                    }
                    return true;
                } else if (i == KeyEvent.KEYCODE_ENTER && sendByEnter && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });
        messageEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (emojiPopup != null && emojiPopup.isShowing()) {
                    showEmojiPopup(false, false);
                }
            }
        });
        messageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                } else if (sendByEnter) {
                    if (keyEvent != null && i == EditorInfo.IME_NULL && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        sendMessage();
                        return true;
                    }
                }
                return false;
            }
        });
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String message = getTrimmedString(charSequence.toString());
                checkSendButton(true);

                if (delegate != null) {
                    if (count > 2 || charSequence == null || charSequence.length() == 0) {
                        messageWebPageSearch = true;
                    }
                    delegate.onTextChanged(charSequence, before > count + 1 || (count - before) > 2);
                }

                if (message.length() != 0 && lastTypingTimeSend < System.currentTimeMillis() - 5000 && !ignoreTextChange) {
                    int currentTime = ConnectionsManager.getInstance().getCurrentTime();
                    TLRPC.User currentUser = null;
                    if ((int) dialog_id > 0) {
                        currentUser = MessagesController.getInstance().getUser((int) dialog_id);
                    }
                    if (currentUser != null && (currentUser.id == UserConfig.getClientUserId() || currentUser.status != null && currentUser.status.expires < currentTime)) {
                        return;
                    }
                    lastTypingTimeSend = System.currentTimeMillis();
                    if (delegate != null) {
                        delegate.needSendTyping();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (sendByEnter && editable.length() > 0 && editable.charAt(editable.length() - 1) == '\n') {
                    sendMessage();
                }
/*
                int i = 0;
                ImageSpan[] arrayOfImageSpan = editable.getSpans(0, editable.length(), ImageSpan.class);
                int j = arrayOfImageSpan.length;
                while (true) {
                    if (i >= j) {
                        Emoji.replaceEmoji(editable, messageEditText.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20));
                        return;
                    }
                    editable.removeSpan(arrayOfImageSpan[i]);
                    i++;
                }
*/
            }
        });

        // Emoji Button
        emojiButton = new ImageView(context);
        Drawable smileDrawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_msg_panel_smiles");
        if (smileDrawable != null){
            emojiButton.setImageDrawable(smileDrawable);
        }else{
            emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
        }
        emojiButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        emojiButton.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(1), 0, 0);
        frameLayout.addView(emojiButton);

        FrameLayout.LayoutParams layoutParamsEmoji = (FrameLayout.LayoutParams) emojiButton.getLayoutParams();
        layoutParamsEmoji.width = AndroidUtilities.dp(48);
        layoutParamsEmoji.height = AndroidUtilities.dp(48);
        layoutParamsEmoji.gravity =  Gravity.BOTTOM | Gravity.RIGHT;
        layoutParamsEmoji.topMargin = AndroidUtilities.dp(2);
        emojiButton.setLayoutParams(layoutParamsEmoji);

        /*
        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEmojiPopup(emojiPopup == null || !emojiPopup.isShowing());
            }
        });
        */

        StickersQuery.checkStickers();
        stickers = StickersQuery.getStickers();
        GridView gridView = new GridView(context);
        gridView.setColumnWidth(AndroidUtilities.dp(72));
        gridView.setNumColumns(-1);
        gridView.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        gridView.setClipToPadding(false);
        stickersGridAdapter = new StickersGridAdapter(context);
        gridView.setAdapter(stickersGridAdapter);
        AndroidUtilities.setListViewEdgeEffectColor(gridView, 0xfff5f6f7);
        setListener(new ChatActivityEnterView.Listener() {
            public void onStickerSelected(TLRPC.Document sticker) {
                ServiceAgent.getInstance().logEvent("Sticker", "Custom", String.valueOf(sticker.id));
                SendMessagesHelper.getInstance().sendSticker(sticker, dialog_id, replyingMessageObject);
                if (delegate != null) {
                    delegate.onMessageSend(null);
                }
            }
        });
        boolean hasItemShop = preferences.getBoolean("item_store", true);
        emojiPopup = new EmojiconsPopup(parent,
                ApplicationLoader.applicationContext,
                (EmojiconEditText)messageEditText,
                emojiButton,
                isChat,
                isEncrypt,
                hasItemShop,
                new StickerUsersGridView(ApplicationLoader.applicationContext, gridView, R.drawable.ic_emoji_sticker));

        emojiPopup.setSizeForSoftKeyboard();

        if (smileDrawable != null) {
            emojiPopup.setSmileIconDrawable(smileDrawable);
        } else {
            emojiPopup.setSmileIcon(R.drawable.ic_msg_panel_smiles);
        }

        emojiPopup.setKeyboardIcon(R.drawable.ic_msg_panel_kb);
        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ServiceAgent.getInstance().logEvent("Chat.ComposePanel.Emoji.Click");
                ServiceAgent.getInstance().logEvent("Chat", "Emoji", "Click");
                showEmojiPopup(true, false);
            }
        });
        emojiPopup.setOnItemStoreClickedListener(new EmojiconsPopup.OnItemStoreClickedListener() {

            @Override
            public void onItemShopClicked() {
                ServiceAgent.getInstance().logEvent("Chat.ComposePanel.Emoji.Store");
                ServiceAgent.getInstance().logEvent("Chat", "ItemStore", "Start");
                if (delegate != null) {
                    delegate.onItemStoreStart();
                }
            }
        });

        emojiPopup.setOnStickerClickedListener(new StickerGridView.OnStickerClickedListener() {

            @Override
            public void onStickerClicked(Sticker sticker) {
                if (sticker.getStickerType() == Sticker.TYPE_LOCAL) {
                    changeSendButton(true);
                    try {
                        String cacheFileName = sticker.getStickerFullName();
                        InputStream is = StickerUtils.getRawData(ApplicationLoader.applicationContext, cacheFileName);
                        ByteBuffer buffer = StickerUtils.readInputStream(is);
                        Bitmap image = Utilities.loadWebpImage(buffer, buffer.limit(), null);
                        stickersPreviewPanel.setSticker(sticker, image);
                        buffer.clear();
                    }catch(Exception e){
                        FileLog.e("tmessages", e);
                    }
                }else if(sticker.getStickerType() == Sticker.TYPE_DOWNLOAD) {
                    changeSendButton(true);
                    try {
                        String cacheFileFinal = sticker.getStickerModel().getPath() + File.separator + sticker.getStickerModel().toStickerWEBP(sticker.getStickerIndex());
                        InputStream is = new FileInputStream(cacheFileFinal);
                        ByteBuffer buffer = StickerUtils.readInputStream(is);
                        Bitmap image = Utilities.loadWebpImage(buffer, buffer.limit(), null);
                        stickersPreviewPanel.setSticker(sticker, image);
                        buffer.clear();
                        is.close();
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
            }
        });

        recordPanel = new FrameLayoutFixed(context);
        recordPanel.setVisibility(GONE);
        if(isCustomTheme) recordPanel.setBackgroundResource(R.drawable.transparent);
        else recordPanel.setBackgroundColor(0xffffffff);
        frameLayout.addView(recordPanel);
        layoutParams1 = (FrameLayout.LayoutParams) recordPanel.getLayoutParams();
        layoutParams1.width = LayoutHelper.MATCH_PARENT;
        layoutParams1.height = AndroidUtilities.dp(48);
        layoutParams1.gravity = Gravity.BOTTOM;
        layoutParams1.topMargin = AndroidUtilities.dp(2); // FIXME
        recordPanel.setLayoutParams(layoutParams1);

        slideText = new LinearLayout(context);
        slideText.setOrientation(LinearLayout.HORIZONTAL);
        recordPanel.addView(slideText);
        layoutParams1 = (FrameLayout.LayoutParams) slideText.getLayoutParams();
        layoutParams1.width = LayoutHelper.WRAP_CONTENT;
        layoutParams1.height = LayoutHelper.WRAP_CONTENT;
        layoutParams1.gravity = Gravity.CENTER;
        layoutParams1.leftMargin = AndroidUtilities.dp(30);
        slideText.setLayoutParams(layoutParams1);

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.slidearrow);
        slideText.addView(imageView);
        layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.topMargin = AndroidUtilities.dp(1);
        imageView.setLayoutParams(layoutParams);

        TextView textView = new TextView(context);
        textView.setText(LocaleController.getString("SlideToCancel", R.string.SlideToCancel));
        textView.setTextColor(0xff999999);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        slideText.addView(textView);
        layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.leftMargin = AndroidUtilities.dp(6);
        textView.setLayoutParams(layoutParams);

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(AndroidUtilities.dp(13), 0, 0, 0);
        if(isCustomTheme) linearLayout.setBackgroundResource(R.drawable.transparent);
        else linearLayout.setBackgroundColor(0xffffffff);
        recordPanel.addView(linearLayout);
        layoutParams1 = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
        layoutParams1.width = LayoutHelper.WRAP_CONTENT;
        layoutParams1.height = LayoutHelper.WRAP_CONTENT;
        layoutParams1.gravity = Gravity.CENTER_VERTICAL;
        linearLayout.setLayoutParams(layoutParams1);

        imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.rec);
        linearLayout.addView(imageView);
        layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.topMargin = AndroidUtilities.dp(1);
        imageView.setLayoutParams(layoutParams);

        recordTimeText = new TextView(context);
        recordTimeText.setText("00:00");
        recordTimeText.setTextColor(0xff4d4c4b);
        recordTimeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        linearLayout.addView(recordTimeText);
        layoutParams = (LinearLayout.LayoutParams) recordTimeText.getLayoutParams();
        layoutParams.width = LayoutHelper.WRAP_CONTENT;
        layoutParams.height = LayoutHelper.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.leftMargin = AndroidUtilities.dp(6);
        recordTimeText.setLayoutParams(layoutParams);

        FrameLayout frameLayout1 = new FrameLayout(context);
        textFieldContainer.addView(frameLayout1);
        layoutParams = (LinearLayout.LayoutParams) frameLayout1.getLayoutParams();
        layoutParams.width = AndroidUtilities.dp(48);
        layoutParams.height = AndroidUtilities.dp(48);
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.topMargin = AndroidUtilities.dp(2); // FIXME
        frameLayout1.setLayoutParams(layoutParams);

        audioSendButton = new ImageView(context);
        audioSendButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        audioSendButton.setImageResource(R.drawable.mic_button_states);
        if(isCustomTheme) audioSendButton.setBackgroundResource(R.drawable.transparent);
        else audioSendButton.setBackgroundColor(0xffffffff);
        audioSendButton.setSoundEffectsEnabled(false);
        audioSendButton.setPadding(0, 0, AndroidUtilities.dp(4), 0);
        frameLayout1.addView(audioSendButton);
        layoutParams1 = (FrameLayout.LayoutParams) audioSendButton.getLayoutParams();
        layoutParams1.width = AndroidUtilities.dp(48);
        layoutParams1.height = AndroidUtilities.dp(48);
        audioSendButton.setLayoutParams(layoutParams1);
        audioSendButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (parentFragment != null) {
                        String action;
                        TLRPC.Chat currentChat;
                        if ((int) dialog_id < 0) {
                            currentChat = MessagesController.getInstance().getChat(-(int) dialog_id);
                            if (currentChat != null && currentChat.participants_count > MessagesController.getInstance().groupBigSize) {
                                action = "bigchat_upload_audio";
                            } else {
                                action = "chat_upload_audio";
                            }
                        } else {
                            action = "pm_upload_audio";
                        }
                        if (!MessagesController.isFeatureEnabled(action, parentFragment)) {
                            return false;
                        }
                    }
                    startedDraggingX = -1;
                    MediaController.getInstance().startRecording(dialog_id, replyingMessageObject);
                    updateAudioRecordIntefrace();
                    audioSendButton.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    startedDraggingX = -1;
                    MediaController.getInstance().stopRecording(true);
                    recordingAudio = false;
                    updateAudioRecordIntefrace();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE && recordingAudio) {
                    float x = motionEvent.getX();
                    if (x < -distCanMove) {
                        MediaController.getInstance().stopRecording(false);
                        recordingAudio = false;
                        updateAudioRecordIntefrace();
                    }

                    x = x + ViewProxy.getX(audioSendButton);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
                    if (startedDraggingX != -1) {
                        float dist = (x - startedDraggingX);
                        params.leftMargin = AndroidUtilities.dp(30) + (int) dist;
                        slideText.setLayoutParams(params);
                        float alpha = 1.0f + dist / distCanMove;
                        if (alpha > 1) {
                            alpha = 1;
                        } else if (alpha < 0) {
                            alpha = 0;
                        }
                        ViewProxy.setAlpha(slideText, alpha);
                    }
                    if (x <= ViewProxy.getX(slideText) + slideText.getWidth() + AndroidUtilities.dp(30)) {
                        if (startedDraggingX == -1) {
                            startedDraggingX = x;
                            distCanMove = (recordPanel.getMeasuredWidth() - slideText.getMeasuredWidth() - AndroidUtilities.dp(48)) / 2.0f;
                            if (distCanMove <= 0) {
                                distCanMove = AndroidUtilities.dp(80);
                            } else if (distCanMove > AndroidUtilities.dp(80)) {
                                distCanMove = AndroidUtilities.dp(80);
                            }
                        }
                    }
                    if (params.leftMargin > AndroidUtilities.dp(30)) {
                        params.leftMargin = AndroidUtilities.dp(30);
                        slideText.setLayoutParams(params);
                        ViewProxy.setAlpha(slideText, 1);
                        startedDraggingX = -1;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }
        });

        sendButton = new ImageView(context);
        sendButton.setVisibility(View.INVISIBLE);
        sendButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        sendButton.setImageResource(R.drawable.ic_send);
        sendButton.setSoundEffectsEnabled(false);
        ViewProxy.setScaleX(sendButton, 0.1f);
        ViewProxy.setScaleY(sendButton, 0.1f);
        ViewProxy.setAlpha(sendButton, 0.0f);
        sendButton.clearAnimation();
        frameLayout1.addView(sendButton);
        layoutParams1 = (FrameLayout.LayoutParams) sendButton.getLayoutParams();
        layoutParams1.width = AndroidUtilities.dp(48);
        layoutParams1.height = AndroidUtilities.dp(48);
        sendButton.setLayoutParams(layoutParams1);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        changeRemoteTheme();
        checkSendButton(false);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                // ??
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.emojiDidLoaded);
            }
        });
    }

    private void setKeyboardTransitionState(int state) {
        if (AndroidUtilities.usingHardwareInput) {
            if (state == 1) {
                showEmojiPopup(false, false);
                keyboardTransitionState = 0;

            } else if (state == 2) {
                int currentHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
                sizeNotifierLayout.setPadding(0, 0, 0, currentHeight);
                keyboardTransitionState = 0;
            }
        } else {
            framesDroped = 0;
            keyboardTransitionState = state;
            if (state == 1) {
                sizeNotifierLayout.setPadding(0, 0, 0, 0);
            }
        }
    }

    public void addTopView(View view, int height) {
        if (view == null) {
            return;
        }
        addView(view, 0);
        topView = view;
        topView.setVisibility(GONE);
        needShowTopView = false;
        LayoutParams layoutParams = (LayoutParams) topView.getLayoutParams();
        layoutParams.width = LayoutHelper.MATCH_PARENT;
        layoutParams.height = height;
        layoutParams.topMargin = AndroidUtilities.dp(2);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        topView.setLayoutParams(layoutParams);
    }

    public void setTopViewAnimation(float progress) {
        topViewAnimation = progress;
        LayoutParams layoutParams2 = (LayoutParams) textFieldContainer.getLayoutParams();
        layoutParams2.topMargin = AndroidUtilities.dp(2) + (int) (topView.getLayoutParams().height * progress);
        textFieldContainer.setLayoutParams(layoutParams2);
    }

    public float getTopViewAnimation() {
        return topViewAnimation;
    }

    public void setForceShowSendButton(boolean value, boolean animated) {
        forceShowSendButton = value;
        checkSendButton(animated);
    }

    public void setAllowStickers(boolean value) {
        allowStickers = value;
    }

    public void showTopView(boolean animated) {
        if (topView == null || topViewShowed) {
            return;
        }

        needShowTopView = true;
        topViewShowed = true;
        if (allowShowTopView) {
            topView.setVisibility(VISIBLE);
            if (currentTopViewAnimation != null) {
                currentTopViewAnimation.cancel();
                currentTopViewAnimation = null;
            }
            if (animated) {
                if (keyboardVisible || emojiPopup != null && emojiPopup.isShowing()) {
                    currentTopViewAnimation = new AnimatorSetProxy();
                    currentTopViewAnimation.playTogether(
                            ObjectAnimatorProxy.ofFloat(ChatActivityEnterView.this, "topViewAnimation", 1.0f)
                    );
                    currentTopViewAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Object animation) {
                            if (animation == currentTopViewAnimation) {
                                setTopViewAnimation(1.0f);
                                if (!forceShowSendButton) {
                                    openKeyboard();
                                }
                                currentTopViewAnimation = null;
                            }
                        }
                    });
                    currentTopViewAnimation.setDuration(200);
                    currentTopViewAnimation.start();
                } else {
                    setTopViewAnimation(1.0f);
                    if (!forceShowSendButton) {
                        openKeyboard();
                    }
                }
            } else {
                setTopViewAnimation(1.0f);
            }
        }
    }

    public void hideTopView(final boolean animated) {
        if (topView == null || !topViewShowed) {
            return;
        }

        topViewShowed = false;
        needShowTopView = false;
        if (allowShowTopView) {
            float resumeValue = 1.0f;
            if (currentTopViewAnimation != null) {
                resumeValue = topViewAnimation;
                currentTopViewAnimation.cancel();
                currentTopViewAnimation = null;
            }
            if (animated) {
                currentTopViewAnimation = new AnimatorSetProxy();
                currentTopViewAnimation.playTogether(
                        ObjectAnimatorProxy.ofFloat(ChatActivityEnterView.this, "topViewAnimation", resumeValue, 0.0f)
                );
                currentTopViewAnimation.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Object animation) {
                        if (animation == currentTopViewAnimation) {
                            topView.setVisibility(GONE);
                            setTopViewAnimation(0.0f);
                            currentTopViewAnimation = null;
                        }
                    }
                });
                currentTopViewAnimation.setDuration(200);
                currentTopViewAnimation.start();
                topView.setVisibility(GONE);
            } else {
                topView.setVisibility(GONE);
                setTopViewAnimation(0.0f);
            }
        }
    }

    public boolean isTopViewVisible() {
        return topView != null && topView.getVisibility() == VISIBLE;
    }

    private void onWindowSizeChanged(int size) {
        if (delegate != null) {
            delegate.onWindowSizeChanged(size);
        }
        if (topView != null) {
            if (size < AndroidUtilities.dp(72) + AndroidUtilities.getCurrentActionBarHeight()) {
                if (allowShowTopView) {
                    allowShowTopView = false;
                    if (needShowTopView) {
                        topView.setVisibility(View.GONE);
                        setTopViewAnimation(0.0f);
                    }
                }
            } else {
                if (!allowShowTopView) {
                    allowShowTopView = true;
                    if (needShowTopView) {
                        topView.setVisibility(View.VISIBLE);
                        setTopViewAnimation(1.0f);
                    }
                }
            }
        }
    }

    public void onDestroy() {
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStartError);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordStopped);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recordProgressChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidSent);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.hideEmojiKeyboard);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioRouteChanged);
        sizeNotifierLayout.getViewTreeObserver().removeOnPreDrawListener(onPreDrawListener);
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
                mWakeLock = null;
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
        if (sizeNotifierLayout != null) {
            if (sizeNotifierLayout instanceof SizeNotifierRelativeLayout) {
                ((SizeNotifierRelativeLayout) sizeNotifierLayout).setDelegate(null);
            } else if (sizeNotifierLayout instanceof SizeNotifierFrameLayout) {
                ((SizeNotifierFrameLayout) sizeNotifierLayout).setDelegate(null);
            }
        }
    }

    public void setDialogId(long id) {
        dialog_id = id;
    }

    public void setReplyingMessageObject(MessageObject messageObject) {
        replyingMessageObject = messageObject;
    }

    public void setWebPage(TLRPC.WebPage webPage, boolean searchWebPages) {
        messageWebPage = webPage;
        messageWebPageSearch = searchWebPages;
    }

    public boolean isMessageWebPageSearchEnabled() {
        return messageWebPageSearch;
    }

    private void sendMessage() {
        ServiceAgent.getInstance().logEvent("Chat.ComposePanel.Send");
        if (parentFragment != null) {
            String action;
            TLRPC.Chat currentChat;
            if ((int) dialog_id < 0) {
                currentChat = MessagesController.getInstance().getChat(-(int) dialog_id);
                if (currentChat != null && currentChat.participants_count > MessagesController.getInstance().groupBigSize) {
                    action = "bigchat_message";
                } else {
                    action = "chat_message";
                }
            } else {
                action = "pm_message";
            }
            if (!MessagesController.isFeatureEnabled(action, parentFragment)) {
                return;
            }
        }
// FIXME send sticker with message.
//        String message = getTrimmedString(messsageEditText.getText().toString());
//        if (message.length() > 0 && stickersPreviewPanel.hasSticker() == true){
//            stickersPreviewPanel.sendSticker();
//        }else{
//            beforeSendMessage();
//        }
        if(stickersPreviewPanel != null){
            stickersPreviewPanel.sendSticker();
        }
        beforeSendMessage();
    }

    public void beforeSendMessage() {
        String message = messageEditText.getText().toString();
        if (processSendingText(message)) {
            messageEditText.setText("");
            lastTypingTimeSend = 0;
            if (delegate != null) {
                delegate.onMessageSend(message);
            }
        } else if (forceShowSendButton) {
            if (delegate != null) {
                delegate.onMessageSend(null);
            }
        }
    }

    public boolean processSendingText(String text) {
        text = getTrimmedString(text);
        if (text.length() != 0) {
            int count = (int) Math.ceil(text.length() / 4096.0f);
            for (int a = 0; a < count; a++) {
                String mess = text.substring(a * 4096, Math.min((a + 1) * 4096, text.length()));
                SendMessagesHelper.getInstance().sendMessage(mess, dialog_id, replyingMessageObject, messageWebPage, messageWebPageSearch);
            }
            return true;
        }
        return false;
    }

    private String getTrimmedString(String src) {
        String result = src.trim();
        if (result.length() == 0) {
            return result;
        }
        while (src.startsWith("\n")) {
            src = src.substring(1);
        }
        while (src.endsWith("\n")) {
            src = src.substring(0, src.length() - 1);
        }
        return src;
    }

    private void checkSendButton(final boolean animated) {
        String message = getTrimmedString(messageEditText.getText().toString());
        if (message.length() > 0 || forceShowSendButton) {
            if (audioSendButton.getVisibility() == View.VISIBLE) {
                if (animated) {
                    changeSendButton(animated);
                    if (runningAnimationType == 1) {
                        return;
                    }
                    if (runningAnimation != null) {
                        runningAnimation.cancel();
                        runningAnimation = null;
                    }
                    if (runningAnimation2 != null) {
                        runningAnimation2.cancel();
                        runningAnimation2 = null;
                    }

                    if (attachButton != null) {
                        runningAnimation2 = new AnimatorSetProxy();
                        runningAnimation2.playTogether(
                                ObjectAnimatorProxy.ofFloat(attachButton, "alpha", 0.0f),
                                ObjectAnimatorProxy.ofFloat(attachButton, "scaleX", 0.0f)
                        );
                        runningAnimation2.setDuration(100);
                        runningAnimation2.addListener(new AnimatorListenerAdapterProxy() {
                            @Override
                            public void onAnimationEnd(Object animation) {
                                if (runningAnimation2.equals(animation)) {
                                    attachButton.setVisibility(View.GONE);
                                    attachButton.clearAnimation();
                                }
                            }
                        });
                        runningAnimation2.start();

                        if (messageEditText != null) {
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
                            layoutParams.rightMargin = AndroidUtilities.dp(0);
                            messageEditText.setLayoutParams(layoutParams);
                        }

                        delegate.onAttachButtonHidden();
                    }

                    sendButton.setVisibility(View.VISIBLE);
                    runningAnimation = new AnimatorSetProxy();
                    runningAnimationType = 1;

                    runningAnimation.playTogether(
                            ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleX", 0.1f),
                            ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleY", 0.1f),
                            ObjectAnimatorProxy.ofFloat(audioSendButton, "alpha", 0.0f),
                            ObjectAnimatorProxy.ofFloat(sendButton, "scaleX", 1.0f),
                            ObjectAnimatorProxy.ofFloat(sendButton, "scaleY", 1.0f),
                            ObjectAnimatorProxy.ofFloat(sendButton, "alpha", 1.0f)
                    );

                    runningAnimation.setDuration(150);
                    runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                        @Override
                        public void onAnimationEnd(Object animation) {
                            if (runningAnimation.equals(animation)) {
                                sendButton.setVisibility(View.VISIBLE);
                                audioSendButton.setVisibility(View.GONE);
                                audioSendButton.clearAnimation();
                                runningAnimation = null;
                                runningAnimationType = 0;
                            }
                        }
                    });
                    runningAnimation.start();
                } else {
                    ViewProxy.setScaleX(audioSendButton, 0.1f);
                    ViewProxy.setScaleY(audioSendButton, 0.1f);
                    ViewProxy.setAlpha(audioSendButton, 0.0f);
                    ViewProxy.setScaleX(sendButton, 1.0f);
                    ViewProxy.setScaleY(sendButton, 1.0f);
                    ViewProxy.setAlpha(sendButton, 1.0f);
                    sendButton.setVisibility(View.VISIBLE);
                    audioSendButton.setVisibility(View.GONE);
                    audioSendButton.clearAnimation();
/*
                    if (attachButton != null) {
                        attachButton.setVisibility(View.GONE);
                        attachButton.clearAnimation();

                        delegate.onAttachButtonHidden();
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
                        layoutParams.rightMargin = AndroidUtilities.dp(0);
                        messageEditText.setLayoutParams(layoutParams);
                    }
*/
                }
            }
        } else if (sendButton.getVisibility() == View.VISIBLE) {
            if (animated) {
                if (runningAnimationType == 2) {
                    return;
                }

                if (runningAnimation != null) {
                    runningAnimation.cancel();
                    runningAnimation = null;
                }
                if (runningAnimation2 != null) {
                    runningAnimation2.cancel();
                    runningAnimation2 = null;
                }

                if (attachButton != null) {
                    attachButton.setVisibility(View.VISIBLE);
                    runningAnimation2 = new AnimatorSetProxy();
                    runningAnimation2.playTogether(
                            ObjectAnimatorProxy.ofFloat(attachButton, "alpha", 1.0f),
                            ObjectAnimatorProxy.ofFloat(attachButton, "scaleX", 1.0f)
                    );
                    runningAnimation2.setDuration(100);
                    runningAnimation2.start();

                    if (messageEditText != null) {
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
                        layoutParams.rightMargin = AndroidUtilities.dp(50);
                        messageEditText.setLayoutParams(layoutParams);
                    }

                    delegate.onAttachButtonShow();
                }

                audioSendButton.setVisibility(View.VISIBLE);
                runningAnimation = new AnimatorSetProxy();
                runningAnimationType = 2;

                runningAnimation.playTogether(
                        ObjectAnimatorProxy.ofFloat(sendButton, "scaleX", 0.1f),
                        ObjectAnimatorProxy.ofFloat(sendButton, "scaleY", 0.1f),
                        ObjectAnimatorProxy.ofFloat(sendButton, "alpha", 0.0f),
                        ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleX", 1.0f),
                        ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleY", 1.0f),
                        ObjectAnimatorProxy.ofFloat(audioSendButton, "alpha", 1.0f)
                );

                runningAnimation.setDuration(150);
                runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Object animation) {
                        if (runningAnimation.equals(animation)) {
                            sendButton.setVisibility(View.GONE);
                            sendButton.clearAnimation();
                            audioSendButton.setVisibility(View.VISIBLE);
                            runningAnimation = null;
                            runningAnimationType = 0;
                        }
                    }
                });
                runningAnimation.start();
            } else {
                ViewProxy.setScaleX(sendButton, 0.1f);
                ViewProxy.setScaleY(sendButton, 0.1f);
                ViewProxy.setAlpha(sendButton, 0.0f);
                ViewProxy.setScaleX(audioSendButton, 1.0f);
                ViewProxy.setScaleY(audioSendButton, 1.0f);
                ViewProxy.setAlpha(audioSendButton, 1.0f);
                sendButton.setVisibility(View.GONE);
                sendButton.clearAnimation();
                audioSendButton.setVisibility(View.VISIBLE);
                if (attachButton != null) {
                    delegate.onAttachButtonShow();
                    attachButton.setVisibility(View.VISIBLE);
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messageEditText.getLayoutParams();
                    layoutParams.rightMargin = AndroidUtilities.dp(50);
                    messageEditText.setLayoutParams(layoutParams);
                }
            }
        }
    }

    private void changeSendButton(boolean animated) {
        if(animated) {
            if (runningAnimationType == 1) {
                return;
            }
            if (runningAnimation != null) {
                runningAnimation.cancel();
                runningAnimation = null;
            }
            if (runningAnimation2 != null) {
                runningAnimation2.cancel();
                runningAnimation2 = null;
            }
/*
                    if (attachButton != null) {
                        runningAnimation2 = new AnimatorSetProxy();
                        runningAnimation2.playTogether(
                                ObjectAnimatorProxy.ofFloat(attachButton, "alpha", 0.0f),
                                ObjectAnimatorProxy.ofFloat(attachButton, "scaleX", 0.0f)
                        );
                        runningAnimation2.setDuration(100);
                        runningAnimation2.addListener(new AnimatorListenerAdapterProxy() {
                            @Override
                            public void onAnimationEnd(Object animation) {
                                if (runningAnimation2.equals(animation)) {
                                    attachButton.setVisibility(View.GONE);
                                    attachButton.clearAnimation();
                                }
                            }
                        });
                        runningAnimation2.start();

                        if (messsageEditText != null) {
                            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) messsageEditText.getLayoutParams();
                            layoutParams.rightMargin = AndroidUtilities.dp(0);
                            messsageEditText.setLayoutParams(layoutParams);
                        }

                        delegate.onAttachButtonHidden();
                    }
*/
            sendButton.setVisibility(View.VISIBLE);
            runningAnimation = new AnimatorSetProxy();
            runningAnimationType = 1;

            runningAnimation.playTogether(
                    ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleX", 0.1f),
                    ObjectAnimatorProxy.ofFloat(audioSendButton, "scaleY", 0.1f),
                    ObjectAnimatorProxy.ofFloat(audioSendButton, "alpha", 0.0f),
                    ObjectAnimatorProxy.ofFloat(sendButton, "scaleX", 1.0f),
                    ObjectAnimatorProxy.ofFloat(sendButton, "scaleY", 1.0f),
                    ObjectAnimatorProxy.ofFloat(sendButton, "alpha", 1.0f)
            );

            runningAnimation.setDuration(150);
            runningAnimation.addListener(new AnimatorListenerAdapterProxy() {
                @Override
                public void onAnimationEnd(Object animation) {
                    if (runningAnimation.equals(animation)) {
                        sendButton.setVisibility(View.VISIBLE);
                        audioSendButton.setVisibility(View.GONE);
                        audioSendButton.clearAnimation();
                        runningAnimation = null;
                        runningAnimationType = 0;
                    }
                }
            });
            runningAnimation.start();
        }
    }
    private void updateAudioRecordIntefrace() {
        try {
            if (recordingAudio) {
                if (audioInterfaceState == 1) {
                    return;
                }
                audioInterfaceState = 1;
                try {
                    if (mWakeLock == null) {
                        PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "audio record lock");
                        mWakeLock.acquire();
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
                AndroidUtilities.lockOrientation(parentActivity);

                // Disable Text Message & Attach Button
                messageEditText.setVisibility(View.INVISIBLE);
                attachButton.setVisibility(View.INVISIBLE);

                recordPanel.setVisibility(View.VISIBLE);
                recordTimeText.setText("00:00");
                lastTimeString = null;

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
                params.leftMargin = AndroidUtilities.dp(30);
                slideText.setLayoutParams(params);
                ViewProxy.setAlpha(slideText, 1);
                ViewProxy.setX(recordPanel, AndroidUtilities.displaySize.x);
                if (runningAnimationAudio != null) {
                    runningAnimationAudio.cancel();
                }
                runningAnimationAudio = ObjectAnimatorProxy.ofFloatProxy(recordPanel, "translationX", 0).setDuration(300);
                runningAnimationAudio.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Object animator) {
                        if (runningAnimationAudio != null && runningAnimationAudio.equals(animator)) {
                            ViewProxy.setX(recordPanel, 0);
                        }
                    }
                });
                runningAnimationAudio.setInterpolator(new AccelerateDecelerateInterpolator());
                runningAnimationAudio.start();
            } else {
                if (mWakeLock != null) {
                    try {
                        mWakeLock.release();
                        mWakeLock = null;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                }
                AndroidUtilities.unlockOrientation(parentActivity);
                if (audioInterfaceState == 0) {
                    return;
                }
                audioInterfaceState = 0;

                if (runningAnimationAudio != null) {
                    runningAnimationAudio.cancel();
                }
                runningAnimationAudio = ObjectAnimatorProxy.ofFloatProxy(recordPanel, "translationX", AndroidUtilities.displaySize.x).setDuration(300);
                runningAnimationAudio.addListener(new AnimatorListenerAdapterProxy() {
                    @Override
                    public void onAnimationEnd(Object animator) {
                        if (runningAnimationAudio != null && runningAnimationAudio.equals(animator)) {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideText.getLayoutParams();
                            params.leftMargin = AndroidUtilities.dp(30);
                            slideText.setLayoutParams(params);
                            ViewProxy.setAlpha(slideText, 1);
                            recordPanel.setVisibility(View.GONE);

                            // Disable Text Message & Attach Button
                            messageEditText.setVisibility(View.VISIBLE);
                            attachButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
                runningAnimationAudio.setInterpolator(new AccelerateDecelerateInterpolator());
                runningAnimationAudio.start();
            }
        }catch(Exception e) {
            // FIXME: java.lang.NullPointerException org.telegram.ui.Components.ChatActivityEnterView.updateAudioRecordIntefrace(ChatActivityEnterView.java:1133)
            FileLog.e("tmessages", e);
        }
    }

    private void showEmojiPopup(boolean show, boolean post) {
        emojiPopup.showEmojiPopup();
//        if(show){
//            onWindowSizeChanged(sizeNotifierLayout.getHeight() - sizeNotifierLayout.getPaddingBottom());
//        }
        /*
        if (show) {
            if (emojiPopup == null) {
                if (parentActivity == null) {
                    return;
                }
                emojiView = new EmojiView(allowStickers, parentActivity);
                emojiView.setListener(new EmojiView.Listener() {
                    public boolean onBackspace() {
                        if (messageEditText.length() == 0) {
                            return false;
                        }
                        messageEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                        return true;
                    }

                    public void onEmojiSelected(String symbol) {
                        int i = messageEditText.getSelectionEnd();
                        if (i < 0) {
                            i = 0;
                        }
                        try {//TODO check
                            CharSequence localCharSequence = Emoji.replaceEmoji(symbol, messageEditText.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20));
                            messageEditText.setText(messageEditText.getText().insert(i, localCharSequence));
                            int j = i + localCharSequence.length();
                            messageEditText.setSelection(j, j);
                        } catch (Exception e) {
                            FileLog.e("tmessages", e);
                        }
                    }

                    public void onStickerSelected(TLRPC.Document sticker) {
                        SendMessagesHelper.getInstance().sendSticker(sticker, dialog_id, replyingMessageObject);
                        if (delegate != null) {
                            delegate.onMessageSend(null);
                        }
                    }
                });
                emojiPopup = new PopupWindow(emojiView);
            }

            if (keyboardHeight <= 0) {
                keyboardHeight = ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).getInt("kbd_height", AndroidUtilities.dp(200));
            }
            if (keyboardHeightLand <= 0) {
                keyboardHeightLand = ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).getInt("kbd_height_land3", AndroidUtilities.dp(200));
            }
            int currentHeight = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y ? keyboardHeightLand : keyboardHeight;
            FileLog.e("tmessages", "show emoji with height = " + currentHeight);
            emojiPopup.setHeight(View.MeasureSpec.makeMeasureSpec(currentHeight, View.MeasureSpec.EXACTLY));
            if (sizeNotifierLayout != null) {
                emojiPopup.setWidth(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.x, View.MeasureSpec.EXACTLY));
            }

            emojiPopup.showAtLocation(parentActivity.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.LEFT, 0, 0);

            if (!keyboardVisible) {
                if (sizeNotifierLayout != null) {
                    sizeNotifierLayout.setPadding(0, 0, 0, currentHeight);
                    emojiButton.setImageResource(R.drawable.ic_msg_panel_hide);
                    showKeyboardOnEmojiButton = false;
                    onWindowSizeChanged(sizeNotifierLayout.getHeight() - sizeNotifierLayout.getPaddingBottom());
                }
                return;
            } else {
                setKeyboardTransitionState(2);
                AndroidUtilities.hideKeyboard(messageEditText);
            }
            emojiButton.setImageResource(R.drawable.ic_msg_panel_kb);
            showKeyboardOnEmojiButton = true;
            return;
        }
        if (emojiButton != null) {
            showKeyboardOnEmojiButton = false;
            emojiButton.setImageResource(R.drawable.ic_msg_panel_smiles);
        }
        if (emojiPopup != null) {
            try {
                emojiPopup.dismiss();
            } catch (Exception e) {
                //don't promt
            }
        }
        if (keyboardTransitionState == 0) {
            if (sizeNotifierLayout != null) {
                if (post) {
                    sizeNotifierLayout.post(new Runnable() {
                        public void run() {
                            if (sizeNotifierLayout != null) {
                                sizeNotifierLayout.setPadding(0, 0, 0, 0);
                                onWindowSizeChanged(sizeNotifierLayout.getHeight());
                            }
                        }
                    });
                } else {
                    sizeNotifierLayout.setPadding(0, 0, 0, 0);
                    onWindowSizeChanged(sizeNotifierLayout.getHeight());
                }
            }
        }*/
    }

    public void hideEmojiPopup() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            showEmojiPopup(false, true);
        }
    }

    public void openKeyboard() {
        AndroidUtilities.showKeyboard(messageEditText);
    }

    public void setDelegate(ChatActivityEnterViewDelegate delegate) {
        this.delegate = delegate;
    }

    public void setFieldText(String text) {
        if (messageEditText == null) {
            return;
        }
        ignoreTextChange = true;
        messageEditText.setText(text);
        messageEditText.setSelection(messageEditText.getText().length());
        ignoreTextChange = false;
        if (delegate != null) {
            delegate.onTextChanged(messageEditText.getText(), true);
        }
    }

    public int getCursorPosition() {
        if (messageEditText == null) {
            return 0;
        }
        return messageEditText.getSelectionStart();
    }

    public void replaceWithText(int start, int len, String text) {
        try {
            StringBuilder builder = new StringBuilder(messageEditText.getText());
            builder.replace(start, start + len, text);
            messageEditText.setText(builder);
            messageEditText.setSelection(start + text.length());
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public void setFieldFocused(boolean focus) {
        if (messageEditText == null) {
            return;
        }
        if (focus) {
            if (!messageEditText.isFocused()) {
                messageEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (messageEditText != null) {
                            try {
                                messageEditText.requestFocus();
                            } catch (Exception e) {
                                FileLog.e("tmessages", e);
                            }
                        }
                    }
                }, 600);
            }
        } else {
            if (messageEditText.isFocused() && !keyboardVisible) {
                messageEditText.clearFocus();
            }
        }
    }

    public boolean hasText() {
        return messageEditText != null && messageEditText.length() > 0;
    }

    public String getFieldText() {
        if (messageEditText != null && messageEditText.length() > 0) {
            return messageEditText.getText().toString();
        }
        return null;
    }

    public boolean isEmojiPopupShowing() {
        return emojiPopup != null && emojiPopup.isShowing();
    }

    public void addToAttachLayout(View view) {
        if (attachButton == null) {
            return;
        }
        if (view.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) view.getParent();
            viewGroup.removeView(view);
        }
        attachButton.addView(view);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.width = AndroidUtilities.dp(48);
        layoutParams.height = LayoutHelper.MATCH_PARENT;
        view.setLayoutParams(layoutParams);
    }

    @Override
    public void onSizeChanged(int height, boolean isWidthGreater) {
/*
        if (height > AndroidUtilities.dp(50) && keyboardVisible) {
            if (isWidthGreater) {
                keyboardHeightLand = height;
                ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).edit().putInt("kbd_height_land3", keyboardHeightLand).commit();
            } else {
                keyboardHeight = height;
                ApplicationLoader.applicationContext.getSharedPreferences("emoji", 0).edit().putInt("kbd_height", keyboardHeight).commit();
            }
        }

        if (emojiPopup != null && emojiPopup.isShowing()) {
            int newHeight = isWidthGreater ? keyboardHeightLand : keyboardHeight;
            final WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) emojiPopup.getContentView().getLayoutParams();
            FileLog.e("tmessages", "update emoji height to = " + newHeight);
            if (layoutParams.width != AndroidUtilities.displaySize.x || layoutParams.height != newHeight) {
                layoutParams.width = AndroidUtilities.displaySize.x;
                layoutParams.height = newHeight;
                WindowManager wm = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
                if (wm != null) {
                    wm.updateViewLayout(emojiPopup.getContentView(), layoutParams);
                    if (!keyboardVisible) {
                        if (sizeNotifierLayout != null) {
                            sizeNotifierLayout.setPadding(0, 0, 0, layoutParams.height);
                            sizeNotifierLayout.requestLayout();
                            onWindowSizeChanged(sizeNotifierLayout.getHeight() - sizeNotifierLayout.getPaddingBottom());
                        }
                    }
                }
            }
        }
*/
        boolean oldValue = keyboardVisible;
        keyboardVisible = height > 0;
        if (keyboardVisible && (sizeNotifierLayout.getPaddingBottom() > 0 || keyboardTransitionState == 1)) {
            setKeyboardTransitionState(1);
        } else if (keyboardTransitionState != 2 && !keyboardVisible && keyboardVisible != oldValue && emojiPopup != null && emojiPopup.isShowing()) {
            showEmojiPopup(false, true);
        }
        if (keyboardTransitionState == 0) {
            onWindowSizeChanged(sizeNotifierLayout.getHeight() - sizeNotifierLayout.getPaddingBottom());
        }

    }

    public int getEmojiHeight() {
        if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
            return keyboardHeightLand;
        } else {
            return keyboardHeight;
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.emojiDidLoaded) {
            if (emojiPopup != null) {
                emojiPopup.invalidateViews();
            }
        } else if (id == NotificationCenter.recordProgressChanged) {
            Long time = (Long) args[0] / 1000;
            String str = String.format("%02d:%02d", time / 60, time % 60);
            if (lastTimeString == null || !lastTimeString.equals(str)) {
                if (time % 5 == 0) {
                    MessagesController.getInstance().sendTyping(dialog_id, 1, 0);
                }
                if (recordTimeText != null) {
                    recordTimeText.setText(str);
                }
            }
        } else if (id == NotificationCenter.closeChats) {
            if (messageEditText != null && messageEditText.isFocused()) {
                AndroidUtilities.hideKeyboard(messageEditText);
            }
        } else if (id == NotificationCenter.recordStartError || id == NotificationCenter.recordStopped) {
            if (recordingAudio) {
                MessagesController.getInstance().sendTyping(dialog_id, 2, 0);
                recordingAudio = false;
                updateAudioRecordIntefrace();
            }
        } else if (id == NotificationCenter.recordStarted) {
            if (!recordingAudio) {
                recordingAudio = true;
                updateAudioRecordIntefrace();
            }
        } else if (id == NotificationCenter.audioDidSent) {
            if (delegate != null) {
                delegate.onMessageSend(null);
            }
        } else if (id == NotificationCenter.hideEmojiKeyboard) {
            hideEmojiPopup();
        } else if (id == NotificationCenter.audioRouteChanged) {
            if (parentActivity != null) {
                boolean frontSpeaker = (Boolean) args[0];
                parentActivity.setVolumeControlStream(frontSpeaker ? AudioManager.STREAM_VOICE_CALL : AudioManager.USE_DEFAULT_STREAM_TYPE);
            }
        } else if (id == NotificationCenter.stickersDidLoaded) {
            if (stickersGridAdapter != null) {
                prepareUserStickers();
            }
        }
    }

    private void prepareUserStickers() {
        stickers = StickersQuery.getStickers();
        if(stickers.size() == 0) {
            ArrayList<TLRPC.TL_stickerSet> stickerSets = StickersQuery.getStickerSets();

            ArrayList<TLRPC.Document> documents = new ArrayList<>();
            for (TLRPC.TL_stickerSet stickersSet : stickerSets) {
                documents.addAll(StickersQuery.getStickersForSet(stickersSet.id));
            }
            stickers = documents;
        }
        sortStickers();
        stickersGridAdapter.notifyDataSetChanged();
    }

    // @@ Custom Theme
    private void changeRemoteTheme() {
        Drawable sendDrawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_send");
        if (sendDrawable != null){
            sendButton.setImageDrawable(sendDrawable);
        }
        Drawable smileDrawable = ThemeManager.getInstance().getRemoteResourceDrawable("ic_msg_panel_smiles");
        if (smileDrawable != null){
            emojiButton.setImageDrawable(smileDrawable);
        }

        Drawable recordDrawable = ThemeManager.getInstance().getRemoteResourceDrawable("mic_button_states");
        if (recordDrawable != null){
            audioSendButton.setImageDrawable(recordDrawable);
            audioSendButton.setBackgroundResource(R.drawable.transparent);
        }
    }

    public void setStickersPreviewPanel(StickersPreviewPanel previewPanel) {
        this.stickersPreviewPanel = previewPanel;
        this.stickersPreviewPanel.setOnStickerPreviewClosedListener(new StickersPreviewPanel.OnStickerPreviewClosedListener() {
            @Override
            public void onStickerClicked(boolean _close) {
                checkSendButton(false);
            }
        });
        this.stickersPreviewPanel.setOnStickerPreviewClickedListener(new StickerGridView.OnStickerClickedListener() {
            @Override
            public void onStickerClicked(Sticker sticker) {
                try {
                    checkSendButton(false);

                    InputStream is = null;
                    String cacheFileName = null;
                    String originalPath = null;
                    if (sticker.getStickerType() == Sticker.TYPE_LOCAL) {
                        cacheFileName = sticker.getStickerFullName();
                        is = StickerUtils.getRawData(ApplicationLoader.applicationContext, cacheFileName);
                        cacheFileName = cacheFileName + ".webp";

                        ServiceAgent.getInstance().logEvent("Chat.ComposePanel.Send.Embeded.Sticker", sticker.getStickerName() + "_" + sticker.getStickerIndex());
                        ServiceAgent.getInstance().logEvent("Sticker", sticker.getStickerName(), String.valueOf(sticker.getStickerIndex()));
                    } else if (sticker.getStickerType() == Sticker.TYPE_DOWNLOAD) {
                        is = new FileInputStream(sticker.getStickerModel().getPath() + File.separator + sticker.getStickerModel().toStickerWEBP(sticker.getStickerIndex()));
                        cacheFileName = "sticker_" + sticker.getStickerModel().toStickerWEBP(sticker.getStickerIndex());

                        ServiceAgent.getInstance().logEvent("Chat.ComposePanel.Send.Downloaded.Sticker", sticker.getStickerModel().getStickerName() + "_" + sticker.getStickerIndex());
                        ServiceAgent.getInstance().logEvent("Sticker_Downloaded", sticker.getStickerModel().getStickerName(), String.valueOf(sticker.getStickerIndex()));
                    } else {
                        return;
                    }

                    CacheStorage cache = new CacheStorage(ApplicationLoader.applicationContext, cacheFileName);
                    originalPath = cache.generateCacheFile(is);
                    is.close();

                    final ArrayList<String> stickers = new ArrayList<>();
                    stickers.add(originalPath);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            SendMessagesHelper.prepareSendingPhotos(stickers, null, dialog_id, replyingMessageObject, null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (stickersGridAdapter != null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.stickersDidLoaded);
            prepareUserStickers();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (stickersGridAdapter != null) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.stickersDidLoaded);
        }
    }

    public void setListener(Listener value) {
        listener = value;
    }

    private void saveRecents() {
        ArrayList<Long> arrayList = new ArrayList<>(Emoji.data[0].length);
        for (int j = 0; j < Emoji.data[0].length; j++) {
            arrayList.add(Emoji.data[0][j]);
        }
        getContext().getSharedPreferences("emoji", 0).edit().putString("recents", TextUtils.join(",", arrayList)).commit();
    }

    private void saveRecentStickers() {
        SharedPreferences preferences = getContext().getSharedPreferences("emoji", Activity.MODE_PRIVATE);
        StringBuilder stringBuilder = new StringBuilder();
        for (HashMap.Entry<Long, Integer> entry : stickersUseHistory.entrySet()) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(entry.getKey());
            stringBuilder.append("=");
            stringBuilder.append(entry.getValue());
        }
        getContext().getSharedPreferences("emoji", 0).edit().putString("stickers", stringBuilder.toString()).commit();
    }

    private void sortStickers() {
        HashMap<Long, Integer> hashMap = new HashMap<>();
        for (TLRPC.Document document : stickers) {
            Integer count = stickersUseHistory.get(document.id);
            if (count != null) {
                hashMap.put(document.id, count);
                stickersUseHistory.remove(document.id);
            }
        }
        if (!stickersUseHistory.isEmpty()) {
            stickersUseHistory = hashMap;
            saveRecents();
        } else {
            stickersUseHistory = hashMap;
        }
        Collections.sort(stickers, new Comparator<TLRPC.Document>() {
            @Override
            public int compare(TLRPC.Document lhs, TLRPC.Document rhs) {
                Integer count1 = stickersUseHistory.get(lhs.id);
                Integer count2 = stickersUseHistory.get(rhs.id);
                if (count1 == null) {
                    count1 = 0;
                }
                if (count2 == null) {
                    count2 = 0;
                }
                if (count1 > count2) {
                    return -1;
                } else if (count1 < count2) {
                    return 1;
                }
                return 0;
            }
        });
    }
    private class StickersGridAdapter extends BaseAdapter {

        Context context;

        public StickersGridAdapter(Context context) {
            this.context = context;
        }
        public int getCount() {
            return stickers.size();
        }
        public Object getItem(int i) {
            return stickers.get(i);
        }
        public long getItemId(int i) {
            return stickers.get(i).id;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = new StickerEmojiCell(context) {
                    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(82), MeasureSpec.EXACTLY));
                    }
                };
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            TLRPC.Document document = ((StickerEmojiCell) v).getSticker();
                            Integer count = stickersUseHistory.get(document.id);
                            if (count == null) {
                                count = 0;
                            }
                            stickersUseHistory.put(document.id, ++count);
                            saveRecentStickers();
                            listener.onStickerSelected(document);
                        }
                    }
                });
            }
            ((StickerEmojiCell) view).setSticker(stickers.get(i), false);
            return view;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }
    }

}
