package org.telegramkr.messenger.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import java.util.ArrayList;

import org.telegram.android.AndroidUtilities;
import org.telegram.android.ContactsController;
import org.telegram.android.MessagesController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.RPCRequest;
import org.telegram.messenger.TLObject;
import org.telegram.messenger.TLRPC;

public class PrivacyManager {
    private static int currentType = 0;
    private static ArrayList<Integer> currentPlus = new ArrayList<>();
    private static ArrayList<Integer> currentMinus = new ArrayList<>();
    private static AlertDialog visibleDialog = null;
    public static void checkPrivacyLastSeen(){
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean showed = preferences.getBoolean("privacyLastSeen", false);
        if (!showed) {
            privacyLastSeen();
        }
    }
    private static void privacyLastSeen(){
        TLRPC.TL_account_getPrivacy req = new TLRPC.TL_account_getPrivacy();
        req.key = new TLRPC.TL_inputPrivacyKeyStatusTimestamp();
        ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
            @Override
            public void run(final TLObject response, final TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (error == null) {

                            TLRPC.TL_account_privacyRules rules = (TLRPC.TL_account_privacyRules) response;
                            MessagesController.getInstance().putUsers(rules.users, false);
                            ArrayList<TLRPC.PrivacyRule> privacyRules = rules.rules;
                            if (privacyRules.size() == 0) {
                                currentType=1;
                                return;
                            }
                            int type = -1;
                            for (TLRPC.PrivacyRule rule : privacyRules) {
                                if (rule instanceof TLRPC.TL_privacyValueAllowUsers) {
                                    currentPlus.addAll(rule.users);
                                } else if (rule instanceof TLRPC.TL_privacyValueDisallowUsers) {
                                    currentMinus.addAll(rule.users);
                                } else if (rule instanceof TLRPC.TL_privacyValueAllowAll) {
                                    type = 0;
                                } else if (rule instanceof TLRPC.TL_privacyValueDisallowAll) {
                                    type = 1;
                                } else {
                                    type = 2;
                                }
                            }
                            if (type == 0 || type == -1 && currentMinus.size() > 0) {
                                currentType = 0;
                            } else if (type == 2 || type == -1 && currentMinus.size() > 0 && currentPlus.size() > 0) {
                                currentType = 2;
                            } else if (type == 1 || type == -1 && currentPlus.size() > 0) {
                                currentType = 1;
                            }
                            if (currentType == 0 || currentType == 2) {
                                currentType = 1;
                                applyCurrentPrivacySettings();
                            }
                        }
                    }
                });
            }
        });
    }

    private static void applyCurrentPrivacySettings() {
        TLRPC.TL_account_setPrivacy req = new TLRPC.TL_account_setPrivacy();
        req.key = new TLRPC.TL_inputPrivacyKeyStatusTimestamp();
        if (currentType != 0 && currentPlus.size() > 0) {
            TLRPC.TL_inputPrivacyValueAllowUsers rule = new TLRPC.TL_inputPrivacyValueAllowUsers();
            for (Integer uid : currentPlus) {
                TLRPC.User user = MessagesController.getInstance().getUser(uid);
                if (user != null) {
                    TLRPC.InputUser inputUser = MessagesController.getInputUser(user);
                    if (inputUser != null) {
                        rule.users.add(inputUser);
                    }
                }
            }
            req.rules.add(rule);
        }
        if (currentType != 1 && currentMinus.size() > 0) {
            TLRPC.TL_inputPrivacyValueDisallowUsers rule = new TLRPC.TL_inputPrivacyValueDisallowUsers();
            for (Integer uid : currentMinus) {
                TLRPC.User user = MessagesController.getInstance().getUser(uid);
                if (user != null) {
                    TLRPC.InputUser inputUser = MessagesController.getInputUser(user);
                    if (inputUser != null) {
                        rule.users.add(inputUser);
                    }
                }
            }
            req.rules.add(rule);
        }
        if (currentType == 0) {
            req.rules.add(new TLRPC.TL_inputPrivacyValueAllowAll());
        } else if (currentType == 1) {
            req.rules.add(new TLRPC.TL_inputPrivacyValueDisallowAll());
        } else if (currentType == 2) {
            req.rules.add(new TLRPC.TL_inputPrivacyValueAllowContacts());
        }

        ConnectionsManager.getInstance().performRpc(req, new RPCRequest.RPCRequestDelegate() {
            @Override
            public void run(final TLObject response, final TLRPC.TL_error error) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (error == null) {
                            TLRPC.TL_account_privacyRules rules = (TLRPC.TL_account_privacyRules) response;
                            MessagesController.getInstance().putUsers(rules.users, false);
                            ContactsController.getInstance().setPrivacyRules(rules.rules);

                            ApplicationLoader.applicationContext.
                                    getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).
                                    edit().putBoolean("privacyLastSeen", true)
                                    .commit();
                        }
                    }
                });
            }
        }, true, RPCRequest.RPCRequestClassGeneric | RPCRequest.RPCRequestClassFailOnServerErrors);
    }
    protected static void showAlertDialog(AlertDialog.Builder builder) {
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
        }
        try {
            visibleDialog = builder.show();
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    visibleDialog = null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
