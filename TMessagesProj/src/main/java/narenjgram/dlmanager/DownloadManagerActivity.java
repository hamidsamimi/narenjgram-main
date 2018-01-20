package powergram.dlmanager;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.TextUtils.TruncateAt;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import org.joda.time.DateTimeConstants;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationCenter.NotificationCenterDelegate;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.exoplayer2.util.MimeTypes;
import org.telegram.messenger.query.DraftQuery;
import org.telegram.messenger.query.MessagesQuery;
import org.telegram.messenger.query.MessagesSearchQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.messenger.support.widget.RecyclerView.Adapter;
import org.telegram.messenger.support.widget.RecyclerView.LayoutParams;
import org.telegram.messenger.support.widget.RecyclerView.OnScrollListener;
import org.telegram.messenger.support.widget.RecyclerView.ViewHolder;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLObject;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.TLRPC.User;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet.Builder;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.StickersAdapter;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.Cells.BotHelpCell;
import org.telegram.ui.Cells.BotHelpCell.BotHelpCellDelegate;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatActionCell.ChatActionCellDelegate;
import org.telegram.ui.Cells.ChatLoadingCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ChatMessageCell.ChatMessageCellDelegate;
import org.telegram.ui.Cells.ChatUnreadCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.ChatBigEmptyView;
import org.telegram.ui.Components.ContextProgressView;
import org.telegram.ui.Components.EmbedBottomSheet;
import org.telegram.ui.Components.ExtendedGridLayoutManager;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.RecyclerListView.OnInterceptTouchListener;
import org.telegram.ui.Components.RecyclerListView.OnItemClickListener;
import org.telegram.ui.Components.RecyclerListView.OnItemLongClickListener;
import org.telegram.ui.Components.ShareAlert;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.URLSpanBotCommand;
import org.telegram.ui.Components.URLSpanMono;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.URLSpanUserMention;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.DialogsActivity.DialogsActivityDelegate;
import org.telegram.ui.LocationActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PhotoViewer.PhotoViewerProvider;
import org.telegram.ui.PhotoViewer.PlaceProviderObject;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.StickerPreviewViewer;

import ir.powergram.messenger.R;

public class DownloadManagerActivity extends BaseFragment implements NotificationCenterDelegate, DialogsActivityDelegate, PhotoViewerProvider, MediaController.FileDownloadProgressListener {
    private static final int copy = 10;
    private static final int delete = 13;
    private static final int delete_all = 2;
    private static final int delete_chat = 14;
    private static final int delete_downloaded = 1;
    private static final int forward = 12;
    private static final int id_chat_compose_panel = 1000;
    private static final int quoteforward = 11;
    private static final int settings = 3;
    private SimpleTextView actionModeSubTextView;
    private SimpleTextView actionModeTextView;
    private FrameLayout actionModeTitleContainer;
    private ArrayList<View> actionModeViews = new ArrayList();
    private TextView addContactItem;
    private TextView addToContactsButton;
    private TextView alertNameTextView;
    private TextView alertTextView;
    private FrameLayout alertView;
    private AnimatorSet alertViewAnimator;
    private boolean allowContextBotPanel;
    private boolean allowContextBotPanelSecond = true;
    private boolean allowStickersPanel;
    private ActionBarMenuItem attachItem;
    private ChatAvatarContainer avatarContainer;
    private ChatBigEmptyView bigEmptyView;
    private MessageObject botButtons;
    private PhotoViewerProvider botContextProvider = new C33941();
    private ArrayList<Object> botContextResults;
    private HashMap<Integer, TLRPC.BotInfo> botInfo = new HashMap();
    private MessageObject botReplyButtons;
    private String botUser;
    private int botsCount;
    private FrameLayout bottomOverlay;
    private FrameLayout bottomOverlayChat;
    private TextView bottomOverlayChatText;
    private TextView bottomOverlayText;
    private boolean[] cacheEndReached = new boolean[2];
    private int cantDeleteMessagesCount;
    private ChatActivityAdapter chatAdapter;
    private ChatAttachAlert chatAttachAlert;
    private long chatEnterTime = 0;
    private LinearLayoutManager chatLayoutManager;
    private long chatLeaveTime = 0;
    private RecyclerListView chatListView;
    private ArrayList<ChatMessageCell> chatMessageCellsCache = new ArrayList();
    private Dialog closeChatDialog;
    private String currentPicturePath;
    private long dialog_id;
    boolean downloaderRunning;
    HashMap<MessageObject, Integer> f534e = new HashMap();
    private AnimatorSet editDoneItemAnimation;
    private ContextProgressView editDoneItemProgress;
    private int editingMessageObjectReqId;
    private View emojiButtonRed;
    private FrameLayout emptyViewContainer;
    private boolean[] endReached = new boolean[2];
    HashMap<Long, List<MessageObject>> f535f = new HashMap();
    private boolean first = true;
    private boolean firstLoading = true;
    private int first_unread_id;
    private boolean forceScrollToTop;
    private boolean[] forwardEndReached = new boolean[]{true, true};
    private ArrayList<MessageObject> forwardingMessages;
    private MessageObject forwaringMessage;
    private ArrayList<CharSequence> foundUrls;
    private TLRPC.WebPage foundWebPage;
    private FragmentContextView fragmentContextView;
    HashMap<Long, MessageObject> f536g = new HashMap();
    private TextView gifHintTextView;
    HashSet<Long> f537h = new HashSet();
    private boolean hasBotsCommands;
    private ActionBarMenuItem headerItem;
    private Runnable hideAlertViewRunnable;
    private int highlightMessageId = Integer.MAX_VALUE;
    HashSet<Long> f538i = new HashSet();
    protected TLRPC.ChatFull info = null;
    private long inlineReturn;
    private int lastLoadIndex;
    private int last_message_id = 0;
    private int linkSearchRequestId;
    private boolean loading;
    private boolean loadingForward;
    private int loadingPinnedMessage;
    private int loadsCount;
    private int[] maxDate = new int[]{MeasureSpec.AT_MOST, MeasureSpec.AT_MOST};
    private int[] maxMessageId = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE};
    private ExtendedGridLayoutManager mentionGridLayoutManager;
    private LinearLayoutManager mentionLayoutManager;
    private AnimatorSet mentionListAnimation;
    private boolean mentionListViewIgnoreLayout;
    private boolean mentionListViewIsScrolling;
    private int mentionListViewLastViewPosition;
    private int mentionListViewLastViewTop;
    private int mentionListViewScrollOffsetY;
    private OnItemClickListener mentionsOnItemClickListener;
    private ActionBarMenuItem menuItem;
    private long mergeDialogId;
    protected ArrayList<MessageObject> messages = new ArrayList();
    private HashMap<String, ArrayList<MessageObject>> messagesByDays = new HashMap();
    private HashMap<Integer, MessageObject>[] messagesDict = new HashMap[]{new HashMap(), new HashMap()};
    private int[] minDate = new int[2];
    private int[] minMessageId = new int[]{MeasureSpec.AT_MOST, MeasureSpec.AT_MOST};
    private TextView muteItem;
    private boolean needSelectFromMessageId;
    private int newUnreadMessageCount;
    OnItemClickListener onItemClickListener = new C33973();
    OnItemLongClickListener onItemLongClickListener = new C33952();
    private boolean openAnimationEnded;
    private boolean openSearchKeyboard;
    private FrameLayout pagedownButton;
    private ObjectAnimator pagedownButtonAnimation;
    private TextView pagedownButtonCounter;
    private boolean pagedownButtonShowedByScroll;
    private boolean paused = true;
    private String pendingLinkSearchString;
    private Runnable pendingWebPageTimeoutRunnable;
    private  TLRPC.FileLocation pinnedImageLocation;
    private BackupImageView pinnedMessageImageView;
    private SimpleTextView pinnedMessageNameTextView;
    private MessageObject pinnedMessageObject;
    private SimpleTextView pinnedMessageTextView;
    private FrameLayout pinnedMessageView;
    private AnimatorSet pinnedMessageViewAnimator;
    private RadialProgressView progressBar;
    private FrameLayout progressView;
    private View progressView2;
    private boolean readWhenResume = false;
    private int readWithDate;
    private int readWithMid;
    private AnimatorSet replyButtonAnimation;
    private ImageView replyCloseImageView;
    private ImageView replyIconImageView;
    private  TLRPC.FileLocation replyImageLocation;
    private BackupImageView replyImageView;
    private SimpleTextView replyNameTextView;
    private SimpleTextView replyObjectTextView;
    private MessageObject replyingMessageObject;
    private TextView reportSpamButton;
    private FrameLayout reportSpamContainer;
    private LinearLayout reportSpamView;
    private AnimatorSet reportSpamViewAnimator;
    private int returnToLoadIndex;
    private int returnToMessageId;
    private AnimatorSet runningAnimation;
    private MessageObject scrollToMessage;
    private int scrollToMessagePosition = -10000;
    private boolean scrollToTopOnResume;
    private boolean scrollToTopUnReadOnResume;
    private FrameLayout searchContainer;
    private SimpleTextView searchCountText;
    private ImageView searchDownButton;
    private ImageView searchUpButton;
    private HashMap<Integer, MessageObject>[] selectedMessagesCanCopyIds = new HashMap[]{new HashMap(), new HashMap()};
    private NumberTextView selectedMessagesCountTextView;
    private HashMap<Integer, MessageObject>[] selectedMessagesIds = new HashMap[]{new HashMap(), new HashMap()};
    private MessageObject selectedObject;
    private int startLoadFromMessageId;
    private String startVideoEdit = null;
    private float startX = 0.0f;
    private float startY = 0.0f;
    private StickersAdapter stickersAdapter;
    private RecyclerListView stickersListView;
    private OnItemClickListener stickersOnItemClickListener;
    private FrameLayout stickersPanel;
    private ImageView stickersPanelArrow;
    private View timeItem2;
    private MessageObject unreadMessageObject;
    private int unread_to_load;
    private boolean userBlocked = false;
    private Runnable waitingForCharaterEnterRunnable;
    private ArrayList<Integer> waitingForLoad = new ArrayList();
    private boolean waitingForReplyMessageLoad;
    private boolean wasPaused = false;

    class C33941 implements PhotoViewerProvider {
        C33941() {
        }

        public PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
            return (index < 0 || index >= botContextResults.size()) ? null : null;
        }

        public Bitmap getThumbForPhoto(MessageObject messageObject,  TLRPC.FileLocation fileLocation, int index) {
            return null;
        }

        public void willSwitchFromPhoto(MessageObject messageObject,  TLRPC.FileLocation fileLocation, int index) {
        }

        public void willHidePhotoViewer() {
        }

        public boolean isPhotoChecked(int index) {
            return false;
        }

        public void setPhotoChecked(int index) {
        }

        public boolean cancelButtonPressed() {
            return false;
        }

        public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo) {
            if (index >= 0 && index < botContextResults.size()) {
            }
        }

        public int getSelectedCount() {
            return 0;
        }

        public void updatePhotoAtIndex(int index) {
        }

        public boolean scaleToFill() {
            return false;
        }

        public boolean allowCaption() {
            return true;
        }
    }

    class C33952 implements OnItemLongClickListener {
        C33952() {
        }

        public boolean onItemClick(View view, int position) {
            if (actionBar.isActionModeShowed()) {
                return false;
            }
            createMenu(view, false);
            return true;
        }
    }

    class C33973 implements OnItemClickListener {
        C33973() {
        }

        public void onItemClick(View view, int position) {
            if (actionBar.isActionModeShowed()) {
                processRowSelect(view);
            } else {
                createMenu(view, true);
            }
        }
    }

    class C34025 implements OnTouchListener {
        C34025() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

    class C34047 implements OnTouchListener {
        C34047() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

    class C34069 implements OnTouchListener {
        C34069() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

    public class ChatActivityAdapter extends Adapter {
        private int botInfoRow = -1;
        private int loadingDownRow;
        private int loadingUpRow;
        private Context mContext;
        private int messagesEndRow;
        private int messagesStartRow;
        private int rowCount;






        private class Holder extends ViewHolder {
            public Holder(View itemView) {
                super(itemView);
            }
        }

        public ChatActivityAdapter(Context context) {
            this.mContext = context;
        }

        public void updateRows() {
            this.rowCount = 0;
            this.botInfoRow = -1;
            if (messages.isEmpty()) {
                this.loadingUpRow = -1;
                this.loadingDownRow = -1;
                this.messagesStartRow = -1;
                this.messagesEndRow = -1;
                return;
            }
            if (endReached[0] && endReached[1]) {
                this.loadingUpRow = -1;
            } else {
                int i = this.rowCount;
                this.rowCount = i + 1;
                this.loadingUpRow = i;
            }
            this.messagesStartRow = this.rowCount;
            this.rowCount += messages.size();
            this.messagesEndRow = this.rowCount;
            if (forwardEndReached[0] && forwardEndReached[1]) {
                this.loadingDownRow = -1;
                return;
            }

           rowCount = rowCount + 1;
          loadingDownRow = rowCount;
        }

        public int getItemCount() {
            return this.rowCount;
        }

        public long getItemId(int i) {
            return -1;
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            if (viewType == 0) {
                if (chatMessageCellsCache.isEmpty()) {
                    view = new ChatMessageCell(this.mContext);
                } else {
                    view = (View) chatMessageCellsCache.get(0);
                    chatMessageCellsCache.remove(0);
                }
                ChatMessageCell chatMessageCell = (ChatMessageCell) view;
                chatMessageCell.setDelegate(new ChatMessageCellDelegate() {
                    public void didPressedShare(ChatMessageCell cell) {
                        if (getParentActivity() != null) {
                            Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit();
                            editor.putInt("forward_type", 1);
                            editor.commit();
                            ArrayList<MessageObject> msgObj = new ArrayList();
                            msgObj.add(cell.getMessageObject());
                            showDialog(new ShareAlert(mContext, cell.getMessageObject(),null,false,null, true));
                        }
                    }

                    public boolean needPlayAudio(MessageObject messageObject) {
                        if (!messageObject.isVoice()) {
                            return messageObject.isMusic() ? MediaController.getInstance().setPlaylist(messages, messageObject) : false;
                        } else {
                            boolean result = MediaController.getInstance().playAudio(messageObject);
                            MediaController.getInstance().setVoiceMessagesPlaylist(result ? createVoiceMessagesPlaylist(messageObject, false) : null, false);
                            return result;
                        }
                    }

                    public void didPressedChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId) {
                    }

                    public void didPressedOther(ChatMessageCell cell) {
                        createMenu(cell, true);
                    }

                    public void didPressedUserAvatar(ChatMessageCell cell, User user) {
                    }

                    public void didPressedBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {
                        if (getParentActivity() != null && bottomOverlayChat.getVisibility() == View.VISIBLE && !(button instanceof TLRPC.TL_keyboardButtonSwitchInline) && !(button instanceof TLRPC.TL_keyboardButtonCallback) && !(button instanceof TLRPC.TL_keyboardButtonGame) && (button instanceof TLRPC.TL_keyboardButtonUrl)) {
                        }
                    }

                    public void didPressedInstantButton(ChatMessageCell cell) {
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.webpage != null && messageObject.messageOwner.media.webpage.cached_page != null) {
                            ArticleViewer.getInstance().setParentActivity(getParentActivity());
                            ArticleViewer.getInstance().open(messageObject);
                        }
                    }

                    public void didPressedCancelSendButton(ChatMessageCell cell) {
                        MessageObject message = cell.getMessageObject();
                        if (message.messageOwner.send_state != 0) {
                            SendMessagesHelper.getInstance().cancelSendingMessage(message);
                        }
                    }

                    public void didLongPressed(ChatMessageCell cell) {
                        createMenu(cell, false);
                    }

                    public boolean canPerformActions() {
                        return (actionBar == null || actionBar.isActionModeShowed()) ? false : true;
                    }

                    public void didPressedUrl(MessageObject messageObject, CharacterStyle url, boolean longPress) {
                        if (url != null) {
                            if (url instanceof URLSpanMono) {
                                ((URLSpanMono) url).copyToClipboard();
                                Toast.makeText(getParentActivity(), LocaleController.getString("TextCopied", R.string.TextCopied), Toast.LENGTH_LONG).show();
                            } else if (url instanceof URLSpanUserMention) {
                                User user = MessagesController.getInstance().getUser(Utilities.parseInt(((URLSpanUserMention) url).getURL()));
                                if (user != null) {
                                    MessagesController.openChatOrProfileWith(user, null, DownloadManagerActivity.this, 0, false);
                                }
                            } else if (url instanceof URLSpanNoUnderline) {
                                String str = ((URLSpanNoUnderline) url).getURL();
                                if (str.startsWith("@")) {
                                    MessagesController.openByUserName(str.substring(1), DownloadManagerActivity.this, 0);
                                } else if (str.startsWith("#") || !str.startsWith("/") || !URLSpanBotCommand.enabled) {
                                }
                            } else {
                                final String urlFinal = ((URLSpan) url).getURL();
                                if (longPress) {
                                    Builder builder = new Builder(getParentActivity());
                                    builder.setTitle(urlFinal);
                                    builder.setItems(new CharSequence[]{LocaleController.getString("Open", R.string.Open), LocaleController.getString("Copy", R.string.Copy)}, new OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            boolean z = true;
                                            if (which == 0) {
                                                Context parentActivity = getParentActivity();
                                                String str = urlFinal;
                                                if (inlineReturn != 0) {
                                                    z = false;
                                                }
                                                Browser.openUrl(parentActivity, str, z);
                                            } else if (which == 1) {
                                                String url = urlFinal;
                                                if (url.startsWith("mailto:")) {
                                                    url = url.substring(7);
                                                } else if (url.startsWith("tel:")) {
                                                    url = url.substring(4);
                                                }
                                                AndroidUtilities.addToClipboard(url);
                                            }
                                        }
                                    });
                                    showDialog(builder.create());
                                } else if (url instanceof URLSpanReplacement) {
                                    showOpenUrlAlert(((URLSpanReplacement) url).getURL(), true);
                                } else if (url instanceof URLSpan) {
                                    if (!(!(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) || messageObject.messageOwner.media.webpage == null || messageObject.messageOwner.media.webpage.cached_page == null)) {
                                        String lowerUrl = urlFinal.toLowerCase();
                                        String lowerUrl2 = messageObject.messageOwner.media.webpage.url.toLowerCase();
                                        if (lowerUrl.contains("telegra.ph") && (lowerUrl.contains(lowerUrl2) || lowerUrl2.contains(lowerUrl))) {
                                            ArticleViewer.getInstance().setParentActivity(getParentActivity());
                                            ArticleViewer.getInstance().open(messageObject);
                                            return;
                                        }
                                    }
                                    Browser.openUrl(getParentActivity(), urlFinal, inlineReturn == 0);
                                } else if (url instanceof ClickableSpan) {
                                    ((ClickableSpan) url).onClick(fragmentView);
                                }
                            }
                        }
                    }

                    public void needOpenWebView(String url, String title, String description, String originalUrl, int w, int h) {
                        EmbedBottomSheet.show(ChatActivityAdapter.this.mContext, title, description, originalUrl, url, w, h);
                    }

                    public void didPressedReplyMessage(ChatMessageCell cell, int id) {
                        MessageObject messageObject = cell.getMessageObject();
                        scrollToMessageId(id, messageObject.getId(), true, messageObject.getDialogId() == mergeDialogId ? 1 : 0);
                    }

                    public void didPressedViaBot(ChatMessageCell cell, String username) {
                        if ((bottomOverlayChat != null && bottomOverlayChat.getVisibility() == View.VISIBLE) || bottomOverlay == null || bottomOverlay.getVisibility() != View.VISIBLE) {
                        }
                    }

                    public void didPressedImage(ChatMessageCell cell) {
                        MessageObject message = cell.getMessageObject();
                        if (message.isSendError()) {
                            createMenu(cell, false);
                        } else if (!message.isSending()) {
                            if (message.type == 13) {
                                showDialog(new StickersAlert(getParentActivity(), DownloadManagerActivity.this, message.getInputStickerSet(), null, null));
                            } else if ((VERSION.SDK_INT >= 16 && message.isVideo()) || message.type == 1 || ((message.type == 0 && !message.isWebpageDocument()) || message.isGif())) {
                                PhotoViewer.getInstance().setParentActivity(getParentActivity());
                                if (!PhotoViewer.getInstance().openPhoto(message, message.type != 0 ? dialog_id : 0, message.type != 0 ? mergeDialogId : 0, DownloadManagerActivity.this)) {
                                }
                            } else if (message.type == 3) {
                                File f = null;
                                try {
                                    if (!(message.messageOwner.attachPath == null || message.messageOwner.attachPath.length() == 0)) {
                                        f = new File(message.messageOwner.attachPath);
                                    }
                                    if (f == null || !f.exists()) {
                                        f = FileLoader.getPathToMessage(message.messageOwner);
                                    }
                                    Intent intent = new Intent("android.intent.action.VIEW");
                                    if (VERSION.SDK_INT >= 24) {
                                        intent.setFlags(1);
                                        intent.setDataAndType(FileProvider.getUriForFile(getParentActivity(), "com.ongram.provider", f), MimeTypes.VIDEO_MP4);
                                    } else {
                                        intent.setDataAndType(Uri.fromFile(f), MimeTypes.VIDEO_MP4);
                                    }
                                    getParentActivity().startActivityForResult(intent, 500);
                                } catch (Exception e) {
                                    alertUserOpenError(message);
                                }
                            } else if (message.type == 4) {
                                if (AndroidUtilities.isGoogleMapsInstalled(DownloadManagerActivity.this)) {
                                    LocationActivity fragment = new LocationActivity();
                                    fragment.setMessageObject(message);
                                    presentFragment(fragment);
                                }
                            } else if (message.type == 9 || message.type == 0) {
                                try {
                                    AndroidUtilities.openForView(message, getParentActivity());
                                } catch (Exception e2) {
                                    alertUserOpenError(message);
                                }
                            }
                        }
                    }
                });
                chatMessageCell.setAllowAssistant(true);
            } else if (viewType == 1) {
                view = new ChatActionCell(this.mContext);
                ((ChatActionCell) view).setDelegate(new ChatActionCellDelegate() {
                    public void didClickedImage(ChatActionCell cell) {
                        MessageObject message = cell.getMessageObject();
                        PhotoViewer.getInstance().setParentActivity(getParentActivity());
                        TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, 640);
                        if (photoSize != null) {
                            PhotoViewer.getInstance().openPhoto(photoSize.location, DownloadManagerActivity.this);
                            return;
                        }
                        PhotoViewer.getInstance().openPhoto(message, 0, 0, DownloadManagerActivity.this);
                    }

                    public void didLongPressed(ChatActionCell cell) {
                        createMenu(cell, false);
                    }

                    public void needOpenUserProfile(int uid) {
                        Bundle args;
                        if (uid < 0) {
                            args = new Bundle();
                            args.putInt("chat_id", -uid);
                            if (MessagesController.checkCanOpenChat(args, DownloadManagerActivity.this)) {
                                presentFragment(new ChatActivity(args), true);
                            }
                        } else if (uid != UserConfig.getClientUserId()) {
                            args = new Bundle();
                            args.putInt("user_id", uid);
                            presentFragment(new ProfileActivity(args));
                        }
                    }

                    public void didPressedReplyMessage(ChatActionCell cell, int id) {
                        MessageObject messageObject = cell.getMessageObject();
                        scrollToMessageId(id, messageObject.getId(), true, messageObject.getDialogId() == mergeDialogId ? 1 : 0);
                    }

                    public void didPressedBotButton(MessageObject messageObject, TLRPC.KeyboardButton button) {
                        if (getParentActivity() != null && bottomOverlayChat.getVisibility() == View.VISIBLE && !(button instanceof TLRPC.TL_keyboardButtonSwitchInline) && !(button instanceof TLRPC.TL_keyboardButtonCallback) && !(button instanceof TLRPC.TL_keyboardButtonGame) && (button instanceof TLRPC.TL_keyboardButtonUrl)) {
                        }
                    }
                });
            } else if (viewType == 2) {
                view = new ChatUnreadCell(this.mContext);
            } else if (viewType == 3) {
                view = new BotHelpCell(this.mContext);
                ((BotHelpCell) view).setDelegate(new BotHelpCellDelegate() {
                    @Override
                    public void didPressUrl(String url) {
                        if (url.startsWith("@")) {
                            MessagesController.openByUserName(url.substring(1), DownloadManagerActivity.this, 0);
                        } else if (url.startsWith("#")) {
                            DialogsActivity fragment = new DialogsActivity(null);
                            fragment.setSearchString(url);
                            presentFragment(fragment);
                        }
                    }
                });
            } else if (viewType == 4) {
                view = new ChatLoadingCell(this.mContext);
            }
            view.setLayoutParams(new LayoutParams(-1, -2));
            return new Holder(view);
        }

        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position != this.botInfoRow) {
                if (position == this.loadingDownRow || position == this.loadingUpRow) {
                    ChatMessageCell cell = (ChatMessageCell) holder.itemView; /// *******
//                    holder.itemView.setProgressVisible(loadsCount > 1);
                } else if (position >= this.messagesStartRow && position < this.messagesEndRow) {
                    MessageObject message = (MessageObject) messages.get((messages.size() - (position - this.messagesStartRow)) - 1);
                    View view = holder.itemView;
                    boolean selected = false;
                    boolean disableSelection = false;
                    if (actionBar.isActionModeShowed()) {
                        if (null != message) {
                            if (!selectedMessagesIds[message.getDialogId() == dialog_id ? 0 : 1].containsKey(Integer.valueOf(message.getId()))) {
                                view.setBackgroundColor(0);
                                disableSelection = true;
                            }
                        }
                        selected = true;
                        disableSelection = true;
                    } else {
                        view.setBackgroundColor(0);
                    }
                    if (view instanceof ChatMessageCell) {
                        ChatMessageCell messageCell = (ChatMessageCell) view;
                        messageCell.isChat = false;
                        messageCell.setMessageObject(message, false, false);
                        boolean z = !disableSelection;
                        boolean z2 = disableSelection && selected;
                        messageCell.setCheckPressed(z, z2);
                        if ((view instanceof ChatMessageCell) && MediaController.getInstance().canDownloadMedia(2)) {
                            ((ChatMessageCell) view).downloadAudioIfNeed();
                        }
                        z2 = highlightMessageId != Integer.MAX_VALUE && message.getId() == highlightMessageId;
                        messageCell.setHighlighted(z2);
                        if (searchContainer == null || searchContainer.getVisibility() != View.VISIBLE || MessagesSearchQuery.getLastSearchQuery() == null) {
                            messageCell.setHighlightedText(null);
                        } else {
                            messageCell.setHighlightedText(MessagesSearchQuery.getLastSearchQuery());
                        }
                    } else if (view instanceof ChatActionCell) {
                        ((ChatActionCell) view).setMessageObject(message);
                    } else if (view instanceof ChatUnreadCell) {
                        ((ChatUnreadCell) view).setText(LocaleController.formatPluralString("NewMessages", unread_to_load));
                    }
                }
            }
        }

        public int getItemViewType(int position) {
            if (position >= this.messagesStartRow && position < this.messagesEndRow) {
                return ((MessageObject) messages.get((messages.size() - (position - this.messagesStartRow)) - 1)).contentType;
            }
            if (position == this.botInfoRow) {
                return 3;
            }
            return 4;
        }

        public void onViewAttachedToWindow(ViewHolder holder) {
            if (holder.itemView instanceof ChatMessageCell) {
                final ChatMessageCell messageCell = (ChatMessageCell) holder.itemView;
                messageCell.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                    public boolean onPreDraw() {
                        messageCell.getViewTreeObserver().removeOnPreDrawListener(this);
                        int height = chatListView.getMeasuredHeight();
                        int top = messageCell.getTop();
                        int bottom = messageCell.getBottom();
                        int viewTop = top >= 0 ? 0 : -top;
                        int viewBottom = messageCell.getMeasuredHeight();
                        if (viewBottom > height) {
                            viewBottom = viewTop + height;
                        }
                        messageCell.setVisiblePart(viewTop, viewBottom - viewTop);
                        return true;
                    }
                });
                boolean z = highlightMessageId != Integer.MAX_VALUE && messageCell.getMessageObject().getId() == highlightMessageId;
                messageCell.setHighlighted(z);
            }
        }

        public void updateRowWithMessageObject(MessageObject messageObject) {
            int index = messages.indexOf(messageObject);
            if (index != -1) {
                notifyItemChanged(((this.messagesStartRow + messages.size()) - index) - 1);
            }
        }

        public void notifyDataSetChanged() {
            updateRows();
            try {
                super.notifyDataSetChanged();
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemChanged(int position) {
            updateRows();
            try {
                super.notifyItemChanged(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemRangeChanged(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeChanged(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemInserted(int position) {
            updateRows();
            try {
                super.notifyItemInserted(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemMoved(int fromPosition, int toPosition) {
            updateRows();
            try {
                super.notifyItemMoved(fromPosition, toPosition);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemRangeInserted(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeInserted(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemRemoved(int position) {
            updateRows();
            try {
                super.notifyItemRemoved(position);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }

        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            updateRows();
            try {
                super.notifyItemRangeRemoved(positionStart, itemCount);
            } catch (Exception e) {
                FileLog.e("tmessages", e);
            }
        }
    }

    public DownloadManagerActivity(Bundle args) {
        super(args);
    }

    public boolean onFragmentCreate() {
        this.downloaderRunning = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).getBoolean("download_running", false);
        this.startLoadFromMessageId = 0;
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.messagesDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didCreatedNewDeleteTask);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.chatInfoCantLoad);
        super.onFragmentCreate();
        this.loading = true;
        if (this.startLoadFromMessageId != 0) {
            this.needSelectFromMessageId = true;
            this.waitingForLoad.add(Integer.valueOf(this.lastLoadIndex));
        } else {
            this.waitingForLoad.add(Integer.valueOf(this.lastLoadIndex));
        }
        DM_LoadMessagesByClassGuid(this.classGuid);
        return true;
    }

    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messagesDeleted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.removeAllMessagesFromDialog);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioProgressDidChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidReset);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioPlayStateChanged);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.FileNewChunkAvailable);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didCreatedNewDeleteTask);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.audioDidStarted);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateMessageMedia);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didUpdatedMessagesViews);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.chatInfoCantLoad);
        if (AndroidUtilities.isTablet()) {
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.openedChatChanged, Long.valueOf(this.dialog_id), Boolean.valueOf(true));
        }
        AndroidUtilities.removeAdjustResize(getParentActivity(), this.classGuid);
        if (this.stickersAdapter != null) {
            this.stickersAdapter.onDestroy();
        }
        if (this.chatAttachAlert != null) {
            this.chatAttachAlert.onDestroy();
        }
        AndroidUtilities.unlockOrientation(getParentActivity());
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        for (RunningServiceInfo service : ((ActivityManager) getParentActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public View createView(Context context) {
        int a;
        if (this.chatMessageCellsCache.isEmpty()) {
            for (a = 0; a < 8; a++) {
                this.chatMessageCellsCache.add(new ChatMessageCell(context));
            }
        }
        for (a = 1; a >= 0; a--) {
            this.selectedMessagesIds[a].clear();
            this.selectedMessagesCanCopyIds[a].clear();
        }
        this.cantDeleteMessagesCount = 0;
        this.hasOwnBackground = true;
        if (this.chatAttachAlert != null) {
            this.chatAttachAlert.onDestroy();
            this.chatAttachAlert = null;
        }
        Theme.createChatResources(context, false);
        this.actionBar.setTitle(LocaleController.getString("DownloadManager", R.string.DownloadManager));
        this.actionBar.setAddToContainer(false);
        this.actionBar.setBackButtonDrawable(new BackDrawable(false));
        final Context context2 = context;
        this.actionBar.setActionBarMenuOnItemClick(new ActionBarMenuOnItemClick() {

            class C33981 implements OnClickListener {
                C33981() {
                }

                public void onClick(DialogInterface dialogInterface, int i) {
                    ArrayList<TLRPC.Message> msgs = new ArrayList();
                    for (int a = 0; a < messages.size(); a++) {
                        msgs.add(((MessageObject) messages.get(a)).messageOwner);
                    }
                    clearChatData();
                    DM_DeleteDownloaded(msgs);
                    DM_LoadMessagesByClassGuid(classGuid);
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    if (messages.isEmpty()) {
                        progressView.setVisibility(View.INVISIBLE);
                        chatListView.setEmptyView(emptyViewContainer);
                    }
                }
            }

            class C33992 implements OnClickListener {
                C33992() {
                }

                public void onClick(DialogInterface dialogInterface, int i) {
                    DM_DeleteAll();
                    messages.clear();
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    if (messages.isEmpty()) {
                        progressView.setVisibility(View.INVISIBLE);
                        chatListView.setEmptyView(emptyViewContainer);
                    }
                }
            }

            class C34003 implements OnClickListener {
                C34003() {
                }

                public void onClick(DialogInterface dialogInterface, int i) {
                    ArrayList<TLRPC.Message> msgs = new ArrayList();
                    for (int a = 1; a >= 0; a--) {
                        for (Entry<Integer, MessageObject> entry : selectedMessagesIds[a].entrySet()) {
                            msgs.add(((MessageObject) entry.getValue()).messageOwner);
                        }
                    }
                    clearChatData();
                    DM_DeleteMessage(msgs);
                    DM_LoadMessagesByClassGuid(classGuid);
                    if (chatAdapter != null) {
                        chatAdapter.notifyDataSetChanged();
                    }
                    if (messages.isEmpty()) {
                        progressView.setVisibility(View.INVISIBLE);
                        chatListView.setEmptyView(emptyViewContainer);
                    }
                    actionBar.hideActionMode();
                }
            }

            public void onItemClick(int id) {
                int a;
                AlertDialog.Builder builder ;
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        for (a = 1; a >= 0; a--) {
                            selectedMessagesIds[a].clear();
                            selectedMessagesCanCopyIds[a].clear();
                        }
                        actionBar.hideActionMode();
                        cantDeleteMessagesCount = 0;
                        updateVisibleRows();
                        return;
                    }
                    finishFragment();
                } else if (id == 1) {
                    builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureToContinue", R.string.AreYouSureToContinue));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new C33981());
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == 2) {
                    builder = new AlertDialog.Builder(getParentActivity());
                    builder.setMessage(LocaleController.getString("AreYouSureToContinue", R.string.AreYouSureToContinue));
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new C33992());
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                } else if (id == 3) {
                    presentFragment(new DownloadSettingsActivity());
                } else if (id == 10) {
                    String str = "";
                    int previousUid = 0;
                    for ( a = 1; a >= 0; a--) {
                        ArrayList<Integer> ids = new ArrayList<>(selectedMessagesCanCopyIds[a].keySet());
                            Collections.sort(ids);
                        for (int b = 0; b < ids.size(); b++) {
                            Integer messageId = ids.get(b);
                            MessageObject messageObject = selectedMessagesCanCopyIds[a].get(messageId);
                            if (str.length() != 0) {
                                str += "\n\n";
                            }
                            str += getMessageContent(messageObject, previousUid, true);
                            previousUid = messageObject.messageOwner.from_id;
                        }
                    }
                    if (str.length() != 0) {
                        AndroidUtilities.addToClipboard(str);
                    }
                    for ( a = 1; a >= 0; a--) {
                        selectedMessagesIds[a].clear();
                        selectedMessagesCanCopyIds[a].clear();
                    }
                    cantDeleteMessagesCount = 0;
                    actionBar.hideActionMode();
                    updateVisibleRows();
                } else if (id == 13) {
                    if (getParentActivity() != null) {
                        builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureToContinue", R.string.AreYouSureToContinue));
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new C34003());
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    }
                } else if (id == 11) {
                    if (getParentActivity() != null) {
                        Bundle args = new Bundle();
                        args.putBoolean("onlySelect", true);
                        args.putInt("dialogsType", 1);
                        DialogsActivity fragment = new DialogsActivity(args);
                        fragment.setDelegate(DownloadManagerActivity.this);
                        presentFragment(fragment);
                    }
                } else if (id == 12 && getParentActivity() != null) {
                    Bundle args = new Bundle();
                    args.putBoolean("onlySelect", true);
                    args.putInt("dialogsType", 1);
                    DialogsActivity fragment = new DialogsActivity(args);
                    fragment.setDelegate(DownloadManagerActivity.this);
                    presentFragment(fragment);
                }
            }
        });
        this.headerItem = this.actionBar.createMenu().addItem(0, (int) R.drawable.ic_ab_other);
        this.headerItem.addSubItem(1, LocaleController.getString("DownloaderDelDownloaded", R.string.DownloaderDelDownloaded));
        this.headerItem.addSubItem(2, LocaleController.getString("DownloaderDelAll", R.string.DownloaderDelAll));
        this.headerItem.addSubItem(3, LocaleController.getString("DownloaderSettings", R.string.DownloaderSettings));
        this.actionModeViews.clear();
        ActionBarMenu actionMode = this.actionBar.createActionMode();
        this.selectedMessagesCountTextView = new NumberTextView(actionMode.getContext());
        this.selectedMessagesCountTextView.setTextSize(18);
        this.selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        this.selectedMessagesCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(this.selectedMessagesCountTextView, LayoutHelper.createLinear(0, -1, 1.0f, 65, 0, 0, 0));
        this.selectedMessagesCountTextView.setOnTouchListener(new C34025());
        this.actionModeTitleContainer = new FrameLayout(context) {
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
                SimpleTextView access$2300 = actionModeTextView;
                int i = (AndroidUtilities.isTablet() || getResources().getConfiguration().orientation != 2) ? 20 : 18;
                access$2300.setTextSize(i);
                actionModeTextView.measure(MeasureSpec.makeMeasureSpec(width,  MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24.0f), MeasureSpec.AT_MOST));
                if (actionModeSubTextView.getVisibility() != GONE) {
                    access$2300 = actionModeSubTextView;
                    i = (AndroidUtilities.isTablet() || getResources().getConfiguration().orientation != 2) ? 16 : 14;
                    access$2300.setTextSize(i);
                    actionModeSubTextView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20.0f), MeasureSpec.AT_MOST));
                }
            }

            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                int textTop;
                int height = bottom - top;
                if (actionModeSubTextView.getVisibility() != GONE) {
                    int textHeight = ((height / 2) - actionModeTextView.getTextHeight()) / 2;
                    float f = (AndroidUtilities.isTablet() || getResources().getConfiguration().orientation != 2) ? 3.0f : 2.0f;
                    textTop = textHeight + AndroidUtilities.dp(f);
                } else {
                    textTop = (height - actionModeTextView.getTextHeight()) / 2;
                }
                actionModeTextView.layout(0, textTop, actionModeTextView.getMeasuredWidth(), actionModeTextView.getTextHeight() + textTop);
                if (actionModeSubTextView.getVisibility() != GONE) {
                    int textHeight2 = (height / 2) + (((height / 2) - actionModeSubTextView.getTextHeight()) / 2);
                    if (AndroidUtilities.isTablet() || getResources().getConfiguration().orientation == 2) {
                        textTop = textHeight2 - AndroidUtilities.dp(1.0f);
                        actionModeSubTextView.layout(0, textTop, actionModeSubTextView.getMeasuredWidth(), actionModeSubTextView.getTextHeight() + textTop);
                    } else {
                        textTop = textHeight2 - AndroidUtilities.dp(1.0f);
                        actionModeSubTextView.layout(0, textTop, actionModeSubTextView.getMeasuredWidth(), actionModeSubTextView.getTextHeight() + textTop);
                    }
                }
            }
        };
        actionMode.addView(this.actionModeTitleContainer, LayoutHelper.createLinear(0, -1, 1.0f, 65, 0, 0, 0));
        this.actionModeTitleContainer.setOnTouchListener(new C34047());
        this.actionModeTitleContainer.setVisibility(View.GONE);
        this.actionModeTextView = new SimpleTextView(context);
        this.actionModeTextView.setTextSize(18);
        this.actionModeTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        this.actionModeTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        this.actionModeTextView.setText(LocaleController.getString("Edit", R.string.Edit));
        this.actionModeTitleContainer.addView(this.actionModeTextView, LayoutHelper.createFrame(-1, -1.0f));
        this.actionModeSubTextView = new SimpleTextView(context);
        this.actionModeSubTextView.setGravity(3);
        this.actionModeSubTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        this.actionModeTitleContainer.addView(this.actionModeSubTextView, LayoutHelper.createFrame(-1, -1.0f));
        actionModeViews.add(actionMode.addItem(copy, R.drawable.ic_ab_fwd_copy, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(quoteforward, R.drawable.ic_ab_fwd_quoteforward, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(forward, R.drawable.ic_ab_fwd_forward, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));
        actionModeViews.add(actionMode.addItem(delete, R.drawable.ic_ab_fwd_delete, Theme.ACTION_BAR_MODE_SELECTOR_COLOR, null, AndroidUtilities.dp(54)));

        actionMode.getItem(10).setVisibility(this.selectedMessagesCanCopyIds[0].size() + this.selectedMessagesCanCopyIds[1].size() != 0 ? View.VISIBLE : View.GONE);
        actionMode.getItem(13).setVisibility(this.cantDeleteMessagesCount == 0 ? View.VISIBLE : View.GONE);
        checkActionBarMenu();
        this.fragmentView = new SizeNotifierFrameLayout(context) {
            int inputFieldHeight = AndroidUtilities.dp(56.0f);

            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (child == actionBar) {
                    parentLayout.drawHeaderShadow(canvas, actionBar.getMeasuredHeight());
                }
                return result;
            }

            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(widthSize, heightSize);
                heightSize -= getPaddingTop();
                measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);
                heightSize -= actionBar.getMeasuredHeight();
                int keyboardSize = getKeyboardHeight();
                int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = getChildAt(i);
                    if (!(child == null || child.getVisibility() == GONE || child == actionBar)) {
                        if (child == chatListView || child == progressView) {
                            child.measure(MeasureSpec.makeMeasureSpec(widthSize,  MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10.0f), (heightSize - this.inputFieldHeight) + AndroidUtilities.dp(2.0f)),  MeasureSpec.EXACTLY));
                        } else if (child == emptyViewContainer) {
                            child.measure(MeasureSpec.makeMeasureSpec(widthSize,  MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize,  MeasureSpec.EXACTLY));
                        } else {
                            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                        }
                    }
                }
            }

            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int count = getChildCount();
                setBottomClip(0);
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (child.getVisibility() != GONE) {
                        int childLeft;
                        int childTop;
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        int width = child.getMeasuredWidth();
                        int height = child.getMeasuredHeight();
                        int gravity = lp.gravity;
                        if (gravity == -1) {
                            gravity = 51;
                        }
                        int verticalGravity = gravity & 112;
                        switch ((gravity & 7) & 7) {
                            case 1:
                                childLeft = ((((r - l) - width) / 2) + lp.leftMargin) - lp.rightMargin;
                                break;
                            case 5:
                                childLeft = (r - width) - lp.rightMargin;
                                break;
                            default:
                                childLeft = lp.leftMargin;
                                break;
                        }
                        switch (verticalGravity) {
                            case 16:
                                childTop = (((((b - 0) - t) - height) / 2) + lp.topMargin) - lp.bottomMargin;
                                break;
                            case 48:
                                childTop = lp.topMargin + getPaddingTop();
                                if (child != actionBar) {
                                    childTop += actionBar.getMeasuredHeight();
                                    break;
                                }
                                break;
                            case 80:
                                childTop = (((b - 0) - t) - height) - lp.bottomMargin;
                                break;
                            default:
                                childTop = lp.topMargin;
                                break;
                        }
                        if (child == emptyViewContainer) {
                            childTop -= (this.inputFieldHeight / 2) - (actionBar.getMeasuredHeight() / 2);
                        } else if (child == gifHintTextView) {
                            childTop -= this.inputFieldHeight;
                        } else if (!(child == chatListView || child == progressView || child != actionBar)) {
                            childTop -= getPaddingTop();
                        }
                        child.layout(childLeft, childTop, childLeft + width, childTop + height);
                    }
                }
                updateMessagesVisisblePart();
                notifyHeightChanged();
            }
        };
        SizeNotifierFrameLayout contentView = (SizeNotifierFrameLayout) this.fragmentView;
        contentView.setBackgroundImage(Theme.getCachedWallpaper());
        this.emptyViewContainer = new FrameLayout(context);
        this.emptyViewContainer.setVisibility(View.INVISIBLE);
        contentView.addView(this.emptyViewContainer, LayoutHelper.createFrame(-1, -2, 17));
        this.emptyViewContainer.setOnTouchListener(new C34069());
        TextView emptyView = new TextView(context);
        emptyView.setText(LocaleController.getString("DownloadQueueIsEmpty", R.string.DownloadQueueIsEmpty));
        emptyView.setTextSize(1, 14.0f);
        emptyView.setGravity(17);
        emptyView.setTextColor(Theme.getColor(Theme.key_chat_serviceText));
        emptyView.setBackgroundResource(R.drawable.system);
        emptyView.getBackground().setColorFilter(Theme.colorFilter);
        emptyView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        emptyView.setPadding(AndroidUtilities.dp(10.0f), AndroidUtilities.dp(2.0f), AndroidUtilities.dp(10.0f), AndroidUtilities.dp(3.0f));
        this.emptyViewContainer.addView(emptyView, new FrameLayout.LayoutParams(-2, -2, 17));
        this.chatListView = new RecyclerListView(context) {
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                forceScrollToTop = false;
            }
        };
        this.chatListView.setTag(Integer.valueOf(1));
        this.chatListView.setVerticalScrollBarEnabled(true);
        RecyclerListView recyclerListView = this.chatListView;
        Adapter chatActivityAdapter = new ChatActivityAdapter(context);
        chatAdapter = (ChatActivityAdapter) chatActivityAdapter;
        recyclerListView.setAdapter(chatActivityAdapter);
        this.chatListView.setClipToPadding(false);
        this.chatListView.setPadding(0, AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(3.0f));
        this.chatListView.setItemAnimator(null);
        this.chatListView.setLayoutAnimation(null);
        this.chatLayoutManager = new LinearLayoutManager(context) {
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        this.chatLayoutManager.setOrientation(1);
        this.chatLayoutManager.setStackFromEnd(true);
        this.chatListView.setLayoutManager(this.chatLayoutManager);
        contentView.addView(this.chatListView, LayoutHelper.createFrame(-1, -1.0f));
        this.chatListView.setOnItemLongClickListener(this.onItemLongClickListener);
        this.chatListView.setOnItemClickListener(this.onItemClickListener);
        this.chatListView.setOnScrollListener(new OnScrollListener() {
            private final int scrollValue = AndroidUtilities.dp(100.0f);
            private float totalDy = 0.0f;

            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == 1 && highlightMessageId != Integer.MAX_VALUE) {
                    highlightMessageId = Integer.MAX_VALUE;
                    updateVisibleRows();
                }
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = chatLayoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = firstVisibleItem == -1 ? 0 : Math.abs(chatLayoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                if (visibleItemCount <= 0 || firstVisibleItem + visibleItemCount != chatAdapter.getItemCount() || forwardEndReached[0]) {
                    updateMessagesVisisblePart();
                } else {
                    updateMessagesVisisblePart();
                }
            }
        });
        this.chatListView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        this.chatListView.setOnInterceptTouchListener(new OnInterceptTouchListener() {
            public boolean onInterceptTouchEvent(MotionEvent event) {
                if (actionBar.isActionModeShowed() || event.getAction() != 0) {
                    return false;
                }
                int x = (int) event.getX();
                int y = (int) event.getY();
                int count = chatListView.getChildCount();
                int a = 0;
                while (a < count) {
                    View view = chatListView.getChildAt(a);
                    int top = view.getTop();
                    int bottom = view.getBottom();
                    if (top > y || bottom < y) {
                        a++;
                    } else if (!(view instanceof ChatMessageCell)) {
                        return false;
                    } else {
                        ChatMessageCell cell = (ChatMessageCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject == null || messageObject.isSending() || !messageObject.isSecretPhoto() || !cell.getPhotoImage().isInsideImage((float) x, (float) (y - top)) || !FileLoader.getPathToMessage(messageObject.messageOwner).exists()) {
                            return false;
                        }
                        startX = (float) x;
                        startY = (float) y;
                        chatListView.setOnItemClickListener(null);
                        return true;
                    }
                }
                return false;
            }
        });
        this.progressView = new FrameLayout(context);
        this.progressView.setVisibility(View.INVISIBLE);
        contentView.addView(this.progressView, LayoutHelper.createFrame(-1, -1, 51));
        this.progressView2 = new View(context);
        this.progressView2.setBackgroundResource(R.drawable.system_loader);
        this.progressView2.getBackground().setColorFilter(Theme.colorFilter);
        this.progressView.addView(this.progressView2, LayoutHelper.createFrame(36, 36, 17));
        this.progressBar = new RadialProgressView(context);
        this.progressBar.setSize(AndroidUtilities.dp(28.0f));
        this.progressBar.setProgressColor(Theme.getColor(Theme.key_chat_serviceText));
        this.progressView.addView(this.progressBar, LayoutHelper.createFrame(32, 32, 17));
        this.alertView = new FrameLayout(context);
        this.alertView.setTag(Integer.valueOf(1));
        this.alertView.setTranslationY((float) (-AndroidUtilities.dp(50.0f)));
        this.alertView.setVisibility(View.GONE);
        this.alertView.setBackgroundResource(R.drawable.blockpanel);
        contentView.addView(this.alertView, LayoutHelper.createFrame(-1, 50, 51));
        this.alertNameTextView = new TextView(context);
        this.alertNameTextView.setTextSize(1, 14.0f);
        this.alertNameTextView.setTextColor(Theme.getColor(Theme.key_chat_topPanelTitle));
        this.alertNameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        this.alertNameTextView.setSingleLine(true);
        this.alertNameTextView.setEllipsize(TruncateAt.END);
        this.alertNameTextView.setMaxLines(1);
        this.alertView.addView(this.alertNameTextView, LayoutHelper.createFrame(-2, -2.0f, 51, 8.0f, 5.0f, 8.0f, 0.0f));
        this.alertTextView = new TextView(context);
        this.alertTextView.setTextSize(1, 14.0f);
        this.alertTextView.setTextColor(Theme.getColor(Theme.key_chat_topPanelMessage));
        this.alertTextView.setSingleLine(true);
        this.alertTextView.setEllipsize(TruncateAt.END);
        this.alertTextView.setMaxLines(1);
        this.alertView.addView(this.alertTextView, LayoutHelper.createFrame(-2, -2.0f, 51, 8.0f, 23.0f, 8.0f, 0.0f));
        FrameLayout anonymousClass15 = new FrameLayout(context) {
            public void setTranslationY(float translationY) {
                super.setTranslationY(translationY);
                if (getVisibility() != GONE) {
                    int height = getLayoutParams().height;
                    if (chatListView != null) {
                        chatListView.setTranslationY(translationY);
                    }
                    if (progressView != null) {
                        progressView.setTranslationY(translationY);
                    }
                    if (pagedownButton != null) {
                        pagedownButton.setTranslationY(translationY);
                    }
                }
            }

            public boolean hasOverlappingRendering() {
                return false;
            }

            public void setVisibility(int visibility) {
                float f = 0.0f;
                super.setVisibility(visibility);
                if (visibility == GONE) {
                    if (chatListView != null) {
                        chatListView.setTranslationY(0.0f);
                    }
                    if (progressView != null) {
                        progressView.setTranslationY(0.0f);
                    }
                    if (pagedownButton != null) {
                        FrameLayout access$4400 = pagedownButton;
                        if (pagedownButton.getTag() == null) {
                            f = (float) AndroidUtilities.dp(100.0f);
                        }
                        access$4400.setTranslationY(f);
                    }
                }
            }
        };
        anonymousClass15.setClickable(true);
        View replyLineView = new View(context);
        replyLineView.setBackgroundColor(Theme.getColor(Theme.key_chat_replyPanelLine));
        anonymousClass15.addView(replyLineView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM | Gravity.LEFT));


        this.replyIconImageView = new ImageView(context);
        this.replyIconImageView.setScaleType(ScaleType.CENTER);
        anonymousClass15.addView(this.replyIconImageView, LayoutHelper.createFrame(52, 46, 51));
        this.replyCloseImageView = new ImageView(context);
        this.replyCloseImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_replyPanelClose), Mode.MULTIPLY));
        this.replyCloseImageView.setImageResource(R.drawable.msg_panel_clear);
        this.replyCloseImageView.setScaleType(ScaleType.CENTER);
        anonymousClass15.addView(this.replyCloseImageView, LayoutHelper.createFrame(52, 46.0f, 53, 0.0f, 0.5f, 0.0f, 0.0f));
        this.replyCloseImageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (forwardingMessages != null) {
                    forwardingMessages.clear();
                }
            }
        });
        this.replyNameTextView = new SimpleTextView(context);
        this.replyNameTextView.setTextSize(14);
        this.replyNameTextView.setTextColor(Theme.getColor(Theme.key_chat_replyPanelName));
        this.replyNameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        anonymousClass15.addView(this.replyNameTextView, LayoutHelper.createFrame(-1, 18.0f, 51, 52.0f, 6.0f, 52.0f, 0.0f));
        this.replyObjectTextView = new SimpleTextView(context);
        this.replyObjectTextView.setTextSize(14);
        this.replyObjectTextView.setTextColor(Theme.getColor(Theme.key_chat_replyPanelMessage));
        anonymousClass15.addView(this.replyObjectTextView, LayoutHelper.createFrame(-1, 18.0f, 51, 52.0f, 24.0f, 52.0f, 0.0f));
        this.replyImageView = new BackupImageView(context);
        anonymousClass15.addView(this.replyImageView, LayoutHelper.createFrame(34, 34.0f, 51, 52.0f, 6.0f, 0.0f, 0.0f));
        this.stickersPanel = new FrameLayout(context);
        this.stickersPanel.setVisibility(View.GONE);
        contentView.addView(this.stickersPanel, LayoutHelper.createFrame(-2, 81.5f, 83, 0.0f, 0.0f, 0.0f, 38.0f));
        this.stickersListView = new RecyclerListView(context) {
            public boolean onInterceptTouchEvent(MotionEvent event) {
                boolean result = StickerPreviewViewer.getInstance().onInterceptTouchEvent(event, stickersListView, 0, null);
                if (super.onInterceptTouchEvent(event) || result) {
                    return true;
                }
                return false;
            }
        };
        this.stickersListView.setTag(Integer.valueOf(3));
        this.stickersListView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return StickerPreviewViewer.getInstance().onTouch(event, stickersListView, 0, stickersOnItemClickListener, null);
            }
        });
        this.stickersListView.setDisallowInterceptTouchEvents(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(0);
        this.stickersListView.setLayoutManager(layoutManager);
        this.stickersListView.setClipToPadding(false);
        this.stickersListView.setOverScrollMode(2);
        this.stickersPanel.addView(this.stickersListView, LayoutHelper.createFrame(-1, 78.0f));
        this.stickersPanelArrow = new ImageView(context);
        this.stickersPanelArrow.setImageResource(R.drawable.stickers_back_arrow);
        this.stickersPanelArrow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_stickersHintPanel), Mode.MULTIPLY));
        this.stickersPanel.addView(this.stickersPanelArrow, LayoutHelper.createFrame(-2, -2.0f, 83, 53.0f, 0.0f, 0.0f, 0.0f));
        this.bottomOverlay = new FrameLayout(context) {
            public void onDraw(Canvas canvas) {
                int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
                Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
                Theme.chat_composeShadowDrawable.draw(canvas);
                canvas.drawRect(0.0f, (float) bottom, (float) getMeasuredWidth(), (float) getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
            }
        };
        this.bottomOverlay.setWillNotDraw(false);
        this.bottomOverlay.setFocusable(true);
        this.bottomOverlay.setFocusableInTouchMode(true);
        this.bottomOverlay.setClickable(true);
        this.bottomOverlay.setPadding(0, AndroidUtilities.dp(3.0f), 0, 0);
        contentView.addView(this.bottomOverlay, LayoutHelper.createFrame(-1, 56, 80));
        this.bottomOverlayText = new TextView(context);
        if (this.downloaderRunning) {
            this.bottomOverlayText.setText(LocaleController.getString("StopDownloader", R.string.StopDownloader));
        } else {
            this.bottomOverlayText.setText(LocaleController.getString("StartDownloader", R.string.StartDownloader));
        }
        this.bottomOverlayText.setTextSize(1, 17.0f);
        this.bottomOverlayText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        this.bottomOverlayText.setTextColor(Theme.getColor(Theme.key_chat_secretChatStatusText));
        this.bottomOverlay.addView(this.bottomOverlayText, LayoutHelper.createFrame(-2, -2, 17));
        this.bottomOverlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                downloaderRunning = !downloaderRunning;
                if (downloaderRunning) {
                    startDownloading(messages);
                    bottomOverlayText.setText(LocaleController.getString("StopDownloader", R.string.StopDownloader));
                    return;
                }
                stopDownloading();
                bottomOverlayText.setText(LocaleController.getString("StartDownloader", R.string.StartDownloader));
            }
        });
        this.chatAdapter.updateRows();
        if (this.loading && this.messages.isEmpty()) {
            this.progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.INVISIBLE);
            this.chatListView.setEmptyView(null);
        } else {
            this.progressView.setVisibility(View.INVISIBLE);
            this.chatListView.setEmptyView(this.emptyViewContainer);
        }
        if (!AndroidUtilities.isTablet() || AndroidUtilities.isSmallTablet()) {
            FragmentContextView view = new FragmentContextView(context, this);
            this.fragmentContextView = (FragmentContextView) view;
            contentView.addView(view, LayoutHelper.createFrame(-1, 39.0f, 51, 0.0f, -36.0f, 0.0f, 0.0f));
        }
        try {
            if (VERSION.SDK_INT >= 23) {
                getParentActivity().getWindow().setFlags(8192, 8192);
            }
        } catch (Throwable e) {
            FileLog.e("tmessages", e);
        }
        fixLayoutInternal();
        contentView.addView(this.actionBar);
        return this.fragmentView;
    }

    public long getDialogId() {
        return this.dialog_id;
    }

    public boolean dismissDialogOnPause(Dialog dialog) {
        return dialog != this.chatAttachAlert && super.dismissDialogOnPause(dialog);
    }

    private void forwardMessages(ArrayList<MessageObject> arrayList, boolean fromMyName) {
        if (arrayList != null && !arrayList.isEmpty()) {
            if (fromMyName) {
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    SendMessagesHelper.getInstance().processForwardFromMyName((MessageObject) it.next(), this.dialog_id);
                }
                return;
            }
            SendMessagesHelper.getInstance().sendMessage(arrayList, this.dialog_id);
        }
    }

    private void moveScrollToLastMessage() {
        if (this.chatListView != null && !this.messages.isEmpty()) {
            this.chatLayoutManager.scrollToPositionWithOffset(this.messages.size() - 1, -100000 - this.chatListView.getPaddingTop());
        }
    }

    private void clearChatData() {
        this.messages.clear();
        this.messagesByDays.clear();
        this.waitingForLoad.clear();
        this.progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.INVISIBLE);
        this.chatListView.setEmptyView(null);
        for (int a = 0; a < 2; a++) {
            this.messagesDict[a].clear();
            this.maxMessageId[a] = Integer.MAX_VALUE;
            this.minMessageId[a] = MeasureSpec.AT_MOST;
            this.maxDate[a] = MeasureSpec.AT_MOST;
            this.minDate[a] = 0;
            this.endReached[a] = false;
            this.cacheEndReached[a] = false;
            this.forwardEndReached[a] = true;
        }
        this.first = true;
        this.firstLoading = true;
        this.loading = true;
        this.loadingForward = false;
        this.waitingForReplyMessageLoad = false;
        this.startLoadFromMessageId = 0;
        this.last_message_id = 0;
        this.needSelectFromMessageId = false;
        this.chatAdapter.notifyDataSetChanged();
    }

    private void updateMessagesVisisblePart() {
        if (this.chatListView != null) {
            int count = this.chatListView.getChildCount();
            int height = this.chatListView.getMeasuredHeight();
            for (int a = 0; a < count; a++) {
                View view = this.chatListView.getChildAt(a);
                if (view instanceof ChatMessageCell) {
                    ChatMessageCell messageCell = (ChatMessageCell) view;
                    int top = messageCell.getTop();
                    int bottom = messageCell.getBottom();
                    int viewTop = top >= 0 ? 0 : -top;
                    int viewBottom = messageCell.getMeasuredHeight();
                    if (viewBottom > height) {
                        viewBottom = viewTop + height;
                    }
                    messageCell.setVisiblePart(viewTop, viewBottom - viewTop);
                }
            }
        }
    }

    private void scrollToMessageId(int id, int fromMessageId, boolean select, int loadIndex) {
        MessageObject object = (MessageObject) this.messagesDict[loadIndex].get(Integer.valueOf(id));
        boolean query = false;
        if (object == null) {
            query = true;
        } else if (this.messages.indexOf(object) != -1) {
            if (select) {
                this.highlightMessageId = id;
            } else {
                this.highlightMessageId = Integer.MAX_VALUE;
            }
            int yOffset = Math.max(0, (this.chatListView.getHeight() - object.getApproximateHeight()) / 2);
            if (this.messages.get(this.messages.size() - 1) == object) {
                this.chatLayoutManager.scrollToPositionWithOffset(0, ((-this.chatListView.getPaddingTop()) - AndroidUtilities.dp(7.0f)) + yOffset);
            } else {
                this.chatLayoutManager.scrollToPositionWithOffset(((this.chatAdapter.messagesStartRow + this.messages.size()) - this.messages.indexOf(object)) - 1, ((-this.chatListView.getPaddingTop()) - AndroidUtilities.dp(7.0f)) + yOffset);
            }
            updateVisibleRows();
            int count = this.chatListView.getChildCount();
            for (int a = 0; a < count; a++) {
                View view = this.chatListView.getChildAt(a);
                MessageObject messageObject;
                if (view instanceof ChatMessageCell) {
                    messageObject = ((ChatMessageCell) view).getMessageObject();
                    if (messageObject != null && messageObject.getId() == object.getId()) {
                        break;
                    }
                } else if (view instanceof ChatActionCell) {
                    messageObject = ((ChatActionCell) view).getMessageObject();
                    if (messageObject != null && messageObject.getId() == object.getId()) {
                        break;
                    }
                } else {
                    continue;
                }
            }
        } else {
            query = true;
        }
        if (query) {
            this.waitingForLoad.clear();
            this.waitingForReplyMessageLoad = true;
            this.highlightMessageId = Integer.MAX_VALUE;
            this.scrollToMessagePosition = -10000;
            this.startLoadFromMessageId = id;
            this.waitingForLoad.add(Integer.valueOf(this.lastLoadIndex));
            MessagesController instance = MessagesController.getInstance();
            long j = loadIndex == 0 ? this.dialog_id : this.mergeDialogId;
            int i = AndroidUtilities.isTablet() ? 30 : 20;
            int i2 = this.startLoadFromMessageId;
            int i3 = this.classGuid;
            int i4 = this.lastLoadIndex;
            this.lastLoadIndex = i4 + 1;
            instance.loadMessages(j, i, i2, 0, true, 0, i3, 3, 0, false, i4);
        }
        this.returnToMessageId = fromMessageId;
        this.returnToLoadIndex = loadIndex;
        this.needSelectFromMessageId = select;
    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
    }

    private void checkActionBarMenu() {
        if (this.menuItem != null) {
            this.menuItem.setVisibility(View.GONE);
        }
        if (this.timeItem2 != null) {
            this.timeItem2.setVisibility(View.GONE);
        }
        if (this.avatarContainer != null) {
            this.avatarContainer.hideTimeItem();
        }
    }

    private int getMessageType(MessageObject messageObject) {
        if (messageObject == null) {
            return -1;
        }
        boolean isBroadcastError;
        if (messageObject.getId() > 0 || !messageObject.isSendError()) {
            isBroadcastError = false;
        } else {
            isBroadcastError = true;
        }
        if ((messageObject.getId() > 0 || !messageObject.isOut()) && !isBroadcastError) {
            if (messageObject.type == 6) {
                return -1;
            }
            if (messageObject.type == 10 || messageObject.type == 11) {
                if (messageObject.getId() == 0) {
                    return -1;
                }
                return 1;
            } else if (messageObject.isVoice()) {
                return 2;
            } else {
                if (messageObject.isSticker()) {
                    TLRPC.InputStickerSet inputStickerSet = messageObject.getInputStickerSet();
                    if (inputStickerSet instanceof TLRPC.InputStickerSet) {
                        if (!StickersQuery.isStickerPackInstalled(inputStickerSet.id)) {
                            return 7;
                        }
                    } else if ((inputStickerSet instanceof TLRPC.TL_inputStickerSetShortName) && !StickersQuery.isStickerPackInstalled(inputStickerSet.short_name)) {
                        return 7;
                    }
                } else if ((messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) || messageObject.getDocument() != null || messageObject.isMusic() || messageObject.isVideo()) {
                    boolean canSave = false;
                    if (!(messageObject.messageOwner.attachPath == null || messageObject.messageOwner.attachPath.length() == 0 || !new File(messageObject.messageOwner.attachPath).exists())) {
                        canSave = true;
                    }
                    if (!canSave && FileLoader.getPathToMessage(messageObject.messageOwner).exists()) {
                        canSave = true;
                    }
                    if (canSave) {
                        if (messageObject.getDocument() != null) {
                            String mime = messageObject.getDocument().mime_type;
                            if (mime != null) {
                                if (mime.endsWith("/xml")) {
                                    return 5;
                                }
                                if (mime.endsWith("/png") || mime.endsWith("/jpg") || mime.endsWith("/jpeg")) {
                                    return 6;
                                }
                            }
                        }
                        return 4;
                    }
                } else if (messageObject.type == 12) {
                    return 8;
                } else {
                    if (messageObject.isMediaEmpty()) {
                        return 3;
                    }
                }
                return 2;
            }
        } else if (!messageObject.isSendError()) {
            return -1;
        } else {
            if (messageObject.isMediaEmpty()) {
                return 20;
            }
            return 0;
        }
    }


    private void addToSelectedMessages(MessageObject messageObject) {
        int index;
        int i = 0;
        if (messageObject.getDialogId() == this.dialog_id) {
            index = 0;
        } else {
            index = 1;
        }
        if (this.selectedMessagesIds[index].containsKey(Integer.valueOf(messageObject.getId()))) {
            this.selectedMessagesIds[index].remove(Integer.valueOf(messageObject.getId()));
            if (messageObject.type == 0 || messageObject.caption != null) {
                this.selectedMessagesCanCopyIds[index].remove(Integer.valueOf(messageObject.getId()));
            }
        } else {
            this.selectedMessagesIds[index].put(Integer.valueOf(messageObject.getId()), messageObject);
            if (messageObject.type == 0 || messageObject.caption != null) {
                this.selectedMessagesCanCopyIds[index].put(Integer.valueOf(messageObject.getId()), messageObject);
            }
        }
        if (!this.actionBar.isActionModeShowed()) {
            return;
        }
        if (this.selectedMessagesIds[0].isEmpty() && this.selectedMessagesIds[1].isEmpty()) {
            this.actionBar.hideActionMode();
            return;
        }
        int copyVisible = this.actionBar.createActionMode().getItem(10).getVisibility();
        ActionBarMenuItem item = this.actionBar.createActionMode().getItem(10);
        if (this.selectedMessagesCanCopyIds[1].size() + this.selectedMessagesCanCopyIds[0].size() != 0) {
            item.setVisibility(View.VISIBLE);
        } else {
            item.setVisibility(View.GONE);
        }

        int newCopyVisible = this.actionBar.createActionMode().getItem(10).getVisibility();
        ActionBarMenuItem item2 = this.actionBar.createActionMode().getItem(13);
        if (this.cantDeleteMessagesCount != 0) {
            item.setVisibility(View.GONE);
        }
    }

    private void processRowSelect(View view) {
        MessageObject message = null;
        if (view instanceof ChatMessageCell) {
            message = ((ChatMessageCell) view).getMessageObject();
        } else if (view instanceof ChatActionCell) {
            message = ((ChatActionCell) view).getMessageObject();
        }
        int type = getMessageType(message);
        if (type >= 2 && type != 20) {
            addToSelectedMessages(message);
            updateActionModeTitle();
            updateVisibleRows();
        }
    }

    private void updateActionModeTitle() {
        if (!this.actionBar.isActionModeShowed()) {
            return;
        }
        if (!this.selectedMessagesIds[0].isEmpty() || !this.selectedMessagesIds[1].isEmpty()) {
            this.selectedMessagesCountTextView.setNumber(this.selectedMessagesIds[0].size() + this.selectedMessagesIds[1].size(), true);
        }
    }

    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
    }

    public void saveSelfArgs(Bundle args) {
        if (this.currentPicturePath != null) {
            args.putString("path", this.currentPicturePath);
        }
    }

    public void restoreSelfArgs(Bundle args) {
        this.currentPicturePath = args.getString("path");
    }

    private void removeUnreadPlane() {
        if (this.unreadMessageObject != null) {
            boolean[] zArr = this.forwardEndReached;
            this.forwardEndReached[1] = true;
            zArr[0] = true;
            this.first_unread_id = 0;
            this.last_message_id = 0;
            this.unread_to_load = 0;
            removeMessageObject(this.unreadMessageObject);
            this.unreadMessageObject = null;
        }
    }

    public void didReceivedNotification(int id, Object... args) {
        int i;
        MessageObject messageObject;
        if (id == NotificationCenter.messagesDidLoaded) {
            if (((Integer) args[10]).intValue() == this.classGuid) {
                HashMap<Long, ArrayList<MessageObject>> hashMap = new HashMap();
                this.endReached[0] = true;
                this.endReached[1] = true;
                ArrayList<MessageObject> messArr = (ArrayList<MessageObject>) args[2];
                int fnid = ((Integer) args[4]).intValue();
                if (fnid != 0) {
                    this.first_unread_id = fnid;
                    this.last_message_id = ((Integer) args[5]).intValue();
                } else {
                    if (messArr.isEmpty()) {
                        this.first_unread_id = 0;
                    }
                    this.last_message_id = ((Integer) args[5]).intValue();
                }
                this.first_unread_id = 0;
                this.last_message_id = 0;
                int load_type = ((Integer) args[8]).intValue();
                if (this.firstLoading) {
                    this.f534e.clear();
                    this.f536g.clear();
                    this.f535f.clear();
                    this.messages.clear();
                    this.messagesByDays.clear();
                    for (i = 0; i < 2; i++) {
                        this.maxMessageId[i] = Integer.MAX_VALUE;
                        this.minMessageId[i] = MeasureSpec.AT_MOST;
                        this.maxDate[i] = MeasureSpec.AT_MOST;
                        this.minDate[i] = 0;
                    }
                    this.firstLoading = false;
                }
                int newRowsCount = 0;
                for (int j = 0; j < messArr.size(); j++) {
                    messageObject = (MessageObject) messArr.get(j);
                    if (!hashMap.containsKey(Long.valueOf(messageObject.getDialogId()))) {
                        hashMap.put(Long.valueOf(messageObject.getDialogId()), new ArrayList());
                    }
                    ((ArrayList) hashMap.get(Long.valueOf(messageObject.getDialogId()))).add(messageObject);
                    this.f534e.put(messageObject, Integer.valueOf(j));
                    if (messageObject.getId() > 0) {
                        this.maxMessageId[1] = Math.min(messageObject.getId(), this.maxMessageId[1]);
                        this.minMessageId[1] = Math.max(messageObject.getId(), this.minMessageId[1]);
                    }
                    if (messageObject.messageOwner.date != 0) {
                        this.maxDate[1] = Math.max(this.maxDate[1], messageObject.messageOwner.date);
                        if (this.minDate[1] == 0 || messageObject.messageOwner.date < this.minDate[1]) {
                            this.minDate[1] = messageObject.messageOwner.date;
                        }
                    }
                    ArrayList<MessageObject> dayArray = (ArrayList) this.messagesByDays.get(messageObject.dateKey);
                    if (dayArray == null) {
                        dayArray = new ArrayList();
                        this.messagesByDays.put(messageObject.dateKey, dayArray);
                        TLRPC.Message dateMsg = new TLRPC.Message();
                        dateMsg.message = LocaleController.formatDateChat((long) messageObject.messageOwner.date);
                        dateMsg.date = messageObject.messageOwner.date - (messageObject.messageOwner.date % DateTimeConstants.SECONDS_PER_DAY);
                        dateMsg.id = 0;
                        MessageObject dateObj = new MessageObject(dateMsg, null, false);
                        dateObj.type = 10;
                        dateObj.contentType = 1;
                        if (load_type == 1) {
                            this.messages.add(0, dateObj);
                        } else {
                            this.messages.add(dateObj);
                        }
                        newRowsCount++;
                    }
                    dayArray.add(messageObject);
                    this.messages.add(this.messages.size() - 1, messageObject);
                }
                for (Long longValue : hashMap.keySet()) {
                    long longValue2 = longValue.longValue();
                    MessagesQuery.loadReplyMessagesForMessages((ArrayList) hashMap.get(Long.valueOf(longValue2)), longValue2);
                }
                if (this.first) {
                    if (this.chatListView != null) {
                        this.first = false;
                    } else {
                        this.first = false;
                    }
                }
                if (this.f537h.size() == this.f535f.size()) {
                    if (this.progressView != null) {
                        this.progressView.setVisibility(View.INVISIBLE);
                    }
                    if (this.messages.isEmpty()) {
                        this.endReached[0] = true;
                        this.endReached[1] = true;
                        this.chatListView.setEmptyView(this.emptyViewContainer);
                        updateVisibleRows();
                        return;
                    }
                    this.endReached[0] = true;
                    this.endReached[1] = true;
                    Collections.sort(this.messages, new Comparator<MessageObject>() {
                        public int compare(MessageObject messageObject, MessageObject messageObject2) {
                            if (messageObject.messageOwner.date > messageObject2.messageOwner.date) {
                                return -1;
                            }
                            if (messageObject.messageOwner.date < messageObject2.messageOwner.date) {
                                return 1;
                            }
                            return 0;
                        }
                    });
                    this.chatAdapter.notifyDataSetChanged();
                    this.chatListView.setEmptyView(this.emptyViewContainer);
                    this.chatListView.setEmptyView(null);
                    moveScrollToLastMessage();
                    return;
                }
                this.chatListView.setEmptyView(this.emptyViewContainer);
                if (this.progressView != null) {
                    this.progressView.setVisibility(View.INVISIBLE);
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            if (this.chatListView != null) {
                this.chatListView.invalidateViews();
            }
            if (this.replyObjectTextView != null) {
                this.replyObjectTextView.invalidate();
            }
            if (this.alertTextView != null) {
                this.alertTextView.invalidate();
            }
            if (this.pinnedMessageTextView != null) {
                this.pinnedMessageTextView.invalidate();
            }
        } else if (id == NotificationCenter.audioDidReset || id == NotificationCenter.audioPlayStateChanged) {
            if (this.chatListView != null) {
                for (int a = 0; a < chatListView.getChildCount(); a++) {
                    if (chatListView.getChildAt(a) instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) chatListView.getChildAt(a);
                        messageObject = cell.getMessageObject();
                        if (messageObject != null && (messageObject.isVoice() || messageObject.isMusic())) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.audioProgressDidChanged) {
            Integer mid = (Integer) args[0];
            if (this.chatListView != null) {
                int count = this.chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    if ( chatListView.getChildAt(a) instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell)  chatListView.getChildAt(a);
                        if (cell.getMessageObject() != null && cell.getMessageObject().getId() == mid.intValue()) {
                            MessageObject playing = cell.getMessageObject();
                            MessageObject player = MediaController.getInstance().getPlayingMessageObject();
                            if (player != null) {
                                playing.audioProgress = player.audioProgress;
                                playing.audioProgressSec = player.audioProgressSec;
                                cell.updateAudioProgress();
                                return;
                            }
                            return;
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.removeAllMessagesFromDialog) {
            if (this.dialog_id == ((Long) args[0]).longValue()) {
                this.messages.clear();
                this.waitingForLoad.clear();
                this.messagesByDays.clear();
                for (int a = 1; a >= 0; a--) {
                    this.messagesDict[a].clear();
                    this.maxMessageId[a] = Integer.MAX_VALUE;
                    this.minMessageId[a] = MeasureSpec.AT_MOST;
                    this.maxDate[a] = MeasureSpec.AT_MOST;
                    this.minDate[a] = 0;
                    this.selectedMessagesIds[a].clear();
                    this.selectedMessagesCanCopyIds[a].clear();
                }
                this.cantDeleteMessagesCount = 0;
                this.actionBar.hideActionMode();
                if (this.botButtons != null) {
                    this.botButtons = null;
                }
                if (((Boolean) args[1]).booleanValue()) {
                    if (this.chatAdapter != null) {
                        this.progressView.setVisibility(chatAdapter.botInfoRow == -1 ? View.VISIBLE : View.GONE);
                        this.chatListView.setEmptyView(null);
                    }
                    for (int a = 0; a < 2; a++) {
                        this.endReached[a] = false;
                        this.cacheEndReached[a] = false;
                        this.forwardEndReached[a] = true;
                    }
                    this.first = true;
                    this.firstLoading = true;
                    this.loading = true;
                    this.startLoadFromMessageId = 0;
                    this.needSelectFromMessageId = false;
                    this.waitingForLoad.add(Integer.valueOf(this.lastLoadIndex));
                } else if (this.progressView != null) {
                    this.progressView.setVisibility(View.INVISIBLE);
                    this.chatListView.setEmptyView(this.emptyViewContainer);
                }
                if (this.chatAdapter != null) {
                    this.chatAdapter.notifyDataSetChanged();
                }
            }
        } else if (id == NotificationCenter.didCreatedNewDeleteTask) {
            SparseArray<ArrayList<Integer>> mids = (SparseArray<ArrayList<Integer>>) args[0];
          boolean  changed = false;
            for (i = 0; i < mids.size(); i++) {
                int key = mids.keyAt(i);
                Iterator it = ((ArrayList) mids.get(key)).iterator();
                while (it.hasNext()) {
                    messageObject = (MessageObject) this.messagesDict[0].get((Integer) it.next());
                    if (messageObject != null) {
                        messageObject.messageOwner.destroyTime = key;
                        changed = true;
                    }
                }
            }
            if (changed) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.audioDidStarted) {
            messageObject = (MessageObject) args[0];
            if (this.chatListView != null) {
               int count = this.chatListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    if (chatListView.getChildAt(a) instanceof ChatMessageCell) {
                        ChatMessageCell cell = (ChatMessageCell) chatListView.getChildAt(a);
                        MessageObject messageObject1 = cell.getMessageObject();
                        if (messageObject1 != null && (messageObject1.isVoice() || messageObject1.isMusic())) {
                            cell.updateButtonState(false);
                        }
                    }
                }
            }
        } else if (id == NotificationCenter.updateMessageMedia) {
            messageObject = (MessageObject) args[0];
            MessageObject existMessageObject = (MessageObject) this.messagesDict[0].get(Integer.valueOf(messageObject.getId()));
            if (existMessageObject != null) {
                existMessageObject.messageOwner.media = messageObject.messageOwner.media;
                existMessageObject.messageOwner.attachPath = messageObject.messageOwner.attachPath;
                existMessageObject.generateThumbs(false);
            }
            updateVisibleRows();
        } else if (id == NotificationCenter.replaceMessagesObjects) {
            long did = ((Long) args[0]).longValue();
            if (did == this.dialog_id || did == this.mergeDialogId) {
                int loadIndex = did == this.dialog_id ? 0 : 1;
              boolean  changed = false;
                boolean mediaUpdated = false;
                ArrayList<MessageObject> messageObjects = (ArrayList<MessageObject>) args[1];
                for (int a = 0; a < messageObjects.size(); a++) {
                    messageObject = (MessageObject) messageObjects.get(a);
                    MessageObject old = (MessageObject) this.messagesDict[loadIndex].get(Integer.valueOf(messageObject.getId()));
                    if (this.pinnedMessageObject != null && this.pinnedMessageObject.getId() == messageObject.getId()) {
                        this.pinnedMessageObject = messageObject;
                    }
                    if (old != null) {
                        if (messageObject.type >= 0) {
                            if (!mediaUpdated && (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage)) {
                                mediaUpdated = true;
                            }
                            if (old.replyMessageObject != null) {
                                messageObject.replyMessageObject = old.replyMessageObject;
                                if (messageObject.messageOwner.action instanceof TLRPC.TL_messageActionGameScore) {
                                    messageObject.generateGameMessageText(null);
                                }
                            }
                            messageObject.messageOwner.attachPath = old.messageOwner.attachPath;
                            messageObject.attachPathExists = old.attachPathExists;
                            messageObject.mediaExists = old.mediaExists;
                            this.messagesDict[loadIndex].put(Integer.valueOf(old.getId()), messageObject);
                        } else {
                            this.messagesDict[loadIndex].remove(Integer.valueOf(old.getId()));
                        }
                        int index = this.messages.indexOf(old);
                        if (index >= 0) {
                            ArrayList<MessageObject> dayArr = (ArrayList) this.messagesByDays.get(old.dateKey);
                            int index2 = -1;
                            if (dayArr != null) {
                                index2 = dayArr.indexOf(old);
                            }
                            if (messageObject.type >= 0) {
                                this.messages.set(index, messageObject);
                                if (this.chatAdapter != null) {
                                    this.chatAdapter.notifyItemChanged(((this.chatAdapter.messagesStartRow + this.messages.size()) - index) - 1);
                                }
                                if (index2 >= 0) {
                                    dayArr.set(index2, messageObject);
                                }
                            } else {
                                this.messages.remove(index);
                                if (this.chatAdapter != null) {
                                    this.chatAdapter.notifyItemRemoved(((this.chatAdapter.messagesStartRow + this.messages.size()) - index) - 1);
                                }
                                if (index2 >= 0) {
                                    dayArr.remove(index2);
                                    if (dayArr.isEmpty()) {
                                        this.messagesByDays.remove(old.dateKey);
                                        this.messages.remove(index);
                                        this.chatAdapter.notifyItemRemoved(this.chatAdapter.messagesStartRow + this.messages.size());
                                    }
                                }
                            }
                            changed = true;
                        }
                    }
                }
                if (changed && this.chatLayoutManager != null && mediaUpdated && this.chatLayoutManager.findLastVisibleItemPosition() >= this.messages.size() - 1) {
                    moveScrollToLastMessage();
                }
            }
        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            if (((Long) args[0]).longValue() == this.dialog_id) {
                updateVisibleRows();
            }
        } else if (id == NotificationCenter.didUpdatedMessagesViews) {
            SparseArray<SparseIntArray> channelViews = (SparseArray<SparseIntArray>) args[0];
            SparseIntArray array = channelViews.get((int) dialog_id);
            if (array != null) {
                boolean updated = false;
                for (int a = 0; a < array.size(); a++) {
                    int messageId = array.keyAt(a);
                    messageObject = (MessageObject) this.messagesDict[0].get(Integer.valueOf(messageId));
                    if (messageObject != null) {
                        int newValue = array.get(messageId);
                        if (newValue > messageObject.messageOwner.views) {
                            messageObject.messageOwner.views = newValue;
                            updated = true;
                        }
                    }
                }
                if (updated) {
                    updateVisibleRows();
                }
            }
        }
    }

    public boolean needDelayOpenAnimation() {
        return this.firstLoading;
    }

    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        NotificationCenter.getInstance().setAllowedNotificationsDutingAnimation(new int[]{NotificationCenter.chatInfoDidLoaded, NotificationCenter.dialogsNeedReload, NotificationCenter.closeChats, NotificationCenter.messagesDidLoaded, NotificationCenter.botKeyboardDidLoaded});
        NotificationCenter.getInstance().setAnimationInProgress(true);
        if (isOpen) {
            this.openAnimationEnded = false;
        }
    }

    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        NotificationCenter.getInstance().setAnimationInProgress(false);
        if (isOpen) {
            this.openAnimationEnded = true;
        }
    }

    protected void onDialogDismiss(Dialog dialog) {
        if (this.closeChatDialog != null && dialog == this.closeChatDialog) {
            MessagesController.getInstance().deleteDialog(this.dialog_id, 0);
            if (this.parentLayout == null || this.parentLayout.fragmentsStack.isEmpty() || this.parentLayout.fragmentsStack.get(this.parentLayout.fragmentsStack.size() - 1) == this) {
                finishFragment();
                return;
            }
            BaseFragment fragment = (BaseFragment) this.parentLayout.fragmentsStack.get(this.parentLayout.fragmentsStack.size() - 1);
            removeSelfFromStack();
            fragment.finishFragment();
        }
    }

    private void checkListViewPaddings() {
        AndroidUtilities.runOnUIThread(new Runnable() {
            public void run() {
                int i = 0;
                try {
                    int firstVisPos = chatLayoutManager.findLastVisibleItemPosition();
                    int top = 0;
                    if (firstVisPos != -1) {
                        View firstVisView = chatLayoutManager.findViewByPosition(firstVisPos);
                        if (firstVisView != null) {
                            i = firstVisView.getTop();
                        }
                        top = i - chatListView.getPaddingTop();
                    }
                    if (chatListView.getPaddingTop() != AndroidUtilities.dp(52.0f) && ((pinnedMessageView != null && pinnedMessageView.getTag() == null) || (reportSpamView != null && reportSpamView.getTag() == null))) {
                        chatListView.setPadding(0, AndroidUtilities.dp(52.0f), 0, AndroidUtilities.dp(3.0f));
                        chatListView.setTopGlowOffset(AndroidUtilities.dp(48.0f));
                        top -= AndroidUtilities.dp(48.0f);
                    } else if (chatListView.getPaddingTop() == AndroidUtilities.dp(4.0f) || ((pinnedMessageView != null && pinnedMessageView.getTag() == null) || (reportSpamView != null && reportSpamView.getTag() == null))) {
                        firstVisPos = -1;
                    } else {
                        chatListView.setPadding(0, AndroidUtilities.dp(4.0f), 0, AndroidUtilities.dp(3.0f));
                        chatListView.setTopGlowOffset(0);
                        top += AndroidUtilities.dp(48.0f);
                    }
                    if (firstVisPos != -1) {
                        chatLayoutManager.scrollToPositionWithOffset(firstVisPos, top);
                    }
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
    }

    public void dismissCurrentDialig() {
        if (this.chatAttachAlert == null || this.visibleDialog != this.chatAttachAlert) {
            super.dismissCurrentDialig();
            return;
        }
        this.chatAttachAlert.closeCamera(false);
        this.chatAttachAlert.dismissInternal();
        this.chatAttachAlert.hideCamera(true);
    }

    public void onResume() {
        super.onResume();
        AndroidUtilities.requestAdjustResize(getParentActivity(), this.classGuid);
        checkActionBarMenu();
        if (!(this.replyImageLocation == null || this.replyImageView == null)) {
            this.replyImageView.setImage(this.replyImageLocation, "50_50", (Drawable) null);
        }
        if (!(this.pinnedImageLocation == null || this.pinnedMessageImageView == null)) {
            this.pinnedMessageImageView.setImage(this.pinnedImageLocation, "50_50", (Drawable) null);
        }
        NotificationsController.getInstance().setOpenedDialogId(this.dialog_id);
        if (this.scrollToTopOnResume) {
            if (!this.scrollToTopUnReadOnResume || this.scrollToMessage == null) {
                moveScrollToLastMessage();
            } else if (this.chatListView != null) {
                int yOffset;
                if (this.scrollToMessagePosition == -9000) {
                    yOffset = Math.max(0, (this.chatListView.getHeight() - this.scrollToMessage.getApproximateHeight()) / 2);
                } else if (this.scrollToMessagePosition == -10000) {
                    yOffset = 0;
                } else {
                    yOffset = this.scrollToMessagePosition;
                }
                this.chatLayoutManager.scrollToPositionWithOffset(this.messages.size() - this.messages.indexOf(this.scrollToMessage), ((-this.chatListView.getPaddingTop()) - AndroidUtilities.dp(7.0f)) + yOffset);
            }
            this.scrollToTopUnReadOnResume = false;
            this.scrollToTopOnResume = false;
            this.scrollToMessage = null;
        }
        this.paused = false;
        if (this.readWhenResume && !this.messages.isEmpty()) {
            Iterator it = this.messages.iterator();
            while (it.hasNext()) {
                MessageObject messageObject = (MessageObject) it.next();
                if (!messageObject.isUnread() && !messageObject.isOut()) {
                    break;
                } else if (!messageObject.isOut()) {
                    messageObject.setIsRead();
                }
            }
            this.readWhenResume = false;
            MessagesController.getInstance().markDialogAsRead(this.dialog_id, ((MessageObject) this.messages.get(0)).getId(), this.readWithMid, this.readWithDate, true, false);
        }
        if (this.wasPaused) {
            this.wasPaused = false;
            if (this.chatAdapter != null) {
                this.chatAdapter.notifyDataSetChanged();
            }
        }
        fixLayout();
        if (this.bottomOverlayChat == null || this.bottomOverlayChat.getVisibility() != View.VISIBLE) {
        }
        if (this.chatListView != null) {
            this.chatListView.setOnItemLongClickListener(this.onItemLongClickListener);
            this.chatListView.setOnItemClickListener(this.onItemClickListener);
            this.chatListView.setLongClickable(true);
        }
    }

    public void onPause() {
        boolean z = true;
        super.onPause();
        if (this.menuItem != null) {
            this.menuItem.closeSubMenu();
        }
        if (this.chatAttachAlert != null) {
            this.chatAttachAlert.onPause();
        }
        this.paused = true;
        this.wasPaused = true;
        NotificationsController.getInstance().setOpenedDialogId(0);
        CharSequence[] message = new CharSequence[]{null};
        ArrayList<TLRPC.MessageEntity> entities = MessagesQuery.getEntities(message);
        long j = this.dialog_id;
        CharSequence charSequence = message[0];
        TLRPC.Message tLRPC$Message = this.replyingMessageObject != null ? this.replyingMessageObject.messageOwner : null;
        if (true) {
            z = false;
        }
        DraftQuery.saveDraft(j, charSequence, entities, tLRPC$Message, z);
        MessagesController.getInstance().cancelTyping(0, this.dialog_id);
    }

    private boolean fixLayoutInternal() {
        boolean z = true;
        if (AndroidUtilities.isTablet() || ApplicationLoader.applicationContext.getResources().getConfiguration().orientation != 2) {
            this.selectedMessagesCountTextView.setTextSize(20);
        } else {
            this.selectedMessagesCountTextView.setTextSize(18);
        }
        if (!AndroidUtilities.isTablet()) {
            return true;
        }
        if (AndroidUtilities.isSmallTablet() && ApplicationLoader.applicationContext.getResources().getConfiguration().orientation == 1) {
            this.actionBar.setBackButtonDrawable(new BackDrawable(false));
            if (this.fragmentContextView == null || this.fragmentContextView.getParent() != null) {
                return false;
            }
            ((ViewGroup) this.fragmentView).addView(this.fragmentContextView, LayoutHelper.createFrame(-1, 39.0f, 51, 0.0f, -36.0f, 0.0f, 0.0f));
            return false;
        }
        ActionBar actionBar = this.actionBar;
        if (!(this.parentLayout == null || this.parentLayout.fragmentsStack.isEmpty() || this.parentLayout.fragmentsStack.get(0) == this || this.parentLayout.fragmentsStack.size() == 1)) {
            z = false;
        }
        actionBar.setBackButtonDrawable(new BackDrawable(z));
        if (this.fragmentContextView == null || this.fragmentContextView.getParent() == null) {
            return false;
        }
        this.fragmentView.setPadding(0, 0, 0, 0);
        ((ViewGroup) this.fragmentView).removeView(this.fragmentContextView);
        return false;
    }

    private void fixLayout() {
        if (this.avatarContainer != null) {
            this.avatarContainer.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                public boolean onPreDraw() {
                    if (avatarContainer != null) {
                        avatarContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return fixLayoutInternal();
                }
            });
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        fixLayout();
    }

    private void createMenu(View v, boolean single) {
        if (!this.actionBar.isActionModeShowed()) {
            MessageObject message = null;
            if (v instanceof ChatMessageCell) {
                message = ((ChatMessageCell) v).getMessageObject();
            } else if (v instanceof ChatActionCell) {
                message = ((ChatActionCell) v).getMessageObject();
            }
            if (message != null) {
                int type = getMessageType(message);
                if (single && (message.messageOwner.action instanceof TLRPC.TL_messageActionPinMessage)) {
                    scrollToMessageId(message.messageOwner.reply_to_msg_id, 0, true, 0);
                    return;
                }
                int a;
                this.selectedObject = null;
                this.forwaringMessage = null;
                for (a = 1; a >= 0; a--) {
                    this.selectedMessagesCanCopyIds[a].clear();
                    this.selectedMessagesIds[a].clear();
                }
                this.cantDeleteMessagesCount = 0;
                this.actionBar.hideActionMode();
                if (!single && type >= 2 && type != 20) {
                    ActionBarMenu actionMode = this.actionBar.createActionMode();
                    View item = actionMode.getItem(12);
                    if (item != null) {
                        item.setVisibility(View.VISIBLE);
                    }
                    item = actionMode.getItem(13);
                    if (item != null) {
                        item.setVisibility(View.VISIBLE);
                    }
                    this.actionBar.showActionMode();
                    AnimatorSet animatorSet = new AnimatorSet();
                    ArrayList<Animator> animators = new ArrayList();
                    for (a = 0; a < this.actionModeViews.size(); a++) {
                        View view = (View) this.actionModeViews.get(a);
                        AndroidUtilities.clearDrawableAnimation(view);
                        float[] fArr = new float[2];
                        animators.add(ObjectAnimator.ofFloat(view, "scaleY", new float[]{0.1f, 1.0f}));
                    }
                    animatorSet.playTogether(animators);
                    animatorSet.setDuration(250);
                    animatorSet.start();
                    addToSelectedMessages(message);
                    this.selectedMessagesCountTextView.setNumber(1, false);
                    updateVisibleRows();
                } else if (type >= 0) {
                    this.selectedObject = message;
                    if (getParentActivity() != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        ArrayList<CharSequence> items = new ArrayList();
                        ArrayList<Integer> options = new ArrayList();
                        if (type == 0) {
                            items.add(LocaleController.getString("Delete", R.string.Delete));
                            options.add(Integer.valueOf(1));
                        } else if (type == 20) {
                            items.add(LocaleController.getString("Copy", R.string.Copy));
                            options.add(Integer.valueOf(3));
                            items.add(LocaleController.getString("Delete", R.string.Delete));
                            options.add(Integer.valueOf(1));
                        } else {
                            if (this.selectedObject.type == 0 || this.selectedObject.caption != null) {
                                items.add(LocaleController.getString("Copy", R.string.Copy));
                                options.add(Integer.valueOf(3));
                            }
                            if (type == 3) {
                                if ((this.selectedObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) && MessageObject.isNewGifDocument(this.selectedObject.messageOwner.media.webpage.document)) {
                                    items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                    options.add(Integer.valueOf(11));
                                }
                            } else if (type == 4) {
                                if (this.selectedObject.isVideo()) {
                                    items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                                    options.add(Integer.valueOf(4));
                                    items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                    options.add(Integer.valueOf(6));
                                } else if (this.selectedObject.isMusic()) {
                                    items.add(LocaleController.getString("SaveToMusic", R.string.SaveToMusic));
                                    options.add(Integer.valueOf(10));
                                    items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                    options.add(Integer.valueOf(6));
                                } else if (this.selectedObject.getDocument() != null) {
                                    if (MessageObject.isNewGifDocument(this.selectedObject.getDocument())) {
                                        items.add(LocaleController.getString("SaveToGIFs", R.string.SaveToGIFs));
                                        options.add(Integer.valueOf(11));
                                    }
                                    items.add(LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                                    options.add(Integer.valueOf(10));
                                    items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                    options.add(Integer.valueOf(6));
                                } else {
                                    items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                                    options.add(Integer.valueOf(4));
                                }
                            } else if (type == 5) {
                                items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                options.add(Integer.valueOf(6));
                            } else if (type == 6) {
                                items.add(LocaleController.getString("SaveToGallery", R.string.SaveToGallery));
                                options.add(Integer.valueOf(7));
                                items.add(LocaleController.getString("SaveToDownloads", R.string.SaveToDownloads));
                                options.add(Integer.valueOf(10));
                                items.add(LocaleController.getString("ShareFile", R.string.ShareFile));
                                options.add(Integer.valueOf(6));
                            }
                        }
                        items.add(LocaleController.getString("Delete", R.string.Delete));
                        options.add(Integer.valueOf(1));
                        items.add(LocaleController.getString("DirectShare", R.string.DirectShare));
                        options.add(Integer.valueOf(2));
                        if (!options.isEmpty()) {
                            final ArrayList<Integer> arrayList = options;
                            builder.setItems((CharSequence[]) items.toArray(new CharSequence[items.size()]), new OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (selectedObject != null && i >= 0 && i < arrayList.size()) {
                                        processSelectedOption(((Integer) arrayList.get(i)).intValue());
                                    }
                                }
                            });
                            builder.setTitle(LocaleController.getString("Message", R.string.Message));
                            showDialog(builder.create());
                        }
                    }
                }
            }
        }
    }

    private String getMessageContent(MessageObject messageObject, int previousUid, boolean name) {
        String str = "";
        if (name && previousUid != messageObject.messageOwner.from_id) {
            if (messageObject.messageOwner.from_id > 0) {
                User user = MessagesController.getInstance().getUser(Integer.valueOf(messageObject.messageOwner.from_id));
                if (user != null) {
                    str = ContactsController.formatName(user.first_name, user.last_name) + ":\n";
                }
            } else if (messageObject.messageOwner.from_id < 0) {
                TLRPC.Chat chat = MessagesController.getInstance().getChat(Integer.valueOf(-messageObject.messageOwner.from_id));
                if (chat != null) {
                    str = chat.title + ":\n";
                }
            }
        }
        if (messageObject.type == 0 && messageObject.messageOwner.message != null) {
            return str + messageObject.messageOwner.message;
        }
        if (messageObject.messageOwner.media == null || messageObject.messageOwner.media.caption == null) {
            return str + messageObject.messageText;
        }
        return str + messageObject.messageOwner.media.caption;
    }

    private void processSelectedOption(int option) {
        if (this.selectedObject != null) {
            String path;
            switch (option) {
                case 1:
                    if (getParentActivity() != null) {
                        final MessageObject messageObject = this.selectedObject;
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setMessage(LocaleController.getString("AreYouSureToContinue", R.string.AreYouSureToContinue));
                        builder.setTitle(LocaleController.getString("Message", R.string.Message));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ArrayList<TLRPC.Message> msgs = new ArrayList();
                                msgs.add(messageObject.messageOwner);
                                DM_DeleteMessage(msgs);
                                messages.remove(messageObject);
                                if (chatAdapter != null) {
                                    chatAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        break;
                    }
                    this.selectedObject = null;
                    return;
                case 2:
                    Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit();
                    editor.putInt("forward_type", 1);
                    editor.commit();
                    this.forwaringMessage = this.selectedObject;
                    if (getParentActivity() != null) {
                        showDialog2(new ShareAlert(getParentActivity(), forwaringMessage,null, false,null, true));
                        break;
                    }
                    return;
                case 3:
                    AndroidUtilities.addToClipboard(getMessageContent(this.selectedObject, 0, false));
                    break;
                case 4:
                    path = this.selectedObject.messageOwner.attachPath;
                    if (!(path == null || path.length() <= 0 || new File(path).exists())) {
                        path = null;
                    }
                    if (path == null || path.length() == 0) {
                        path = FileLoader.getPathToMessage(this.selectedObject.messageOwner).toString();
                    }
                    if (this.selectedObject.type == 3 || this.selectedObject.type == 1) {
                        if (VERSION.SDK_INT < 23 || getParentActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED) {
                            MediaController.saveFile(path, getParentActivity(), this.selectedObject.type == 3 ? 1 : 0, null, null);
                            break;
                        }
                        if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            getParentActivity().requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 4);
                        }
                        this.selectedObject = null;
                        return;
                    }
                    break;
                case 6:
                    path = this.selectedObject.messageOwner.attachPath;
                    if (!(path == null || path.length() <= 0 || new File(path).exists())) {
                        path = null;
                    }
                    if (path == null || path.length() == 0) {
                        path = FileLoader.getPathToMessage(this.selectedObject.messageOwner).toString();
                    }
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.setType(this.selectedObject.getDocument().mime_type);
                    intent.putExtra("android.intent.extra.STREAM", Uri.fromFile(new File(path)));
                    getParentActivity().startActivityForResult(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)), 500);
                    break;
                case 7:
                    path = this.selectedObject.messageOwner.attachPath;
                    if (!(path == null || path.length() <= 0 || new File(path).exists())) {
                        path = null;
                    }
                    if (path == null || path.length() == 0) {
                        path = FileLoader.getPathToMessage(this.selectedObject.messageOwner).toString();
                    }
                    if (VERSION.SDK_INT < 23 || getParentActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") ==  PackageManager.PERMISSION_GRANTED) {
                        MediaController.saveFile(path, getParentActivity(), 0, null, null);
                        break;
                    }
                    if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        getParentActivity().requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 4);
                    }
                    this.selectedObject = null;
                    return;
                case 10:
                    if (VERSION.SDK_INT < 23 || getParentActivity().checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") ==  PackageManager.PERMISSION_GRANTED) {
                        String fileName = FileLoader.getDocumentFileName(this.selectedObject.getDocument());
                        if (fileName == null || fileName.length() == 0) {
                            fileName = this.selectedObject.getFileName();
                        }
                        path = this.selectedObject.messageOwner.attachPath;
                        if (!(path == null || path.length() <= 0 || new File(path).exists())) {
                            path = null;
                        }
                        if (path == null || path.length() == 0) {
                            path = FileLoader.getPathToMessage(this.selectedObject.messageOwner).toString();
                        }
                        MediaController.saveFile(path, getParentActivity(), this.selectedObject.isMusic() ? 3 : 2, fileName, this.selectedObject.getDocument() != null ? this.selectedObject.getDocument().mime_type : "");
                        break;
                    }
                    if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        getParentActivity().requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 4);
                    }
                    this.selectedObject = null;
                    break;
                case 11:
                    MessagesController.getInstance().saveGif(this.selectedObject.getDocument());
                    break;
            }
            this.selectedObject = null;
        }
    }

    public void didSelectDialog(DialogsActivity activity, long did, boolean param) {
        if (this.dialog_id == 0) {
            return;
        }
        if (this.forwaringMessage != null || !this.selectedMessagesIds[0].isEmpty() || !this.selectedMessagesIds[1].isEmpty()) {
            ArrayList<MessageObject> fmessages = new ArrayList();
            if (this.forwaringMessage != null) {
                fmessages.add(this.forwaringMessage);
                this.forwaringMessage = null;
            } else {
                for (int a = 1; a >= 0; a--) {
                    ArrayList<Integer> ids = new ArrayList(this.selectedMessagesIds[a].keySet());
                    Collections.sort(ids);
                    for (int b = 0; b < ids.size(); b++) {
                        Integer id = (Integer) ids.get(b);
                        MessageObject message = (MessageObject) this.selectedMessagesIds[a].get(id);
                        if (message != null && id.intValue() > 0) {
                            fmessages.add(message);
                        }
                    }
                    this.selectedMessagesCanCopyIds[a].clear();
                    this.selectedMessagesIds[a].clear();
                }
                this.cantDeleteMessagesCount = 0;
                this.actionBar.hideActionMode();
            }
            if (did != this.dialog_id) {
                int lower_part = (int) did;
                if (lower_part != 0) {
                    Bundle args = new Bundle();
                    args.putBoolean("scrollToTopOnResume", this.scrollToTopOnResume);
                    if (lower_part > 0) {
                        args.putInt("user_id", lower_part);
                    } else if (lower_part < 0) {
                        args.putInt("chat_id", -lower_part);
                    }
                    if (MessagesController.checkCanOpenChat(args, activity)) {
                        ChatActivity chatActivity = new ChatActivity(args);
                        if (presentFragment(chatActivity, true)) {
                            chatActivity.showReplyPanel(true, null, fmessages, null, false);
                            if (!AndroidUtilities.isTablet()) {
                                removeSelfFromStack();
                                return;
                            }
                            return;
                        }
                        activity.finishFragment();
                        return;
                    }
                    return;
                }
                activity.finishFragment();
                return;
            }
            activity.finishFragment();
            moveScrollToLastMessage();
            if (AndroidUtilities.isTablet()) {
                this.actionBar.hideActionMode();
            }
            updateVisibleRows();
        }
    }

    public boolean onBackPressed() {
        if (this.actionBar == null || !this.actionBar.isActionModeShowed()) {
            return true;
        }
        for (int a = 1; a >= 0; a--) {
            this.selectedMessagesIds[a].clear();
            this.selectedMessagesCanCopyIds[a].clear();
        }
        this.actionBar.hideActionMode();
        this.cantDeleteMessagesCount = 0;
        updateVisibleRows();
        return false;
    }

    private void updateVisibleRows() {
        if (this.chatListView != null) {
            int count = this.chatListView.getChildCount();
            for (int a = 0; a < count; a++) {
                View view = this.chatListView.getChildAt(a);
                if (view instanceof ChatMessageCell) {
                    ChatMessageCell cell = (ChatMessageCell) view;
                    boolean disableSelection = false;
                    boolean selected = false;
                    if (this.actionBar.isActionModeShowed()) {
                        MessageObject messageObject = cell.getMessageObject();
                        if (this.selectedMessagesIds[messageObject.getDialogId() == this.dialog_id ? 0 : 1].containsKey(Integer.valueOf(messageObject.getId()))) {
                            view.setBackgroundColor(Theme.getColor(Theme.key_chat_selectedBackground));
                            selected = true;
                        } else {
                            view.setBackgroundDrawable(null);
                        }
                        disableSelection = true;
                    } else {
                        view.setBackgroundDrawable(null);
                    }
                    cell.setMessageObject(cell.getMessageObject(), cell.isPinnedBottom(), cell.isPinnedTop());
                    boolean z = !disableSelection;
                    boolean z2 = disableSelection && selected;
                    cell.setCheckPressed(z, z2);
                    z2 = (this.highlightMessageId == Integer.MAX_VALUE || cell.getMessageObject() == null || cell.getMessageObject().getId() != this.highlightMessageId) ? false : true;
                    cell.setHighlighted(z2);
                    if (this.searchContainer == null || this.searchContainer.getVisibility() != View.VISIBLE || MessagesSearchQuery.getLastSearchQuery() == null) {
                        cell.setHighlightedText(null);
                    } else {
                        cell.setHighlightedText(MessagesSearchQuery.getLastSearchQuery());
                    }
                } else if (view instanceof ChatActionCell) {
                    ChatActionCell cell2 = (ChatActionCell) view;
                    cell2.setMessageObject(cell2.getMessageObject());
                }
            }
        }
    }

    private ArrayList<MessageObject> createVoiceMessagesPlaylist(MessageObject startMessageObject, boolean playingUnreadMedia) {
        ArrayList<MessageObject> messageObjects = new ArrayList();
        messageObjects.add(startMessageObject);
        int messageId = startMessageObject.getId();
        if (messageId != 0) {
            for (int a = this.messages.size() - 1; a >= 0; a--) {
                MessageObject messageObject = (MessageObject) this.messages.get(a);
                if (messageObject.getId() > messageId && messageObject.isVoice() && (!playingUnreadMedia || (messageObject.isContentUnread() && !messageObject.isOut()))) {
                    messageObjects.add(messageObject);
                }
            }
        }
        return messageObjects;
    }

    private void alertUserOpenError(MessageObject message) {
        if (getParentActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            if (message.type == 3) {
                builder.setMessage(LocaleController.getString("NoPlayerInstalled", R.string.NoPlayerInstalled));
            } else {
                builder.setMessage(LocaleController.formatString("NoHandleAppInstalled", R.string.NoHandleAppInstalled, new Object[]{message.getDocument().mime_type}));
            }
            showDialog(builder.create());
        }
    }

    public void updatePhotoAtIndex(int index) {
    }

    public boolean allowCaption() {
        return true;
    }

    public boolean scaleToFill() {
        return false;
    }

  
 

    @Override
    public PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        int count = this.chatListView.getChildCount();
        for (int a = 0; a < count; a++) {
            ImageReceiver imageReceiver = null;
            View view = this.chatListView.getChildAt(a);
            MessageObject message;
            if (view instanceof ChatMessageCell) {
                if (messageObject != null) {
                    ChatMessageCell cell = (ChatMessageCell) view;
                    message = cell.getMessageObject();
                    if (message != null && message.getId() == messageObject.getId()) {
                        imageReceiver = cell.getPhotoImage();
                    }
                }
            } else if (view instanceof ChatActionCell) {
                ChatActionCell cell2 = (ChatActionCell) view;
                message = cell2.getMessageObject();
                if (message != null) {
                    if (messageObject != null) {
                        if (message.getId() == messageObject.getId()) {
                            imageReceiver = cell2.getPhotoImage();
                        }
                    } else if (fileLocation != null && message.photoThumbs != null) {
                        for (int b = 0; b < message.photoThumbs.size(); b++) {
                            TLRPC.PhotoSize photoSize = ( TLRPC.PhotoSize) message.photoThumbs.get(b);
                            if (photoSize.location.volume_id == fileLocation.volume_id && photoSize.location.local_id == fileLocation.local_id) {
                                imageReceiver = cell2.getPhotoImage();
                                break;
                            }
                        }
                    }
                }
            }
            if (imageReceiver != null) {
                int[] coords = new int[2];
                view.getLocationInWindow(coords);
                PlaceProviderObject object = new PlaceProviderObject();
                object.viewX = coords[0];
                object.viewY = coords[1] - (VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
                object.parentView = this.chatListView;
                object.imageReceiver = imageReceiver;
                object.thumb = imageReceiver.getBitmap();
                object.radius = imageReceiver.getRoundRadius();
                if ((this.pinnedMessageView == null || this.pinnedMessageView.getTag() != null) && (this.reportSpamView == null || this.reportSpamView.getTag() != null)) {
                    return object;
                }
                object.clipTopAddition = AndroidUtilities.dp(48.0f);
                return object;
            }
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
        return null;
    }

    @Override
    public void willSwitchFromPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {

    }

    public void willHidePhotoViewer() {
    }

    public boolean isPhotoChecked(int index) {
        return false;
    }

    public void setPhotoChecked(int index) {
    }

    public boolean cancelButtonPressed() {
        return true;
    }

    public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo) {
    }

    public int getSelectedCount() {
        return 0;
    }

    public void sendMedia(MediaController.PhotoEntry photoEntry, boolean mutedVideo) {
        if (photoEntry.isVideo) {
            VideoEditedInfo videoEditedInfo = null;
            long size = 0;
            if (mutedVideo) {
                videoEditedInfo = new VideoEditedInfo();
                videoEditedInfo.bitrate = -1;
                videoEditedInfo.originalPath = photoEntry.path;
                videoEditedInfo.endTime = -1;
                videoEditedInfo.startTime = -1;
                size = new File(photoEntry.path).length();
            }
            SendMessagesHelper.prepareSendingVideo(photoEntry.path, size, 0, 0, 0, videoEditedInfo, this.dialog_id, this.replyingMessageObject, photoEntry.caption != null ? photoEntry.caption.toString() : null);
        } else if (photoEntry.imagePath != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.imagePath, null, this.dialog_id, this.replyingMessageObject, photoEntry.caption, photoEntry.stickers, null);
            DraftQuery.cleanDraft(this.dialog_id, true);
        } else if (photoEntry.path != null) {
            SendMessagesHelper.prepareSendingPhoto(photoEntry.path, null, this.dialog_id, this.replyingMessageObject, photoEntry.caption, photoEntry.stickers, null);
            DraftQuery.cleanDraft(this.dialog_id, true);
        }
    }

    public void showOpenUrlAlert(final String url, boolean ask) {
        boolean z = true;
        if (Browser.isInternalUrl(url) || !ask) {
            Context parentActivity = getParentActivity();
            if (this.inlineReturn != 0) {
                z = false;
            }
            Browser.openUrl(parentActivity, url, z);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
        builder.setMessage(LocaleController.formatString("OpenUrlAlert", R.string.OpenUrlAlert, new Object[]{url}));
        builder.setPositiveButton(LocaleController.getString("Open", R.string.Open), new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Browser.openUrl(getParentActivity(), url, inlineReturn == 0);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void removeMessageObject(MessageObject messageObject) {
        int index = this.messages.indexOf(messageObject);
        if (index != -1) {
            this.messages.remove(index);
            if (this.chatAdapter != null) {
                this.chatAdapter.notifyItemRemoved(((this.chatAdapter.messagesStartRow + this.messages.size()) - index) - 1);
            }
        }
    }

    public void onFailedDownload(String fileName) {
    }

    public void onSuccessDownload(String fileName) {
        startDownloading(this.messages);
    }

    public void onProgressDownload(String fileName, float progress) {
    }

    public void onProgressUpload(String fileName, float progress, boolean isEncrypted) {
    }

    public int getObserverTag() {
        return 0;
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
        if (attach instanceof  TLRPC.PhotoSize) {
            FileLoader.getInstance().loadFile(( TLRPC.PhotoSize) attach, null, false);
        } else if (attach instanceof TLRPC.Document) {
            FileLoader.getInstance().loadFile((TLRPC.Document) attach, true, false);
        }
    }

    private void startDownloading(ArrayList<MessageObject> messageObjects) {
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit().putBoolean("download_running", true).commit();
        Iterator it = messageObjects.iterator();
        while (it.hasNext()) {
            MessageObject messageObject = (MessageObject) it.next();
            TLObject attach = getDownloadObject(messageObject);
            loadFile(attach);
            File pathToMessage = FileLoader.getPathToMessage(messageObject.messageOwner);
            if (pathToMessage != null && !pathToMessage.exists()) {
                MediaController.getInstance().addLoadingFileObserver(FileLoader.getAttachFileName(attach), this);
                return;
            }
        }
    }

    private void stopDownloading() {
        ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0).edit().putBoolean("download_running", false).commit();
        for (int i = 0; i < this.messages.size(); i++) {
            MessageObject messageObject = (MessageObject) this.messages.get(i);
            if (messageObject != null) {
                TLObject attach = getDownloadObject(messageObject);
                if (attach instanceof  TLRPC.PhotoSize) {
                    FileLoader.getInstance().cancelLoadFile(( TLRPC.PhotoSize) attach);
                } else if (attach instanceof TLRPC.Document) {
                    FileLoader.getInstance().cancelLoadFile((TLRPC.Document) attach);
                }
            }
        }
    }

    private boolean downloaded(MessageObject messageObject) {
        boolean downloaded = false;
        if (!(messageObject.messageOwner.attachPath == null || messageObject.messageOwner.attachPath.length() == 0 || !new File(messageObject.messageOwner.attachPath).exists())) {
            downloaded = true;
        }
        if (downloaded || !FileLoader.getPathToMessage(messageObject.messageOwner).exists()) {
            return downloaded;
        }
        return true;
    }

    public void DM_DeleteMessage(final ArrayList<TLRPC.Message> messages) {
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                int a = 0;
                while (a < messages.size()) {
                    try {
                        TLRPC.Message message = (TLRPC.Message) messages.get(a);
                        MessagesStorage.getInstance().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM turbo_idm WHERE mid = %d", new Object[]{Integer.valueOf(message.id)})).stepThis().dispose();
                        a++;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        return;
                    }
                }
            }
        });
    }

    public void DM_DeleteDownloaded(final ArrayList<TLRPC.Message> messages) {
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                int a = 0;
                while (a < messages.size()) {
                    try {
                        TLRPC.Message message = (TLRPC.Message) messages.get(a);
                        boolean downloaded = false;
                        if (!(message.attachPath == null || message.attachPath.length() == 0 || !new File(message.attachPath).exists())) {
                            downloaded = true;
                        }
                        if (!downloaded && FileLoader.getPathToMessage(message).exists()) {
                            downloaded = true;
                        }
                        if (downloaded) {
                            MessagesStorage.getInstance().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM turbo_idm WHERE mid = %d", new Object[]{Integer.valueOf(message.id)})).stepThis().dispose();
                        }
                        a++;
                    } catch (Exception e) {
                        FileLog.e("tmessages", e);
                        return;
                    }
                }
            }
        });
    }

    public void DM_DeleteAll() {
        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                try {
                    MessagesStorage.getInstance().getDatabase().executeFast(String.format(Locale.US, "DELETE FROM turbo_idm")).stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
    }

    public void DM_LoadMessagesByClassGuid(final int classGuid) {

        MessagesStorage.getInstance().getStorageQueue().postRunnable(new Runnable() {
            public void run() {
                ArrayList<MessageObject> objects = new ArrayList<MessageObject>();
                HashMap<Integer, User> usersDict =  new HashMap<Integer, User>();
                HashMap<Integer, TLRPC.Chat> chatsDict = new HashMap<Integer, TLRPC.Chat>();
                int a;
                TLRPC.TL_messages_messages res = new TLRPC.TL_messages_messages();
                SQLiteCursor cursor = null;
                try {
                    cursor = MessagesStorage.getInstance().getDatabase().queryFinalized(String.format(Locale.US, "SELECT * FROM turbo_idm ORDER BY date ASC"));
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
                    final ArrayList<MessageObject> finalObjects = objects;
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesDidLoaded, Integer.valueOf(0), Integer.valueOf(finalObjects.size()), finalObjects, Boolean.valueOf(true), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Boolean.valueOf(true), Integer.valueOf(classGuid), Integer.valueOf(0));
                        }
                    });
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
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
                final ArrayList<MessageObject> finalObjects1 = objects;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messagesDidLoaded, Integer.valueOf(0), Integer.valueOf(finalObjects1.size()), finalObjects1, Boolean.valueOf(true), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), Boolean.valueOf(true), Integer.valueOf(classGuid), Integer.valueOf(0));
                    }
                });
            }
        });
    }
}
