/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.android.AndroidUtilities;
import org.telegram.messenger.TLRPC;
import org.telegram.android.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.android.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Cells.ChatOrUserCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ContactsActivitySearchAdapter extends BaseFragmentAdapter {
    private Context mContext;
    private HashMap<Integer, TLRPC.User> ignoreUsers;
    private ArrayList<TLRPC.User> searchResult;
    private ArrayList<CharSequence> searchResultNames;
    private Timer searchTimer;

    public ContactsActivitySearchAdapter(Context context, HashMap<Integer, TLRPC.User> arg1) {
        mContext = context;
        ignoreUsers = arg1;
    }

    public void searchDialogs(final String query) {
        if (query == null) {
            searchResult = null;
            searchResultNames = null;
            notifyDataSetChanged();
        } else {
            try {
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
            searchTimer = new Timer();
            searchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        searchTimer.cancel();
                        searchTimer = null;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                    }
                    processSearch(query);
                }
            }, 100, 300);
        }
    }

    public static class SoundSearcher {
        public SoundSearcher() { }

        private static final char HANGUL_BEGIN_UNICODE = 44032;
        private static final char HANGUL_LAST_UNICODE = 55203;
        private static final char HANGUL_BASE_UNIT = 588;
        private static final char[] INITIAL_SOUND = { 'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ' };

        private static boolean isInitialSound(char ichar){
            for(char c:INITIAL_SOUND){
                if(c == ichar) return true;
            }
            return false;
        }

        private static char getInitialSound(char c) {
            int hanBegin = (c - HANGUL_BEGIN_UNICODE);
            int index = hanBegin / HANGUL_BASE_UNIT;
            return INITIAL_SOUND[index];
        }

        private static boolean isKorean(char c) {
            return HANGUL_BEGIN_UNICODE <= c && c <= HANGUL_LAST_UNICODE;
        }

        public static boolean matchString(String value, String search){
            int seof = value.length() - search.length();
            int slen = search.length();
            if(seof < 0)
                return false;
            for(int i = 0, t = 0;i <= seof;i++){
                while(t < slen){
                    if(isInitialSound(search.charAt(t))== true && isKorean(value.charAt(i+t))){
                        if(getInitialSound(value.charAt(i+t)) == search.charAt(t))
                            t++;
                        else
                            break;
                    } else {
                        if(value.charAt(i+t) == search.charAt(t))
                            t++;
                        else
                            break;
                    }
                }
                if(t == slen)
                    return true;

                t = 0;
            }
            return false;
        }
    }

    private void processSearch(final String query) {
        AndroidUtilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<TLRPC.TL_contact> contactsCopy = new ArrayList<TLRPC.TL_contact>();
                contactsCopy.addAll(ContactsController.getInstance().contacts);
                Utilities.searchQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        String q = query.trim().toLowerCase();
                        if (q.length() == 0) {
                            updateSearchResults(new ArrayList<TLRPC.User>(), new ArrayList<CharSequence>());
                            return;
                        }
                        long time = System.currentTimeMillis();
                        ArrayList<TLRPC.User> resultArray = new ArrayList<TLRPC.User>();
                        ArrayList<CharSequence> resultArrayNames = new ArrayList<CharSequence>();

                        for (TLRPC.TL_contact contact : contactsCopy) {
                            TLRPC.User user = MessagesController.getInstance().getUser(contact.user_id);
                            String name = ContactsController.formatName(user.first_name, user.last_name).toLowerCase();
                            if (SoundSearcher.matchString(name, q) || name.startsWith(q) || name.contains(" " + q)) {
                                if (user.id == UserConfig.getClientUserId()) {
                                    continue;
                                }
                                resultArrayNames.add(Utilities.generateSearchName(user.first_name, user.last_name, q));
                                resultArray.add(user);
                            }
                        }

                        updateSearchResults(resultArray, resultArrayNames);
                    }
                });
            }
        });
    }

    private void updateSearchResults(final ArrayList<TLRPC.User> users, final ArrayList<CharSequence> names) {
        AndroidUtilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                searchResult = users;
                searchResultNames = names;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public int getCount() {
        if (searchResult == null) {
            return 0;
        }
        return searchResult.size();
    }

    @Override
    public TLRPC.User getItem(int i) {
        if (searchResult != null) {
            if (i >= 0 && i < searchResult.size()) {
                return searchResult.get(i);
            }
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = new ChatOrUserCell(mContext);
            ((ChatOrUserCell)view).usePadding = false;
        }

        ((ChatOrUserCell) view).useSeparator = i != searchResult.size() - 1;

        Object obj = searchResult.get(i);
        TLRPC.User user = MessagesController.getInstance().getUser(((TLRPC.User)obj).id);

        if (user != null) {
            ((ChatOrUserCell)view).setData(user, null, null, searchResultNames.get(i), null);

            if (ignoreUsers != null) {
                if (ignoreUsers.containsKey(user.id)) {
                    ((ChatOrUserCell)view).drawAlpha = 0.5f;
                } else {
                    ((ChatOrUserCell)view).drawAlpha = 1.0f;
                }
            }
        }
        return view;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return searchResult == null || searchResult.size() == 0;
    }
}
