/*
 * This is the source code of TelegramKr for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright http://www.androidpub.com/45681, 2014.
 */

package org.telegramkr.core;

public class SoundSearcher {
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
