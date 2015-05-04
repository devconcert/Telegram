/*
 * This is the source code of Telegram for Android v. 2.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2015.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.makeday.emoticonsdk.EmojiconHandler;

import org.telegram.android.AndroidUtilities;
import org.telegram.android.Emoji;
import org.telegram.android.query.StickersQuery;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.TLRPC;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class StickerEmojiCell extends FrameLayout {

    private BackupImageView imageView;
    private TLRPC.Document sticker;
    private TextView emojiTextView;

    public StickerEmojiCell(Context context) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        addView(imageView, LayoutHelper.createFrame(66, 66, Gravity.CENTER));

        emojiTextView = new TextView(context);
        emojiTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        addView(emojiTextView, LayoutHelper.createFrame(28, 28, Gravity.BOTTOM | Gravity.RIGHT));
    }

    @Override
    public void setPressed(boolean pressed) {
        if (imageView.getImageReceiver().getPressed() != pressed) {
            imageView.getImageReceiver().setPressed(pressed);
            imageView.invalidate();
        }
        super.setPressed(pressed);
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public void setSticker(TLRPC.Document document, boolean showEmoji) {
        if (document != null) {
            sticker = document;
            imageView.setImage(document.thumb.location, null, "webp", null);


            if (showEmoji) {
                boolean set = false;
                for (TLRPC.DocumentAttribute attribute : document.attributes) {
                    if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                        if (attribute.alt != null && attribute.alt.length() > 0) {
//                            emojiTextView.setText(Emoji.replaceEmoji(attribute.alt, emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
                            SpannableStringBuilder builder = new SpannableStringBuilder(attribute.alt);
                            EmojiconHandler.addEmojis(ApplicationLoader.applicationContext, builder, AndroidUtilities.dp(14), emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16));
                            emojiTextView.setText(builder);
                            set = true;
                        }
                        break;
                    }
                }
                if (!set) {
//                    emojiTextView.setText(Emoji.replaceEmoji(StickersQuery.getEmojiForSticker(sticker.id), emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16)));
                    SpannableStringBuilder builder = new SpannableStringBuilder(StickersQuery.getEmojiForSticker(sticker.id));
                    EmojiconHandler.addEmojis(ApplicationLoader.applicationContext, builder, AndroidUtilities.dp(14), emojiTextView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(16));
                    emojiTextView.setText(builder);
                }
                emojiTextView.setVisibility(VISIBLE);
            } else {
                emojiTextView.setVisibility(INVISIBLE);
            }
        }
    }
}
