package powergram.dlmanager.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import com.koushikdutta.ion.loader.MediaFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.TLRPC.User;
public class DownloadReceiver extends WakefulBroadcastReceiver implements MediaController.FileDownloadProgressListener {
    AlarmManager mAlarmManager;
    PendingIntent mPendingIntent;
    PendingIntent mPendingIntent_end;
    ArrayList<MessageObject> messageObjects;
    private DispatchQueue storageQueue;

    public DownloadReceiver() {
        this.messageObjects = new ArrayList();
        this.messageObjects = DM_LoadMessages();
    }

    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);
        if (preferences.getBoolean("download_receiver", false)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (intent.getIntExtra("start_end", 0) == 1000) {
                if (preferences.getBoolean("download_ewifi", false)) {
                    wifiManager.setWifiEnabled(true);
                }
                WakeLocker.acquire(context);
                startDownloading(this.messageObjects);
                return;
            }
            if (preferences.getBoolean("download_dwifi", false)) {
                wifiManager.setWifiEnabled(false);
            }
            stopDownloading(this.messageObjects);
            WakeLocker.release();
        }
    }

    public void setAlarm(Context context, Calendar calendar, Calendar calendar2, int i) {
        Intent intent = new Intent(context, DownloadReceiver.class);
        intent.putExtra("Reminder_ID", 100);
        intent.putExtra("start_end", 1000);
        this.mPendingIntent = PendingIntent.getBroadcast(context, 100, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        this.mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.mAlarmManager.set(2, (calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) + SystemClock.elapsedRealtime(), this.mPendingIntent);
        Intent intent2 = new Intent(context, DownloadReceiver.class);
        intent.putExtra("Reminder_ID", 200);
        intent.putExtra("start_end", 900);
        this.mPendingIntent_end = PendingIntent.getBroadcast(context, 200, intent2,  PendingIntent.FLAG_UPDATE_CURRENT);
        this.mAlarmManager.set(2, (calendar2.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) + SystemClock.elapsedRealtime(), this.mPendingIntent_end);
    }

    public void setRepeatAlarm(Context context, Calendar calendar, Calendar calendar2, int i) {
        Intent intent = new Intent(context, DownloadReceiver.class);
        intent.putExtra("start_end", 1000);
        this.mPendingIntent = PendingIntent.getBroadcast(context, i, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        this.mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.mAlarmManager.setRepeating(2, (calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) + SystemClock.elapsedRealtime(), 604800000, this.mPendingIntent);
        intent = new Intent(context, DownloadReceiver.class);
        intent.putExtra("start_end", 900);
        this.mPendingIntent_end = PendingIntent.getBroadcast(context, i + 10, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        this.mAlarmManager.setRepeating(2, (calendar2.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) + SystemClock.elapsedRealtime(), 604800000, this.mPendingIntent_end);
    }

    public void cancelAlarm(Context context) {
        this.mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.mPendingIntent = PendingIntent.getBroadcast(context, 100, new Intent(context, DownloadReceiver.class), 0);
        this.mAlarmManager.cancel(this.mPendingIntent);
        this.mPendingIntent_end = PendingIntent.getBroadcast(context, 200, new Intent(context, DownloadReceiver.class), 0);
        this.mAlarmManager.cancel(this.mPendingIntent_end);
        for (int i = 1; i < 8; i++) {
            this.mPendingIntent = PendingIntent.getBroadcast(context, i + MediaFile.FILE_TYPE_DTS, new Intent(context, DownloadReceiver.class), 0);
            this.mAlarmManager.cancel(this.mPendingIntent);
            this.mPendingIntent_end = PendingIntent.getBroadcast(context, (i + MediaFile.FILE_TYPE_DTS) + 10, new Intent(context, DownloadReceiver.class), 0);
            this.mAlarmManager.cancel(this.mPendingIntent_end);
        }
    }

    public TLObject getDownloadObject(MessageObject messageObject) {
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media != null) {
            if (media.document != null) {
                return media.document;
            }
            if (media.webpage != null && media.webpage.document != null) {
                return media.webpage.document;
            }
            if (media.webpage != null && media.webpage.photo != null) {
                return FileLoader.getClosestPhotoSizeWithSize(media.webpage.photo.sizes, AndroidUtilities.getPhotoSize());
            }
            if (media.photo != null) {
                return FileLoader.getClosestPhotoSizeWithSize(media.photo.sizes, AndroidUtilities.getPhotoSize());
            }
        }
        return new TLRPC.TL_messageMediaEmpty();
    }

    private void loadFile(TLObject attach) {
        if (attach instanceof TLRPC.PhotoSize) {
            FileLoader.getInstance().loadFile((TLRPC.PhotoSize) attach, null, false);
        } else if (attach instanceof TLRPC.Document) {
            FileLoader.getInstance().loadFile(( TLRPC.Document) attach, true, false);
        }
    }

    private void startDownloading(final ArrayList<MessageObject> messageObjects) {
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {

            class C34181 implements Runnable {
                C34181() {
                }

                public void run() {
                    ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit().putBoolean("download_running", true).commit();
                    Iterator it = messageObjects.iterator();
                    while (it.hasNext()) {
                        MessageObject messageObject = (MessageObject) it.next();
                        TLObject attach = DownloadReceiver.this.getDownloadObject(messageObject);
                        DownloadReceiver.this.loadFile(attach);
                        File pathToMessage = FileLoader.getPathToMessage(messageObject.messageOwner);
                        if (pathToMessage != null && !pathToMessage.exists()) {
                            MediaController.getInstance().addLoadingFileObserver(FileLoader.getAttachFileName(attach), DownloadReceiver.this);
                            return;
                        }
                    }
                }
            }

            public void run() {
                AndroidUtilities.runOnUIThread(new C34181());
            }
        });
    }

    private void stopDownloading(ArrayList<MessageObject> messageObjects) {
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit().putBoolean("download_running", false).commit();
        for (int i = 0; i < messageObjects.size(); i++) {
            MessageObject messageObject = (MessageObject) messageObjects.get(i);
            if (messageObject != null) {
                TLObject attach = getDownloadObject(messageObject);
                if (attach instanceof TLRPC.PhotoSize) {
                    FileLoader.getInstance().cancelLoadFile((TLRPC.PhotoSize) attach);
                } else if (attach instanceof  TLRPC.Document) {
                    FileLoader.getInstance().cancelLoadFile(( TLRPC.Document) attach);
                }
            }
        }
    }

    public ArrayList<MessageObject> DM_LoadMessages() {
        final ArrayList<MessageObject> objects = new ArrayList();
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                HashMap<Integer, User> usersDict;
                HashMap<Integer, TLRPC.Chat> chatsDict;
                int a;
                TLRPC.TL_messages_messages res = new TLRPC.TL_messages_messages();
                SQLiteCursor cursor = null;
                try {
                    cursor = MessagesStorage.getInstance().getDatabase().queryFinalized(String.format(Locale.US, "SELECT * FROM turbo_idm ORDER BY date DESC"));
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(3);
                        if (data != null) {
                            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                            data.reuse();
                            message.id = cursor.intValue(0);
                            message.dialog_id = (long) cursor.intValue(1);
                            message.date = cursor.intValue(2);
                            res.messages.add(message);
                        }
                    }
                    if (cursor != null) {
                        cursor.dispose();
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                    usersDict = new HashMap();
                    chatsDict = new HashMap();
                    for (a = 0; a < res.users.size(); a++) {
                        User u = (User) res.users.get(a);
                        usersDict.put(Integer.valueOf(u.id), u);
                    }
                    for (a = 0; a < res.chats.size(); a++) {
                        TLRPC.Chat c = (TLRPC.Chat) res.chats.get(a);
                        chatsDict.put(Integer.valueOf(c.id), c);
                    }
                    for (a = 0; a < res.messages.size(); a++) {
                        objects.add(new MessageObject((TLRPC.Message) res.messages.get(a), usersDict, chatsDict, true));
                    }
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
                usersDict = new HashMap();
                chatsDict = new HashMap();
                for (a = 0; a < res.users.size(); a++) {
                    User u2 = (User) res.users.get(a);
                    usersDict.put(Integer.valueOf(u2.id), u2);
                }
                for (a = 0; a < res.chats.size(); a++) {
                    TLRPC.Chat c2 = (TLRPC.Chat) res.chats.get(a);
                    chatsDict.put(Integer.valueOf(c2.id), c2);
                }
                for (a = 0; a < res.messages.size(); a++) {
                    objects.add(new MessageObject((TLRPC.Message) res.messages.get(a), usersDict, chatsDict, true));
                }
            }
        });
        return objects;
    }

    public void onFailedDownload(String fileName) {
    }

    public void onSuccessDownload(String fileName) {
        startDownloading(this.messageObjects);
    }

    public void onProgressDownload(String fileName, float progress) {
    }

    public void onProgressUpload(String fileName, float progress, boolean isEncrypted) {
    }

    public int getObserverTag() {
        return 0;
    }
}
