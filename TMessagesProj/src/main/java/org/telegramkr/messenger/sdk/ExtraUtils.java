package org.telegramkr.messenger.sdk;

import org.telegram.android.LocaleController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtraUtils {
    public static String getPhoneNumber(String phoneNumber){
        Pattern p = Pattern.compile("82");
        Matcher m = p.matcher(phoneNumber);
        if (m.find(0)) {
            StringBuffer sb = new StringBuffer();
            m.appendReplacement(sb, "0");
            m.appendTail(sb);
            return sb.toString();
        } else {
            return "+" + phoneNumber;
        }
    }

    public static String getNames(String dbName){
        String name = dbName;
        if (LocaleController.getCurrentLanguageCode().equals("ko")) {
            try {
                String[] splitUserName = name.split(";;;");
                String[] names = splitUserName[0].split(" ");
                if (names.length > 1) {
                    name = names[1] + names[0];
                }
            }catch(Exception e){
            }
        }
        return name;
    }
}
