package com.soloplayer.android;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.PictureInPictureParams;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Build.VERSION;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Base64;
import android.util.Rational;
import android.util.Size;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Display;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.MimeTypeMap;
import android.widget.ImageView.ScaleType;
import com.soloplayer.android.data.library.MediaLibraryRepository;
import com.soloplayer.android.model.MediaItem;
import com.soloplayer.android.data.repository.StreamBookmarkRepository;
import com.soloplayer.android.streaming.M3uPlaylistParser;
import com.soloplayer.android.ui.player.AudioArtworkEffectView;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.DisplayManager;
import org.videolan.libvlc.util.VLCVideoLayout;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends androidx.fragment.app.FragmentActivity {
   private static final int BG = Color.rgb(10, 10, 10);
   private static final int PANEL = Color.rgb(24, 24, 24);
   private static final int CARD = Color.rgb(34, 34, 34);
   private static final int PURPLE = Color.rgb(157, 65, 255);
   private static final int LILAC = Color.rgb(207, 142, 255);
   private static final int MUTED = Color.rgb(156, 156, 156);
   private static final int GREEN = Color.rgb(66, 220, 150);
   private static final int PICK_MEDIA = 10;
   private static final int PICK_FOLDER = 11;
   private static final int PICK_SUBTITLE = 12;
   private static final int PICK_PLAYLIST_FILES = 13;
   private static final int PICK_PLAYLIST_FOLDER = 14;
   private static final int PICK_VAULT_FILES = 15;
   private static final int PICK_VAULT_FOLDER = 19;
   private static final int MOVE_PRIVATE_MEDIA = 20;
   private static final int UNLOCK_PRIVATE_VAULT = 16;
   private static final int RESET_PRIVATE_VAULT_PIN = 18;
   private final ArrayList<MediaItem> library = new ArrayList();
   private MediaLibraryRepository mediaLibraryRepository;
   private static final ArrayList<MediaItem> queue = new ArrayList();
   private final ArrayList<MediaItem> channels = new ArrayList();
   private static final int CHANNEL_PAGE_SIZE = 60;
   private int channelPageStart = 0;
   private String channelPageQuery = null;
   private final ArrayList<MediaItem> recentStreams = new ArrayList();
   private final ArrayList<MediaItem> streamBookmarks = new ArrayList();
   private StreamBookmarkRepository streamBookmarkRepository;
   private View streamLoadingOverlay;
   private TextView streamLoadingText;
   private android.app.ProgressDialog playlistLoadingDialog;
   private boolean streamHasPlayed = false;
   private final Runnable streamConnectionTimeout = () -> {
      if (this.current != null && this.current.network && this.streamLoadingOverlay != null && this.streamLoadingOverlay.getVisibility() == View.VISIBLE) this.showNetworkStreamFailure("The stream did not start or recover before the connection timed out.");
   };
   private final ArrayList<MediaItem> recentPlayed = new ArrayList();
   private long pendingResumePosition = -1L;
   private String pendingResumeUri = "";
   private long lastRecentPositionSaveAt = 0L;
   private final LinkedHashMap<String, ArrayList<String>> playlists = new LinkedHashMap();
   private final LinkedHashMap<String, ArrayList<View>> pageViewCache = new LinkedHashMap<String, ArrayList<View>>(8, 0.75F, true);
   private String attachedPageCacheKey;
   private final ExecutorService worker = Executors.newSingleThreadExecutor();
   private final ExecutorService thumbnailWorker = Executors.newFixedThreadPool(2);
   private final Handler handler = new Handler();
   private Object backCallback;
   private AudioManager audioManager;
   private AudioFocusRequest audioFocusRequest;
   private final AudioManager.OnAudioFocusChangeListener focusListener = (focus) -> {
      this.handleAudioFocus(focus);
   };
   private static LibVLC vlc;
   private static MediaPlayer player;
   private static AssetFileDescriptor activeMediaFd;
   private static MediaItem current;
   private static boolean playbackRequested = false;
   private static boolean playbackStartPending = false;
   private static String externalSubtitlePath = "";
   private static String externalSubtitleMediaUri = "";
   private long pendingSubtitleSeek = -1L;
   private boolean pendingSubtitleResume = true;
   private boolean waitingSubtitleRestore = false;
   private boolean decoderRestartPending = false;
   private boolean decoderRestartShouldResume = true;
   private static MainActivity activeActivity;
   private VLCVideoLayout videoSurface;
   private ImageView pausedFrameOverlay;
   private Bitmap pausedFrameBitmap;
   private long pausedFramePosition = -1L;
   private boolean pausedFrameTransitionInProgress = false;
   private boolean pendingAutoFullscreen = false;
   private ImageView fullscreenArtwork;
   private AudioArtworkEffectView fullscreenArtworkEffect;
   private FrameLayout videoFrame;
   private GestureDetector videoTapDetector;
   private TextView portraitRatioButton;
   private View seekFeedback;
   private Runnable hideSeekFeedback;
   private LinearLayout root;
   private LinearLayout toolbar;
   private LinearLayout pageBody;
   private LinearLayout navBar;
   private LinearLayout miniPlayer;
   private final ArrayList<LinearLayout> navItems = new ArrayList();
   private final ArrayList<TextView> navGlyphs = new ArrayList();
   private final ArrayList<TextView> navLabels = new ArrayList();
   private ScrollView pageScroll;
   private TextView pageTitle;
   private TextView miniTitle;
   private TextView miniPlay;
   private TextView playbackTime;
   private TextView playbackTitle;
   private TextView playbackSubtitle;
   private TextView playbackPlayButton;
   private TextView fullscreenPlayButton;
   private TextView fullscreenOrientationButton;
   private TextView playbackPresetStatus;
   private TextView audioPresetStatus;
   private TextView videoPresetStatus;
   private ImageView playbackArtwork;
   private AudioArtworkEffectView playbackArtworkEffect;
   private SeekBar playbackSeek;
   private boolean seeking = false;
   private boolean permissionRequestInFlight = false;
   private boolean softwareDecode = false;
   private boolean fullscreen = false;
   private boolean videoSurfaceAttached = false;
   private boolean videoRebindPending = false;
   private String orientationMode = "Adaptive";
   private boolean pickerOpen = false;
   private long lastVideoSurfaceRecoveryAt = 0L;
   private final Runnable recoverVideoSurface = () -> {
      if (!this.activityStarted || !this.page.equals("Player") || !this.currentIsVideo() || this.player == null || !this.player.hasMedia() || this.videoSurface == null) return;
      VLCVideoLayout surface = this.videoSurface;
      boolean landscape = this.fullscreen;
      surface.requestLayout();
      surface.post(() -> {
         if (this.videoSurface != surface || this.fullscreen != landscape || this.player == null || !this.player.hasMedia()) return;
         if (this.videoRebindPending || !this.videoSurfaceAttached || !this.player.getVLCVout().areViewsAttached()) {
            try { this.player.detachViews(); } catch (Exception ignored) {}
            this.videoSurfaceAttached = false;
            this.videoRebindPending = false;
            this.attachVideoSurface(surface, landscape, 0);
         }
      });
   };
   private boolean resumeAfterFocusLoss = false;
   private boolean pausedForInterruption = false;
   private boolean activityStarted = false;
   private int decoderMode = 0;
   private FrameLayout fullscreenContainer;
   private FrameLayout fullscreenDrawer;
   private FrameLayout screenLockOverlay;
   private TextView screenUnlockButton;
   private LinearLayout fullscreenQueue;
   private LinearLayout fullscreenOverlay;
   private LinearLayout fullscreenTopOverlay;
   private LinearLayout portraitControlsPanel;
   private View portraitVideoTools;
   private boolean portraitControlsVisible = true;
   private LinearLayout fullscreenChapters;
   private LinearLayout fullscreenTools;
   private LinearLayout fullscreenAudioTools;
   private LinearLayout fullscreenVideoTools;
   private LinearLayout fullscreenSubtitleTools;
   private LinearLayout fullscreenPlaybackTools;
   private LinearLayout fullscreenSettingCategoryButtons;
   private TextView fullscreenPlaylistTab;
   private TextView fullscreenChaptersTab;
   private TextView fullscreenControlsTab;
   private EditText fullscreenChannelSearch;
   private int fullscreenQueuePageStart = 0;
   private String fullscreenQueueQuery = null;
   private LinearLayout brightnessHud;
   private LinearLayout volumeHud;
   private SeekBar brightnessSlider;
   private SeekBar volumeSlider;
   private TextView brightnessValue;
   private TextView volumeValue;
   private float touchStartX;
   private float touchStartY;
   private float touchStartBrightness;
   private int touchStartVolume;
   private int touchZone;
   private boolean touchMoved = false;
   private boolean suppressTapGestures = false;
   private boolean pickerPausedVideo = false;
   private boolean pickerVideoWasPlaying = false;
   private long pickerVideoPosition = 0L;
   private boolean deferPlaybackUntilVideoSurface = false;
   private float gestureBrightness = 0.8F;
   private int gestureVolume = 100;
   private TextView fullscreenTitle; private TextView landscapeRatioButton;
   private TextView fullscreenTime;
   private SeekBar fullscreenSeek;
   private String aspectMode = "Best fit";
   private String videoPreset = "Standard";
   private String audioPreset = "Standard";
   private int audioBoostDb = 0;
   private int bassBoostDb = 0;
   private int audioDelayMs = 0;
   private int subtitleDelayMs = 0;
   private boolean backgroundAudioEnabled = true;
   private boolean screenTouchesLocked = false;
   private long abRepeatStartMs = -1L;
   private long abRepeatEndMs = -1L;
   private String abRepeatUri = "";
   private long sleepTimerDeadlineMs = 0L;
   private final Runnable sleepTimer = () -> {
      this.sleepTimerDeadlineMs = 0L;
      if (this.player != null && this.player.hasMedia()) {
         playbackRequested = false;
         this.player.pause();
         this.syncPlaybackUi();
         this.toast("Sleep timer paused playback");
      }
   };
   private float[] customEqBands;
   private float originalVideoPresetBrightness = Float.NaN;
   private int originalPreferredDisplayModeId = -1;
   private int networkCache = 1500;
   private CastContext castContext;
   private MediaRouter castMediaRouter;
   private MediaRouteSelector castRouteSelector;
   private MediaRouter.Callback castRouteCallback;
   private Dialog castPickerDialog;
   private LinearLayout castPickerRouteList;
   private TextView castPickerStatus;
   private ProgressBar castPickerProgress;
   private View castPickerProgressContainer;
   private final ArrayList<MediaRouter.RouteInfo> discoveredCastRoutes = new ArrayList<>();
   private final Runnable castDiscoveryTimeout = () -> {
      if (this.castPickerDialog == null || !this.castPickerDialog.isShowing()) return;
      this.refreshCastRoutes();
      if (this.discoveredCastRoutes.isEmpty()) {
         this.stopCastRouteScan();
         if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.GONE);
         if (this.castPickerStatus != null) this.castPickerStatus.setText("No Google Cast devices found. Check that Wi‑Fi is on, both devices share the same network, and the TV supports Chromecast built-in.");
      }
   };
   private TextView castStatusText;
   private CastSession activeCastSession;
   private LocalMediaHttpServer castHttpServer;
   private long castResumePosition = 0L;
   private boolean resumeLocalAfterCast = false;
   private JSONArray privateVideos = new JSONArray();
   private final ArrayList<String> pendingPrivateMoveIds = new ArrayList<>();
   private android.app.ProgressDialog privateImportProgress;
   private String privateMediaSearch = "";
   private boolean privateVaultSelectionMode = false;
   private final java.util.LinkedHashSet<String> privateSelectedIds = new java.util.LinkedHashSet<>();
   private String privateMediaPath = "";
   private boolean privateVaultUnlocked = false;
   private File activeVaultTempFile;
   private boolean pipPrepared = false;
   private boolean pipWasFullscreen = false;
   private int[] pipSavedChildVisibility;
   private ViewGroup.LayoutParams pipSavedFrameParams;
   private int pipSavedToolbarVisibility;
   private int pipSavedNavVisibility;
   private int pipSavedMiniVisibility;
   private int pipSavedOverlayVisibility;
   private int pipSavedTopVisibility;
   private final SessionManagerListener<CastSession> castSessionListener = new SessionManagerListener<CastSession>() {
      public void onSessionStarting(CastSession session) {}
      public void onSessionStarted(CastSession session, String sessionId) { activeCastSession = session; updateCastUiStatus(); startCastPlayback(session); }
      public void onSessionStartFailed(CastSession session, int error) { stopLocalCastServer(); toast("Could not connect to Cast device"); }
      public void onSessionEnding(CastSession session) {}
      public void onSessionEnded(CastSession session, int error) { onCastSessionEnded(); }
      public void onSessionResuming(CastSession session, String sessionId) {}
      public void onSessionResumed(CastSession session, boolean wasSuspended) { activeCastSession = session; updateCastUiStatus(); startCastPlayback(session); }
      public void onSessionResumeFailed(CastSession session, int error) { onCastSessionEnded(); }
      public void onSessionSuspended(CastSession session, int reason) {}
   };
   private String page = "Home";
   private String previousPage = "Home";
   private String renderedPage = "";
   private String activeFolder = "";
   private String activePlaylist = "Favorites";
   private String pendingPlaylistName = "";
   private boolean exitPromptPending = false;
   private long lastLibraryScanAt = 0L;
   private boolean libraryScanInFlight = false;
   private String mediaFilter = "All";
   private String musicMode = "Songs";
   private String streamAgent = "";
   private String streamReferer = "";
   private String searchQuery = ""; private String networkSearchQuery = "";
   private TextView libraryStatus;
   private boolean browsingAudioFolders = false;
   private String folderReturnPage = "Folders";
   private final Runnable ticker = new Runnable(){public void run(){updatePlaybackTime();persistResumePositionPeriodically();handler.postDelayed(this,450);}};
   private final Runnable hideGestureHud = () -> {
      if (this.brightnessHud != null) {
         this.brightnessHud.setVisibility(8);
      }

      if (this.volumeHud != null) {
         this.volumeHud.setVisibility(8);
      }

   };
   private final Runnable hidePortraitControls = () -> {
      if (!this.fullscreen && this.page.equals("Player") && this.currentIsVideo() && this.portraitControlsVisible) this.setPlayerControlsVisible(false, true);
   };
   private final Runnable hideFullscreenControls = () -> {
      if (this.fullscreen && this.fullscreenOverlay != null && (this.fullscreenDrawer == null || this.fullscreenDrawer.getVisibility() != 0)) {
         this.fullscreenOverlay.animate().alpha(0.0F).setDuration(180L).withEndAction(() -> {
            if (this.fullscreenOverlay != null) {
               this.fullscreenOverlay.setVisibility(8);
            }

         });
         if (this.fullscreenTopOverlay != null) {
            this.fullscreenTopOverlay.animate().alpha(0.0F).setDuration(180L).withEndAction(() -> {
               if (this.fullscreenTopOverlay != null) {
                  this.fullscreenTopOverlay.setVisibility(8);
               }

            });
         }
      }

   };

   public void onCreate(Bundle state) {
      super.onCreate(state);
      this.getWindow().setStatusBarColor(BG);
      this.getWindow().setNavigationBarColor(BG);
      this.getWindow().setSoftInputMode(16);
      this.decoderMode = this.prefs().getInt("decoder", this.prefs().getBoolean("software", false) ? 1 : 0);
      this.softwareDecode = this.decoderMode == 1;
      this.networkCache = this.prefs().getInt("cache", 1500);
      this.videoPreset = this.prefs().getString("video_preset", "Standard");
      this.audioPreset = this.prefs().getString("audio_preset", "Standard");
      this.audioBoostDb = this.prefs().getInt("audio_boost_db", 0);
      this.bassBoostDb = this.prefs().getInt("bass_boost_db", 0);
      this.audioDelayMs = this.prefs().getInt("audio_delay_ms", 0);
      this.subtitleDelayMs = this.prefs().getInt("subtitle_delay_ms", 0);
      this.backgroundAudioEnabled = this.prefs().getBoolean("background_audio_enabled", true);
      try { this.privateVideos = new JSONArray(this.prefs().getString("private_media", this.prefs().getString("private_videos", "[]"))); } catch (Exception ignored) { this.privateVideos = new JSONArray(); }
      this.aspectMode = this.prefs().getString("aspect_mode", "Best fit");
      if (!Arrays.asList("Fit screen", "Center", "16:9", "Fill", "Best fit", "4:3", "16:10", "1:1", "2.35:1", "Original", "Crop", "Maximize", "Fit").contains(this.aspectMode)) this.aspectMode = "Best fit";
      this.orientationMode = this.prefs().getString("orientation_mode", "Adaptive");
      if (!this.orientationMode.equals("Portrait Locked") && !this.orientationMode.equals("Landscape Locked") && !this.orientationMode.equals("Adaptive")) this.orientationMode = "Adaptive";
      this.streamBookmarkRepository = new StreamBookmarkRepository(this);
      this.mediaLibraryRepository = new MediaLibraryRepository(this);
      this.restoreData();
      if (this.vlc == null || this.player == null) {
         this.vlc = new LibVLC(this.getApplicationContext(), new ArrayList(Arrays.asList("--no-video-title-show", "--network-caching=" + this.networkCache, "--file-caching=750")));
         this.player = new MediaPlayer(this.vlc);
      }
      this.player.setEventListener((e) -> {
         this.handler.post(() -> {
            this.onPlayerEvent(e);
         });
      });
      try { this.castContext = CastContext.getSharedInstance(this); } catch (Exception castError) { Log.w("SoloPlayer", "Cast initialization failed", castError); }
      File vaultDir = this.vaultDirectory();
      File[] staleVaultTemps = this.getCacheDir().listFiles((dir, name) -> name.startsWith("solo-vault-") && name.endsWith(".mp4"));
      if (staleVaultTemps != null) for (File stale : staleVaultTemps) stale.delete();
      this.buildShell();
      this.render();
      this.updateMini();
      this.handler.post(this.ticker);
      if (VERSION.SDK_INT >= 33) {
         this.backCallback = com.soloplayer.android.MainActivity.Api33BackHelper.register(this, () -> {
            this.handleBack();
         });
      }

      this.ensureMediaPermission();
   }

   private SharedPreferences prefs() {
      return this.getSharedPreferences("solo_player", 0);
   }

   private GradientDrawable shape(int color, int radius) {
      GradientDrawable d = new GradientDrawable();
      d.setColor(color);
      d.setCornerRadius((float)this.dp((float)radius));
      return d;
   }

   private TextView compactButton(String s, boolean primary) {
      TextView t = this.text(s, 13, -1, true);
      t.setGravity(17);
      t.setPadding(this.dp(8.0F), this.dp(6.0F), this.dp(8.0F), this.dp(6.0F));
      t.setMinHeight(this.dp(42.0F));
      t.setBackground(this.shape(primary ? PURPLE : CARD, 13));
      return t;
   }

   private int dp(float v) {
      return (int)(v * this.getResources().getDisplayMetrics().density + 0.5F);
   }

   private TextView text(String s, int size, int color, boolean bold) {
      TextView t = new TextView(this);
      t.setText(s);
      t.setTextSize((float)size);
      t.setTextColor(color);
      t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", bold ? 1 : 0));
      return t;
   }

   private LinearLayout col() {
      LinearLayout l = new LinearLayout(this);
      l.setOrientation(1);
      return l;
   }

   private LinearLayout row() {
      LinearLayout l = new LinearLayout(this);
      l.setOrientation(0);
      l.setGravity(16);
      return l;
   }

   private LinearLayout.LayoutParams lp(int w, int h) {
      return new LinearLayout.LayoutParams(w, h);
   }

   private TextView button(String s, boolean primary) {
      TextView t = this.text(s, 14, -1, true);
      t.setGravity(17);
      t.setPadding(this.dp(13.0F), this.dp(10.0F), this.dp(13.0F), this.dp(10.0F));
      t.setBackground(this.shape(primary ? PURPLE : CARD, 15));
      return t;
   }

   private void buildShell() {
      this.root = this.col();
      this.root.setBackgroundColor(BG);
      this.setContentView(this.root);
      this.toolbar = this.row();
      this.toolbar.setPadding(this.dp(10.0F), this.dp(7.0F), this.dp(10.0F), this.dp(7.0F));
      TextView menu = this.compactButton("☰", false);
      this.toolbar.addView(menu, this.lp(this.dp(38.0F), this.dp(40.0F)));
      menu.setOnClickListener((v) -> {
         this.page = "More";
         this.render();
      });
      ImageView icon = new ImageView(this);
      icon.setImageResource(R.drawable.solo_player_logo);
      icon.setScaleType(ScaleType.FIT_CENTER);
      LinearLayout.LayoutParams ilp = this.lp(this.dp(30.0F), this.dp(30.0F));
      ilp.leftMargin = this.dp(4.0F);
      this.toolbar.addView(icon, ilp);
      this.pageTitle = this.text("SOLO Player", 16, PURPLE, true);
      LinearLayout.LayoutParams titleLp = this.lp(0, -2);
      titleLp.weight = 1.0F;
      titleLp.leftMargin = this.dp(5.0F);
      this.toolbar.addView(this.pageTitle, titleLp);
      TextView search = this.compactButton("⌕", false);
      TextView pip = this.compactButton("▣", false);
      pip.setContentDescription("Open floating video window");
      this.toolbar.addView(search, this.lp(this.dp(38.0F), this.dp(40.0F)));
      MediaRouteButton homeCast = this.createCastButton();
      this.toolbar.addView(homeCast, this.lp(this.dp(42.0F), this.dp(40.0F)));
      search.setOnClickListener((v) -> {
         this.searchDialog();
      });
      TextView live = this.compactButton("LIVE", true);
      LinearLayout.LayoutParams llp = this.lp(this.dp(48.0F), this.dp(40.0F));
      llp.leftMargin = this.dp(5.0F);
      this.toolbar.addView(live, llp);
      live.setOnClickListener((v) -> {
         this.page = "Network Stream";
         this.render();
      });
      this.root.addView(this.toolbar);
      this.pageScroll = new ScrollView(this);
      this.pageScroll.setFillViewport(true);
      this.pageScroll.setClipToPadding(false);
      this.pageBody = this.col();
      this.pageBody.setPadding(this.dp(14.0F), this.dp(7.0F), this.dp(14.0F), this.dp(16.0F));
      this.pageScroll.addView(this.pageBody);
      this.root.addView(this.pageScroll, new LinearLayout.LayoutParams(-1, 0, 1.0F));
      this.miniPlayer = this.row();
      this.miniPlayer.setPadding(this.dp(10.0F), this.dp(7.0F), this.dp(10.0F), this.dp(7.0F));
      this.miniPlayer.setBackground(this.shape(Color.rgb(38, 13, 62), 12));
      ImageView art = new ImageView(this);
      art.setImageResource(R.drawable.solo_player_logo);
      this.miniPlayer.addView(art, this.lp(this.dp(38.0F), this.dp(38.0F)));
      LinearLayout miniMeta = this.col();
      miniMeta.setPadding(this.dp(9.0F), 0, 0, 0);
      this.miniTitle = this.text("Nothing playing", 12, -1, true);
      miniMeta.addView(this.miniTitle);
      miniMeta.addView(this.text("Solo Player", 10, MUTED, false));
      this.miniPlayer.addView(miniMeta, new LinearLayout.LayoutParams(0, -2, 1.0F));
      this.miniPlay = this.button("▶", true);
      this.miniPlayer.addView(this.miniPlay, this.lp(this.dp(42.0F), this.dp(38.0F)));
      LinearLayout.LayoutParams mlp = this.lp(-1, -2);
      mlp.setMargins(this.dp(12.0F), this.dp(5.0F), this.dp(12.0F), this.dp(5.0F));
      this.root.addView(this.miniPlayer, mlp);
      this.miniPlayer.setVisibility(8);
      this.miniPlayer.setOnClickListener((v) -> {
         this.page = "Player";
         this.render();
      });
      this.miniPlay.setOnClickListener((v) -> {
         this.togglePlayback();
      });
      this.navBar = this.row();
      this.navBar.setPadding(this.dp(3.0F), this.dp(6.0F), this.dp(3.0F), this.dp(7.0F));
      this.navBar.setBackgroundColor(PANEL);
      String[] labels = new String[]{"Home", "Movies", "Music", "Playlists", "More"};
      String[] glyphs = new String[]{"⌂", "▣", "♫", "☷", "•••"};

      for(int i = 0; i < labels.length; ++i) {
         String dest = labels[i];
         LinearLayout item = this.col();
         item.setGravity(17);
         TextView g = this.text(glyphs[i], 18, MUTED, true);
         g.setGravity(17);
         item.addView(g);
         TextView n = this.text(labels[i], 9, MUTED, false);
         n.setGravity(17);
         item.addView(n);
         this.navItems.add(item);
         this.navGlyphs.add(g);
         this.navLabels.add(n);
         LinearLayout.LayoutParams np = this.lp(0, -2);
         np.weight = 1.0F;
         this.navBar.addView(item, np);
         item.setOnClickListener((v) -> {
            boolean switchingMediaTab = !this.page.equals(dest)
               && (this.page.equals("Music") || this.page.equals("Movies") || dest.equals("Music") || dest.equals("Movies"));
            if (switchingMediaTab && this.pageScroll != null) this.pageScroll.scrollTo(0, 0);
            if (this.page.equals("Player") && dest.equals("Home")) {
               this.closePlayerToHome();
            } else {
               if (this.page.equals("Player") && this.player != null && this.player.hasMedia()) {
                  this.closePlayerToHome();
               }

               this.page = dest;
               this.render();
            }
         });
      }

      this.root.addView(this.navBar);
   }

   private void render() {
      if(!this.page.equals("Player")&&this.pausedFrameBitmap!=null)this.clearPausedFrameOverlay();
      if (this.current != null && this.current.privateVault) this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
      else this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
      if (!this.page.equals("Player")) { this.playbackTitle = null; this.playbackSubtitle = null; this.playbackPlayButton = null; this.fullscreenPlayButton = null; this.playbackArtwork = null; this.playbackArtworkEffect = null; this.portraitRatioButton = null; this.videoFrame = null; this.videoTapDetector = null; this.portraitControlsPanel = null; this.portraitVideoTools = null; }
      if (this.pageBody != null) {
         String targetCacheKey = this.pageCacheKey(this.page);
         boolean cacheStateChanged = targetCacheKey == null ? this.attachedPageCacheKey != null : !targetCacheKey.equals(this.attachedPageCacheKey);
         if (this.attachedPageCacheKey != null && cacheStateChanged && this.pageBody.getChildCount() > 0) {
            ArrayList<View> children = new ArrayList();
            while (this.pageBody.getChildCount() > 0) { children.add(this.pageBody.getChildAt(0)); this.pageBody.removeViewAt(0); }
            this.pageViewCache.put(this.attachedPageCacheKey, children);
            while (this.pageViewCache.size() > 4) this.pageViewCache.remove(this.pageViewCache.keySet().iterator().next());
         } else if (!cacheStateChanged && targetCacheKey != null) {
            this.pageViewCache.remove(targetCacheKey);
         }
         this.attachedPageCacheKey = null;
         if (!this.page.equals(this.renderedPage)) {
            if (!this.renderedPage.isEmpty()) {
               this.previousPage = this.renderedPage;
            }

            this.renderedPage = this.page;
         }

         this.updateNavigation();
         if (this.toolbar != null) {
            this.toolbar.setVisibility(this.page.equals("Player") ? 8 : 0);
         }

         if (this.navBar != null) {
            this.navBar.setVisibility(this.page.equals("Player") ? 8 : 0);
         }

         if (this.videoSurface != null && this.player != null) {
            try {
               this.player.detachViews();
               this.videoSurfaceAttached = false;
            } catch (Exception var3) {
            }

            this.videoSurface = null;
         }

         this.pageBody.removeAllViews();
         this.pageTitle.setText(this.page.equals("Player") && this.current != null ? this.current.name : (Arrays.asList("Home", "Movies", "Folders", "History", "Network Stream").contains(this.page) ? "SOLO Player" : this.page));
         if (targetCacheKey != null) {
            ArrayList<View> cachedViews = this.pageViewCache.remove(targetCacheKey);
            if (cachedViews != null) {
               for (View cachedView : cachedViews) this.pageBody.addView(cachedView);
               this.attachedPageCacheKey = targetCacheKey;
               this.updateMini();
               return;
            }
         }
         switch (this.page) {
            case "Home":
               this.homePage();
               break;
            case "History":
               this.historyPage();
               break;
            case "Movies":
               this.moviesPage();
               break;
            case "Music":
               this.musicPage();
               break;
            case "Playlists":
               this.playlistsPage();
               break;
            case "More":
               this.morePage();
               break;
            case "Folders":
               this.foldersPage();
               break;
            case "Folder":
               this.folderPage();
               break;
            case "Network Stream":
               this.networkPage();
               break;
            case "Channels":
               this.channelsPage();
               break;
            case "Settings":
               this.settingsPage();
               break;
            case "Playlist":
               this.playlistPage();
               break;
            case "Player":
               this.playerPage();
               break;
            default:
               this.homePage();
         }

         this.attachedPageCacheKey = targetCacheKey;
         this.updateMini();
      }
   }

   private String pageCacheKey(String pageName) {
      if (pageName.equals("Home")) return "Home|" + this.searchQuery;
      if (pageName.equals("Movies")) return "Movies|" + this.mediaFilter + "|" + this.searchQuery;
      if (pageName.equals("Music")) return "Music|" + this.musicMode + "|" + this.searchQuery;
      if (pageName.equals("Playlists")) return "Playlists";
      if (pageName.equals("More")) return "More";
      return null;
   }

   private void invalidatePageCachePrefix(String prefix) {
      this.pageViewCache.keySet().removeIf(key -> key.startsWith(prefix));
      if (this.attachedPageCacheKey != null && this.attachedPageCacheKey.startsWith(prefix)) this.attachedPageCacheKey = null;
   }

   private void updateNavigation() {
      String selected = !this.page.equals("Player") && !this.page.equals("Playlist") ? (!this.page.equals("History") && !this.page.equals("Folders") && !this.page.equals("Folder") && !this.page.equals("Network Stream") && !this.page.equals("Channels") && !this.page.equals("Settings") ? this.page : "More") : (this.page.equals("Player") ? "Home" : "Playlists");
      String[] names = new String[]{"Home", "Movies", "Music", "Playlists", "More"};

      for(int i = 0; i < this.navItems.size(); ++i) {
         boolean on = names[i].equals(selected);
         ((TextView)this.navGlyphs.get(i)).setTextColor(on ? LILAC : MUTED);
         ((TextView)this.navLabels.get(i)).setTextColor(on ? LILAC : MUTED);
         ((LinearLayout)this.navItems.get(i)).setBackground(on ? this.shape(Color.rgb(39, 20, 58), 12) : null);
      }

   }

   private void section(String label, Runnable seeAll) {
      LinearLayout h = this.row();
      h.setPadding(this.dp(1.0F), this.dp(16.0F), this.dp(1.0F), this.dp(8.0F));
      h.addView(this.text(label, 17, -1, true), new LinearLayout.LayoutParams(0, -2, 1.0F));
      if (seeAll != null) {
         TextView a = this.text("See all", 11, LILAC, true);
         h.addView(a);
         a.setOnClickListener((v) -> {
            seeAll.run();
         });
      }

      this.pageBody.addView(h);
   }

   private void browserTabs(String selected) {
      String[] labels = new String[]{"Videos (" + this.videoCount() + ")", "Folders", "History", "Network"};
      LinearLayout tabs = this.row();
      tabs.setBackgroundColor(PANEL);

      for(int i = 0; i < labels.length; ++i) {
         String dest = (new String[]{"Videos", "Folders", "History", "Network"})[i];
         boolean on = i == 0 && selected.equals("Videos") || dest.equals(selected);
         TextView b = this.text(labels[i], 11, on ? PURPLE : MUTED, true);
         b.setGravity(17);
         b.setPadding(this.dp(3.0F), this.dp(12.0F), this.dp(3.0F), this.dp(12.0F));
         b.setBackground(on ? this.shape(Color.rgb(43, 27, 15), 8) : null);
         LinearLayout.LayoutParams p = this.lp(0, -2);
         p.weight = 1.0F;
         tabs.addView(b, p);
         b.setOnClickListener((v) -> {
            if (dest.equals("Videos")) {
               this.page = "Movies";
               this.mediaFilter = "All";
            } else if (dest.equals("Network")) {
               this.page = "Network Stream";
            } else {
               this.page = dest;
            }

            this.render();
         });
      }

      LinearLayout.LayoutParams tabsParams = this.lp(-1, -2);
      tabsParams.bottomMargin = this.dp(10.0F);
      this.pageBody.addView(tabs, tabsParams);
      LinearLayout searchRow = this.row();
      EditText search = this.edit("Search videos");
      search.setText(this.searchQuery);
      search.setPadding(this.dp(12.0F), 0, this.dp(8.0F), 0);
      search.setBackground(this.shape(CARD, 18));
      searchRow.addView(search, new LinearLayout.LayoutParams(0, this.dp(40.0F), 1.0F));
      TextView go = this.compactButton("⌕", false);
      LinearLayout.LayoutParams gp = this.lp(this.dp(42.0F), this.dp(40.0F));
      gp.leftMargin = this.dp(6.0F);
      searchRow.addView(go, gp);
      Runnable apply = () -> {
         this.searchQuery = search.getText().toString().trim().toLowerCase(Locale.ROOT);
         this.page = "Movies";
         this.render();
      };
      go.setOnClickListener((v) -> {
         apply.run();
      });
      search.setOnEditorActionListener((v, id, event) -> {
         apply.run();
         return true;
      });
      LinearLayout.LayoutParams srp = this.lp(-1, -2);
      srp.topMargin = this.dp(7.0F);
      if(!selected.equals("Network"))this.pageBody.addView(searchRow, srp);
   }

   private void homePage() {
      this.browserTabs("Videos");
      this.section("Recently played", () -> { this.page = "History"; this.render(); });
      if (!this.recentPlayed.isEmpty()) {
         int recentCount = Math.min(5, this.recentPlayed.size());
         for (int i = 0; i < recentCount; i++) this.mediaRow(this.recentPlayed.get(i), true);
      } else this.pageBody.addView(this.hint("Your last 5 audio and video items will appear here."));
      TextView local = this.text("Local  ·  " + this.videoCount() + " videos", 12, -1, true);
      local.setPadding(this.dp(3.0F), this.dp(10.0F), 0, this.dp(5.0F));
      this.pageBody.addView(local);
      if (this.library.isEmpty()) {
         LinearLayout empty = this.card();
         empty.addView(this.text(this.permissionRequestInFlight ? "Waiting for media access…" : "Scanning device videos", 14, -1, true));
         empty.addView(this.hint("Your videos appear here automatically after media access is allowed."));
         TextView scan = this.button("Scan device", true);
         empty.addView(scan);
         scan.setOnClickListener((v) -> {
            this.ensureMediaPermission();
         });
         this.pageBody.addView(empty);
      } else {
         int n = 0;
         Iterator var6 = this.library.iterator();

         while(var6.hasNext()) {
            MediaItem m = (MediaItem)var6.next();
            if (m.video && this.matchesSearch(m)) {
               this.mediaRow(m, true);
               ++n;
            }
         }

         if (n == 0) {
            this.pageBody.addView(this.hint("No videos found. Open Folders to scan a storage location."));
         }
      }

   }

   private void historyPage() {
      this.browserTabs("History");
      this.section("Recently played", (Runnable)null);
      if (this.recentPlayed.isEmpty()) {
         this.pageBody.addView(this.hint("Audio and video you play will appear here."));
      } else {
         int recentCount = Math.min(5, this.recentPlayed.size());
         for (int i = 0; i < recentCount; i++) this.mediaRow(this.recentPlayed.get(i), true);
      }

   }

   private LinearLayout card() {
      LinearLayout l = this.col();
      l.setPadding(this.dp(13.0F), this.dp(12.0F), this.dp(13.0F), this.dp(12.0F));
      l.setBackground(this.shape(CARD, 16));
      return l;
   }

   private TextView hint(String s) {
      TextView t = this.text(s, 12, MUTED, false);
      t.setPadding(this.dp(3.0F), this.dp(7.0F), this.dp(3.0F), this.dp(7.0F));
      return t;
   }

   private void chips(String[] names, String selected, Consumer<String> action) {
      LinearLayout r = this.row();
      String[] var5 = names;
      int var6 = names.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String n = var5[var7];
         TextView c = this.text(n, 11, selected.equals(n) ? -1 : MUTED, true);
         c.setGravity(17);
         c.setPadding(this.dp(14.0F), this.dp(8.0F), this.dp(14.0F), this.dp(8.0F));
         c.setBackground(this.shape(selected.equals(n) ? PURPLE : CARD, 18));
         LinearLayout.LayoutParams p = this.lp(-2, -2);
         p.rightMargin = this.dp(6.0F);
         r.addView(c, p);
         c.setOnClickListener((v) -> {
            action.accept(n);
         });
      }

      HorizontalScrollView h = new HorizontalScrollView(this);
      h.setHorizontalScrollBarEnabled(false);
      h.addView(r);
      LinearLayout.LayoutParams stripParams = this.lp(-1, -2);
      stripParams.bottomMargin = this.dp(10.0F);
      this.pageBody.addView(h, stripParams);
   }

   private void moviesPage() {
      this.browserTabs("Videos");
      TextView count = this.text("Local videos  ·  " + this.videoCount(), 12, MUTED, false);
      this.pageBody.addView(count);
      Iterator var2 = this.library.iterator();

      while(var2.hasNext()) {
         MediaItem m = (MediaItem)var2.next();
         if (m.video && this.matchesSearch(m)) {
            this.mediaRow(m, true);
         }
      }

      if (this.videoCount() == 0) {
         this.pageBody.addView(this.hint("No videos found. Scan device media from More."));
      }

   }

   private void musicPage() {
      this.chips(new String[]{"Songs", "Albums", "Artists", "Folders"}, this.musicMode, (s) -> {
         this.musicMode = s;
         this.render();
      });
      if (this.musicMode.equals("Folders")) {
         this.folderRows(true);
      } else {
         if (this.musicMode.equals("Songs")) {
            ArrayList<MediaItem> songs = new ArrayList();
            for (MediaItem song : this.library) if (!song.video && this.matchesSearch(song)) songs.add(song);
            for (MediaItem song : songs) this.mediaRow(song, true, songs);
         } else {
            LinkedHashMap<String, ArrayList<MediaItem>> groups = new LinkedHashMap();
            Iterator var10 = this.library.iterator();

            while(var10.hasNext()) {
               MediaItem m = (MediaItem)var10.next();
               if (!m.video && this.matchesSearch(m)) {
                  String k = this.musicMode.equals("Albums") ? (m.album.isEmpty() ? "Unknown album" : m.album) : (m.artist.isEmpty() ? "Unknown artist" : m.artist);
                  ((ArrayList)groups.computeIfAbsent(k, (x) -> {
                     return new ArrayList();
                  })).add(m);
               }
            }

            var10 = groups.keySet().iterator();

            while(var10.hasNext()) {
               String k = (String)var10.next();
               MediaItem first = (MediaItem)((ArrayList)groups.get(k)).get(0);
               LinearLayout r = this.row();
               r.setPadding(this.dp(12.0F), this.dp(10.0F), this.dp(10.0F), this.dp(10.0F));
               r.setBackground(this.shape(CARD, 14));
               TextView ic = this.text("♫", 20, LILAC, true);
               r.addView(ic, this.lp(this.dp(42.0F), this.dp(42.0F)));
               LinearLayout info = this.col();
               info.setPadding(this.dp(9.0F), 0, 0, 0);
               info.addView(this.text(k, 13, -1, true));
               info.addView(this.text(((ArrayList)groups.get(k)).size() + " songs", 11, MUTED, false));
               r.addView(info);
               LinearLayout.LayoutParams rp = this.lp(-1, -2);
               rp.bottomMargin = this.dp(6.0F);
               this.pageBody.addView(r, rp);
               r.setOnClickListener((v) -> {
                  this.musicMode = "Songs";
                  this.render();
               });
            }
         }

         if (this.library.isEmpty()) {
            this.pageBody.addView(this.hint("Your songs will appear here after media access is allowed."));
         }

      }
   }

   private void playlistsPage() {
      TextView add = this.button("＋  New playlist", true);
      LinearLayout.LayoutParams ap = this.lp(-1, -2);
      ap.bottomMargin = this.dp(10.0F);
      this.pageBody.addView(add, ap);
      add.setOnClickListener((v) -> {
         this.createPlaylist();
      });
      Iterator var3 = this.playlists.keySet().iterator();

      while(var3.hasNext()) {
         String name = (String)var3.next();
         int n = ((ArrayList)this.playlists.get(name)).size();
         this.playlistRow(name, n);
      }

   }

   private void playlistRow(String name, int n) {
      LinearLayout r = this.row();
      r.setPadding(this.dp(12.0F), this.dp(10.0F), this.dp(10.0F), this.dp(10.0F));
      r.setBackground(this.shape(CARD, 14));
      TextView icon = this.text(name.equals("Favorites") ? "♥" : "♫", 20, LILAC, true);
      icon.setGravity(17);
      icon.setBackground(this.shape(Color.rgb(49, 18, 76), 12));
      r.addView(icon, this.lp(this.dp(44.0F), this.dp(44.0F)));
      LinearLayout info = this.col();
      info.setPadding(this.dp(10.0F), 0, 0, 0);
      info.addView(this.text(name, 13, -1, true));
      info.addView(this.text(n + " tracks", 11, MUTED, false));
      r.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0F));
      TextView more = this.text("⋮", 20, MUTED, true);
      r.addView(more);
      LinearLayout.LayoutParams rp = this.lp(-1, -2);
      rp.bottomMargin = this.dp(7.0F);
      this.pageBody.addView(r, rp);
      r.setOnClickListener((v) -> {
         this.activePlaylist = name;
         this.page = "Playlist";
         this.render();
      });
   }

   private void playlistPage() {
      this.section(this.activePlaylist, (Runnable)null);
      TextView addMedia = this.button("＋  Add media", false);
      this.pageBody.addView(addMedia, this.lp(-1, -2));
      addMedia.setOnClickListener((v) -> this.showPlaylistMediaPicker(this.activePlaylist));
      ArrayList<String> ids = (ArrayList)this.playlists.get(this.activePlaylist);
      ArrayList<MediaItem> playlistQueue = new ArrayList();
      if (ids != null) for (String id : ids) { MediaItem item = this.findMedia(id); if (item != null) playlistQueue.add(item); }
      if (!playlistQueue.isEmpty()) {
         for (MediaItem item : playlistQueue) this.mediaRow(item, true, playlistQueue);
      } else {
         this.pageBody.addView(this.hint("This playlist is empty. Add songs from Music."));
      }
   }

   private void morePage() {
      String[] items = new String[]{"Folders", "Network Stream", "Saved Streams", "Downloads", "Playback History", "Settings", "Open files"};
      String[] glyphs = new String[]{"▰", "◉", "◷", "↓", "▣", "⚙", "＋"};

      for(int i = 0; i < items.length; ++i) {
         String name = items[i];
         LinearLayout r = this.row();
         r.setPadding(this.dp(12.0F), this.dp(12.0F), this.dp(10.0F), this.dp(12.0F));
         r.setBackground(this.shape(CARD, 14));
         TextView ic = this.text(glyphs[i], 18, LILAC, true);
         ic.setGravity(17);
         ic.setBackground(this.shape(Color.rgb(47, 19, 71), 11));
         r.addView(ic, this.lp(this.dp(42.0F), this.dp(42.0F)));
         TextView label = this.text(name, 13, -1, true);
         LinearLayout.LayoutParams p = this.lp(0, -2);
         p.weight = 1.0F;
         p.leftMargin = this.dp(11.0F);
         r.addView(label, p);
         TextView arrow = this.text("›", 20, MUTED, true);
         r.addView(arrow);
         LinearLayout.LayoutParams rp = this.lp(-1, -2);
         rp.bottomMargin = this.dp(7.0F);
         this.pageBody.addView(r, rp);
         r.setOnClickListener((v) -> {
            switch (name) {
               case "Folders":
                  this.page = "Folders";
                  break;
               case "Network Stream":
                  this.page = "Network Stream";
                  break;
               case "Saved Streams":
                  this.page = "Network Stream";
                  break;
               case "Downloads":
                  this.activeFolder = "Download";
                  this.page = "Folder";
                  break;
               case "Playback History":
                  this.page = "Home";
                  break;
               case "Settings":
                  this.page = "Settings";
                  break;
               default:
                  this.pickMedia();
                  return;
            }

            this.render();
         });
      }

      LinearLayout castRow = this.row(); castRow.setPadding(this.dp(12), this.dp(8), this.dp(12), this.dp(8)); castRow.setBackground(this.shape(CARD, 13));
      LinearLayout castInfo = this.col();
      castInfo.addView(this.text("Cast to TV or speaker", 13, Color.WHITE, true));
      this.castStatusText = this.hint(""); castInfo.addView(this.castStatusText); this.updateCastUiStatus();
      castRow.addView(castInfo, new LinearLayout.LayoutParams(0, -2, 1.0F)); castRow.addView(this.createCastButton(), this.lp(this.dp(48), this.dp(48)));
      castRow.setOnClickListener(v -> {
         if (this.castContext == null) this.toast("Casting could not initialize. Check Google Play services and reopen Solo Player.");
         else castRow.getChildAt(1).performClick();
      });
      LinearLayout.LayoutParams castRowLp = this.lp(-1, -2); castRowLp.bottomMargin = this.dp(7); this.pageBody.addView(castRow, castRowLp);
      TextView refresh = this.button("↻  Scan device media", false);
      this.pageBody.addView(refresh);
      refresh.setOnClickListener((v) -> {
         this.lastLibraryScanAt = 0L;
         this.ensureMediaPermission();
      });
   }

   private void privateMediaPage() {
      this.pageBody.addView(this.hint("Your private files are encrypted in Solo Player's app storage. Copy keeps the original visible to other apps; Move asks Android to remove the original where supported."));
      LinearLayout actions = this.row();
      TextView addFiles = this.button("＋  Add media", true);
      TextView addFolder = this.button("▰  Add folder", false);
      LinearLayout.LayoutParams half = this.lp(0, -2); half.weight = 1f; half.rightMargin = this.dp(5);
      actions.addView(addFiles, half);
      LinearLayout.LayoutParams half2 = this.lp(0, -2); half2.weight = 1f; half2.leftMargin = this.dp(5);
      actions.addView(addFolder, half2);
      this.pageBody.addView(actions, this.lp(-1, -2));
      addFiles.setOnClickListener(v -> this.pickPrivateVideos());
      addFolder.setOnClickListener(v -> this.pickPrivateFolder());
      LinearLayout security = this.row();
      TextView changePin = this.button("Change PIN", false); changePin.setOnClickListener(v -> this.changePrivateVaultPin());
      TextView lock = this.button("🔒 Lock", false); lock.setOnClickListener(v -> { this.privateVaultUnlocked = false; this.privateMediaPath = ""; this.page = "More"; this.render(); });
      LinearLayout.LayoutParams sec1 = this.lp(0,-2); sec1.weight=1f; sec1.rightMargin=this.dp(5); security.addView(changePin,sec1);
      LinearLayout.LayoutParams sec2 = this.lp(0,-2); sec2.weight=1f; sec2.leftMargin=this.dp(5); security.addView(lock,sec2);
      this.pageBody.addView(security,this.lp(-1,-2));
      TextView recovery = this.button("Forgot PIN? Reset securely", false);
      this.pageBody.addView(recovery, this.lp(-1, -2)); recovery.setOnClickListener(v -> this.resetPrivateVaultPin());
      EditText search = this.edit("Search private media");
      search.setText(this.privateMediaSearch);
      LinearLayout.LayoutParams searchLp=this.lp(-1,this.dp(42));searchLp.topMargin=this.dp(8);this.pageBody.addView(search,searchLp);
      LinearLayout listing=this.col(); LinearLayout.LayoutParams listLp=this.lp(-1,-2);listLp.topMargin=this.dp(8);this.pageBody.addView(listing,listLp);
      if(this.privateVaultSelectionMode){LinearLayout bar=this.row();TextView count=this.text(this.privateSelectedIds.size()+" selected",13,LILAC,true);bar.addView(count,new LinearLayout.LayoutParams(0,-2,1));TextView remove=this.button("Remove",false);bar.addView(remove);TextView cancel=this.button("Cancel",false);bar.addView(cancel);listing.addView(bar);remove.setOnClickListener(v->{if(this.privateSelectedIds.isEmpty()){this.toast("Select at least one item");return;}new AlertDialog.Builder(this).setTitle("Remove selected private copies?").setMessage("Only the encrypted vault copies are removed. Originals that were copied remain where they are; moved originals stay in the vault until restored.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{for(String id:new ArrayList<>(this.privateSelectedIds))this.removePrivateVaultRecordOnly(id);this.privateSelectedIds.clear();this.privateVaultSelectionMode=false;this.lastLibraryScanAt=0L;this.scanLibrary();this.render();this.toast("Selected items removed from Private Media");}).show();});cancel.setOnClickListener(v->{this.privateVaultSelectionMode=false;this.privateSelectedIds.clear();this.render();});}
      if (!this.privateMediaPath.isEmpty()) {
         TextView back = this.button("‹  Folders  /  " + this.privateMediaPath, false);
         listing.addView(back, this.lp(-1,-2));
         back.setOnClickListener(v -> { int slash=this.privateMediaPath.lastIndexOf('/'); this.privateMediaPath=slash<0?"":this.privateMediaPath.substring(0,slash); this.render(); });
      }
      Set<String> folders = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      ArrayList<JSONObject> files = new ArrayList<>();
      for (int i=0;i<this.privateVideos.length();i++) {
         JSONObject entry=this.privateVideos.optJSONObject(i); if(entry==null) continue;
         String path=entry.optString("folderPath", "");
         if (path.equals(this.privateMediaPath)) { if (!"folder".equals(entry.optString("kind","file"))) files.add(entry); continue; }
         String prefix=this.privateMediaPath.isEmpty()?"":this.privateMediaPath+"/";
         if (!path.startsWith(prefix)) continue;
         String tail=path.substring(prefix.length()); int slash=tail.indexOf('/');
         if (slash>=0) folders.add(prefix+tail.substring(0,slash));
      }
      for (String folder : folders) {
         LinearLayout row=this.row(); row.setPadding(this.dp(12),this.dp(12),this.dp(10),this.dp(12)); row.setBackground(this.shape(CARD,14));
         TextView icon=this.text("▰",20,LILAC,true); icon.setGravity(Gravity.CENTER); row.addView(icon,this.lp(this.dp(44),this.dp(44)));
         String label=folder.substring(folder.lastIndexOf('/')+1); TextView title=this.text(label,14,Color.WHITE,true); LinearLayout.LayoutParams tlp=this.lp(0,-2);tlp.weight=1;tlp.leftMargin=this.dp(10);row.addView(title,tlp);
         row.setTag(label);
         TextView arrow=this.text("›",22,MUTED,true);row.addView(arrow); LinearLayout.LayoutParams rlp=this.lp(-1,-2);rlp.bottomMargin=this.dp(7);listing.addView(row,rlp);
         row.setOnClickListener(v->{this.privateMediaPath=folder;this.render();});
      }
      for (JSONObject entry : files) {
         final String id=entry.optString("id", ""); final String name=entry.optString("name","Private media"); final String kind=entry.optString("kind","file");
         boolean chosen=this.privateSelectedIds.contains(id);LinearLayout row=this.row();row.setPadding(this.dp(12),this.dp(12),this.dp(10),this.dp(12));row.setBackground(this.shape(chosen?Color.rgb(57,24,83):CARD,14));
         row.setTag(name);
         TextView icon=this.text("image".equals(kind)?"▧":(this.isVideoName(name)?"▣":"♫"),20,LILAC,true);icon.setGravity(Gravity.CENTER);row.addView(icon,this.lp(this.dp(44),this.dp(44)));
         TextView title=this.text(name,13,Color.WHITE,true);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams titleLp=this.lp(0,-2);titleLp.weight=1;titleLp.leftMargin=this.dp(10);row.addView(title,titleLp);
         String detail=entry.optString("mime","Media file")+(entry.optBoolean("sourceDeleted",false)?" · moved":" · encrypted copy");
         TextView subtitle=this.text(detail,10,MUTED,false);LinearLayout info=this.col();info.addView(title);info.addView(subtitle);row.removeView(title);row.addView(info,titleLp);
         if(this.privateVaultSelectionMode)row.addView(this.text(chosen?"✓":"○",18,chosen?LILAC:MUTED,true));TextView remove=this.text("⋮",21,MUTED,true);row.addView(remove);
         row.setOnClickListener(v->{if(this.privateVaultSelectionMode){if(!this.privateSelectedIds.add(id))this.privateSelectedIds.remove(id);this.pageViewCache.clear();this.attachedPageCacheKey=null;this.render();}else this.openPrivateMedia(id);});
         row.setOnLongClickListener(v->{if(!this.privateVaultSelectionMode){this.privateVaultSelectionMode=true;this.privateSelectedIds.clear();this.privateSelectedIds.add(id);this.pageViewCache.clear();this.attachedPageCacheKey=null;this.render();return true;}return false;});
         remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Restore to public storage","Remove encrypted private copy"},(d,w)->{
            if(w==0)this.restorePrivateMedia(id);else new AlertDialog.Builder(this).setTitle("Remove private copy?").setMessage("This deletes the encrypted vault copy. If you previously chose Copy, the original will remain in its original location.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(dd,ww)->{this.removePrivateVideo(id);this.toast("Removed from Private Media");}).show();
         }).show());
         LinearLayout.LayoutParams rowLp=this.lp(-1,-2);rowLp.bottomMargin=this.dp(7);listing.addView(row,rowLp);
      }
      if (folders.isEmpty() && files.isEmpty()) listing.addView(this.hint(this.privateMediaPath.isEmpty()?"No private media yet. Add files or an entire folder.":"This folder contains no supported media."));
      search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){privateMediaSearch=s.toString().trim().toLowerCase(Locale.ROOT);for(int i=0;i<listing.getChildCount();i++){View item=listing.getChildAt(i);Object tag=item.getTag();String label=tag==null?"":tag.toString().toLowerCase(Locale.ROOT);item.setVisibility(privateMediaSearch.isEmpty()||label.contains(privateMediaSearch)?View.VISIBLE:View.GONE);}}public void afterTextChanged(Editable e){}});
   }

   private File vaultDirectory() {
      File dir = new File(this.getNoBackupFilesDir(), "private-videos");
      if (!dir.exists()) dir.mkdirs();
      return dir;
   }

   private void openPrivateVault() {
      KeyguardManager keyguard = (KeyguardManager)this.getSystemService(KEYGUARD_SERVICE);
      if (keyguard == null || !keyguard.isDeviceSecure()) {
         new AlertDialog.Builder(this).setTitle("Enable secure PIN recovery")
            .setMessage("Set a secure screen lock first. It is only used to recover your Private Media PIN.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open security settings", (d, w) -> {
               try { this.startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)); }
               catch (Exception error) { this.toast("Could not open security settings"); }
            }).show();
         return;
      }
      if (this.hasPrivateVaultPin()) {
         this.showPrivateVaultUnlock();
      } else {
         this.createPrivateVaultPin("setup");
      }
   }

   private boolean hasPrivateVaultPin() {
      return !this.prefs().getString("private_vault_pin_hash", "").isEmpty()
         && !this.prefs().getString("private_vault_pin_salt", "").isEmpty();
   }

   private void showPrivatePinDialog(String title, String detail, String actionLabel, boolean allowRecovery, PrivatePinAction action) {
      EditText input = new EditText(this);
      input.setSingleLine(true);
      input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
      input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
      input.setHint("4–8 digit PIN"); input.setHintTextColor(MUTED); input.setTextColor(Color.WHITE);
      input.setGravity(Gravity.CENTER); input.setPadding(this.dp(14), this.dp(12), this.dp(14), this.dp(12));
      input.setBackground(this.shape(CARD, 12));
      TextView feedback = this.text("", 12, Color.rgb(255, 112, 130), false);
      LinearLayout content = this.col(); content.setPadding(this.dp(18), this.dp(8), this.dp(18), this.dp(4));
      content.addView(this.text(detail, 13, MUTED, false));
      LinearLayout.LayoutParams inputLp = this.lp(-1, -2); inputLp.topMargin = this.dp(14); content.addView(input, inputLp);
      LinearLayout.LayoutParams feedbackLp = this.lp(-1, -2); feedbackLp.topMargin = this.dp(6); content.addView(feedback, feedbackLp);
      AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(title).setView(content)
         .setNegativeButton("Cancel", null).setPositiveButton(actionLabel, null);
      if (allowRecovery) builder.setNeutralButton("Forgot PIN?", (d, w) -> this.resetPrivateVaultPin());
      AlertDialog dialog = builder.create();
      dialog.setOnShowListener(v -> {
         this.styleThemedDialog(dialog);
         dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String problem = action.submit(input.getText().toString());
            if (problem == null) dialog.dismiss(); else feedback.setText(problem);
         });
      });
      dialog.show();
      if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      input.requestFocus();
   }

   private void showPrivateVaultUnlock() {
      this.showPrivatePinDialog("Unlock Private Media", "Enter your PIN to open the encrypted vault.", "Unlock", true, pin -> {
         if (!this.validVaultPin(pin)) return "Enter a PIN with 4 to 8 digits.";
         if (!this.matchesPrivateVaultPin(pin)) return "Incorrect PIN. Try again or use secure recovery.";
         this.privateVaultUnlocked = true; this.page = "Private Media"; this.render();
         return null;
      });
   }

   private void createPrivateVaultPin(String flow) {
      this.showPrivatePinDialog("Create Private Media PIN", "Choose a 4 to 8 digit PIN. You will confirm it next.", "Continue", false, pin -> {
         if (!this.validVaultPin(pin)) return "PIN must contain 4 to 8 digits.";
         this.confirmPrivateVaultPin(pin, flow);
         return null;
      });
   }

   private void confirmPrivateVaultPin(String firstPin, String flow) {
      this.showPrivatePinDialog("Confirm Private Media PIN", "Enter the same PIN again to confirm it.", "Save PIN", false, pin -> {
         if (!firstPin.equals(pin)) return "The PINs do not match. Try again.";
         try {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            byte[] digest = this.derivePrivateVaultPin(pin, salt);
            this.prefs().edit().putString("private_vault_pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
               .putString("private_vault_pin_hash", Base64.encodeToString(digest, Base64.NO_WRAP)).apply();
            this.privateVaultUnlocked = true; this.page = "Private Media"; this.render();
            this.toast(flow.equals("change") ? "Private Media PIN changed" : "Private Media PIN created");
            return null;
         } catch (Exception error) { Log.e("SoloPlayer", "Could not save Private Media PIN", error); return "Could not save the PIN. Please try again."; }
      });
   }

   private boolean validVaultPin(String pin) { return pin != null && pin.matches("[0-9]{4,8}"); }

   private byte[] derivePrivateVaultPin(String pin, byte[] salt) throws Exception {
      PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
      try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
      finally { spec.clearPassword(); }
   }

   private boolean matchesPrivateVaultPin(String pin) {
      try {
         byte[] salt = Base64.decode(this.prefs().getString("private_vault_pin_salt", ""), Base64.NO_WRAP);
         byte[] expected = Base64.decode(this.prefs().getString("private_vault_pin_hash", ""), Base64.NO_WRAP);
         return MessageDigest.isEqual(expected, this.derivePrivateVaultPin(pin, salt));
      } catch (Exception error) { Log.w("SoloPlayer", "Could not verify Private Media PIN", error); return false; }
   }

   private void changePrivateVaultPin() {
      this.showPrivatePinDialog("Verify current PIN", "Enter the current PIN before changing it.", "Continue", true, pin -> {
         if (!this.validVaultPin(pin) || !this.matchesPrivateVaultPin(pin)) return "Current PIN is incorrect.";
         this.createPrivateVaultPin("change");
         return null;
      });
   }

   private void resetPrivateVaultPin() {
      KeyguardManager keyguard = (KeyguardManager)this.getSystemService(KEYGUARD_SERVICE);
      if (keyguard == null || !keyguard.isDeviceSecure()) {
         this.toast("A secure device screen lock is required to recover the PIN.");
         return;
      }
      try {
         BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) { createPrivateVaultPin("reset"); }
            @Override public void onAuthenticationError(int code, CharSequence message) {
               if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) toast("PIN was not reset");
            }
         });
         prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Recover Private Media PIN")
            .setSubtitle("Verify with your device screen lock or biometrics").setDeviceCredentialAllowed(true).build());
      } catch (Exception error) {
         Intent confirm = keyguard.createConfirmDeviceCredentialIntent("Recover Private Media PIN", "Verify your device screen lock to set a new PIN");
         if (confirm == null) { this.toast("Device authentication is unavailable"); return; }
         this.startActivityForResult(confirm, RESET_PRIVATE_VAULT_PIN);
      }
   }

   private interface PrivatePinAction { String submit(String pin); }

   private void pickPrivateVideos() {
      Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT); picker.setType("*/*");
      picker.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","audio/*","image/*"});
      picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true); picker.addCategory(Intent.CATEGORY_OPENABLE);
      picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
      this.pickerOpen=true; this.startActivityForResult(picker,PICK_VAULT_FILES);
   }

   private void pickPrivateFolder() {
      Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
      this.pickerOpen=true; this.startActivityForResult(picker,PICK_VAULT_FOLDER);
   }

   private SecretKey vaultKey() throws Exception {
      KeyStore store=KeyStore.getInstance("AndroidKeyStore"); store.load(null); String alias="solo_private_video_vault_v1";
      if(store.containsAlias(alias)) return (SecretKey)store.getKey(alias,null);
      KeyGenerator generator=KeyGenerator.getInstance("AES","AndroidKeyStore");
      generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(alias,android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
         .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
      return generator.generateKey();
   }

   private String displayName(Uri uri) {
      try(Cursor cursor=this.getContentResolver().query(uri,null,null,null,null)) { if(cursor!=null&&cursor.moveToFirst()){int i=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(i>=0)return cursor.getString(i);} } catch(Exception ignored){}
      String value=uri.getLastPathSegment(); return value==null?"Private media":value;
   }

   private String privateSourcePath(Uri uri) {
      try { if(DocumentsContract.isDocumentUri(this,uri)){String id=DocumentsContract.getDocumentId(uri);int colon=id.indexOf(':');if(colon>=0)id=id.substring(colon+1);return id.replace('\\','/');} } catch(Exception ignored){}
      return "";
   }

   private String privateMime(Uri uri,String name) {
      try { String type=this.getContentResolver().getType(uri); if(type!=null)return type; }catch(Exception ignored){}
      String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
      String mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext); return mime==null?"application/octet-stream":mime;
   }

   private JSONObject encryptPrivateMedia(Uri uri,String folderPath) throws Exception {
      String name=this.displayName(uri), mime=this.privateMime(uri,name);
      if(!(mime.startsWith("video/")||mime.startsWith("audio/")||mime.startsWith("image/")||this.isMediaName(name)||this.isImageName(name))) return null;
      String id=UUID.randomUUID().toString(); File encrypted=new File(this.vaultDirectory(),id+".svlt"); File partial=new File(this.vaultDirectory(),id+".part");
      try(InputStream source=this.getContentResolver().openInputStream(uri)) {
         if(source==null)throw new IOException("File could not be opened");
         Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,this.vaultKey());byte[] iv=cipher.getIV();
         try(OutputStream raw=new BufferedOutputStream(new FileOutputStream(partial))){raw.write(iv.length);raw.write(iv);try(CipherOutputStream secure=new CipherOutputStream(raw,cipher)){byte[] buffer=new byte[64*1024];int read;while((read=source.read(buffer))!=-1)secure.write(buffer,0,read);}}
         if(!partial.renameTo(encrypted))throw new IOException("Could not finalize encrypted file");
      } catch(Exception e){partial.delete();encrypted.delete();throw e;}
      JSONObject entry=new JSONObject();entry.put("id",id);entry.put("name",name);entry.put("mime",mime);entry.put("kind",mime.startsWith("image/")||this.isImageName(name)?"image":"file");entry.put("folderPath",folderPath==null?"":folderPath);entry.put("sourceUri",uri.toString());entry.put("sourcePath",this.privateSourcePath(uri));
      for(MediaItem item:this.library)if(uri.toString().equals(item.uri)){entry.put("sourceRelativePath",item.path==null?"":item.path);break;}
      return entry;
   }

   private boolean isPrivateDuplicate(Uri uri) {
      String target=uri==null?"":uri.toString();String path=this.privateSourcePath(uri);
      for(int i=0;i<this.privateVideos.length();i++){JSONObject entry=this.privateVideos.optJSONObject(i);if(entry==null)continue;if(target.equals(entry.optString("sourceUri","")))return true;if(!path.isEmpty()&&path.equals(entry.optString("sourcePath","")))return true;}
      return false;
   }

   private void askPrivateImportMode(ArrayList<Uri> uris, String folderPath) {
      if(uris==null||uris.isEmpty())return;
      new AlertDialog.Builder(this).setTitle("Add to Private Media")
         .setMessage("Copy encrypts a private version and leaves the original visible to other apps. Move encrypts first, then asks Android to remove the original where supported.")
         .setNegativeButton("Cancel",null)
         .setNeutralButton("Copy",(d,w)->this.processPrivateImports(uris,folderPath,false))
         .setPositiveButton("Move",(d,w)->this.processPrivateImports(uris,folderPath,true)).show();
   }

   private void processPrivateImports(ArrayList<Uri> uris,String folderPath,boolean move) {
      final ArrayList<Uri> batch=new ArrayList<>(uris);final int total=batch.size();
      android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setTitle(move?"Moving to Private Media":"Encrypting Private Media");progress.setMessage("Preparing selected media…");progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);progress.setMax(total);progress.setProgress(0);progress.setCancelable(false);progress.show();this.privateImportProgress=progress;
      final ArrayList<JSONObject> imported=new ArrayList<>();final ArrayList<Uri> movable=new ArrayList<>();final int[] skipped={0},failed={0};
      this.worker.execute(()->{
         for(int i=0;i<batch.size();i++){Uri uri=batch.get(i);final int step=i+1;this.handler.post(()->{if(progress.isShowing())progress.setMessage("Encrypting "+this.displayName(uri)+" ("+step+" of "+total+")");});
            try{if(this.isPrivateDuplicate(uri)){skipped[0]++;continue;}JSONObject entry=this.encryptPrivateMedia(uri,folderPath);if(entry==null){skipped[0]++;continue;}synchronized(this.privateVideos){this.privateVideos.put(entry);}imported.add(entry);if(move&&"media".equals(uri.getAuthority()))movable.add(uri);}
            catch(Exception error){failed[0]++;Log.w("SoloPlayer","Could not encrypt private item "+uri,error);}
            final int done=i+1;this.handler.post(()->{if(progress.isShowing())progress.setProgress(done);});
         }
         this.handler.post(()->{
            if(progress.isShowing())progress.dismiss();this.privateImportProgress=null;
            if(imported.isEmpty()){this.toast(failed[0]>0?"No media was added; "+failed[0]+" item(s) failed":"No new supported media selected");return;}
            for(JSONObject entry:imported)this.hidePrivateMediaFromPublicLists(entry);this.savePrivateVideos();this.page="Private Media";this.privateVaultUnlocked=true;this.render();
            if(move&&!movable.isEmpty()&&VERSION.SDK_INT>=30){this.pendingPrivateMoveIds.clear();for(JSONObject entry:imported)if("media".equals(Uri.parse(entry.optString("sourceUri","")).getAuthority()))this.pendingPrivateMoveIds.add(entry.optString("id"));
               try{android.app.PendingIntent request=MediaStore.createDeleteRequest(getContentResolver(),movable);startIntentSenderForResult(request.getIntentSender(),MOVE_PRIVATE_MEDIA,null,0,0,0);}
               catch(Exception error){this.pendingPrivateMoveIds.clear();this.toast(imported.size()+" item(s) encrypted. Android could not open the delete confirmation; originals remain visible in other apps.");}
            }else if(move){this.toast(imported.size()+" item(s) encrypted. Android move is available for MediaStore files on Android 11 or later; originals were kept.");}
            else this.toast(imported.size()+" item(s) encrypted in Private Media"+(skipped[0]>0?" · "+skipped[0]+" duplicate/unsupported skipped":"")+(failed[0]>0?" · "+failed[0]+" failed":""));
         });
      });
   }

   private void importPrivateMedia(Uri uri) { ArrayList<Uri> selected=new ArrayList<>();selected.add(uri);this.askPrivateImportMode(selected,this.privateMediaPath); }

   private void importPrivateFolder(Uri tree) {
      try{this.getContentResolver().takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){}
      new AlertDialog.Builder(this).setTitle("Add folder to Private Media")
         .setMessage("The folder structure is preserved in the encrypted vault. Copy keeps originals visible to other apps; Move encrypts first, then removes supported files through the selected storage provider where allowed.")
         .setNegativeButton("Cancel",null).setNeutralButton("Copy",(d,w)->this.startPrivateFolderImport(tree,false))
         .setPositiveButton("Move",(d,w)->this.startPrivateFolderImport(tree,true)).show();
   }

   private void startPrivateFolderImport(Uri tree,boolean move) {
      final String rootName=this.displayDocumentName(tree); final String prefix=this.privateMediaPath.isEmpty()?rootName:this.privateMediaPath+"/"+rootName;
      android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setTitle(move?"Moving folder":"Encrypting folder");progress.setMessage("Scanning and encrypting supported media…");progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);progress.setIndeterminate(true);progress.setCancelable(false);progress.show();this.privateImportProgress=progress;
      this.worker.execute(()->{ArrayList<JSONObject> imported=new ArrayList<>();int[] skipped={0},processed={0};try{this.scanAndEncryptPrivateTree(tree,DocumentsContract.getTreeDocumentId(tree),prefix,imported,skipped,processed);}
         catch(Exception error){Log.e("SoloPlayer","Private folder import failed",error);}
         int moved=0;if(move)for(JSONObject entry:imported){String source=entry.optString("sourceDocumentUri","");try{if(!source.isEmpty()&&DocumentsContract.deleteDocument(getContentResolver(),Uri.parse(source))){entry.put("sourceDeleted",true);moved++;}}catch(Exception error){Log.w("SoloPlayer","Provider did not allow moving private document",error);}}
         final int count=imported.size(),skip=skipped[0],movedCount=moved;this.handler.post(()->{if(progress.isShowing())progress.dismiss();this.privateImportProgress=null;for(JSONObject entry:imported){if(!this.isPrivateDuplicate(Uri.parse(entry.optString("sourceUri","")))){this.privateVideos.put(entry);this.hidePrivateMediaFromPublicLists(entry);}else new File(this.vaultDirectory(),entry.optString("id")+".svlt").delete();}this.savePrivateVideos();this.privateMediaPath="";this.page="Private Media";this.privateVaultUnlocked=true;this.render();this.toast(count>0?(move?(movedCount+" original file(s) removed; "+(count-movedCount)+" kept by provider policy"):(count+" media files encrypted"))+(skip>0?" · "+skip+" unsupported/duplicate skipped":""):"No supported media files found in that folder");});});
   }

   private String displayDocumentName(Uri uri) {
      try{String id=DocumentsContract.getTreeDocumentId(uri);int colon=id.lastIndexOf(':');String tail=colon>=0?id.substring(colon+1):id;int slash=tail.lastIndexOf('/');return slash>=0?tail.substring(slash+1):tail;}catch(Exception e){return "Folder";}
   }

   private void scanAndEncryptPrivateTree(Uri tree,String documentId,String path,ArrayList<JSONObject> imported,int[] skipped,int[] processed) throws Exception {
      Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,documentId);
      try(Cursor cursor=this.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
         if(cursor==null)return;int idCol=cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID),nameCol=cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME),mimeCol=cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
         while(cursor.moveToNext()){String childId=cursor.getString(idCol),name=cursor.getString(nameCol),mime=cursor.getString(mimeCol);String childPath=path.isEmpty()?name:path+"/"+name;
            if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){scanAndEncryptPrivateTree(tree,childId,childPath,imported,skipped,processed);continue;}
            Uri child=DocumentsContract.buildDocumentUriUsingTree(tree,childId);
            try{if(this.isPrivateDuplicate(child)){skipped[0]++;continue;}JSONObject entry=this.encryptPrivateMedia(child,path);if(entry!=null){entry.put("sourcePath",this.privateSourcePath(child));entry.put("treeUri",tree.toString());entry.put("sourceDocumentUri",child.toString());String rootId=DocumentsContract.getTreeDocumentId(tree);String relative=childId.startsWith(rootId)?childId.substring(rootId.length()).replaceFirst("^/+",""):name;entry.put("treeRelativePath",relative);entry.put("folderPath",path);imported.add(entry);}else skipped[0]++;}catch(Exception error){skipped[0]++;Log.w("SoloPlayer","Could not add private document "+name,error);}finally{processed[0]++;if(processed[0]==1||processed[0]%10==0){final int count=processed[0];this.handler.post(()->{if(this.privateImportProgress!=null&&this.privateImportProgress.isShowing())this.privateImportProgress.setMessage("Encrypted "+count+" supported file(s)…");});}}
         }
      }
   }

   private void savePrivateVideos() { this.prefs().edit().putString("private_media",this.privateVideos.toString()).putString("private_videos",this.privateVideos.toString()).apply();this.pageViewCache.clear();this.attachedPageCacheKey=null; }

   private File decryptPrivateMedia(JSONObject entry) throws Exception {
      String id=entry.optString("id","");File encrypted=new File(this.vaultDirectory(),id+".svlt");String name=entry.optString("name","media.bin");String suffix=name.contains(".")?name.substring(name.lastIndexOf('.')):".bin";
      File output=File.createTempFile("solo-vault-",suffix,this.getCacheDir());
      try(InputStream raw=new BufferedInputStream(new FileInputStream(encrypted))){int ivLength=raw.read();if(ivLength<=0||ivLength>32)throw new IOException("Private file header is invalid");byte[] iv=new byte[ivLength];int n=0;while(n<ivLength){int read=raw.read(iv,n,ivLength-n);if(read<0)throw new IOException("Private file header is incomplete");n+=read;}Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,this.vaultKey(),new GCMParameterSpec(128,iv));try(CipherInputStream secure=new CipherInputStream(raw,cipher);OutputStream out=new BufferedOutputStream(new FileOutputStream(output))){byte[] buffer=new byte[64*1024];int read;while((read=secure.read(buffer))!=-1)out.write(buffer,0,read);}}
      return output;
   }

   private void openPrivateMedia(String id) {
      if(!this.privateVaultUnlocked){this.openPrivateVault();return;}JSONObject entry=this.privateVideoEntry(id);if(entry==null)return;
      try{File temp=this.decryptPrivateMedia(entry);String name=entry.optString("name","Private media");String kind=entry.optString("kind","file");
         if("image".equals(kind)){ImageView image=new ImageView(this);image.setImageBitmap(BitmapFactory.decodeFile(temp.getAbsolutePath()));image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setPadding(this.dp(12),this.dp(12),this.dp(12),this.dp(12));new AlertDialog.Builder(this).setTitle(name).setView(image).setPositiveButton("Close",(d,w)->temp.delete()).setOnCancelListener(d->temp.delete()).show();return;}
         boolean video=this.isVideoName(name)||entry.optString("mime","").startsWith("video/");
         ArrayList<JSONObject> siblings=new ArrayList<>();String folder=entry.optString("folderPath","");for(int i=0;i<this.privateVideos.length();i++){JSONObject sibling=this.privateVideos.optJSONObject(i);if(sibling!=null&&"file".equals(sibling.optString("kind","file"))&&folder.equals(sibling.optString("folderPath","")))siblings.add(sibling);}
         this.queue.clear();for(JSONObject sibling:siblings){File playable=sibling==entry?temp:this.decryptPrivateMedia(sibling);MediaItem item=new MediaItem(sibling.optString("name","Private media"),Uri.fromFile(playable).toString(),this.isVideoName(sibling.optString("name",""))||sibling.optString("mime","").startsWith("video/"));item.privateVault=true;item.privateVaultId=sibling.optString("id");item.path="";this.queue.add(item);}
         MediaItem selected=null;for(MediaItem item:this.queue)if(id.equals(item.privateVaultId)){selected=item;break;}if(selected==null){selected=new MediaItem(name,Uri.fromFile(temp).toString(),video);selected.privateVault=true;selected.privateVaultId=id;this.queue.clear();this.queue.add(selected);}this.play(selected);
      }catch(Exception error){Log.e("SoloPlayer","Private media decrypt/play failed",error);this.toast("Could not open private media: "+error.getMessage());}
   }

   private JSONObject privateVideoEntry(String id) { for(int i=0;i<this.privateVideos.length();i++){JSONObject entry=this.privateVideos.optJSONObject(i);if(entry!=null&&entry.optString("id").equals(id))return entry;}return null; }

   private void hidePrivateMediaFromPublicLists(JSONObject entry){JSONArray one=new JSONArray();one.put(entry);this.library.removeIf(item->this.isPrivateSource(item,one));this.recentPlayed.removeIf(item->this.isPrivateSource(item,this.privateVideos));this.saveData();}

   private void removePrivateVideo(String id) {
      this.removePrivateVaultRecordOnly(id);this.lastLibraryScanAt=0L;this.scanLibrary();this.render();
   }

   private void restorePrivateMedia(String id) {
      JSONObject entry=this.privateVideoEntry(id);if(entry==null)return;
      if(!entry.optBoolean("sourceDeleted",false)){this.removePrivateVideo(id);this.toast("Removed from Private Media; the original file was already in its original location.");return;}
      if(entry.has("treeUri")&&!entry.optString("treeRelativePath","").isEmpty()){
         new AlertDialog.Builder(this).setTitle("Restore media?").setMessage("The encrypted file will be restored inside the folder you originally selected.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(d,w)->this.restoreMovedMedia(id)).show();return;
      }
      if(VERSION.SDK_INT<29||!"media".equals(Uri.parse(entry.optString("sourceUri","")).getAuthority())){this.toast("This provider does not expose a safe restore destination. The private copy is retained.");return;}
      new AlertDialog.Builder(this).setTitle("Restore media?").setMessage("The encrypted file will be restored to shared storage and will appear in the normal media library again.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(d,w)->this.restoreMovedMedia(id)).show();
   }

   private void removePrivateVaultRecordOnly(String id) {
      JSONObject entry=this.privateVideoEntry(id);if(entry==null)return;new File(this.vaultDirectory(),id+".svlt").delete();JSONArray kept=new JSONArray();for(int i=0;i<this.privateVideos.length();i++){JSONObject item=this.privateVideos.optJSONObject(i);if(item!=null&&!item.optString("id").equals(id))kept.put(item);}this.privateVideos=kept;this.savePrivateVideos();
   }

   private void restoreMovedMedia(String id) {
      JSONObject entry=this.privateVideoEntry(id);if(entry==null)return;
      android.app.ProgressDialog progress=new android.app.ProgressDialog(this);progress.setMessage("Restoring "+entry.optString("name","media")+"…");progress.setCancelable(false);progress.show();
      this.worker.execute(()->{String problem=null;Uri restored=null;File temp=null;try{
         String mime=entry.optString("mime","application/octet-stream");
         if(entry.has("treeUri")&&!entry.optString("treeRelativePath","").isEmpty())restored=this.createPrivateRestoreDocument(Uri.parse(entry.optString("treeUri")),entry.optString("treeRelativePath"),mime);
         else {if(VERSION.SDK_INT<29)throw new IOException("Android cannot restore this item through MediaStore on this version");Uri collection=mime.startsWith("video/")?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:mime.startsWith("audio/")?MediaStore.Audio.Media.EXTERNAL_CONTENT_URI:mime.startsWith("image/")?MediaStore.Images.Media.EXTERNAL_CONTENT_URI:MediaStore.Files.getContentUri("external");
            android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,entry.optString("name","Restored media"));values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,mime);
            String relative=entry.optString("sourceRelativePath","");if(relative.isEmpty())relative=mime.startsWith("video/")?"Movies/":mime.startsWith("audio/")?"Music/":mime.startsWith("image/")?"Pictures/":"Download/";values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,relative);
            restored=getContentResolver().insert(collection,values);if(restored==null)throw new IOException("Android could not create the restored file");}
         temp=this.decryptPrivateMedia(entry);try(InputStream input=new BufferedInputStream(new FileInputStream(temp));OutputStream output=getContentResolver().openOutputStream(restored,"w")){if(output==null)throw new IOException("Could not open the restored file");byte[] buffer=new byte[64*1024];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);output.flush();}
      }catch(Exception error){problem=error.getMessage()==null?"Restore failed":error.getMessage();if(restored!=null)try{getContentResolver().delete(restored,null,null);}catch(Exception ignored){}Log.e("SoloPlayer","Private media restore failed",error);}finally{if(temp!=null)temp.delete();}
         final String failure=problem;this.handler.post(()->{if(progress.isShowing())progress.dismiss();if(failure!=null){this.toast("Could not restore media: "+failure);return;}this.removePrivateVaultRecordOnly(id);this.lastLibraryScanAt=0L;this.scanLibrary();this.render();this.toast("Restored to shared storage");});
      });
   }

   private Uri createPrivateRestoreDocument(Uri tree,String relative,String mime) throws Exception {
      String[] parts=relative.replace('\\','/').split("/");if(parts.length==0)throw new IOException("Original folder path is missing");String parentId=DocumentsContract.getTreeDocumentId(tree);
      for(int i=0;i<parts.length-1;i++){String part=parts[i];if(part.isEmpty())continue;String found=null;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parentId);try(Cursor cursor=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(cursor!=null)while(cursor.moveToNext())if(part.equals(cursor.getString(1))&&DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))){found=cursor.getString(0);break;}}if(found==null){Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,parentId);Uri created=DocumentsContract.createDocument(getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,part);if(created==null)throw new IOException("Could not recreate original folder: "+part);found=DocumentsContract.getDocumentId(created);}parentId=found;}
      String fileName=parts[parts.length-1];Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,parentId);Uri created=DocumentsContract.createDocument(getContentResolver(),parent,mime,fileName);if(created==null)throw new IOException("Could not create restored file");return created;
   }

   private boolean isImageName(String name){return name!=null&&name.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|gif|webp|bmp|heic|heif)$");}

   private JSONArray privateMediaSnapshot(){try{synchronized(this.privateVideos){return new JSONArray(this.privateVideos.toString());}}catch(Exception ignored){return new JSONArray();}}
   private boolean isPrivateSource(MediaItem item,JSONArray entries) {
      String itemUri=item.uri==null?"":item.uri;String itemPath=(item.path==null?"":item.path).replace('\\','/');String itemName=item.name==null?"":item.name;
      for(int i=0;i<entries.length();i++){JSONObject e=entries.optJSONObject(i);if(e==null)continue;if(itemUri.equals(e.optString("sourceUri","")))return true;String src=e.optString("sourcePath","").replace('\\','/');if(!src.isEmpty()){int slash=src.lastIndexOf('/');String srcParent=slash<0?"":src.substring(0,slash);String srcName=slash<0?src:src.substring(slash+1);if(srcName.equalsIgnoreCase(itemName)&&(itemPath.equalsIgnoreCase(srcParent)||itemPath.equalsIgnoreCase(srcParent+"/")))return true;}}
      return false;
   }

   private void deleteActiveVaultTemp() {
      if (this.activeVaultTempFile != null) { this.activeVaultTempFile.delete(); this.activeVaultTempFile = null; }
   }

   private MediaRouteButton createCastButton() {
      MediaRouteButton button = new MediaRouteButton(new ContextThemeWrapper(this, R.style.SoloCastButtonTheme)) {
         @Override public boolean performClick() {
            playSoundEffect(android.view.SoundEffectConstants.CLICK);
            MainActivity.this.showCastDevicePicker();
            return true;
         }
      };
      button.setContentDescription("Choose a Cast device");
      button.setBackground(this.shape(CARD, 12));
      button.setAlwaysVisible(true);
      if (this.castContext == null) {
         button.setOnClickListener(v -> this.toast("Casting is unavailable. Check Google Play services and try again."));
      } else {
         try { this.castRouteSelector = this.castContext.getMergedSelector(); button.setRouteSelector(this.castRouteSelector); }
         catch (Exception error) {
            Log.e("SoloPlayer", "Cast button setup failed", error);
            button.setOnClickListener(v -> this.toast("Could not open Cast devices. Please try again."));
         }
      }
      return button;
   }

   private void showCastDevicePicker() {
      if (this.castContext == null) { this.toast("Google Cast is unavailable. Check Google Play services."); return; }
      if (this.castPickerDialog != null && this.castPickerDialog.isShowing()) return;
      this.castRouteSelector = this.castContext.getMergedSelector();
      this.castMediaRouter = MediaRouter.getInstance(this);
      LinearLayout panel = this.col(); panel.setPadding(this.dp(18), this.dp(16), this.dp(18), this.dp(12)); panel.setBackground(this.shape(PANEL, 20));
      LinearLayout titleRow = this.row(); titleRow.setGravity(Gravity.CENTER_VERTICAL);
      TextView title = this.text("Cast to a device", 19, Color.WHITE, true); titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
      TextView close = this.text("×", 27, LILAC, true); close.setGravity(Gravity.CENTER); titleRow.addView(close, this.lp(this.dp(40), this.dp(40))); panel.addView(titleRow);
      this.castPickerStatus = this.hint("Searching for Google Cast devices…"); LinearLayout.LayoutParams statusLp = this.lp(-1, -2); statusLp.topMargin = this.dp(5); panel.addView(this.castPickerStatus, statusLp);
      LinearLayout scanIndicator = this.row(); scanIndicator.setGravity(Gravity.CENTER); scanIndicator.setPadding(0, this.dp(10), 0, this.dp(8)); scanIndicator.setBackground(this.shape(0x332D0B50, 36));
      this.castPickerProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge); this.castPickerProgress.setIndeterminate(true); this.castPickerProgress.setIndeterminateTintList(ColorStateList.valueOf(LILAC));
      scanIndicator.addView(this.castPickerProgress, this.lp(this.dp(64), this.dp(64))); LinearLayout.LayoutParams progressLp = this.lp(-1, this.dp(86)); progressLp.topMargin = this.dp(12); panel.addView(scanIndicator, progressLp); this.castPickerProgressContainer = scanIndicator;
      ScrollView scroll = new ScrollView(this); this.castPickerRouteList = this.col(); LinearLayout.LayoutParams listLp = this.lp(-1, -2); listLp.topMargin = this.dp(10); scroll.addView(this.castPickerRouteList); panel.addView(scroll, listLp);
      LinearLayout actions = this.row(); actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); LinearLayout.LayoutParams actionLp = this.lp(-1, -2); actionLp.topMargin = this.dp(10); panel.addView(actions, actionLp);
      TextView retry = this.text("Scan again", 14, LILAC, true); retry.setGravity(Gravity.CENTER); retry.setPadding(this.dp(12), this.dp(10), this.dp(12), this.dp(10)); retry.setOnClickListener(v -> this.startCastRouteScan()); actions.addView(retry);
      TextView cancel = this.text("CLOSE", 14, LILAC, true); cancel.setGravity(Gravity.CENTER); cancel.setPadding(this.dp(14), this.dp(10), this.dp(10), this.dp(10)); actions.addView(cancel);
      Dialog dialog = new Dialog(this); dialog.setContentView(panel); this.castPickerDialog = dialog;
      close.setOnClickListener(v -> dialog.dismiss()); cancel.setOnClickListener(v -> dialog.dismiss());
      dialog.setOnDismissListener(d -> this.stopCastRouteScan()); dialog.show();
      if (dialog.getWindow() != null) { dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); int max = this.dp(460); int width = Math.min(max, getResources().getDisplayMetrics().widthPixels - this.dp(28)); dialog.getWindow().setLayout(width, -2); }
      if (this.isCasting() && this.activeCastSession.getCastDevice() != null) this.renderCastPickerRoutes(); else this.startCastRouteScan();
   }

   private void startCastRouteScan() {
      if (this.castPickerDialog == null || !this.castPickerDialog.isShowing() || this.castMediaRouter == null || this.castRouteSelector == null) return;
      this.stopCastRouteScan(); this.discoveredCastRoutes.clear(); this.renderCastPickerRoutes();
      android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      if (wifi == null || !wifi.isWifiEnabled()) {
         if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.GONE);
         if (this.castPickerStatus != null) this.castPickerStatus.setText("Turn on Wi‑Fi and connect to the same network as your Google Cast device.");
         return;
      }
      if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.VISIBLE);
      if (this.castPickerStatus != null) this.castPickerStatus.setText("Searching for Google Cast devices…");
      this.castRouteCallback = new MediaRouter.Callback() {
         @Override public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) { refreshCastRoutes(); }
         @Override public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) { refreshCastRoutes(); }
         @Override public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) { refreshCastRoutes(); }
      };
      try {
         this.castMediaRouter.addCallback(this.castRouteSelector, this.castRouteCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
         this.handler.removeCallbacks(this.castDiscoveryTimeout); this.handler.postDelayed(this.castDiscoveryTimeout, 15000L);
         this.refreshCastRoutes();
      } catch (Exception error) {
         Log.e("SoloPlayer", "Google Cast discovery could not start", error);
         if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.GONE);
         if (this.castPickerStatus != null) this.castPickerStatus.setText("Device discovery failed. Check Google Play services and your Wi‑Fi network, then scan again.");
      }
   }

   private void refreshCastRoutes() {
      this.discoveredCastRoutes.clear();
      if (this.castMediaRouter != null && this.castRouteSelector != null) {
         for (MediaRouter.RouteInfo route : this.castMediaRouter.getRoutes()) {
            if (route.isEnabled() && !route.isDefault() && route.matchesSelector(this.castRouteSelector)) this.discoveredCastRoutes.add(route);
         }
      }
      this.renderCastPickerRoutes();
   }

   private void renderCastPickerRoutes() {
      if (this.castPickerRouteList == null) return;
      this.castPickerRouteList.removeAllViews();
      if (this.isCasting() && this.activeCastSession.getCastDevice() != null) {
         String name = this.activeCastSession.getCastDevice().getFriendlyName();
         TextView connected = this.text("Connected to " + name, 15, LILAC, true); connected.setPadding(this.dp(12), this.dp(14), this.dp(12), this.dp(14)); connected.setBackground(this.shape(CARD, 14)); this.castPickerRouteList.addView(connected, this.lp(-1, -2));
         TextView disconnect = this.text("Stop casting", 15, Color.WHITE, true); disconnect.setGravity(Gravity.CENTER); disconnect.setPadding(this.dp(10), this.dp(13), this.dp(10), this.dp(13)); disconnect.setBackground(this.shape(PURPLE, 14)); LinearLayout.LayoutParams disconnectLp = this.lp(-1, -2); disconnectLp.topMargin = this.dp(8); this.castPickerRouteList.addView(disconnect, disconnectLp); disconnect.setOnClickListener(v -> { if (this.castContext != null) this.castContext.getSessionManager().endCurrentSession(true); if (this.castPickerDialog != null) this.castPickerDialog.dismiss(); });
         if (this.castPickerStatus != null) this.castPickerStatus.setText("Casting is active"); if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.GONE); return;
      }
      for (MediaRouter.RouteInfo route : new ArrayList<>(this.discoveredCastRoutes)) {
         TextView device = this.text("▣   " + route.getName(), 15, Color.WHITE, true); device.setPadding(this.dp(12), this.dp(14), this.dp(12), this.dp(14)); device.setBackground(this.shape(CARD, 14)); LinearLayout.LayoutParams rowLp = this.lp(-1, -2); rowLp.bottomMargin = this.dp(7); this.castPickerRouteList.addView(device, rowLp);
         device.setOnClickListener(v -> { try { route.select(); if (this.castPickerDialog != null) this.castPickerDialog.dismiss(); } catch (Exception error) { Log.e("SoloPlayer", "Cast device selection failed", error); this.toast("Could not connect to " + route.getName()); } });
      }
      if (!this.discoveredCastRoutes.isEmpty()) {
         if (this.castPickerStatus != null) this.castPickerStatus.setText(this.discoveredCastRoutes.size() + (this.discoveredCastRoutes.size() == 1 ? " compatible device found" : " compatible devices found"));
         if (this.castPickerProgressContainer != null) this.castPickerProgressContainer.setVisibility(View.GONE);
      }
   }

   private void stopCastRouteScan() {
      this.handler.removeCallbacks(this.castDiscoveryTimeout);
      if (this.castMediaRouter != null && this.castRouteCallback != null) { try { this.castMediaRouter.removeCallback(this.castRouteCallback); } catch (Exception ignored) {} }
      this.castRouteCallback = null;
   }

   private boolean isCasting() { return this.activeCastSession != null && this.activeCastSession.isConnected(); }

   private void startCastPlayback(CastSession session) {
      if (this.current == null || session == null) return;
      String playbackUrl;
      if (this.current.network) {
         String scheme = Uri.parse(this.current.uri).getScheme();
         if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            this.toast("This stream type cannot be sent to Cast. Use an HTTP, HTTPS, or HLS stream."); return;
         }
         playbackUrl = this.current.uri;
      } else {
         try {
            this.stopLocalCastServer();
            this.castHttpServer = new LocalMediaHttpServer(this, this.current);
            this.castHttpServer.start();
            playbackUrl = this.castHttpServer.url();
         } catch (Exception error) { this.toast("Could not share this file to Cast: " + error.getMessage()); return; }
      }
      RemoteMediaClient client = session.getRemoteMediaClient();
      if (client == null) { this.toast("Cast device is not ready"); return; }
      MediaMetadata metadata = new MediaMetadata(this.current.video ? MediaMetadata.MEDIA_TYPE_MOVIE : MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
      metadata.putString(MediaMetadata.KEY_TITLE, this.current.name);
      String mime = this.castMimeType(this.current.name, this.current.video);
      MediaInfo info = new MediaInfo.Builder(playbackUrl)
         .setStreamType(this.current.network && playbackUrl.toLowerCase(Locale.ROOT).contains(".m3u8") ? MediaInfo.STREAM_TYPE_LIVE : MediaInfo.STREAM_TYPE_BUFFERED)
         .setContentType(mime)
         .setMetadata(metadata)
         .build();
      this.castResumePosition = this.player != null && this.player.hasMedia() ? Math.max(0L, this.player.getTime()) : 0L;
      this.resumeLocalAfterCast = this.player != null && this.player.isPlaying();
      if (this.player != null && this.player.hasMedia()) this.player.pause();
      MediaLoadRequestData request = new MediaLoadRequestData.Builder()
         .setMediaInfo(info).setAutoplay(true).setCurrentTime(this.castResumePosition).build();
      try {
         client.load(request);
      } catch (Exception error) {
         Log.e("SoloPlayer", "Cast media load failed", error);
         if (this.resumeLocalAfterCast && this.player != null && this.player.hasMedia()) {
            try { if (this.castResumePosition > 0L && this.player.isSeekable()) this.player.setTime(this.castResumePosition); this.player.play(); } catch (Exception ignored) {}
         }
         this.resumeLocalAfterCast = false;
         this.stopLocalCastServer();
         this.syncPlaybackUi();
         this.toast("Could not start casting. Local playback is still available.");
         return;
      }
      this.syncPlaybackUi();
      this.toast("Casting " + this.current.name);
   }

   private String castMimeType(String name, boolean video) {
      String ext = this.extension(name).toLowerCase(Locale.ROOT);
      if (ext.equals("m3u8")) return "application/x-mpegURL";
      if (ext.equals("mpd")) return "application/dash+xml";
      String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
      return mime != null ? mime : (video ? "video/mp4" : "audio/mpeg");
   }

   private void updateCastUiStatus() {
      this.renderCastPickerRoutes();
      if (this.castStatusText == null) return;
      if (this.isCasting() && this.activeCastSession.getCastDevice() != null) {
         this.castStatusText.setText("Connected to " + this.activeCastSession.getCastDevice().getFriendlyName() + " · tap to manage or disconnect");
         this.castStatusText.setTextColor(LILAC);
      } else {
         this.castStatusText.setText(this.castContext == null ? "Casting is unavailable on this device" : "Tap to discover nearby devices");
         this.castStatusText.setTextColor(MUTED);
      }
   }

   private void onCastSessionEnded() {
      this.activeCastSession = null;
      this.updateCastUiStatus();
      this.stopLocalCastServer();
      if (this.resumeLocalAfterCast && this.player != null && this.player.hasMedia()) {
         try { if (this.castResumePosition > 0L && this.player.isSeekable()) this.player.setTime(this.castResumePosition); this.player.play(); }
         catch (Exception ignored) {}
      }
      this.resumeLocalAfterCast = false;
      this.syncPlaybackUi();
   }

   private void stopLocalCastServer() {
      if (this.castHttpServer != null) { this.castHttpServer.stop(); this.castHttpServer = null; }
   }

   private void updatePipParams(boolean autoEnter) {
      if (VERSION.SDK_INT < 26 || !this.currentIsVideo() || this.videoSurface == null) return;
      int width = this.videoSurface.getWidth();
      int height = this.videoSurface.getHeight();
      if (width <= 0 || height <= 0 || Math.max(width, height) > Math.min(width, height) * 2.39) { width = 16; height = 9; }
      int gcd = this.gcd(width, height);
      PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder().setAspectRatio(new Rational(width / gcd, height / gcd));
      Rect bounds = new Rect();
      if (this.videoSurface.getGlobalVisibleRect(bounds)) builder.setSourceRectHint(bounds);
      ArrayList<RemoteAction> actions = new ArrayList<>();
      actions.add(this.createPipAction(this.isPlaybackActive() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
         this.isPlaybackActive() ? "Pause" : "Play", "pip_toggle", 1));
      actions.add(this.createPipAction(android.R.drawable.ic_media_previous, "Previous", "pip_previous", 2));
      actions.add(this.createPipAction(android.R.drawable.ic_media_next, "Next", "pip_next", 3));
      builder.setActions(actions);
      if (VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(autoEnter && this.isPlaybackActive());
      try { this.setPictureInPictureParams(builder.build()); } catch (Exception error) { Log.w("SoloPlayer", "Unable to update PiP parameters", error); }
   }

   private RemoteAction createPipAction(int iconResource, String title, String action, int requestCode) {
      Intent intent = new Intent(this, MainActivity.class).setAction("com.soloplayer.android." + action).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      int flags = PendingIntent.FLAG_UPDATE_CURRENT;
      if (VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
      PendingIntent pending = PendingIntent.getActivity(this, requestCode, intent, flags);
      return new RemoteAction(Icon.createWithResource(this, iconResource), title, title, pending);
   }

   private int gcd(int a, int b) { while (b != 0) { int t = a % b; a = b; b = t; } return Math.max(1, a); }

   private void enterPlayerPip() {
      if (VERSION.SDK_INT < 26 || !this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
         this.toast("Floating video is not supported on this device"); return;
      }
      if (!this.currentIsVideo() || !this.isPlaybackActive()) { this.toast("Start a video before opening the floating window"); return; }
      this.updatePipParams(false);
      this.preparePipUi();
      try {
         this.enterPictureInPictureMode();
      } catch (Exception error) { this.restorePipUi(); this.toast("Could not open floating video: " + error.getMessage()); }
   }

   private void preparePipUi() {
      if (this.pipPrepared) return;
      this.pipPrepared = true;
      this.pipWasFullscreen = this.fullscreen;
      if (this.fullscreen) {
         this.pipSavedOverlayVisibility = this.fullscreenOverlay == null ? View.GONE : this.fullscreenOverlay.getVisibility();
         this.pipSavedTopVisibility = this.fullscreenTopOverlay == null ? View.GONE : this.fullscreenTopOverlay.getVisibility();
         if (this.fullscreenOverlay != null) this.fullscreenOverlay.setVisibility(View.GONE);
         if (this.fullscreenTopOverlay != null) this.fullscreenTopOverlay.setVisibility(View.GONE);
         if (this.fullscreenDrawer != null) this.fullscreenDrawer.setVisibility(View.GONE);
         if (this.brightnessHud != null) this.brightnessHud.setVisibility(View.GONE);
         if (this.volumeHud != null) this.volumeHud.setVisibility(View.GONE);
      } else if (this.pageBody != null && this.videoFrame != null) {
         this.pipSavedToolbarVisibility = this.toolbar.getVisibility();
         this.pipSavedNavVisibility = this.navBar.getVisibility();
         this.pipSavedMiniVisibility = this.miniPlayer.getVisibility();
         this.toolbar.setVisibility(View.GONE); this.navBar.setVisibility(View.GONE); this.miniPlayer.setVisibility(View.GONE);
         this.pipSavedChildVisibility = new int[this.pageBody.getChildCount()];
         for (int i = 0; i < this.pageBody.getChildCount(); i++) {
            View child = this.pageBody.getChildAt(i);
            this.pipSavedChildVisibility[i] = child.getVisibility();
            if (child != this.videoFrame) child.setVisibility(View.GONE);
         }
         this.pipSavedFrameParams = this.videoFrame.getLayoutParams();
         this.videoFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
      }
   }

   private void restorePipUi() {
      if (!this.pipPrepared) return;
      if (this.pipWasFullscreen) {
         if (this.fullscreenOverlay != null) this.fullscreenOverlay.setVisibility(this.pipSavedOverlayVisibility);
         if (this.fullscreenTopOverlay != null) this.fullscreenTopOverlay.setVisibility(this.pipSavedTopVisibility);
      } else if (this.pageBody != null && this.videoFrame != null) {
         if (this.pipSavedFrameParams != null) this.videoFrame.setLayoutParams(this.pipSavedFrameParams);
         if (this.pipSavedChildVisibility != null) for (int i = 0; i < Math.min(this.pageBody.getChildCount(), this.pipSavedChildVisibility.length); i++) this.pageBody.getChildAt(i).setVisibility(this.pipSavedChildVisibility[i]);
         if (this.toolbar != null) this.toolbar.setVisibility(this.pipSavedToolbarVisibility);
         if (this.navBar != null) this.navBar.setVisibility(this.pipSavedNavVisibility);
         if (this.miniPlayer != null) this.miniPlayer.setVisibility(this.pipSavedMiniVisibility);
      }
      this.pipPrepared = false; this.pipSavedChildVisibility = null; this.pipSavedFrameParams = null;
      this.scheduleVideoSurfaceRecovery(120L);
   }

   @Override public void onUserLeaveHint() {
      super.onUserLeaveHint();
      if (VERSION.SDK_INT >= 26 && !this.isInPictureInPictureMode() && this.currentIsVideo() && this.isPlaybackActive() && !this.isCasting()) this.enterPlayerPip();
   }

   @Override protected void onNewIntent(Intent intent) {
      super.onNewIntent(intent);
      if (intent == null || intent.getAction() == null) return;
      switch (intent.getAction()) {
         case "com.soloplayer.android.pip_toggle": this.togglePlayback(); break;
         case "com.soloplayer.android.pip_previous": this.playAdjacent(-1); break;
         case "com.soloplayer.android.pip_next": this.playAdjacent(1); break;
         default: return;
      }
      this.updatePipParams(false);
   }

   @Override public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
      super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
      if (!isInPictureInPictureMode) this.restorePipUi();
      else {
         this.preparePipUi();
         if (this.fullscreenOverlay != null) this.fullscreenOverlay.setVisibility(View.GONE);
         if (this.fullscreenTopOverlay != null) this.fullscreenTopOverlay.setVisibility(View.GONE);
      }
   }

   private void foldersPage() {
      this.browserTabs("Folders");
      this.section("Folders on this device", (Runnable)null);
      this.folderRows(false);
   }

   private String normalizedMediaFolder(MediaItem m) {
      String p = m.path == null ? "" : m.path.replace('\\', '/').trim();
      if (p.startsWith("/storage/emulated/0/")) {
         p = p.substring("/storage/emulated/0/".length());
      } else if (p.startsWith("/")) {
         p = p.substring(1);
      }

      while(p.endsWith("/")) {
         p = p.substring(0, p.length() - 1);
      }

      return p;
   }

   private void folderRows(boolean audioOnly) {
      this.browsingAudioFolders = audioOnly;
      this.folderReturnPage = audioOnly ? "Music" : "Folders";
      this.renderFolderContents("", audioOnly);
   }

   private void folderPage() {
      this.renderFolderContents(this.activeFolder, this.browsingAudioFolders);
   }

   private void renderFolderContents(String base, boolean audioOnly) {
      String normalizedBase = base == null ? "" : base.replace('\\', '/').replaceAll("^/+|/+$", "");
      if (!normalizedBase.isEmpty()) {
         LinearLayout up = this.row();
         TextView back = this.compactButton("‹  Back", false);
         up.addView(back);
         back.setOnClickListener((v) -> {
            int slash = normalizedBase.lastIndexOf(47);
            this.activeFolder = slash < 0 ? "" : normalizedBase.substring(0, slash);
            this.page = this.activeFolder.isEmpty() ? this.folderReturnPage : "Folder";
            this.render();
         });
         this.pageBody.addView(up);
         this.section(normalizedBase, (Runnable)null);
      }

      LinkedHashMap<String, Integer> dirs = new LinkedHashMap();
      ArrayList<MediaItem> direct = new ArrayList();
      Iterator var6 = this.library.iterator();

      while(true) {
         MediaItem m;
         String path;
         do {
            if (!var6.hasNext()) {
               var6 = dirs.keySet().iterator();

               while(var6.hasNext()) {
                  String child = (String)var6.next();
                  final String folderPath = normalizedBase.isEmpty() ? child : normalizedBase + "/" + child;
                  LinearLayout r = this.row();
                  r.setPadding(this.dp(12.0F), this.dp(10.0F), this.dp(10.0F), this.dp(10.0F));
                  r.setBackground(this.shape(CARD, 14));
                  TextView ic = this.text("▰", 19, LILAC, true);
                  r.addView(ic, this.lp(this.dp(38.0F), this.dp(38.0F)));
                  LinearLayout info = this.col();
                  info.addView(this.text(child, 13, -1, true));
                  info.addView(this.text(dirs.get(child) + " media files in this folder", 11, MUTED, false));
                  r.addView(info, new LinearLayout.LayoutParams(0, -2, 1.0F));
                  LinearLayout.LayoutParams rp = this.lp(-1, -2);
                  rp.bottomMargin = this.dp(5.0F);
                  this.pageBody.addView(r, rp);
                  r.setOnClickListener((v) -> {
                     this.activeFolder = folderPath;
                     this.page = "Folder";
                     this.render();
                  });
               }

               var6 = direct.iterator();

               while(var6.hasNext()) {
                  m = (MediaItem)var6.next();
                  this.mediaRow(m, true, direct);
               }

               if (dirs.isEmpty() && direct.isEmpty()) {
                  this.pageBody.addView(this.hint("No media in this folder."));
               }

               return;
            }

            m = (MediaItem)var6.next();
         } while(audioOnly && m.video);

         path = this.normalizedMediaFolder(m);
         if (normalizedBase.isEmpty()) {
            if (path.isEmpty()) {
               direct.add(m);
            } else {
               String[] parts = path.split("/");
               dirs.put(parts[0], (Integer)dirs.getOrDefault(parts[0], 0) + 1);
            }
         } else if (path.equals(normalizedBase)) {
            direct.add(m);
         } else if (path.startsWith(normalizedBase + "/")) {
            String rest = path.substring(normalizedBase.length() + 1);
            String child = rest.split("/")[0];
            dirs.put(child, (Integer)dirs.getOrDefault(child, 0) + 1);
         }
      }
   }

   private void networkPage() {
      this.browserTabs("Network");
      EditText filter = this.edit("Search channels and bookmarks"); filter.setText(this.networkSearchQuery);
      this.pageBody.addView(filter, this.lp(-1, this.dp(42.0F)));
      LinearLayout results = this.col(); this.pageBody.addView(results);
      filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int f){}public void onTextChanged(CharSequence x,int a,int before,int count){networkSearchQuery=x.toString();refreshNetworkResults(results,networkSearchQuery);}public void afterTextChanged(Editable x){}});
      LinearLayout panel = this.card(); panel.setPadding(this.dp(15), this.dp(14), this.dp(15), this.dp(14));
      panel.addView(this.text("Open Network Stream", 16, Color.WHITE, true));
      panel.addView(this.hint("Play HTTP(S), RTSP, RTMP, UDP, HLS and M3U streams. SRT support depends on the VLC engine build."));
      EditText url = this.edit("Enter stream URL"); panel.addView(url);
      EditText name = this.edit("Channel name (optional)"); panel.addView(name);
      EditText ua = this.edit("HTTP User-Agent (optional)"); panel.addView(ua);
      EditText referer = this.edit("Referer (optional)"); panel.addView(referer);
      LinearLayout actions = this.row(); TextView open = this.button("Open Stream", true); TextView bookmark = this.button("Bookmark", false);
      LinearLayout.LayoutParams half = this.lp(0, -2); half.weight = 1f; half.rightMargin = this.dp(4); actions.addView(open, half);
      LinearLayout.LayoutParams half2 = this.lp(0, -2); half2.weight = 1f; half2.leftMargin = this.dp(4); actions.addView(bookmark, half2);
      LinearLayout.LayoutParams actionLp = this.lp(-1, -2); actionLp.topMargin = this.dp(9); panel.addView(actions, actionLp);
      open.setOnClickListener(v -> {
         String u = url.getText().toString().trim(); if (!M3uPlaylistParser.isSupportedUrl(u)) { this.toast("Enter a valid supported stream URL"); return; }
         String nm = name.getText().toString().trim(); if (nm.isEmpty()) nm = this.defaultStreamName(u);
         String agent = ua.getText().toString().trim(), ref = referer.getText().toString().trim();
         if (this.isPlaylistUrl(u)) this.loadM3u(u, nm, agent, ref);
         else { MediaItem item = new MediaItem(nm, u, true, "Network", 0L, 0L, "", ""); item.network = true; item.agent = agent; item.referer = ref; this.play(item); }
      });
      bookmark.setOnClickListener(v -> {
         String u = url.getText().toString().trim(); if (!M3uPlaylistParser.isSupportedUrl(u)) { this.toast("Enter a valid supported stream URL before bookmarking"); return; }
         String nm = name.getText().toString().trim(); if (nm.isEmpty()) nm = this.defaultStreamName(u);
         MediaItem item = new MediaItem(nm, u, true, "Network", 0L, 0L, "", ""); item.network = true; item.agent = ua.getText().toString().trim(); item.referer = referer.getText().toString().trim(); if (this.saveStreamBookmark(item, null)) this.render();
      });
      this.pageBody.addView(panel); this.refreshNetworkResults(results, this.networkSearchQuery);
   }

   private void refreshNetworkResults(LinearLayout target, String query) {
      target.removeAllViews();
      String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
      if (this.channelPageQuery == null || !this.channelPageQuery.equals(q)) {
         this.channelPageQuery = q;
         this.channelPageStart = 0;
      }
      this.addNetworkSectionTitle(target, "Bookmarks");
      int bookmarkCount = 0;
      for (MediaItem item : new ArrayList<>(this.streamBookmarks)) {
         if (this.matchesStream(item, q)) { this.addBookmarkResult(item, target); bookmarkCount++; }
      }
      if (bookmarkCount == 0) target.addView(this.hint(this.streamBookmarks.isEmpty()
              ? "Save a stream URL to access it here later." : "No bookmarks match your search."));

      if (!this.channels.isEmpty()) {
         int matches = 0;
         for (MediaItem item : this.channels) if (this.matchesStream(item, q)) matches++;
         int pageEnd = Math.min(this.channelPageStart + CHANNEL_PAGE_SIZE, matches);
         String range = matches == 0 ? "0" : (this.channelPageStart + 1) + "–" + pageEnd;
         this.addNetworkSectionTitle(target, "Channels  ·  " + range + " of " + matches);
         int matched = 0;
         for (MediaItem item : this.channels) {
            if (!this.matchesStream(item, q)) continue;
            if (matched >= this.channelPageStart && matched < pageEnd) this.addNetworkResult(item, target);
            matched++;
         }
         if (matches == 0) target.addView(this.hint("No channels match your search."));
         else if (matches > CHANNEL_PAGE_SIZE) {
            LinearLayout pager = this.row();
            if (this.channelPageStart > 0) {
               TextView previous = this.button("‹  Previous", false);
               LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, -2, 1f);
               side.rightMargin = this.dp(4);
               pager.addView(previous, side);
               previous.setOnClickListener(v -> {
                  this.channelPageStart = Math.max(0, this.channelPageStart - CHANNEL_PAGE_SIZE);
                  this.refreshNetworkResults(target, this.networkSearchQuery);
               });
            }
            int page = this.channelPageStart / CHANNEL_PAGE_SIZE + 1;
            int pageCount = (matches + CHANNEL_PAGE_SIZE - 1) / CHANNEL_PAGE_SIZE;
            TextView pageLabel = this.text(page + " / " + pageCount, 11, MUTED, true);
            pageLabel.setGravity(Gravity.CENTER);
            pager.addView(pageLabel, new LinearLayout.LayoutParams(0, -2, 0.65f));
            if (pageEnd < matches) {
               TextView next = this.button("Next  ›", false);
               LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, -2, 1f);
               side.leftMargin = this.dp(4);
               pager.addView(next, side);
               next.setOnClickListener(v -> {
                  this.channelPageStart += CHANNEL_PAGE_SIZE;
                  this.refreshNetworkResults(target, this.networkSearchQuery);
               });
            }
            LinearLayout.LayoutParams pagerParams = this.lp(-1, -2);
            pagerParams.topMargin = this.dp(6);
            pagerParams.bottomMargin = this.dp(10);
            target.addView(pager, pagerParams);
         }
      }
      if (!this.recentStreams.isEmpty()) {
         this.addNetworkSectionTitle(target, "Recent streams");
         int count = 0;
         for (MediaItem item : new ArrayList<>(this.recentStreams)) {
            if (this.matchesStream(item, q)) { this.addNetworkResult(item, target); count++; }
         }
         if (count == 0) target.addView(this.hint("No recent streams match your search."));
      }
   }
   private void addNetworkSectionTitle(LinearLayout target, String title) { TextView label = this.text(title, 14, LILAC, true); label.setPadding(this.dp(3), this.dp(12), 0, this.dp(5)); target.addView(label); }
   private boolean matchesStream(MediaItem item, String q) { return q.isEmpty() || item.name.toLowerCase(Locale.ROOT).contains(q) || item.uri.toLowerCase(Locale.ROOT).contains(q) || item.group.toLowerCase(Locale.ROOT).contains(q) || item.tvgId.toLowerCase(Locale.ROOT).contains(q); }
   private String defaultStreamName(String uri) { String host = Uri.parse(uri).getHost(); return host == null || host.isEmpty() ? uri : host; }
   private void loadChannelLogo(MediaItem item, ImageView target) {
      String logo = item.logo == null ? "" : item.logo.trim(); target.setTag(logo);
      if (!(logo.startsWith("http://") || logo.startsWith("https://"))) return;
      this.thumbnailWorker.execute(() -> {
         Bitmap bitmap = null; HttpURLConnection connection = null;
         try {
            connection = (HttpURLConnection)new URL(logo).openConnection(); connection.setConnectTimeout(4000); connection.setReadTimeout(4000); connection.setInstanceFollowRedirects(true);
            int length = connection.getContentLength(); if (length > 1024 * 1024) throw new IOException("Channel logo is too large");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
               byte[] buffer = new byte[8192]; int read; int total = 0;
               while ((read = input.read(buffer)) != -1 && total < 1024 * 1024) { output.write(buffer, 0, Math.min(read, 1024 * 1024 - total)); total += read; }
               byte[] bytes = output.toByteArray(); BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 2; bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            }
         } catch (Exception ignored) { } finally { if (connection != null) connection.disconnect(); }
         if (bitmap != null) { Bitmap result = bitmap; this.runOnUiThread(() -> { if (!this.isFinishing() && logo.equals(target.getTag())) target.setImageBitmap(result); }); }
      });
   }
   private void addNetworkResult(MediaItem item, LinearLayout target) {
      LinearLayout r = this.row(); r.setPadding(this.dp(12), this.dp(10), this.dp(10), this.dp(10)); r.setBackground(this.shape(CARD, 13));
      ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.solo_player_logo); icon.setScaleType(ScaleType.CENTER_CROP); icon.setBackground(this.shape(Color.rgb(46, 18, 68), 9)); icon.setClipToOutline(true); r.addView(icon, this.lp(this.dp(36), this.dp(36))); this.loadChannelLogo(item, icon);
      LinearLayout info = this.col(); info.setPadding(this.dp(9), 0, this.dp(5), 0); info.addView(this.text(item.name, 13, Color.WHITE, true)); info.addView(this.text(item.group.isEmpty() ? item.uri : item.group + "  ·  " + item.uri, 10, MUTED, false)); r.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
      TextView save = this.text("☆", 22, LILAC, true); r.addView(save); save.setContentDescription("Bookmark stream"); save.setOnClickListener(v -> { if (this.saveStreamBookmark(item, null)) this.refreshNetworkResults(target, this.networkSearchQuery); });
      LinearLayout.LayoutParams rp = this.lp(-1, -2); rp.bottomMargin = this.dp(5); target.addView(r, rp); r.setOnClickListener(v -> this.play(item));
   }
   private void addBookmarkResult(MediaItem item, LinearLayout target) {
      LinearLayout r = this.row(); r.setPadding(this.dp(12), this.dp(10), this.dp(10), this.dp(10)); r.setBackground(this.shape(CARD, 13));
      ImageView icon = new ImageView(this); icon.setImageResource(R.drawable.solo_player_logo); icon.setScaleType(ScaleType.CENTER_CROP); icon.setBackground(this.shape(Color.rgb(46, 18, 68), 9)); icon.setClipToOutline(true); r.addView(icon, this.lp(this.dp(36), this.dp(36))); this.loadChannelLogo(item, icon);
      LinearLayout info = this.col(); info.setPadding(this.dp(9), 0, this.dp(5), 0); info.addView(this.text(item.name, 13, Color.WHITE, true)); info.addView(this.text(item.group.isEmpty() ? item.uri : item.group + "  ·  " + item.uri, 10, MUTED, false)); r.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
      TextView more = this.text("⋮", 21, MUTED, true); r.addView(more); LinearLayout.LayoutParams rp = this.lp(-1, -2); rp.bottomMargin = this.dp(5); target.addView(r, rp);
      r.setOnClickListener(v -> this.play(item));
      more.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(new String[]{"Play", "Edit", "Delete"}, (d, which) -> {
         if (which == 0) this.play(item); else if (which == 1) this.showStreamBookmarkEditor(item);
         else new AlertDialog.Builder(this).setTitle("Delete bookmark?").setMessage(item.name).setNegativeButton("Cancel", null).setPositiveButton("Delete", (dd, ww) -> { this.streamBookmarks.removeIf(saved -> saved.uri.equals(item.uri)); this.saveData(); this.refreshNetworkResults(target, this.networkSearchQuery); }).show();
      }).show());
   }
   private void showStreamBookmarkEditor(MediaItem existing) {
      LinearLayout form = this.col(); form.setPadding(this.dp(8), this.dp(4), this.dp(8), this.dp(4));
      EditText name = this.edit("Channel name"); name.setText(existing.name); form.addView(name);
      EditText url = this.edit("Stream URL"); url.setText(existing.uri); form.addView(url);
      EditText agent = this.edit("HTTP User-Agent (optional)"); agent.setText(existing.agent); form.addView(agent);
      EditText referer = this.edit("Referer (optional)"); referer.setText(existing.referer); form.addView(referer);
      AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Edit stream bookmark").setView(form).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
      dialog.setOnShowListener(v -> { dialog.getButton(-1).setTextColor(LILAC); dialog.getButton(-1).setOnClickListener(button -> {
         String newUrl = url.getText().toString().trim(), newName = name.getText().toString().trim();
         if (!M3uPlaylistParser.isSupportedUrl(newUrl)) { url.setError("Enter a valid supported stream URL"); return; }
         if (newName.isEmpty()) { name.setError("Enter a channel name"); return; }
         MediaItem replacement = new MediaItem(newName, newUrl, true, "Network", 0L, 0L, "", ""); replacement.network = true; replacement.agent = agent.getText().toString().trim(); replacement.referer = referer.getText().toString().trim(); replacement.logo = existing.logo; replacement.group = existing.group; replacement.tvgId = existing.tvgId;
         if (this.saveStreamBookmark(replacement, existing)) { dialog.dismiss(); this.render(); }
      }); }); dialog.show();
   }
   private boolean saveStreamBookmark(MediaItem item, MediaItem replacing) {
      if (!M3uPlaylistParser.isSupportedUrl(item.uri)) { this.toast("Enter a valid supported stream URL"); return false; }
      for (MediaItem saved : this.streamBookmarks) if (saved != replacing && saved.uri.equalsIgnoreCase(item.uri)) { this.toast("This stream URL is already bookmarked"); return false; }
      if (replacing != null) this.streamBookmarks.remove(replacing); item.network = true; this.streamBookmarks.add(0, item); this.saveData(); this.toast("Bookmark saved: " + item.name); return true;
   }
   private void editOrAddCurrentStreamBookmark() {
      if (this.current == null || !this.current.network) return;
      MediaItem saved = null; for (MediaItem item : this.streamBookmarks) if (item.uri.equalsIgnoreCase(this.current.uri)) { saved = item; break; }
      this.showStreamBookmarkEditor(saved == null ? this.current : saved);
   }
   private EditText edit(String hint) {
      EditText e = new EditText(this);
      e.setSingleLine(true);
      e.setTextColor(-1);
      e.setHintTextColor(MUTED);
      e.setHint(hint);
      e.setTextSize(13.0F);
      return e;
   }

   private void channelsPage(){EditText filter=edit("Search channels");filter.setText(networkSearchQuery);pageBody.addView(filter,lp(-1,dp(42)));LinearLayout results=col();pageBody.addView(results);filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int f){}public void onTextChanged(CharSequence x,int a,int before,int count){networkSearchQuery=x.toString();refreshNetworkResults(results,networkSearchQuery);}public void afterTextChanged(Editable x){}});refreshNetworkResults(results,networkSearchQuery);}
   private void settingsPage() {
      this.section("Network", null);
      this.settingRow("Stream", "Open a network stream or channel list", () -> { this.page = "Network Stream"; this.render(); });
      LinearLayout castRow = this.row();
      castRow.setPadding(this.dp(12), this.dp(8), this.dp(12), this.dp(8));
      castRow.setBackground(this.shape(CARD, 12));
      LinearLayout castInfo = this.col();
      castInfo.addView(this.text("Cast device", 13, Color.WHITE, true));
      castInfo.addView(this.hint("Discover and connect to a TV or speaker"));
      castRow.addView(castInfo, new LinearLayout.LayoutParams(0, -2, 1.0F));
      castRow.addView(this.createCastButton(), this.lp(this.dp(48), this.dp(48)));
      LinearLayout.LayoutParams castRowLp = this.lp(-1, -2); castRowLp.bottomMargin = this.dp(8); this.pageBody.addView(castRow, castRowLp);
      this.section("Audio", null);
      this.settingRow("Audio preset", this.audioPreset, () -> this.showPlaybackPresets(false));
      this.settingRow("Equalizer", "Frequency bands · custom sound curve", () -> { this.audioPreset = "Custom"; this.prefs().edit().putString("audio_preset", "Custom").apply(); this.showCustomEqualizer(false); });
      this.settingRow("Volume boost", this.audioBoostDb + " dB", () -> this.showAudioEnhancementDialog(false));
      this.settingRow("Bass boost", this.bassBoostDb + " dB", () -> this.showAudioEnhancementDialog(true));
      this.section("Video", null);
      this.settingRow("Video preset", this.videoPreset, () -> this.showPlaybackPresets(true));
      this.section("Playback", null);
      this.settingRow("Network cache", this.networkCache + " ms", () -> {
         String[] x = new String[]{"750 ms (fast)", "1500 ms (balanced)", "3000 ms (stable)"};
         (new AlertDialog.Builder(this)).setTitle("Network cache").setItems(x, (d, w) -> {
            this.networkCache = (new int[]{750, 1500, 3000})[w];
            this.prefs().edit().putInt("cache", this.networkCache).apply();
            this.render();
         }).show();
      });
      this.settingRow("Background audio", this.backgroundAudioEnabled ? "Enabled" : "Disabled", () -> this.setBackgroundAudioEnabled(!this.backgroundAudioEnabled));
      this.settingRow("Playback speed", "Change during playback", () -> this.speedDialog());
      this.settingRow("Media library", this.library.size() + " visible files", () -> this.ensureMediaPermission());
      this.settingRow("About Solo Player", "Version 1.2.4", () -> {
         (new AlertDialog.Builder(this)).setMessage("Solo Player for Android · LibVLC").setPositiveButton("OK", (DialogInterface.OnClickListener)null).show();
      });
   }

   private void settingRow(String label, String value, Runnable click) {
      LinearLayout r = this.row();
      r.setPadding(this.dp(12.0F), this.dp(13.0F), this.dp(12.0F), this.dp(13.0F));
      r.setBackground(this.shape(CARD, 12));
      r.addView(this.text(label, 13, -1, true), new LinearLayout.LayoutParams(0, -2, 1.0F));
      r.addView(this.text(value + "  ›", 11, LILAC, false));
      LinearLayout.LayoutParams p = this.lp(-1, -2);
      p.bottomMargin = this.dp(6.0F);
      this.pageBody.addView(r, p);
      r.setOnClickListener((v) -> {
         click.run();
      });
   }

   private void showChannelPicker() {
      if (this.current == null || !this.current.network) { this.toast("There is no active stream to switch"); return; }
      if (this.channels.isEmpty()) { this.toast("No channels are loaded"); return; }
      LinearLayout content = this.col();
      content.setPadding(this.dp(12.0F), this.dp(5.0F), this.dp(12.0F), this.dp(8.0F));
      EditText search = this.edit("Search channels");
      content.addView(search, this.lp(-1, this.dp(44.0F)));
      ScrollView scroll = new ScrollView(this);
      LinearLayout results = this.col();
      scroll.addView(results);
      content.addView(scroll, new LinearLayout.LayoutParams(-1, this.dp(390.0F)));
      AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Switch channel").setView(content).setNegativeButton("Close", null).create();
      int[] pageStart = {0};
      String[] previousQuery = {null};
      Runnable refresh = () -> this.renderChannelPickerRows(results, search, dialog, pageStart, previousQuery);
      search.addTextChangedListener(new TextWatcher() {
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
         public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
         public void afterTextChanged(Editable s) {}
      });
      refresh.run();
      dialog.show();
   }

   private void renderChannelPickerRows(LinearLayout results, EditText search, AlertDialog dialog,
                                        int[] pageStart, String[] previousQuery) {
      if (results == null || search == null || dialog == null) return;
      results.removeAllViews();
      String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
      if (!query.equals(previousQuery[0])) { previousQuery[0] = query; pageStart[0] = 0; }
      int matches = 0;
      for (MediaItem channel : this.channels) if (this.matchesStream(channel, query)) matches++;
      int end = Math.min(pageStart[0] + CHANNEL_PAGE_SIZE, matches);
      int matched = 0;
      for (MediaItem channel : this.channels) {
         if (!this.matchesStream(channel, query)) continue;
         if (matched >= pageStart[0] && matched < end) {
            LinearLayout row = this.row();
            row.setPadding(this.dp(10.0F), this.dp(10.0F), this.dp(8.0F), this.dp(10.0F));
            boolean selected = this.current != null && channel.uri.equals(this.current.uri);
            row.setBackground(this.shape(selected ? Color.rgb(47, 15, 72) : CARD, 10));
            row.addView(this.text("◉", 16, LILAC, true), this.lp(this.dp(34.0F), -2));
            TextView label = this.text(channel.name, 13, Color.WHITE, true);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0F));
            results.addView(row, this.lp(-1, -2));
            row.setOnClickListener(v -> {
               if (this.current == null || !this.current.network) {
                  dialog.dismiss();
                  this.toast("The active stream has already closed");
                  return;
               }
               dialog.dismiss();
               if (this.fullscreen) this.playFullscreen(channel); else this.play(channel);
            });
         }
         matched++;
         if (matched >= end) break;
      }
      if (matches == 0) results.addView(this.hint("No channels match your search."));
      else this.addChannelPageNavigation(results, matches, pageStart[0], value -> {
         pageStart[0] = value;
         this.renderChannelPickerRows(results, search, dialog, pageStart, previousQuery);
      });
   }

   private void addChannelPageNavigation(LinearLayout target, int matches, int start,
                                         Consumer<Integer> onPageChanged) {
      if (matches <= CHANNEL_PAGE_SIZE) return;
      LinearLayout pager = this.row();
      if (start > 0) {
         TextView previous = this.button("‹  Previous", false);
         LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, -2, 1f);
         side.rightMargin = this.dp(4);
         pager.addView(previous, side);
         previous.setOnClickListener(v -> onPageChanged.accept(Math.max(0, start - CHANNEL_PAGE_SIZE)));
      }
      TextView pageLabel = this.text((start / CHANNEL_PAGE_SIZE + 1) + " / "
              + ((matches + CHANNEL_PAGE_SIZE - 1) / CHANNEL_PAGE_SIZE), 11, MUTED, true);
      pageLabel.setGravity(Gravity.CENTER);
      pager.addView(pageLabel, new LinearLayout.LayoutParams(0, -2, 0.65f));
      if (start + CHANNEL_PAGE_SIZE < matches) {
         TextView next = this.button("Next  ›", false);
         LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, -2, 1f);
         side.leftMargin = this.dp(4);
         pager.addView(next, side);
         next.setOnClickListener(v -> onPageChanged.accept(start + CHANNEL_PAGE_SIZE));
      }
      LinearLayout.LayoutParams pagerParams = this.lp(-1, -2);
      pagerParams.topMargin = this.dp(6);
      pagerParams.bottomMargin = this.dp(10);
      target.addView(pager, pagerParams);
   }
   private void playerPage() {
      if (this.current == null) {
         this.pageBody.addView(this.hint("Choose a song or video first."));
      } else {
         LinearLayout meta = this.row();
         meta.addView(this.text("‹", 26, -1, true), this.lp(this.dp(38.0F), this.dp(40.0F)));
         meta.getChildAt(0).setOnClickListener((v) -> {
            this.closePlayerToHome();
         });
         LinearLayout labels = this.col();
         this.playbackTitle = this.text(this.current.name, 15, -1, true);
         this.playbackSubtitle = this.text(this.current.video ? "Now Playing (Video)" : "Now Playing (Audio)", 11, MUTED, false);
         labels.addView(this.playbackTitle);
         labels.addView(this.playbackSubtitle);
         meta.addView(labels, new LinearLayout.LayoutParams(0, -2, 1.0F));
         TextView fav = this.text(this.isFavorite(this.current.uri) ? "♥" : "♡", 22, LILAC, true);
         meta.addView(fav);
         fav.setOnClickListener((v) -> {
            this.toggleFavorite(this.current);
         });
         if (this.current.network) {
            TextView bookmarkStream = this.text("☆", 22, LILAC, true); meta.addView(bookmarkStream);
            bookmarkStream.setContentDescription("Bookmark this stream");
            bookmarkStream.setOnClickListener(v -> this.editOrAddCurrentStreamBookmark());
         }
         this.pageBody.addView(meta);
         FrameLayout frame = new FrameLayout(this);
         this.videoFrame = frame;
         frame.setBackground(this.shape(-16777216, 16));
         this.videoSurface = new VLCVideoLayout(this);
         this.videoSurfaceAttached = false;
         frame.addView(this.videoSurface, new FrameLayout.LayoutParams(-1, -1));
         this.videoTapDetector = this.createVideoTapDetector();
         View tapLayer = new View(this);
         tapLayer.setBackgroundColor(Color.TRANSPARENT);
         tapLayer.setOnTouchListener((v, event) -> this.handleVideoTouch(event, frame));
         frame.addView(tapLayer, new FrameLayout.LayoutParams(-1, -1));
         this.addGestureHuds(frame);
         if (this.current.network && playbackStartPending) this.addStreamLoadingOverlay(frame);
         if (!this.current.video) {
            FrameLayout audioStage = this.createAudioArtworkStage(this.current, false);
            frame.addView(audioStage, new FrameLayout.LayoutParams(this.dp(220), this.dp(220), Gravity.CENTER));
         }

         int videoWidth = this.getResources().getDisplayMetrics().widthPixels - this.dp(28.0F);
         int availableHeight = this.getResources().getDisplayMetrics().heightPixels;
         int videoHeight = this.current.video ? Math.max(this.dp(320.0F), Math.min(this.dp(440.0F), Math.round(availableHeight * 0.48F))) : this.dp(220.0F);
         LinearLayout.LayoutParams fp = this.lp(-1, videoHeight);
         fp.topMargin = this.dp(12.0F);
         this.pageBody.addView(frame, fp);
         if (this.current.network && !this.channels.isEmpty()) {
            TextView channelSearch = this.button("⌕  Search / switch channel", false);
            this.pageBody.addView(channelSearch, this.lp(-1, -2));
            channelSearch.setOnClickListener((v) -> this.showChannelPicker());
         }
         LinearLayout videoTools = this.row();
         this.portraitVideoTools = videoTools;
         videoTools.setGravity(21);
         TextView ratioButton = this.button("▭  " + this.aspectMode, false);
         this.portraitRatioButton = ratioButton;
         this.bindAspectRatioControl(ratioButton);
         videoTools.addView(ratioButton);
         MediaRouteButton portraitCast = this.createCastButton();
         videoTools.addView(portraitCast, this.lp(this.dp(42), this.dp(42)));
         TextView pip = this.compactButton("▣", false);
         pip.setContentDescription("Open floating video window");
         pip.setOnClickListener(v -> this.enterPlayerPip());
         videoTools.addView(pip, this.lp(this.dp(42), this.dp(42)));
         FrameLayout.LayoutParams toolsLp = new FrameLayout.LayoutParams(-2, -2, 53);
         frame.addView(videoTools, toolsLp);
         this.applyTranslucentControlBackgrounds(videoTools, 175);
         frame.setOnClickListener((v) -> {
            if (this.fullscreen) {
               this.toggleFullscreen();
            }

         });
         if (this.current.video) this.attachVideoSurface(this.videoSurface, this.fullscreen, 0);
         this.portraitControlsPanel = this.col();
         this.portraitControlsPanel.setPadding(this.dp(7), this.dp(4), this.dp(7), this.dp(5));
         this.portraitControlsPanel.setBackground(this.shape(Color.argb(92, 14, 10, 24), 12));
         this.playbackSeek = new SeekBar(this);
         this.playbackSeek.setMax(1000);
         this.playbackSeek.setProgressTintList(ColorStateList.valueOf(PURPLE));
         this.playbackSeek.setThumbTintList(ColorStateList.valueOf(LILAC));
         this.portraitControlsPanel.addView(this.playbackSeek);
         this.playbackSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){long length=activePlaybackLength();if(length>0){if(isCasting())seekBy(length*s.getProgress()/1000L-activePlaybackTime());else player.setTime(length*s.getProgress()/1000L);}seeking=false;}public void onProgressChanged(SeekBar s,int p,boolean u){}});
         this.playbackTime = this.text(this.playbackClock(0L, this.player.getLength()) + "     /     " + this.playbackClock(this.player.getLength(), this.player.getLength()), 11, MUTED, false);
         this.playbackTime.setGravity(17);
         this.portraitControlsPanel.addView(this.playbackTime);
         LinearLayout controls = this.row();
         controls.setGravity(17);
         TextView previous = this.compactButton("|◀", false);
         TextView back = this.compactButton("−10", false);
         TextView play = this.compactButton(this.isPlaybackActive() ? "❚❚" : "▶", true);
         this.playbackPlayButton = play;
         this.setPlaybackButtonIcon(play, this.isPlaybackActive());
         TextView forward = this.compactButton("+10", false);
         TextView next = this.compactButton("▶|", false);
         TextView[] var18 = new TextView[]{previous, back, play, forward, next};
         int var19 = var18.length;

         for(int var20 = 0; var20 < var19; ++var20) {
            TextView b = var18[var20];
            LinearLayout.LayoutParams cp = this.lp(-2, -2);
            cp.setMargins(this.dp(2.0F), this.dp(3.0F), this.dp(2.0F), this.dp(3.0F));
            b.setTextSize(12.0F);
            b.setPadding(this.dp(6.0F), this.dp(4.0F), this.dp(6.0F), this.dp(4.0F));
            b.setMinHeight(this.dp(36.0F));
            controls.addView(b, cp);
         }

         this.portraitControlsPanel.addView(controls);
         previous.setOnClickListener((v) -> {
            this.playAdjacent(-1);
         });
         next.setOnClickListener((v) -> {
            this.playAdjacent(1);
         });
         back.setOnClickListener((v) -> {
            this.seekBy(-10000L);
         });
         forward.setOnClickListener((v) -> {
            this.seekBy(10000L);
         });
         play.setOnClickListener((v) -> {
            this.togglePlayback();
         });
         LinearLayout advanced = this.col();
         LinearLayout options = this.row();
         String[] ops = new String[]{"Speed", "Ratio", "CC", "Audio", "Decode", "Preset"};
         String[] var31 = ops;
         int var33 = ops.length;

         for(int var23 = 0; var23 < var33; ++var23) {
            String op = var31[var23];
            TextView b = this.compactButton(op, false);
            LinearLayout.LayoutParams bp = this.lp(0, -2);
            bp.weight = 1.0F;
            bp.setMargins(this.dp(5.0F), 0, this.dp(5.0F), 0);
            options.addView(b, bp);
            b.setOnClickListener((v) -> {
               if (op.equals("Speed")) {
                  this.speedDialog();
               } else if (op.equals("Ratio")) {
                  this.cycleAspectMode();
               } else if (op.equals("Preset")) {
                  this.showPresetTypeMenu();
               } else if (op.equals("Decode")) {
                  this.decoderDialog();
               } else {
                  this.showTracks(op.equals("CC"));
               }

            });
            if (op.equals("Ratio")) b.setOnLongClickListener(v -> { this.aspectRatioDialog(); return true; });
         }

         LinearLayout.LayoutParams olp = this.lp(-1, -2);
         olp.topMargin = this.dp(8.0F);
         olp.bottomMargin = this.dp(3.0F);
         advanced.addView(options, olp);
         LinearLayout volume = this.row();
         volume.addView(this.text("Vol", 11, MUTED, false), this.lp(this.dp(38.0F), -2));
         SeekBar vol = new SeekBar(this);
         vol.setMax(100);
         vol.setProgress(this.getPlaybackVolumePercent());
         vol.setProgressTintList(ColorStateList.valueOf(PURPLE));
         vol.setThumbTintList(ColorStateList.valueOf(LILAC));
         volume.addView(vol, new LinearLayout.LayoutParams(0, -2, 1.0F));
         vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean u){if(u)setPlaybackVolumePercent(p);}});
         advanced.addView(volume);
         TextView moreControls = this.compactButton("⌃  Fewer controls", false);
         moreControls.setTextSize(11.0F);
         LinearLayout.LayoutParams moreLp = this.lp(-1, this.dp(34.0F));
         moreLp.topMargin = this.dp(3.0F);
         this.portraitControlsPanel.addView(moreControls, moreLp);
         this.portraitControlsPanel.addView(advanced);
         advanced.setVisibility(View.GONE);
         moreControls.setText("⌄  More controls");
         if (this.current.video) {
            FrameLayout.LayoutParams portraitOverlayLp = new FrameLayout.LayoutParams(-1, -2, 80);
            portraitOverlayLp.setMargins(this.dp(6), 0, this.dp(6), this.dp(6));
            frame.addView(this.portraitControlsPanel, portraitOverlayLp);
         } else {
            this.pageBody.addView(this.portraitControlsPanel, this.lp(-1, -2));
         }
         this.applyTranslucentControlBackgrounds(this.portraitControlsPanel, 165);
         this.portraitControlsVisible = true;
         this.portraitControlsPanel.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) this.handler.removeCallbacks(this.hidePortraitControls);
            else if (event.getActionMasked() == MotionEvent.ACTION_UP) this.handler.postDelayed(this.hidePortraitControls, 4000L);
            return false;
         });
         this.handler.removeCallbacks(this.hidePortraitControls);
         this.handler.postDelayed(this.hidePortraitControls, 4000L);
         moreControls.setOnClickListener((v) -> {
            boolean open = advanced.getVisibility() != 0;
            advanced.setVisibility(open ? 0 : 8);
            moreControls.setText(open ? "⌃  Fewer controls" : "⌄  More controls");
         });
      }
   }

   private void mediaRow(MediaItem m, boolean playable) {
      this.mediaRow(m, playable, null);
   }

   private void mediaRow(MediaItem m, boolean playable, ArrayList<MediaItem> sourceQueue) {
      LinearLayout r = this.row();
      r.setPadding(this.dp(9.0F), this.dp(8.0F), this.dp(9.0F), this.dp(8.0F));
      r.setBackground(this.shape(CARD, 13));
      ImageView icon = new ImageView(this);
      icon.setImageResource(R.drawable.solo_player_logo);
      icon.setScaleType(ScaleType.CENTER_CROP);
      icon.setBackground(this.shape(Color.rgb(46, 18, 68), 10));
      icon.setClipToOutline(true);
      r.addView(icon, m.video ? this.lp(this.dp(64.0F), this.dp(48.0F)) : this.lp(this.dp(49.0F), this.dp(49.0F)));
      this.loadArtwork(m, icon);
      LinearLayout meta = this.col();
      meta.setPadding(this.dp(9.0F), 0, this.dp(4.0F), 0);
      meta.addView(this.text(m.name, 12, -1, true));
      String sub = m.video ? (m.path.isEmpty() ? "Local video" : m.path) + (m.size > 0L ? " · " + this.fileSize(m.size) : "") + (m.duration > 0L ? " · " + this.clock(m.duration) : "") : (!m.artist.isEmpty() ? m.artist : "Audio") + (m.duration > 0L ? " · " + this.clock(m.duration) : "");
      if (m.positionMs > 0L) sub += " · Resume " + this.clock(m.positionMs);
      meta.addView(this.text(sub, 10, MUTED, false));
      r.addView(meta, new LinearLayout.LayoutParams(0, -2, 1.0F));
      TextView more = this.text("⋮", 20, MUTED, true);
      r.addView(more);
      LinearLayout.LayoutParams p = this.lp(-1, -2);
      p.bottomMargin = this.dp(6.0F);
      this.pageBody.addView(r, p);
      r.setOnClickListener((v) -> {
         if (m.folder) {
            this.activeFolder = m.path;
            this.page = "Folder";
            this.render();
         } else if (playable) {
            if (sourceQueue != null && !sourceQueue.isEmpty()) {
               int selected = -1;
               for (int i = 0; i < sourceQueue.size(); i++) if (sourceQueue.get(i).uri.equals(m.uri)) { selected = i; break; }
               if (selected >= 0) {
                  this.queue.clear();
                  this.queue.addAll(sourceQueue);
               }
            }
            this.play(m);
         }

      });
      more.setOnClickListener((v) -> {
         this.mediaMenu(m);
      });
   }

   private FrameLayout createAudioArtworkStage(MediaItem media, boolean expanded) {
      FrameLayout stage = new FrameLayout(this); stage.setClipChildren(false); stage.setClipToPadding(false); stage.setClickable(false);
      AudioArtworkEffectView effect = new AudioArtworkEffectView(this, PURPLE, LILAC); effect.setPlaying(this.current != null && !this.current.video && this.isPlaybackActive());
      stage.addView(effect, new FrameLayout.LayoutParams(-1, -1));
      ImageView art = new ImageView(this); art.setImageResource(R.drawable.solo_player_logo); art.setScaleType(ScaleType.CENTER_CROP);
      art.setBackground(this.shape(Color.rgb(24, 12, 34), 16)); art.setClipToOutline(true); art.setClickable(false);
      int imageSize = this.dp(expanded ? 172 : 142); FrameLayout.LayoutParams artLp = new FrameLayout.LayoutParams(imageSize, imageSize, Gravity.TOP | Gravity.CENTER_HORIZONTAL); artLp.topMargin = this.dp(expanded ? 12 : 10);
      stage.addView(art, artLp); this.loadArtwork(media, art);
      if (expanded) { this.fullscreenArtwork = art; this.fullscreenArtworkEffect = effect; }
      else { this.playbackArtwork = art; this.playbackArtworkEffect = effect; }
      return stage;
   }

   private void loadArtwork(MediaItem m, ImageView target) {
      Uri uri = Uri.parse(m.uri);
      this.thumbnailWorker.execute(() -> {
         Bitmap art = null;

         try {
            if (m.video && VERSION.SDK_INT >= 29) {
               art = this.getContentResolver().loadThumbnail(uri, new Size(this.dp(160.0F), this.dp(160.0F)), (CancellationSignal)null);
            } else {
               MediaMetadataRetriever retriever = new MediaMetadataRetriever();

               try {
                  retriever.setDataSource(this, uri);
                  byte[] bytes = m.video ? null : retriever.getEmbeddedPicture();
                  if (bytes != null) {
                     art = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                  }

                  if (art == null && m.video) {
                     art = retriever.getFrameAtTime(1000000L, 2);
                  }
               } finally {
                  retriever.release();
               }
            }
         } catch (Exception var11) {
         }

         if (art != null) { final Bitmap bitmap = art;
            this.runOnUiThread(() -> {
               if (!this.isFinishing()) {
                  target.setImageBitmap(bitmap);
               }

            });
         }

      });
   }

   private String fileSize(long n) {
      if (n < 1024L) {
         return n + " B";
      } else {
         return n < 1048576L ? String.format(Locale.US, "%.0f KB", (float)n / 1024.0F) : String.format(Locale.US, "%.1f MB", (float)n / 1048576.0F);
      }
   }

   private void mediaMenu(MediaItem m) {
      String[] options = new String[]{"Play now", "Add to queue", this.isFavorite(m.uri) ? "Remove favorite" : "Add to favorites", "Add to playlist"};
      (new AlertDialog.Builder(this)).setItems(options, (d, w) -> {
         if (w == 0) {
            this.play(m);
         } else if (w == 1) {
            this.queue.add(m);
            this.saveData();
            this.toast("Added to queue");
         } else if (w == 2) {
            this.toggleFavorite(m);
         } else if(w==3) {
            this.addToPlaylist(m);
         }

      }).show();
   }

   private void mediaGridButton(String label, String glyph, String dest) {
      TextView b = this.button(glyph + "\n" + label, false);
      b.setTextSize(11.0F);
      b.setMinHeight(this.dp(64.0F));
      b.setOnClickListener((v) -> {
         this.page = dest;
         this.render();
      });
   }

   private boolean isChannelUri(String uri) {
      for (MediaItem channel : this.channels) if (channel.uri.equals(uri)) return true;
      return false;
   }

   private boolean channelQueueReady() {
      if (this.channels.isEmpty()) return false;
      for (MediaItem channel : this.channels) if (!this.queueHas(channel.uri)) return false;
      return true;
   }

   private void ensureQueueForItem(MediaItem item) {
      if (item.network && this.isChannelUri(item.uri) && !this.channelQueueReady()) {
         this.queue.clear();
         this.queue.addAll(this.channels);
      } else if (!this.queueHas(item.uri)) {
         this.queue.add(0, item);
      }
   }

   private long savedPositionFor(String uri) {
      if (uri == null) return 0L;
      for (MediaItem recent : this.recentPlayed) if (uri.equals(recent.uri)) return Math.max(0L, recent.positionMs);
      return 0L;
   }

   private void recordRecent(MediaItem item) {
      if (item == null || item.network) return;
      for (MediaItem recent : this.recentPlayed) {
         if (recent.uri.equals(item.uri)) {
            if (item.positionMs <= 0L) item.positionMs = recent.positionMs;
            if (item.duration <= 0L) item.duration = recent.duration;
            break;
         }
      }
      if (this.isPrivateSource(item, this.privateVideos)) return;
      this.recentPlayed.removeIf((recent) -> recent.uri.equals(item.uri));
      this.recentPlayed.add(0, item);
      while (this.recentPlayed.size() > 5) this.recentPlayed.remove(this.recentPlayed.size() - 1);
      this.invalidatePageCachePrefix("Home|");
      this.saveData();
   }

   private void setRecentPosition(String uri, long position, long duration) {
      for (MediaItem recent : this.recentPlayed) {
         if (recent.uri.equals(uri)) {
            recent.positionMs = Math.max(0L, position);
            if (duration > 0L) recent.duration = duration;
            this.saveData();
            return;
         }
      }
   }

   private void saveCurrentResumePosition() {
      if (this.current == null || this.current.network || this.player == null || !this.player.hasMedia()) return;
      long position = this.waitingSubtitleRestore && this.pendingSubtitleSeek >= 0L ? this.pendingSubtitleSeek : this.activePlaybackTime();
      long duration = this.activePlaybackLength();
      if (duration > 0L && position >= duration - 1500L) position = 0L;
      for (MediaItem recent : this.recentPlayed) {
         if (recent.uri.equals(this.current.uri)) {
            this.setRecentPosition(recent.uri, position, duration);
            return;
         }
      }
   }

   private void persistResumePositionPeriodically() {
      long now = android.os.SystemClock.elapsedRealtime();
      if (now - this.lastRecentPositionSaveAt < 2000L) return;
      this.lastRecentPositionSaveAt = now;
      this.saveCurrentResumePosition();
   }

   private void play(MediaItem item) {
      if (item == null || item.uri == null || item.uri.trim().isEmpty()) {
         this.toast("This channel has no playable stream URL");
         return;
      }
      if (this.player == null || this.vlc == null) {
         this.toast("The media player is not ready. Please try again.");
         return;
      }
      if(this.current!=null&&!this.current.uri.equals(item.uri)&&this.pausedFrameBitmap!=null)this.clearPausedFrameOverlay();
      if (this.current != null && this.current.privateVault && !this.current.uri.equals(item.uri)) this.deleteActiveVaultTemp();
      boolean keepFullscreen = this.fullscreen && this.videoSurface != null;
      VLCVideoLayout fullscreenSurface = keepFullscreen ? this.videoSurface : null;
      if (this.current != null && !this.current.uri.equals(item.uri)) this.clearAbRepeat();
      boolean reusePlayerUi = this.page.equals("Player") && !this.fullscreen && current != null && current.video == item.video && current.network == item.network;
      if (item.video || item.network) AudioPlaybackService.stop(this.getApplicationContext());
      long resumeAt = this.savedPositionFor(item.uri);
      this.pendingResumePosition = resumeAt > 0L ? resumeAt : -1L;
      this.pendingResumeUri = item.uri;
      this.current = item;
      if (item.network) { this.streamHasPlayed = false; this.setStreamLoading(true, "Connecting to stream…"); this.startStreamConnectionTimeout(); }
      else { this.handler.removeCallbacks(this.streamConnectionTimeout); this.setStreamLoading(false, null); }
      this.updatePlaybackPresetStatus();
      playbackRequested = true;
      playbackStartPending = true;
      this.ensureQueueForItem(item);
      this.recordRecent(item);

      try {
         if (this.activeMediaFd != null) {
            try {
               this.activeMediaFd.close();
            } catch (Exception var4) {
            }

            this.activeMediaFd = null;
         }

         Uri source = Uri.parse(item.uri);
         Media media;
         if ("content".equalsIgnoreCase(source.getScheme()) && !item.network) {
            this.activeMediaFd = this.getContentResolver().openAssetFileDescriptor(source, "r");
            if (this.activeMediaFd == null) {
               throw new IOException("Android could not open this media file");
            }

            media = new Media(this.vlc, this.activeMediaFd);
         } else {
            media = new Media(this.vlc, source);
         }

         media.setHWDecoderEnabled(this.decoderMode != 1, this.decoderMode == 2);
         if (item.video && item.uri.equals(externalSubtitleMediaUri) && !externalSubtitlePath.isEmpty()) media.addOption(":sub-file=" + externalSubtitlePath);
         media.addOption(":network-caching=" + this.networkCache);
         if (item.network) {
            if (!item.agent.isEmpty()) {
               media.addOption(":http-user-agent=" + this.clean(item.agent));
            }

            if (!item.referer.isEmpty()) {
               media.addOption(":http-referrer=" + this.clean(item.referer));
            }

            if (item.uri.toLowerCase(Locale.ROOT).startsWith("rtsp:")) {
               media.addOption(":rtsp-tcp");
            }
         }

         this.player.setMedia(media);
         media.release();
         this.applyStoredSyncDelays();
         this.applyPlaybackPresetForCurrentMedia();
         this.page = "Player";
         boolean deferVideoStart=item.video&&(keepFullscreen||!reusePlayerUi||!this.videoSurfaceAttached);
         this.pendingAutoFullscreen=item.video&&!keepFullscreen&&!reusePlayerUi&&!this.fullscreen;
         if(deferVideoStart&&this.requestPlaybackFocus()){
            playbackRequested=true;playbackStartPending=true;this.pendingSubtitleResume=true;this.deferPlaybackUntilVideoSurface=true;
         } else if(deferVideoStart){
            playbackRequested=false;playbackStartPending=false;this.pendingSubtitleResume=false;this.deferPlaybackUntilVideoSurface=false;
         } else if (this.requestPlaybackFocus()) {
            this.player.play();
            playbackRequested = true;
            if (this.isCasting() && this.activeCastSession != null) this.startCastPlayback(this.activeCastSession);
         } else {
            playbackRequested = false;
            playbackStartPending = false;
         }

         if (keepFullscreen) {
            try { this.player.detachViews(); this.videoSurfaceAttached = false; } catch (Exception ignored) {}
            if (fullscreenSurface.getParent() instanceof ViewGroup) ((ViewGroup)fullscreenSurface.getParent()).removeView(fullscreenSurface);
            this.buildLandscapePlayerUi(fullscreenSurface);
            if (this.currentIsVideo()) this.attachVideoSurface(fullscreenSurface, true, 0);
            this.showFullscreenControls();
         } else if (reusePlayerUi) {
            if (this.pageTitle != null) this.pageTitle.setText(item.name);
            if (this.playbackTitle != null) this.playbackTitle.setText(item.name);
            if (this.playbackSubtitle != null) this.playbackSubtitle.setText(item.video ? "Now Playing (Video)" : "Now Playing (Audio)");
            if (this.playbackArtwork != null) this.loadArtwork(item, this.playbackArtwork);
            if (this.playbackSeek != null) this.playbackSeek.setProgress(0);
            if (this.playbackTime != null) this.playbackTime.setText(this.playbackClock(0L, this.player.getLength()) + "     /     " + this.playbackClock(this.player.getLength(), this.player.getLength()));
            this.refreshFullscreenQueue();
         } else {
            this.render();
         }
         this.applyAspectRatio();
         this.syncPlaybackUi();
         if (!this.fullscreen && !this.pendingAutoFullscreen) this.toggleFullscreen();
         if (this.currentIsVideo()) this.applyOrientationMode();
      } catch (Exception var5) {
         this.handler.removeCallbacks(this.streamConnectionTimeout);
         playbackRequested = false;
         playbackStartPending = false;
         this.waitingSubtitleRestore = false;
         this.pendingSubtitleSeek = -1L;
         this.syncPlaybackUi();
         if (item.network) this.showNetworkStreamFailure("Could not open stream: " + var5.getMessage()); else this.toast("Could not open media: " + var5.getMessage());
      }

   }

   private void playAdjacent(int delta) {
      if (this.queue.isEmpty() || this.current == null) return;
      int index = -1;
      for (int i = 0; i < this.queue.size(); i++) if (this.queue.get(i).uri.equals(this.current.uri)) { index = i; break; }
      if (index < 0) return;
      int next = index + delta;
      if (next >= 0 && next < this.queue.size()) this.play(this.queue.get(next));
   }

   private void seekBy(long ms) {
      if (this.isCasting()) {
         RemoteMediaClient remote = this.activeCastSession.getRemoteMediaClient();
         if (remote != null && remote.hasMediaSession()) {
            long length = remote.getStreamDuration();
            long target = Math.max(0L, remote.getApproximateStreamPosition() + ms);
            if (length > 0L) target = Math.min(length, target);
            remote.seek(target);
            this.updatePlaybackTime();
         }
         return;
      }
      if (this.player.hasMedia() && this.player.getLength() > 0L) {
         this.player.setTime(Math.max(0L, Math.min(this.player.getLength(), this.player.getTime() + ms)));
         this.updatePlaybackTime();
      }

   }

   private void seekByGesture(float horizontalDelta) {
      if (this.player == null || !this.player.hasMedia()) return;
      float width = this.fullscreenContainer == null ? this.getResources().getDisplayMetrics().widthPixels : this.fullscreenContainer.getWidth();
      int seconds = Math.max(10, Math.min(120, Math.round(Math.abs(horizontalDelta) / Math.max(1.0F, width) * 180.0F)));
      long offsetMs = (horizontalDelta > 0.0F ? seconds : -seconds) * 1000L;
      this.seekBy(offsetMs);
      long actualPosition = this.player.getTime();
      this.showSwipeSeekFeedback(offsetMs, actualPosition);
   }

   private boolean isPlaybackActive() {
      if (this.isCasting()) {
         RemoteMediaClient remote = this.activeCastSession.getRemoteMediaClient();
         return remote != null && remote.hasMediaSession() && remote.isPlaying();
      }
      return this.player != null && (this.player.isPlaying() || playbackRequested);
   }

   private long activePlaybackTime() {
      if (this.isCasting()) { RemoteMediaClient remote = this.activeCastSession.getRemoteMediaClient(); if (remote != null && remote.hasMediaSession()) return remote.getApproximateStreamPosition(); }
      return this.player == null ? 0L : Math.max(0L, this.player.getTime());
   }

   private long activePlaybackLength() {
      if (this.isCasting()) { RemoteMediaClient remote = this.activeCastSession.getRemoteMediaClient(); if (remote != null && remote.hasMediaSession()) return remote.getStreamDuration(); }
      return this.player == null ? 0L : Math.max(0L, this.player.getLength());
   }

   private boolean isBackgroundAudioSession() {
      return this.backgroundAudioEnabled && current != null && !current.video && !current.network && this.player != null && this.player.hasMedia();
   }

   private void syncAudioService() {
      if (this.isBackgroundAudioSession()) {
         AudioPlaybackService.startOrUpdate(this.getApplicationContext(), current.name, this.isPlaybackActive());
      }
   }

   private void syncPlaybackButtons() {
      boolean playing = this.isPlaybackActive();
      this.setPlaybackButtonIcon(this.playbackPlayButton, playing);
      this.setPlaybackButtonIcon(this.fullscreenPlayButton, playing);
      this.setPlaybackButtonIcon(this.miniPlay, playing);
      boolean animateAudio = this.current != null && !this.current.video && playing;
      if (this.playbackArtworkEffect != null) this.playbackArtworkEffect.setPlaying(animateAudio);
      if (this.fullscreenArtworkEffect != null) this.fullscreenArtworkEffect.setPlaying(animateAudio);
   }

   private void setPlaybackButtonIcon(TextView button, boolean playing) {
      if (button == null) return;
      if (!playing) {
         button.setText("▶");
         return;
      }
      Drawable pause = this.getDrawable(R.drawable.pause_icon);
      if (pause == null) return;
      int size = this.dp(20.0F);
      pause.setBounds(0, 0, size, size);
      SpannableString content = new SpannableString(" ");
      content.setSpan(new ImageSpan(pause, ImageSpan.ALIGN_CENTER), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      button.setText(content);
   }

   private void syncPlaybackUi() {
      this.syncPlaybackButtons();
      this.updateMini();
      this.syncAudioService();
      if (VERSION.SDK_INT >= 26 && this.currentIsVideo() && this.videoSurface != null) this.updatePipParams(true);
   }

   static MediaPlayer retainedPlayer() { return player; }
   static boolean retainedPlaybackRequested() { return playbackRequested; }
   static void setRetainedPlaybackRequested(boolean value) { playbackRequested = value; }
   static void refreshRetainedPlaybackUi() {
      MainActivity activity = activeActivity;
      if (activity != null) activity.handler.post(() -> { activity.syncPlaybackButtons(); activity.updateMini(); });
   }

   static void stopRetainedAudio() {
      playbackRequested = false;
      playbackStartPending = false;
      if (player != null) {
         try { if (player.hasMedia()) player.stop(); } catch (Exception ignored) {}
         try { player.setMedia((IMedia)null); } catch (Exception ignored) {}
      }
      if (activeMediaFd != null) {
         try { activeMediaFd.close(); } catch (Exception ignored) {}
         activeMediaFd = null;
      }
      current = null;
      queue.clear();
      MainActivity activity = activeActivity;
      if (activity != null) activity.handler.post(() -> { activity.updateMini(); activity.syncPlaybackUi(); });
   }

   private void togglePlayback() {
      if (this.isCasting()) {
         RemoteMediaClient remote = this.activeCastSession.getRemoteMediaClient();
         if (remote != null && remote.hasMediaSession()) remote.togglePlayback();
         this.syncPlaybackUi();
         return;
      }
      if (this.player != null && this.player.hasMedia()) {
         if (this.isPlaybackActive()) {
            playbackRequested = false;
            playbackStartPending = false;
            this.player.pause();
         } else if (this.requestPlaybackFocus()) {
            if (this.waitingSubtitleRestore) this.pendingSubtitleResume = true;
            playbackRequested = true;
            this.player.play();
         }
         this.syncPlaybackUi();
      }
   }

   private void onPlayerEvent(MediaPlayer.Event event) {
      switch (event.type) {
         case 258:
            if (this.current != null && this.current.network) this.setStreamLoading(true, "Connecting to stream…");
            break;
         case 259:
            if (this.current != null && this.current.network) { this.setStreamLoading(true, "Buffering stream…"); if (this.streamHasPlayed) this.startStreamConnectionTimeout(); }
            break;
         case 260:
            playbackStartPending = false;
            playbackRequested = true;
            if (this.current != null && this.current.network) this.streamHasPlayed = true;
            this.handler.removeCallbacks(this.streamConnectionTimeout);
            this.setStreamLoading(false, null);
            if (this.waitingSubtitleRestore) {
               this.waitingSubtitleRestore = false;
               if (this.pendingSubtitleSeek >= 0L) this.player.setTime(this.pendingSubtitleSeek);
               this.pendingSubtitleSeek = -1L;
               if (this.decoderRestartPending && !this.decoderRestartShouldResume) {
                  this.decoderRestartPending = false;
                  playbackRequested = false;
                  this.player.pause();
               } else if (this.decoderRestartPending) {
                  this.decoderRestartPending = false;
               } else if (!this.pendingSubtitleResume) {
                  playbackRequested = false;
                  this.player.pause();
               }
            }
            if (this.current != null && this.pendingResumeUri.equals(this.current.uri)) {
               long requestedPosition = this.pendingResumePosition;
               long duration = Math.max(0L, this.player.getLength());
               if (requestedPosition > 0L) {
                  long resumePosition = duration > 0L && requestedPosition >= duration - 1500L ? 0L : requestedPosition;
                  if (resumePosition > 0L) this.player.setTime(resumePosition);
                  this.setRecentPosition(this.current.uri, resumePosition, duration);
               }
               this.pendingResumePosition = -1L;
               this.pendingResumeUri = "";
            }
            this.syncPlaybackUi();
            break;
         case 261:
            this.handler.removeCallbacks(this.streamConnectionTimeout);
            this.setStreamLoading(false, null);
            playbackStartPending = false;
            playbackRequested = false;
            this.saveCurrentResumePosition();
            this.syncPlaybackUi();
            break;
         case 262:
            this.handler.removeCallbacks(this.streamConnectionTimeout);
            this.setStreamLoading(false, null);
            if (!playbackStartPending) playbackRequested = false;
            this.syncPlaybackUi();
            break;
         case 263:
         case 264:
         default:
            break;
         case 265:
            playbackStartPending = false;
            playbackRequested = false;
            if (this.current != null) this.setRecentPosition(this.current.uri, 0L, Math.max(0L, this.player.getLength()));
            this.playAdjacent(1);
            break;
         case 266:
            playbackStartPending = false;
            playbackRequested = false;
            if (this.current != null && this.current.network) this.showNetworkStreamFailure(this.playbackErrorMessage());
            else { this.syncPlaybackUi(); this.toast(this.playbackErrorMessage()); }
      }

   }

   private String playbackErrorMessage() {
      if (this.current != null && this.current.network) {
         String scheme = Uri.parse(this.current.uri).getScheme();
         return scheme != null && scheme.equalsIgnoreCase("srt") ? "SRT is not available in this Android VLC engine build. Try HLS, HTTP, or RTSP." : "Could not play the network stream. Check the URL, network, credentials, and stream format.";
      } else {
         return "Playback failed. Check the media address or file permission.";
      }
   }

   private void updateMini() {
      if (this.miniPlayer != null) {
         if (this.current != null && !this.page.equals("Player")) {
            this.miniPlayer.setVisibility(0);
            this.miniTitle.setText(this.current.name);
            this.setPlaybackButtonIcon(this.miniPlay, this.isPlaybackActive());
         } else {
            this.miniPlayer.setVisibility(8);
         }
      }
   }

   private void updatePlaybackTime() {
      if(this.pausedFrameOverlay!=null&&this.player!=null&&this.isPlaybackActive()&&this.player.getTime()>this.pausedFramePosition+80L)this.clearPausedFrameOverlay();
      long d;
      long p;
      if (this.fullscreen && this.player != null && this.fullscreenSeek != null) {
         d = this.activePlaybackLength();
         p = this.activePlaybackTime();
         this.applyAbRepeatIfNeeded(p);
         if (!this.seeking && d > 0L) {
            this.fullscreenSeek.setProgress((int)(1000L * p / d));
         }

         if (this.fullscreenTime != null) {
            this.fullscreenTime.setText(this.playbackClock(p, d) + "  /  " + this.playbackClock(d, d));
         }
      } else if (this.page.equals("Player") && this.player != null && this.playbackSeek != null) {
         d = this.activePlaybackLength();
         p = this.activePlaybackTime();
         this.applyAbRepeatIfNeeded(p);
         if (!this.seeking && d > 0L) {
            this.playbackSeek.setProgress((int)(1000L * p / d));
         }

         this.playbackTime.setText(this.playbackClock(p, d) + "     /     " + this.playbackClock(d, d));
      }

   }

   private String clock(long ms) {
      long totalSeconds = Math.max(0L, ms / 1000L);
      long hours = totalSeconds / 3600L;
      long minutes = (totalSeconds % 3600L) / 60L;
      long seconds = totalSeconds % 60L;
      return hours > 0L
         ? String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
         : String.format(Locale.US, "%02d:%02d", minutes, seconds);
   }

   private String playbackClock(long ms, long durationMs) {
      if (durationMs < 3600000L) return this.clock(ms);
      long totalSeconds = Math.max(0L, ms / 1000L);
      return String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600L, (totalSeconds % 3600L) / 60L, totalSeconds % 60L);
   }

   private void tracksDialog() {
      (new AlertDialog.Builder(this)).setTitle("Audio and subtitles").setItems(new String[]{"Audio track", "Subtitle track"}, (d, w) -> {
         this.showTracks(w == 1);
      }).show();
   }

   private void showTracks(boolean subtitles) {
      if (!this.player.hasMedia()) {
         this.toast("Start playback first");
      } else {
         MediaPlayer.TrackDescription[] tracks = subtitles ? this.player.getSpuTracks() : this.player.getAudioTracks();
         if (tracks != null && tracks.length != 0) {
            String[] names = new String[tracks.length + (subtitles ? 1 : 0)];
            int off = subtitles ? 1 : 0;
            if (subtitles) {
               names[0] = "Disable subtitles";
            }

            for(int i = 0; i < tracks.length; ++i) {
               names[i + off] = tracks[i].name == null ? "Track " + (i + 1) : tracks[i].name;
            }

            (new AlertDialog.Builder(this)).setTitle(subtitles ? "Subtitle track" : "Audio track").setItems(names, (d, w) -> {
               if (subtitles && w == 0) {
                  this.player.setSpuTrack(-1);
               } else if (subtitles) {
                  this.player.setSpuTrack(tracks[w - 1].id);
               } else {
                  this.player.setAudioTrack(tracks[w].id);
               }
            }).setPositiveButton("Add subtitle file", subtitles ? (d, w) -> {
               this.pickSubtitle();
            } : null).show();
         } else if (subtitles) {
            new AlertDialog.Builder(this)
               .setTitle("No subtitles found")
               .setMessage("This video has no embedded subtitle tracks. Would you like to choose a subtitle file from your device?")
               .setPositiveButton("Choose file", (dialog, which) -> this.pickSubtitle())
               .setNegativeButton("Cancel", null)
               .show();
         } else {
            this.toast("No audio tracks");
         }
      }
   }

   private void refreshVideoSurfaceAfterTrackChange() {
      // VLC updates the active output when a track is switched; keep its surface attached.
   }

   private String decoderName() {
      return this.decoderMode == 1 ? "SW" : (this.decoderMode == 2 ? "HW+" : "HW");
   }

   private void applyDecoderModeToCurrentVideo() {
      if (this.current == null || !this.current.video || !this.fullscreen || !this.page.equals("Player") || this.player == null || !this.player.hasMedia()) {
         this.toast("Decoder mode will be used for the next video");
         return;
      }
      MediaItem item = this.current;
      boolean resume = this.isPlaybackActive();
      long position = Math.max(0L, this.player.getTime());
      this.setRecentPosition(item.uri, position, Math.max(0L, this.player.getLength()));
      this.decoderRestartPending = true;
      this.decoderRestartShouldResume = resume;
      this.pendingSubtitleSeek = position;
      this.pendingSubtitleResume = true; // Start the new decoder, then restore paused state if needed.
      this.waitingSubtitleRestore = true;
      this.playFullscreen(item);
      this.toast("Decoder changed to " + this.decoderName() + " at the current position");
   }

   private void decoderDialog() {
      String[] modes = new String[]{"HW  ·  hardware decoding", "SW  ·  software decoding", "HW+  ·  force hardware decoding"};
      (new AlertDialog.Builder(this)).setTitle("Video decoder").setMessage("Decoder changes reload the current video at its saved position.").setSingleChoiceItems(modes, this.decoderMode, (d, w) -> {
         this.decoderMode = w;
         this.softwareDecode = w == 1;
         this.prefs().edit().putInt("decoder", this.decoderMode).putBoolean("software", this.softwareDecode).apply();
         d.dismiss();
         this.applyDecoderModeToCurrentVideo();
         if (this.page.equals("Settings")) {
            this.render();
         }

      }).setNegativeButton("Cancel", (DialogInterface.OnClickListener)null).show();
   }

   private boolean isPortraitPlayerOrientation() {
      return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
   }

   private String orientationModeGlyph() {
      if (this.orientationMode.equals("Portrait Locked")) return "▯";
      if (this.orientationMode.equals("Landscape Locked")) return "▭";
      return "↻";
   }

   private void updateOrientationSwitchButton() {
      if (this.fullscreenOrientationButton == null) return;
      this.fullscreenOrientationButton.setText(this.orientationModeGlyph());
      this.fullscreenOrientationButton.setBackground(this.shape(this.orientationMode.equals("Adaptive") ? CARD : PURPLE, 13));
      this.fullscreenOrientationButton.setContentDescription("Orientation mode: " + this.orientationMode);
   }

   private void applyOrientationMode() {
      if (this.orientationMode.equals("Portrait Locked")) this.setRequestedOrientation(1);
      else if (this.orientationMode.equals("Landscape Locked")) this.setRequestedOrientation(0);
      else this.setRequestedOrientation(10);
      this.updateOrientationSwitchButton();
   }

   private void showOrientationModeMenu(View anchor) {
      String[] modes = new String[]{"Portrait Locked", "Landscape Locked", "Adaptive"};
      LinearLayout menu = this.col();
      menu.setPadding(this.dp(7.0F), this.dp(7.0F), this.dp(7.0F), this.dp(7.0F));
      menu.setBackground(this.shape(Color.rgb(20, 14, 29), 15));
      for (String mode : modes) {
         boolean selected = mode.equals(this.orientationMode);
         TextView option = this.text(this.orientationGlyph(mode) + "    " + mode + (selected ? "   ✓" : ""), 13, selected ? Color.WHITE : LILAC, selected);
         option.setGravity(Gravity.CENTER_VERTICAL);
         option.setPadding(this.dp(13.0F), 0, this.dp(13.0F), 0);
         option.setBackground(this.shape(selected ? Color.rgb(74, 25, 125) : Color.TRANSPARENT, 11));
         menu.addView(option, this.lp(-1, this.dp(44.0F)));
         option.setOnClickListener(v -> {
            this.orientationMode = mode;
            this.prefs().edit().putString("orientation_mode", mode).apply();
            this.applyOrientationMode();
            this.toast(mode);
            this.showFullscreenControls();
            ((PopupWindow) v.getTag()).dismiss();
         });
      }
      PopupWindow popup = new PopupWindow(menu, this.dp(210.0F), -2, true);
      popup.setBackgroundDrawable(this.shape(Color.rgb(20, 14, 29), 15));
      popup.setElevation(this.dp(12.0F));
      for (int i = 0; i < menu.getChildCount(); i++) menu.getChildAt(i).setTag(popup);
      popup.setOutsideTouchable(true);
      popup.showAsDropDown(anchor, -this.dp(90.0F), -this.dp(158.0F));
   }

   private String orientationGlyph(String mode) {
      if (mode.equals("Portrait Locked")) return "▯";
      if (mode.equals("Landscape Locked")) return "▭";
      return "↻";
   }

   private void openOrientationModeMenu(View anchor) {
      if (!this.fullscreen || !this.currentIsVideo()) return;
      this.showOrientationModeMenu(anchor);
      this.showFullscreenControls();
   }

   private void speedDialog() {
      String[] values = new String[]{"0.5×", "0.75×", "1.0×", "1.25×", "1.5×", "2.0×"};
      (new AlertDialog.Builder(this)).setTitle("Playback speed").setItems(values, (d, w) -> {
         this.player.setRate((new float[]{0.5F, 0.75F, 1.0F, 1.25F, 1.5F, 2.0F})[w]);
      }).show();
   }

   private String currentPlaybackPreset() {
      return this.currentIsVideo() ? this.videoPreset : this.audioPreset;
   }

   private void showPlaybackPresets() {
      this.showPlaybackPresets(this.currentIsVideo());
   }

   private void showPresetTypeMenu() {
      AlertDialog dialog = new AlertDialog.Builder(this)
         .setTitle("Playback presets")
         .setItems(new String[]{"Video presets · " + this.videoPreset, "Audio presets · " + this.audioPreset}, (d, which) -> this.showPlaybackPresets(which == 0))
         .setNegativeButton("Close", null)
         .create();
      dialog.setOnShowListener(v -> this.styleThemedDialog(dialog));
      dialog.show();
   }

   private void showPlaybackPresets(boolean video) {
      String[] names = video
         ? new String[]{"Movie", "Sports", "Music Video", "Vivid", "Standard"}
         : new String[]{"Music", "Movie", "Podcast", "Speech", "Standard", "Custom"};
      String[] labels = video
         ? new String[]{"Movie  ·  cinematic brightness", "Sports  ·  highest supported display refresh", "Music Video  ·  brighter presentation", "Vivid  ·  strongest screen brightness", "Standard  ·  restore device defaults"}
         : new String[]{"Music  ·  balanced bass and clarity", "Movie  ·  rich bass and immersive sound", "Podcast  ·  clear voice, softer bass", "Speech  ·  dialogue and vocal clarity", "Standard  ·  flat sound", "Custom  ·  adjust equalizer manually"};
      String active = video ? this.videoPreset : this.audioPreset;
      int selected = 0;
      for (int i = 0; i < names.length; i++) if (names[i].equals(active)) selected = i;
      AlertDialog dialog = new AlertDialog.Builder(this)
         .setTitle(video ? "Video mode preset" : "Audio mode preset")
         .setSingleChoiceItems(labels, selected, (choiceDialog, which) -> {
            String preset = names[which];
            if (video) {
               this.videoPreset = preset;
               this.prefs().edit().putString("video_preset", preset).apply();
            } else {
               this.audioPreset = preset;
               this.prefs().edit().putString("audio_preset", preset).apply();
            }
            this.applyPlaybackPresetForCurrentMedia();
            this.updatePlaybackPresetStatus();
            choiceDialog.dismiss();
            if (this.page.equals("Settings")) this.render();
            this.toast((video ? "Video" : "Audio") + " preset: " + preset);
            if (preset.equals("Custom") && !video) this.showCustomEqualizer(false);
            this.showFullscreenControls();
         })
         .setNegativeButton("Close", null)
         .create();
      dialog.setOnShowListener(v -> this.styleThemedDialog(dialog));
      dialog.show();
   }

   private void updatePlaybackPresetStatus() {
      if (this.playbackPresetStatus != null) {
         this.playbackPresetStatus.setText((this.currentIsVideo() ? "Video · " : "Audio · ") + this.currentPlaybackPreset());
      }
      if (this.audioPresetStatus != null) this.audioPresetStatus.setText(this.audioPreset);
      if (this.videoPresetStatus != null) this.videoPresetStatus.setText(this.videoPreset);
   }

   private void applyStoredSyncDelays() {
      if (this.player == null || !this.player.hasMedia()) return;
      this.player.setAudioDelay(this.audioDelayMs * 1000L);
      this.player.setSpuDelay(this.subtitleDelayMs * 1000L);
   }

   private void applyPlaybackPresetForCurrentMedia() {
      if (this.current == null) return;
      boolean video = this.current.video;
      String preset = video ? this.videoPreset : this.audioPreset;
      if (video) {
         this.applyVideoPresetBrightness(preset);
         this.applyVideoPresetDisplayMode(preset);
      }
      else if (!Float.isNaN(this.originalVideoPresetBrightness)) {
         WindowManager.LayoutParams attributes = this.getWindow().getAttributes();
         attributes.screenBrightness = this.originalVideoPresetBrightness;
         this.getWindow().setAttributes(attributes);
         this.gestureBrightness = attributes.screenBrightness < 0.0F ? 0.8F : attributes.screenBrightness;
         this.originalVideoPresetBrightness = Float.NaN;
      }
      if (!video) this.applyVideoPresetDisplayMode("Standard");
      String audioProfile = this.audioPreset;
      if (this.player == null || !this.player.hasMedia()) return;
      if (audioProfile.equals("Standard") && this.audioBoostDb == 0 && this.bassBoostDb == 0) {
         this.player.setEqualizer(null);
         return;
      }
      int count = MediaPlayer.Equalizer.getBandCount();
      float[] gains = audioProfile.equals("Custom") ? this.loadCustomEqualizer(count) : (audioProfile.equals("Standard") ? new float[count] : this.presetGains(audioProfile, false, count));
      MediaPlayer.Equalizer equalizer = MediaPlayer.Equalizer.create();
      if (equalizer == null) return;
      equalizer.setPreAmp((audioProfile.equals("Standard") ? 0.0F : -2.0F) + this.audioBoostDb);
      for (int i = 0; i < count; i++) {
         float hz = MediaPlayer.Equalizer.getBandFrequency(i);
         float bass = hz <= 100.0F ? this.bassBoostDb : (hz <= 250.0F ? this.bassBoostDb * 0.55F : 0.0F);
         equalizer.setAmp(i, Math.max(-12.0F, Math.min(12.0F, gains[i] + bass)));
      }
      this.player.setEqualizer(equalizer);
   }

   private void showAudioEnhancementDialog(boolean bass) {
      int currentDb = bass ? this.bassBoostDb : this.audioBoostDb;
      int maxDb = bass ? 9 : 6;
      LinearLayout content = this.col();
      content.setPadding(this.dp(18), this.dp(12), this.dp(18), this.dp(7));
      TextView value = this.text(currentDb + " dB", 13, LILAC, true);
      value.setGravity(Gravity.CENTER);
      content.addView(value);
      SeekBar seek = new SeekBar(this);
      seek.setMax(maxDb);
      seek.setProgress(currentDb);
      seek.setProgressTintList(ColorStateList.valueOf(PURPLE));
      seek.setThumbTintList(ColorStateList.valueOf(LILAC));
      content.addView(seek);
      TextView hint = this.hint(bass ? "Boosts low-frequency equalizer bands." : "Adds up to +6 dB through VLC’s audio equalizer preamp.");
      content.addView(hint);
      seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
         public void onStartTrackingTouch(SeekBar bar) {}
         public void onStopTrackingTouch(SeekBar bar) {}
         public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) return;
            value.setText(progress + " dB");
            if (bass) {
               bassBoostDb = progress;
               prefs().edit().putInt("bass_boost_db", progress).apply();
            } else {
               audioBoostDb = progress;
               prefs().edit().putInt("audio_boost_db", progress).apply();
            }
            applyPlaybackPresetForCurrentMedia();
         }
      });
      new AlertDialog.Builder(this).setTitle(bass ? "Bass boost" : "Audio boost").setView(content)
         .setPositiveButton("Done", null).setNegativeButton("Reset", (dialog, which) -> {
            if (bass) { this.bassBoostDb = 0; this.prefs().edit().putInt("bass_boost_db", 0).apply(); }
            else { this.audioBoostDb = 0; this.prefs().edit().putInt("audio_boost_db", 0).apply(); }
            this.applyPlaybackPresetForCurrentMedia();
         }).show();
   }

   private void setSyncDelay(boolean subtitles, int delayMs) {
      int bounded = Math.max(-500, Math.min(500, delayMs));
      if (subtitles) {
         this.subtitleDelayMs = bounded;
         this.prefs().edit().putInt("subtitle_delay_ms", bounded).apply();
      } else {
         this.audioDelayMs = bounded;
         this.prefs().edit().putInt("audio_delay_ms", bounded).apply();
      }
      if (this.player != null && this.player.hasMedia()) {
         if (subtitles) this.player.setSpuDelay(bounded * 1000L);
         else this.player.setAudioDelay(bounded * 1000L);
      }
   }

   private void showSyncDelayDialog(boolean subtitles) {
      int initial = subtitles ? this.subtitleDelayMs : this.audioDelayMs;
      int[] requested = new int[]{initial};
      LinearLayout content = this.col();
      content.setPadding(this.dp(18), this.dp(12), this.dp(18), this.dp(8));
      TextView value = this.text(String.format(Locale.US, "%+d ms", initial), 13, LILAC, true);
      value.setGravity(Gravity.CENTER);
      content.addView(value);
      SeekBar slider = new SeekBar(this);
      slider.setMax(100);
      slider.setProgress((initial + 500) / 10);
      slider.setProgressTintList(ColorStateList.valueOf(PURPLE));
      slider.setThumbTintList(ColorStateList.valueOf(LILAC));
      content.addView(slider);
      content.addView(this.hint("Adjust from −500 to +500 ms. Changes apply to the current track immediately."));
      slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
         public void onStartTrackingTouch(SeekBar bar) {}
         public void onStopTrackingTouch(SeekBar bar) { setSyncDelay(subtitles, requested[0]); }
         public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) return;
            requested[0] = progress * 10 - 500;
            value.setText(String.format(Locale.US, "%+d ms", requested[0]));
            setSyncDelay(subtitles, requested[0]);
         }
      });
      new AlertDialog.Builder(this).setTitle(subtitles ? "Subtitle synchronization" : "Audio synchronization")
         .setView(content).setPositiveButton("Done", null)
         .setNegativeButton("Reset", (dialog, which) -> this.setSyncDelay(subtitles, 0)).show();
   }

   private void showBrightnessDialog() {
      LinearLayout body = this.col();
      body.setPadding(this.dp(18), this.dp(8), this.dp(18), this.dp(4));
      TextView label = this.text("Screen brightness · " + this.getWindowBrightnessPercent() + "%", 12, LILAC, true);
      body.addView(label);
      SeekBar slider = new SeekBar(this);
      slider.setMax(100);
      slider.setProgress(this.getWindowBrightnessPercent());
      slider.setProgressTintList(ColorStateList.valueOf(PURPLE));
      slider.setThumbTintList(ColorStateList.valueOf(LILAC));
      body.addView(slider);
      slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
         public void onStartTrackingTouch(SeekBar bar) {}
         public void onStopTrackingTouch(SeekBar bar) {}
         public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
            if (fromUser) { setWindowBrightnessPercent(Math.max(1, value)); label.setText("Screen brightness · " + Math.max(1, value) + "%"); }
         }
      });
      new AlertDialog.Builder(this).setTitle("Video brightness").setView(body).setPositiveButton("Done", null).show();
   }

   private void showSleepTimerDialog() {
      String[] options = new String[]{"Off", "15 minutes", "30 minutes", "60 minutes", "90 minutes"};
      (new AlertDialog.Builder(this)).setTitle("Sleep timer")
         .setItems(options, (dialog, which) -> {
            this.handler.removeCallbacks(this.sleepTimer);
            if (which == 0) {
               this.sleepTimerDeadlineMs = 0L;
               this.toast("Sleep timer off");
            } else {
               int minutes = new int[]{0, 15, 30, 60, 90}[which];
               this.sleepTimerDeadlineMs = android.os.SystemClock.elapsedRealtime() + minutes * 60000L;
               this.handler.postDelayed(this.sleepTimer, minutes * 60000L);
               this.toast("Playback will pause in " + minutes + " minutes");
            }
         }).show();
   }

   private String abRepeatDescription() {
      if (this.abRepeatStartMs < 0L) return "Set start and end points during playback";
      if (this.abRepeatEndMs < 0L) return "A set at " + this.clock(this.abRepeatStartMs) + " · choose B";
      return "Repeating " + this.clock(this.abRepeatStartMs) + " – " + this.clock(this.abRepeatEndMs);
   }

   private void clearAbRepeat() {
      this.abRepeatStartMs = -1L;
      this.abRepeatEndMs = -1L;
      this.abRepeatUri = "";
   }

   private void showAbRepeatDialog() {
      if (this.player == null || !this.player.hasMedia() || !this.player.isSeekable()) {
         this.toast("A–B repeat requires a seekable media file");
         return;
      }
      new AlertDialog.Builder(this).setTitle("A–B repeat")
         .setMessage(this.abRepeatDescription())
         .setNeutralButton(this.abRepeatStartMs < 0L ? "Set A" : "Reset A", (dialog, which) -> {
            this.abRepeatStartMs = Math.max(0L, this.player.getTime());
            this.abRepeatEndMs = -1L;
            this.abRepeatUri = this.current == null ? "" : this.current.uri;
            this.toast("A set at " + this.clock(this.abRepeatStartMs) + " · choose B");
         })
         .setPositiveButton("Set B", (dialog, which) -> {
            long position = Math.max(0L, this.player.getTime());
            if (this.abRepeatStartMs < 0L || !this.abRepeatUri.equals(this.current == null ? "" : this.current.uri)) {
               this.toast("Set point A first");
            } else if (position <= this.abRepeatStartMs) {
               this.toast("Point B must be after point A");
            } else {
               this.abRepeatEndMs = position;
               this.toast("A–B repeat enabled");
            }
         })
         .setNegativeButton("Clear", (dialog, which) -> this.clearAbRepeat())
         .show();
   }

   private void applyAbRepeatIfNeeded(long position) {
      if (this.abRepeatStartMs < 0L || this.abRepeatEndMs <= this.abRepeatStartMs || this.player == null || !this.player.hasMedia()) return;
      if (this.current == null || !this.abRepeatUri.equals(this.current.uri)) { this.clearAbRepeat(); return; }
      if (this.player.isPlaying() && position >= this.abRepeatEndMs) this.player.setTime(this.abRepeatStartMs);
   }

   private void ensureScreenLockOverlay() {
      if (this.fullscreenContainer == null || this.screenLockOverlay != null) return;
      this.screenLockOverlay = new FrameLayout(this);
      this.screenLockOverlay.setBackgroundColor(Color.TRANSPARENT);
      this.screenLockOverlay.setClickable(true);
      this.screenLockOverlay.setOnTouchListener((view, event) -> true);
      TextView unlock = this.compactButton("🔒  Unlock controls", true);
      this.screenUnlockButton = unlock;
      FrameLayout.LayoutParams unlockLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
      unlockLp.topMargin = this.dp(24.0F);
      this.screenLockOverlay.addView(unlock, unlockLp);
      unlock.setOnClickListener(view -> this.setScreenTouchesLocked(false));
      this.fullscreenContainer.addView(this.screenLockOverlay, new FrameLayout.LayoutParams(-1, -1));
      this.screenLockOverlay.setVisibility(View.GONE);
   }

   private void setScreenTouchesLocked(boolean locked) {
      this.screenTouchesLocked = locked;
      this.ensureScreenLockOverlay();
      if (this.screenLockOverlay != null) this.screenLockOverlay.setVisibility(locked ? View.VISIBLE : View.GONE);
      if (locked) {
         this.hideFullscreenControlsNow();
         this.handler.removeCallbacks(this.hideGestureHud);
         if (this.brightnessHud != null) this.brightnessHud.setVisibility(View.GONE);
         if (this.volumeHud != null) this.volumeHud.setVisibility(View.GONE);
         this.toast("Screen locked · tap Unlock controls to resume");
      } else {
         this.showFullscreenControls();
         this.toast("Screen unlocked");
      }
   }

   private void setBackgroundAudioEnabled(boolean enabled) {
      this.backgroundAudioEnabled = enabled;
      this.prefs().edit().putBoolean("background_audio_enabled", enabled).apply();
      if (!enabled) AudioPlaybackService.stop(this.getApplicationContext());
      this.toast(enabled ? "Background audio enabled" : "Background audio disabled");
   }

   private float[] presetGains(String preset, boolean video, int count) {
      float[] gains = new float[count];
      for (int i = 0; i < count; i++) {
         float hz = MediaPlayer.Equalizer.getBandFrequency(i);
         if (preset.equals("Sports")) {
            gains[i] = hz < 100 ? 1.0F : (hz < 400 ? -0.5F : (hz < 1200 ? 1.5F : (hz < 5000 ? 3.5F : 2.0F)));
         } else if (preset.equals("Podcast") || preset.equals("Speech")) {
            gains[i] = hz < 120 ? -4.0F : (hz < 500 ? -1.0F : (hz < 1200 ? 2.0F : (hz < 5000 ? (preset.equals("Speech") ? 4.0F : 3.0F) : 1.0F)));
         } else if (preset.equals("Movie")) {
            gains[i] = hz < 80 ? 4.0F : (hz < 200 ? 2.5F : (hz < 800 ? 0.5F : (hz < 4000 ? 1.0F : 1.5F)));
         } else if (preset.equals("Music Video")) {
            gains[i] = hz < 120 ? 2.0F : (hz < 500 ? 1.0F : (hz < 2000 ? 0.0F : 1.5F));
         } else if (preset.equals("Vivid")) {
            gains[i] = hz < 100 ? 2.5F : (hz < 500 ? 0.5F : (hz < 2000 ? 1.0F : (hz < 7000 ? 2.5F : 1.5F)));
         } else if (preset.equals("Music")) {
            gains[i] = hz < 100 ? 3.0F : (hz < 250 ? 2.0F : (hz < 1000 ? 0.0F : (hz < 6000 ? 1.0F : 2.0F)));
         }
         gains[i] = Math.max(-12.0F, Math.min(12.0F, gains[i]));
      }
      return gains;
   }

   private void applyVideoPresetBrightness(String preset) {
      if (preset.equals("Custom")) return;
      WindowManager.LayoutParams attributes = this.getWindow().getAttributes();
      if (preset.equals("Standard")) {
         if (!Float.isNaN(this.originalVideoPresetBrightness)) {
            attributes.screenBrightness = this.originalVideoPresetBrightness;
            this.getWindow().setAttributes(attributes);
            this.gestureBrightness = attributes.screenBrightness < 0.0F ? 0.8F : attributes.screenBrightness;
            this.originalVideoPresetBrightness = Float.NaN;
         }
         return;
      }
      if (Float.isNaN(this.originalVideoPresetBrightness)) this.originalVideoPresetBrightness = attributes.screenBrightness;
      int brightness = preset.equals("Sports") ? 92 : (preset.equals("Vivid") ? 96 : (preset.equals("Music Video") ? 88 : 80));
      this.setWindowBrightnessPercent(brightness);
   }

   private void applyVideoPresetDisplayMode(String preset) {
      WindowManager.LayoutParams attributes = this.getWindow().getAttributes();
      if (!preset.equals("Sports")) {
         if (this.originalPreferredDisplayModeId >= 0) {
            attributes.preferredDisplayModeId = this.originalPreferredDisplayModeId;
            this.getWindow().setAttributes(attributes);
            this.originalPreferredDisplayModeId = -1;
         }
         return;
      }
      if (android.os.Build.VERSION.SDK_INT < 23) return;
      if (this.originalPreferredDisplayModeId < 0) this.originalPreferredDisplayModeId = attributes.preferredDisplayModeId;
      Display display = this.getWindowManager().getDefaultDisplay();
      Display.Mode currentMode = display.getMode();
      Display.Mode bestMode = currentMode;
      for (Display.Mode mode : display.getSupportedModes()) {
         boolean sameSize = mode.getPhysicalWidth() == currentMode.getPhysicalWidth() && mode.getPhysicalHeight() == currentMode.getPhysicalHeight();
         if (sameSize && mode.getRefreshRate() > bestMode.getRefreshRate()) bestMode = mode;
      }
      attributes.preferredDisplayModeId = bestMode.getModeId();
      this.getWindow().setAttributes(attributes);
   }

   private float[] loadCustomEqualizer(int count) {
      if (this.customEqBands == null || this.customEqBands.length != count) {
         this.customEqBands = new float[count];
         String saved = this.prefs().getString("custom_eq_bands", "");
         if (!saved.isEmpty()) {
            String[] parts = saved.split(",");
            for (int i = 0; i < Math.min(parts.length, count); i++) {
               try { this.customEqBands[i] = Float.parseFloat(parts[i]); } catch (Exception ignored) {}
            }
         }
      }
      return this.customEqBands.clone();
   }

   private void saveCustomEqualizer(float[] gains) {
      this.customEqBands = gains.clone();
      StringBuilder serialized = new StringBuilder();
      for (int i = 0; i < gains.length; i++) {
         if (i > 0) serialized.append(',');
         serialized.append(gains[i]);
      }
      this.prefs().edit().putString("custom_eq_bands", serialized.toString()).apply();
   }

   private void showCustomEqualizer(boolean video) {
      int count = MediaPlayer.Equalizer.getBandCount();
      float[] gains = this.loadCustomEqualizer(count);
      LinearLayout rows = this.col();
      rows.setPadding(this.dp(14.0F), this.dp(8.0F), this.dp(14.0F), this.dp(8.0F));
      rows.addView(this.hint(video ? "Adjust the sound curve; picture brightness remains available through the player gesture." : "Tune the sound curve. Changes apply when each slider is released."));
      ScrollView scroll = new ScrollView(this);
      scroll.addView(rows);
      for (int i = 0; i < count; i++) {
         final int band = i;
         float frequency = MediaPlayer.Equalizer.getBandFrequency(i);
         String label = frequency >= 1000.0F ? String.format(Locale.US, "%.1f kHz", frequency / 1000.0F) : String.format(Locale.US, "%.0f Hz", frequency);
         LinearLayout row = this.col();
         TextView value = this.text(label + "  ·  " + String.format(Locale.US, "%+.1f dB", gains[i]), 11, LILAC, true);
         row.addView(value);
         SeekBar slider = new SeekBar(this);
         slider.setMax(24);
         slider.setProgress(Math.max(0, Math.min(24, Math.round(gains[i] + 12.0F))));
         slider.setProgressTintList(ColorStateList.valueOf(PURPLE));
         slider.setThumbTintList(ColorStateList.valueOf(LILAC));
         row.addView(slider);
         LinearLayout.LayoutParams rowParams = this.lp(-1, -2);
         rowParams.bottomMargin = this.dp(4.0F);
         rows.addView(row, rowParams);
         slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {
               gains[band] = bar.getProgress() - 12.0F;
               value.setText(label + "  ·  " + String.format(Locale.US, "%+.1f dB", gains[band]));
               saveCustomEqualizer(gains);
               applyPlaybackPresetForCurrentMedia();
            }
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
               if (fromUser) value.setText(label + "  ·  " + String.format(Locale.US, "%+.1f dB", progress - 12.0F));
            }
         });
      }
      new AlertDialog.Builder(this).setTitle("Custom equalizer").setView(scroll)
         .setPositiveButton("Done", (dialog, which) -> { this.saveCustomEqualizer(gains); this.applyPlaybackPresetForCurrentMedia(); this.updatePlaybackPresetStatus(); })
         .setNegativeButton("Close", null).show();
   }

   private void toggleFavorite(MediaItem m) {
      ArrayList<String> f = (ArrayList)this.playlists.computeIfAbsent("Favorites", (x) -> {
         return new ArrayList();
      });
      if (f.contains(m.uri)) {
         f.remove(m.uri);
      } else {
         f.add(m.uri);
      }

      this.invalidatePageCachePrefix("Playlists");
      this.invalidatePageCachePrefix("Home|");
      this.saveData();
      if (this.page.equals("Player")) {
         this.render();
      }

   }

   private boolean isFavorite(String uri) {
      return this.playlists.containsKey("Favorites") && ((ArrayList)this.playlists.get("Favorites")).contains(uri);
   }

   private void addToPlaylist(MediaItem m) {
      String[] names = (String[])this.playlists.keySet().toArray(new String[0]);
      (new AlertDialog.Builder(this)).setTitle("Add to playlist").setItems(names, (d, w) -> {
         ((ArrayList)this.playlists.get(names[w])).add(m.uri);
         this.invalidatePageCachePrefix("Playlists");
         this.saveData();
         this.toast("Added to " + names[w]);
      }).setPositiveButton("New playlist", (d, w) -> {
         this.createPlaylist(m);
      }).show();
   }

   private void createPlaylist() {
      this.createPlaylist((MediaItem)null);
   }

   private void createPlaylist(MediaItem add) {
      EditText name = this.edit("Playlist name");
      (new AlertDialog.Builder(this)).setTitle("New playlist").setView(name).setNegativeButton("Cancel", (DialogInterface.OnClickListener)null).setPositiveButton("Create", (d, w) -> {
         String n = name.getText().toString().trim();
         if (!n.isEmpty()) {
            ArrayList<String> ids = new ArrayList();
            if (add != null) ids.add(add.uri);
            this.playlists.put(n, ids);
            this.activePlaylist = n;
            this.page = "Playlist";
            this.saveData();
            this.render();
            this.showPlaylistMediaPicker(n);
         }
      }).show();
   }

   private void showPlaylistMediaPicker(String playlistName) {
      this.pendingPlaylistName = playlistName;
      new AlertDialog.Builder(this).setTitle("Add media to " + playlistName)
         .setItems(new String[]{"Browse and select files", "Select a folder"}, (dialog, which) -> {
            if (which == 0) this.pickPlaylistFiles(); else this.pickPlaylistFolder();
         }).setNegativeButton("Cancel", null).show();
   }

   private void pickPlaylistFiles() {
      this.pickerOpen = true;
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.setType("*/*");
      intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
      this.startActivityForResult(intent, PICK_PLAYLIST_FILES);
   }

   private void pickPlaylistFolder() {
      this.pickerOpen = true;
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
      this.startActivityForResult(intent, PICK_PLAYLIST_FOLDER);
   }

   private void addPlaylistUri(Uri uri, ArrayList<String> items) {
      try { this.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
      String value = uri.toString();
      if (!items.contains(value)) items.add(value);
   }

   private void scanPlaylistTree(Uri tree, String playlistName) {
      try {
         ArrayList<String> found = new ArrayList();
         this.scanPlaylistDocument(tree, DocumentsContract.getTreeDocumentId(tree), found);
         this.handler.post(() -> {
            ArrayList<String> ids = this.playlists.computeIfAbsent(playlistName, key -> new ArrayList());
            for (String uri : found) if (!ids.contains(uri)) ids.add(uri);
            this.saveData();
            this.page = "Playlist";
            this.activePlaylist = playlistName;
            this.render();
            this.toast(found.isEmpty() ? "No audio or video files found in that folder" : found.size() + " media files added");
         });
      } catch (Exception error) {
         this.handler.post(() -> this.toast("Could not add folder: " + error.getMessage()));
      }
   }

   private void scanPlaylistDocument(Uri tree, String documentId, ArrayList<String> found) {
      Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId);
      String[] columns = new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};
      Cursor cursor = this.getContentResolver().query(children, columns, null, null, null);
      if (cursor == null) return;
      try {
         while (cursor.moveToNext()) {
            String childId = cursor.getString(0);
            String name = cursor.getString(1);
            String mime = cursor.getString(2);
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
               this.scanPlaylistDocument(tree, childId, found);
            } else if (this.isMediaName(name)) {
               found.add(DocumentsContract.buildDocumentUriUsingTree(tree, childId).toString());
            }
         }
      } finally {
         cursor.close();
      }
   }

   private MediaItem findMedia(String uri) {
      for (MediaItem item : this.library) if (item.uri.equals(uri)) return item;
      for (MediaItem item : this.recentPlayed) if (item.uri.equals(uri)) return item;
      for (MediaItem item : this.recentStreams) if (item.uri.equals(uri)) return item;
      try {
         Uri parsed = Uri.parse(uri);
         String name = parsed.getLastPathSegment();
         Cursor cursor = this.getContentResolver().query(parsed, null, null, null, null);
         if (cursor != null) {
            try {
               if (cursor.moveToFirst()) {
                  int index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                  if (index >= 0) name = cursor.getString(index);
               }
            } finally { cursor.close(); }
         }
         if (name == null || name.isEmpty()) name = "Media file";
         String mime = this.getContentResolver().getType(parsed);
         boolean video = (mime != null && mime.startsWith("video/")) || this.isVideoName(name);
         return new MediaItem(name, uri, video, "", 0L, 0L, "", "");
      } catch (Exception ignored) {
         return null;
      }
   }

   private void searchDialog() {
      EditText q = this.edit("Search movies, songs, albums");
      (new AlertDialog.Builder(this)).setTitle("Search library").setView(q).setPositiveButton("Search", (d, w) -> {
         this.searchQuery = q.getText().toString().trim().toLowerCase(Locale.ROOT);
         this.page = "Movies";
         this.render();
      }).setNegativeButton("Cancel", (DialogInterface.OnClickListener)null).show();
   }

   private int videoCount() {
      int n = 0;
      Iterator var2 = this.library.iterator();

      while(var2.hasNext()) {
         MediaItem m = (MediaItem)var2.next();
         if (m.video) {
            ++n;
         }
      }

      return n;
   }

   private void requestMediaPermission() {
      ArrayList<String> p = new ArrayList();
      if (VERSION.SDK_INT >= 33) {
         if (this.checkSelfPermission("android.permission.READ_MEDIA_AUDIO") != 0) {
            p.add("android.permission.READ_MEDIA_AUDIO");
         }

         if (this.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") != 0) {
            p.add("android.permission.READ_MEDIA_VIDEO");
         }
      } else if (VERSION.SDK_INT >= 23 && this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
         p.add("android.permission.READ_EXTERNAL_STORAGE");
      }

      if (p.isEmpty()) {
         this.scanLibrary();
      } else {
         this.permissionRequestInFlight = true;
         this.requestPermissions((String[])p.toArray(new String[0]), 77);
      }
   }

   private void ensureMediaPermission() {
      boolean any = VERSION.SDK_INT >= 33 ? this.checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == 0 || this.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == 0 : VERSION.SDK_INT < 23 || this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0;
      if (any) {
         this.scanLibrary();
      } else if (!this.permissionRequestInFlight) {
         this.requestMediaPermission();
      }

   }

   public void onRequestPermissionsResult(int code, String[] names, int[] results) {
      super.onRequestPermissionsResult(code, names, results);
      if (code == 77) {
         this.permissionRequestInFlight = false;
         boolean access = VERSION.SDK_INT >= 33 ? this.checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == 0 || this.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == 0 : VERSION.SDK_INT < 23 || this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0;
         if (access) {
            this.scanLibrary();
         } else {
            this.render();
         }

      }
   }

   public void onConfigurationChanged(Configuration config) {
      super.onConfigurationChanged(config);
      if (VERSION.SDK_INT >= 26 && this.isInPictureInPictureMode()) return;
      if (this.current == null || !this.page.equals("Player")) return;
      this.updateOrientationSwitchButton();
      if (!this.fullscreen && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
         if(this.currentIsVideo())this.capturePausedFrameThen(()->{if(!this.fullscreen&&this.page.equals("Player"))this.toggleFullscreen();});
         else this.toggleFullscreen();
      } else if (this.fullscreen) {
         VLCVideoLayout surface = this.videoSurface;
         if (this.currentIsVideo() && surface != null && this.player != null && this.player.hasMedia()) {
            Runnable resize=()->{surface.requestLayout();surface.post(()->{if(!this.fullscreen||surface!=this.videoSurface||this.player==null||!this.player.hasMedia())return;try{if(this.player.getVLCVout().areViewsAttached()){this.player.updateVideoSurfaces();this.videoSurfaceAttached=true;this.applyAspectRatio();}else this.attachVideoSurface(surface,true,0);}catch(Exception error){Log.w("SoloPlayer","Could not resize video output after rotation",error);this.scheduleVideoSurfaceRecovery(80L);}});};
            if(this.isPlaybackActive()||this.playbackStartPending)resize.run();else this.capturePausedFrameThen(resize);
         }
      }
   }

   private SurfaceView findVideoSurfaceView(View view) {
      if(view instanceof SurfaceView)return (SurfaceView)view;
      if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){SurfaceView found=this.findVideoSurfaceView(group.getChildAt(i));if(found!=null)return found;}}
      return null;
   }

   private void capturePausedFrameThen(Runnable transition) {
      if(this.current==null||!this.currentIsVideo()||this.player==null||!this.player.hasMedia()||this.isPlaybackActive()||this.pausedFrameOverlay!=null){this.performPausedFrameTransition(transition);return;}
      SurfaceView source=this.videoSurface==null?null:this.findVideoSurfaceView(this.videoSurface);
      if(VERSION.SDK_INT<24||source==null||source.getWidth()<=0||source.getHeight()<=0){this.performPausedFrameTransition(transition);return;}
      int width=Math.min(1600,source.getWidth());int height=Math.max(1,Math.round(source.getHeight()*(width/(float)source.getWidth())));
      Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);this.pausedFramePosition=Math.max(0L,this.player.getTime());
      final boolean[] continued={false};Runnable continueTransition=()->{if(continued[0])return;continued[0]=true;this.performPausedFrameTransition(transition);};
      this.handler.postDelayed(continueTransition,350L);
      try{PixelCopy.request(source,bitmap,result->{if(result==PixelCopy.SUCCESS){if(this.pausedFrameBitmap!=null&&this.pausedFrameBitmap!=bitmap)this.pausedFrameBitmap.recycle();this.pausedFrameBitmap=bitmap;if(continued[0])this.installPausedFrameOverlay();}else bitmap.recycle();continueTransition.run();},this.handler);}
      catch(Exception error){bitmap.recycle();continueTransition.run();}
   }

   private void performPausedFrameTransition(Runnable transition) {
      this.pausedFrameTransitionInProgress=true;try{transition.run();}finally{this.pausedFrameTransitionInProgress=false;}this.installPausedFrameOverlay();
   }

   private void installPausedFrameOverlay() {
      if(this.pausedFrameBitmap==null||this.pausedFrameBitmap.isRecycled()||this.videoSurface==null)return;
      ViewParent parent=this.videoSurface.getParent();if(!(parent instanceof FrameLayout))return;FrameLayout frame=(FrameLayout)parent;
      if(this.pausedFrameOverlay!=null&&this.pausedFrameOverlay.getParent()==frame){this.pausedFrameOverlay.setImageBitmap(this.pausedFrameBitmap);return;}
      if(this.pausedFrameOverlay!=null&&this.pausedFrameOverlay.getParent() instanceof ViewGroup)((ViewGroup)this.pausedFrameOverlay.getParent()).removeView(this.pausedFrameOverlay);
      ImageView overlay=new ImageView(this);overlay.setImageBitmap(this.pausedFrameBitmap);overlay.setScaleType(ScaleType.FIT_CENTER);overlay.setBackgroundColor(Color.BLACK);overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);this.pausedFrameOverlay=overlay;
      int index=Math.max(0,frame.indexOfChild(this.videoSurface)+1);frame.addView(overlay,index,new FrameLayout.LayoutParams(-1,-1));
   }

   private void clearPausedFrameOverlay() {
      if(this.pausedFrameOverlay!=null&&this.pausedFrameOverlay.getParent() instanceof ViewGroup)((ViewGroup)this.pausedFrameOverlay.getParent()).removeView(this.pausedFrameOverlay);
      this.pausedFrameOverlay=null;if(this.pausedFrameBitmap!=null&&!this.pausedFrameBitmap.isRecycled())this.pausedFrameBitmap.recycle();this.pausedFrameBitmap=null;this.pausedFramePosition=-1L;
   }

   protected void onResume() {
      super.onResume();
      this.scheduleVideoSurfaceRecovery(120L);
      if (this.pageBody != null && !this.permissionRequestInFlight) {
         boolean access = VERSION.SDK_INT >= 33 ? this.checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == 0 || this.checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == 0 : VERSION.SDK_INT < 23 || this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0;
         if (access) {
            this.scanLibrary();
         }
      }

   }

   @Override public void onWindowFocusChanged(boolean hasFocus) {
      super.onWindowFocusChanged(hasFocus);
      if (hasFocus) this.scheduleVideoSurfaceRecovery(100L);
   }

   private void scheduleVideoSurfaceRecovery(long delayMs) {
      if (!this.currentIsVideo() || this.player == null || !this.player.hasMedia() || !this.page.equals("Player")) return;
      if (this.videoSurfaceAttached) return;
      long now = android.os.SystemClock.elapsedRealtime();
      if (now - this.lastVideoSurfaceRecoveryAt < 350L) return;
      this.lastVideoSurfaceRecoveryAt = now;
      this.handler.removeCallbacks(this.recoverVideoSurface);
      this.handler.postDelayed(this.recoverVideoSurface, delayMs);
   }

   private void scanLibrary() {
      if (this.pageBody != null) {
         long now = android.os.SystemClock.elapsedRealtime();
         if (this.libraryScanInFlight || (this.lastLibraryScanAt > 0L && now - this.lastLibraryScanAt < 60000L)) return;
         this.lastLibraryScanAt = now;
         this.libraryScanInFlight = true;
         this.worker.execute(() -> {
            ArrayList<MediaItem> found = new ArrayList<>(this.mediaLibraryRepository.scan());

            final JSONArray privateSnapshot=this.privateMediaSnapshot();
            found.removeIf(item -> this.isPrivateSource(item, privateSnapshot));
            Collections.sort(found, (a, b) -> {
               return Long.compare(b.added, a.added);
            });
            this.handler.post(() -> {
               this.libraryScanInFlight = false;
               this.library.clear();
               this.library.addAll(found);
               this.pageViewCache.clear();
               this.attachedPageCacheKey = null;
               this.saveData();
               if (this.pageBody != null && !this.page.equals("Player") && !this.fullscreen) {
                  this.render();
               }

            });
         });
      }
   }

   private void restoreData() {
      this.playlists.put("Favorites", new ArrayList());
      String json = this.prefs().getString("data", "");
      if (!json.isEmpty()) {
         try {
            JSONObject o = new JSONObject(json);
            JSONArray p = o.optJSONArray("playlists");
            if (p != null) {
               for(int i = 0; i < p.length(); ++i) {
                  JSONObject x = p.getJSONObject(i);
                  String name = x.getString("name");
                  JSONArray a = x.getJSONArray("items");
                  ArrayList<String> ids = new ArrayList();

                  for(int j = 0; j < a.length(); ++j) {
                     ids.add(a.getString(j));
                  }

                  this.playlists.put(name, ids);
               }
            }

            JSONArray h = o.optJSONArray("history");
            if (h != null) {
               for(int i = 0; i < h.length(); ++i) {
                  Object entry = h.get(i);
                  MediaItem m;
                  if (entry instanceof JSONObject) {
                     JSONObject item = (JSONObject)entry;
                     String uri = item.optString("uri", "");
                     if (uri.isEmpty()) continue;
                     String name = item.optString("name", uri);
                     m = new MediaItem(name, uri, item.optBoolean("video", this.isVideoName(name)));
                     m.path = item.optString("path", "");
                     m.positionMs = Math.max(0L, item.optLong("position", 0L));
                     m.duration = Math.max(0L, item.optLong("duration", 0L));
                     m.artist = item.optString("artist", "");
                     m.album = item.optString("album", "");
                  } else {
                     String uri = String.valueOf(entry);
                     String name = Uri.parse(uri).getLastPathSegment();
                     if (name == null || name.isEmpty()) name = uri;
                     m = new MediaItem(name, uri, this.isVideoName(name));
                  }
                  if (!m.network && !this.isPrivateSource(m, this.privateVideos)) this.recentPlayed.add(m);
               }
               while (this.recentPlayed.size() > 5) this.recentPlayed.remove(this.recentPlayed.size() - 1);
            }

            JSONArray s = o.optJSONArray("streams");
            if (s != null) {
               for(int i = 0; i < s.length(); ++i) {
                  JSONObject x = s.getJSONObject(i);
                  MediaItem m = new MediaItem(x.getString("name"), x.getString("uri"), false);
                  m.network = true;
                  m.agent = x.optString("agent");
                  m.referer = x.optString("referer");
                  m.logo = x.optString("logo", ""); m.group = x.optString("group", ""); m.tvgId = x.optString("tvgId", "");
                  this.recentStreams.add(m);
               }
            }
            this.streamBookmarks.clear();
            this.streamBookmarks.addAll(this.streamBookmarkRepository.load(o.optJSONArray("streamBookmarks")));
         } catch (Exception var10) {
         }

      }
   }

   private void saveData() {
      try {
         JSONObject o = new JSONObject();
         JSONArray ps = new JSONArray();
         Iterator var3 = this.playlists.keySet().iterator();

         while(var3.hasNext()) {
            String name = (String)var3.next();
            JSONObject p = new JSONObject();
            p.put("name", name);
            JSONArray ids = new JSONArray();
            Iterator var7 = ((ArrayList)this.playlists.get(name)).iterator();

            while(var7.hasNext()) {
               String id = (String)var7.next();
               ids.put(id);
            }

            p.put("items", ids);
            ps.put(p);
         }

         o.put("playlists", ps);
         JSONArray h = new JSONArray();
         Iterator var11 = this.recentPlayed.iterator();

         while(var11.hasNext()) {
            MediaItem m = (MediaItem)var11.next();
            JSONObject item = new JSONObject();
            item.put("name", m.name);
            item.put("uri", m.uri);
            item.put("video", m.video);
            item.put("path", m.path);
            item.put("position", m.positionMs);
            item.put("duration", m.duration);
            item.put("artist", m.artist);
            item.put("album", m.album);
            h.put(item);
         }

         o.put("history", h);
         JSONArray st = new JSONArray();
         Iterator var14 = this.recentStreams.iterator();

         while(var14.hasNext()) {
            MediaItem m = (MediaItem)var14.next();
            JSONObject s = new JSONObject();
            s.put("name", m.name);
            s.put("uri", m.uri);
            s.put("agent", m.agent);
            s.put("referer", m.referer);
            s.put("logo", m.logo); s.put("group", m.group); s.put("tvgId", m.tvgId);
            st.put(s);
         }

         o.put("streams", st);
         this.prefs().edit().putString("data", o.toString()).apply();
      } catch (Exception var9) {
      }
      if (this.streamBookmarkRepository != null) this.streamBookmarkRepository.save(this.streamBookmarks);

   }

   private void loadM3u(String address, String name, String ua, String ref) {
      String scheme = Uri.parse(address).getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) { this.toast("Channel playlists must use HTTP or HTTPS"); return; }
      if (this.playlistLoadingDialog != null && this.playlistLoadingDialog.isShowing()) this.playlistLoadingDialog.dismiss();
      this.playlistLoadingDialog = new android.app.ProgressDialog(this); this.playlistLoadingDialog.setTitle("Loading channels"); this.playlistLoadingDialog.setMessage("Connecting to playlist…"); this.playlistLoadingDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER); this.playlistLoadingDialog.setCancelable(false); this.playlistLoadingDialog.show();
      this.worker.execute(() -> {
         HttpURLConnection connection = null;
         try {
            connection = (HttpURLConnection)(new URL(address)).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Accept", "audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*");
            if (!ua.isEmpty()) {
               connection.setRequestProperty("User-Agent", ua);
            }
            if (!ref.isEmpty()) {
               connection.setRequestProperty("Referer", ref);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) throw new IOException("Server returned HTTP " + status);
            M3uPlaylistParser.Result parsed;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
               parsed = M3uPlaylistParser.parse(r, connection.getURL().toString(), name, ua, ref);
            }
            ArrayList<MediaItem> result = new ArrayList<>();
            for (M3uPlaylistParser.Channel channel : parsed.channels) {
               MediaItem item = new MediaItem(channel.name, channel.url, true, "Network", 0L, 0L, "", "");
               item.network = true; item.agent = channel.userAgent; item.referer = channel.referer;
               item.logo = channel.logo; item.group = channel.group; item.tvgId = channel.tvgId;
               result.add(item);
            }
            if (result.isEmpty()) throw new IOException("No channels were found in this playlist. Confirm it contains #EXTINF entries and stream URLs.");
            ArrayList<MediaItem> channelsFound = result; int invalidCount = parsed.invalidEntries;
            this.handler.post(() -> {
               if (this.playlistLoadingDialog != null && this.playlistLoadingDialog.isShowing()) this.playlistLoadingDialog.dismiss();
               this.playlistLoadingDialog = null;
               this.channels.clear();
               this.channels.addAll(channelsFound);
               this.networkSearchQuery = "";
               this.channelPageQuery = null;
               this.channelPageStart = 0;
               this.fullscreenQueueQuery = null;
               this.fullscreenQueuePageStart = 0;
               this.page = "Channels";
               this.render();
               if (invalidCount > 0) this.toast(channelsFound.size() + " channels loaded · " + invalidCount + " invalid entries skipped");
            });
         } catch (Exception error) {
            this.handler.post(() -> {
               if (this.playlistLoadingDialog != null && this.playlistLoadingDialog.isShowing()) this.playlistLoadingDialog.dismiss();
               this.playlistLoadingDialog = null;
               new AlertDialog.Builder(this).setTitle("Playlist could not be loaded")
                  .setMessage(error.getMessage() == null ? "Check the URL and network, then retry." : error.getMessage())
                  .setPositiveButton("Retry", (dialog, which) -> this.loadM3u(address, name, ua, ref))
                  .setNegativeButton("Close", null).show();
            });
         } finally { if (connection != null) connection.disconnect(); }
      });
   }

   private boolean isPlaylistUrl(String s) {
      String x = s.toLowerCase(Locale.ROOT);
      return x.matches(".*\\.m3u8?(?:[?#].*)?$") || x.contains("type=m3u");
   }

   private String clean(String s) {
      return s.replace("\r", "").replace("\n", "");
   }

   private void pickMedia() {
      this.pauseVideoForPicker();
      this.pickerOpen = true;
      Intent i = new Intent("android.intent.action.OPEN_DOCUMENT");
      i.setType("*/*");
      i.putExtra("android.intent.extra.MIME_TYPES", new String[]{"audio/*", "video/*"});
      i.putExtra("android.intent.extra.ALLOW_MULTIPLE", true);
      i.addCategory("android.intent.category.OPENABLE");
      this.startActivityForResult(i, 10);
   }

   private void pickFolder() {
      this.pickerOpen = true;
      Intent i = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
      i.addFlags(65);
      this.startActivityForResult(i, 11);
   }

   private void pickSubtitle() {
      this.pauseVideoForPicker();
      this.pickerOpen = true;
      Intent i = new Intent("android.intent.action.OPEN_DOCUMENT");
      i.setType("*/*");
      i.putExtra("android.intent.extra.MIME_TYPES", new String[]{"text/*", "application/x-subrip", "application/octet-stream"});
      i.addCategory("android.intent.category.OPENABLE");
      this.startActivityForResult(i, 12);
   }

   private void pauseVideoForPicker() {
      if (!this.currentIsVideo() || this.player == null || !this.player.hasMedia()) return;
      this.saveCurrentResumePosition();
      this.pickerPausedVideo = true;
      this.pickerVideoWasPlaying = this.isPlaybackActive();
      this.pickerVideoPosition = Math.max(0L, this.player.getTime());
      if (this.pickerVideoWasPlaying) {
         playbackRequested = false;
         this.player.pause();
         this.syncPlaybackUi();
      }
   }

   private void resumeVideoAfterPicker() {
      if (!this.pickerPausedVideo) return;
      boolean resume = this.pickerVideoWasPlaying;
      long position = this.pickerVideoPosition;
      this.pickerPausedVideo = false;
      if (!resume || !this.currentIsVideo() || this.player == null || !this.player.hasMedia()) return;
      if (this.videoRebindPending || this.deferPlaybackUntilVideoSurface) {
         this.pendingSubtitleSeek = position;
         this.pendingSubtitleResume = true;
         this.deferPlaybackUntilVideoSurface = true;
         this.scheduleVideoSurfaceRecovery(180L);
         return;
      }
      if (position > 0L) this.player.setTime(position);
      if (this.requestPlaybackFocus()) {
         playbackRequested = true;
         this.player.play();
      }
      this.syncPlaybackUi();
   }

   private void addSubtitle(Uri uri) {
      this.worker.execute(() -> {
         try {
            File tmp = File.createTempFile("solo-sub-", ".srt", this.getCacheDir());
            InputStream in = this.getContentResolver().openInputStream(uri);

            try {
               OutputStream out = new FileOutputStream(tmp);

               try {
                  byte[] b = new byte[8192];

                  int n;
                  while((n = in.read(b)) > 0) {
                     out.write(b, 0, n);
                  }
               } catch (Throwable var9) {
                  try {
                     out.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               out.close();
            } catch (Throwable var10) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var7) {
                     var10.addSuppressed(var7);
                  }
               }

               throw var10;
            }

            if (in != null) {
               in.close();
            }

            this.handler.post(() -> {
               if (this.player.hasMedia() && this.currentIsVideo()) {
                  MediaItem item = this.current;
                  this.pendingSubtitleSeek = this.pickerPausedVideo ? this.pickerVideoPosition : Math.max(0L, this.player.getTime());
                  this.pendingSubtitleResume = this.pickerPausedVideo ? this.pickerVideoWasPlaying : this.isPlaybackActive();
                  this.pickerPausedVideo = false;
                  this.waitingSubtitleRestore = true;
                  externalSubtitleMediaUri = item.uri;
                  externalSubtitlePath = tmp.getAbsolutePath();
                  if (this.fullscreen) this.playFullscreen(item); else this.play(item);
                  this.toast("Subtitle added");
               } else if (this.player.hasMedia()) {
                  this.toast("External subtitles are available during video playback");
               } else {
                  this.toast("Start playback before adding subtitles");
               }

            });
         } catch (Exception var11) {
            this.handler.post(() -> {
               this.resumeVideoAfterPicker();
               this.toast("Could not load subtitle: " + var11.getMessage());
            });
         }

      });
   }

   protected void onActivityResult(int code, int result, Intent data) {
      super.onActivityResult(code, result, data);
      this.pickerOpen = false;
      if (code == RESET_PRIVATE_VAULT_PIN) {
         if (result == RESULT_OK) this.createPrivateVaultPin("reset");
         else this.toast("PIN was not reset");
         return;
      }
      if (code == PICK_VAULT_FILES) {
         if(result==RESULT_OK&&data!=null){ArrayList<Uri> selected=new ArrayList<>();try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++){Uri uri=data.getClipData().getItemAt(i).getUri();selected.add(uri);try{this.getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}}}else if(data.getData()!=null){Uri uri=data.getData();selected.add(uri);try{this.getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}}}catch(Exception error){this.toast("Could not prepare selected media: "+error.getMessage());}this.askPrivateImportMode(selected,this.privateMediaPath);}else this.render();
         return;
      }
      if(code==MOVE_PRIVATE_MEDIA){
         if(result==RESULT_OK){for(String id:new ArrayList<>(this.pendingPrivateMoveIds)){JSONObject entry=this.privateVideoEntry(id);if(entry!=null)try{entry.put("sourceDeleted",true);}catch(Exception ignored){}}this.savePrivateVideos();this.lastLibraryScanAt=0L;this.scanLibrary();this.render();this.toast("Original media removed from shared storage and encrypted in Private Media");}
         else {for(String id:new ArrayList<>(this.pendingPrivateMoveIds))this.removePrivateVaultRecordOnly(id);this.savePrivateVideos();this.lastLibraryScanAt=0L;this.scanLibrary();this.render();this.toast("Move cancelled. Original files were left unchanged.");}
         this.pendingPrivateMoveIds.clear();return;
      }
      if(code==PICK_VAULT_FOLDER){if(result==RESULT_OK&&data!=null&&data.getData()!=null)this.importPrivateFolder(data.getData());else this.render();return;}
      if (this.pickerPausedVideo && !(result == -1 && code == 12)) {
         if (result == -1 && data != null && code == 10) this.pickerPausedVideo = false;
         else this.resumeVideoAfterPicker();
      }
      if (result == -1 && data != null) {
         if (code == PICK_PLAYLIST_FILES) {
            String playlistName = this.pendingPlaylistName;
            ArrayList<String> ids = this.playlists.computeIfAbsent(playlistName, key -> new ArrayList());
            if (data.getClipData() != null) {
               for (int i = 0; i < data.getClipData().getItemCount(); i++) this.addPlaylistUri(data.getClipData().getItemAt(i).getUri(), ids);
            } else if (data.getData() != null) this.addPlaylistUri(data.getData(), ids);
            this.pendingPlaylistName = "";
            this.invalidatePageCachePrefix("Playlists");
            this.saveData();
            this.activePlaylist = playlistName;
            this.page = "Playlist";
            this.render();
            this.toast("Media added to " + playlistName);
         } else if (code == PICK_PLAYLIST_FOLDER) {
            Uri tree = data.getData();
            String playlistName = this.pendingPlaylistName;
            this.pendingPlaylistName = "";
            try { this.getContentResolver().takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
            this.worker.execute(() -> this.scanPlaylistTree(tree, playlistName));
         } else if (code == 10) {
            if (data.getClipData() != null) {
               for(int i = 0; i < data.getClipData().getItemCount(); ++i) {
                  this.addPickedUri(data.getClipData().getItemAt(i).getUri());
               }
            } else if (data.getData() != null) {
               this.addPickedUri(data.getData());
            }

            // addPickedUri() opens the selected item and renders the Player page itself.
         } else if (code == 11 && data.getData() != null) {
            Uri tree = data.getData();

            try {
               this.getContentResolver().takePersistableUriPermission(tree, 1);
            } catch (Exception var6) {
            }

            this.worker.execute(() -> {
               this.scanTree(tree);
            });
         } else if (code == 12 && data.getData() != null) {
            this.addSubtitle(data.getData());
         }

      }
   }

   private void addPickedUri(Uri u) {
      try {
         this.getContentResolver().takePersistableUriPermission(u, 1);
      } catch (Exception var7) {
      }

      String name = u.getLastPathSegment();
      Cursor c = this.getContentResolver().query(u, (String[])null, (String)null, (String[])null, (String)null);

      try {
         if (c != null && c.moveToFirst()) {
            int i = c.getColumnIndex("_display_name");
            if (i >= 0) {
               name = c.getString(i);
            }
         }
      } catch (Throwable var8) {
         if (c != null) {
            try {
               c.close();
            } catch (Throwable var6) {
               var8.addSuppressed(var6);
            }
         }

         throw var8;
      }

      if (c != null) {
         c.close();
      }

      boolean vid = this.isVideoName(name == null ? "" : name);
      MediaItem m = new MediaItem(name == null ? "Media file" : name, u.toString(), vid, "", 0L, 0L, "", "");
      if (!this.queueHas(m.uri)) {
         this.queue.add(m);
      }

      this.saveData();
      this.play(m);
   }

   private void scanTree(Uri tree) {
      try {
         this.scanDocument(tree, DocumentsContract.getTreeDocumentId(tree));
         this.handler.post(() -> {
            this.render();
            this.toast("Folder scan complete");
         });
      } catch (Exception var3) {
         this.handler.post(() -> {
            this.toast("Could not scan folder: " + var3.getMessage());
         });
      }

   }

   private void scanDocument(Uri tree, String id) {
      Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
      String[] cols = new String[]{"document_id", "_display_name", "mime_type"};

      try {
         Cursor c = this.getContentResolver().query(children, cols, (String)null, (String[])null, (String)null);

         label62: {
            try {
               if (c != null) {
                  while(true) {
                     if (!c.moveToNext()) {
                        break label62;
                     }

                     String child = c.getString(0);
                     String name = c.getString(1);
                     String mime = c.getString(2);
                     if ("vnd.android.document/directory".equals(mime)) {
                        this.scanDocument(tree, child);
                     } else if (this.isMediaName(name)) {
                        Uri u = DocumentsContract.buildDocumentUriUsingTree(tree, child);
                        if (!this.queueHas(u.toString())) {
                           MediaItem m = new MediaItem(name, u.toString(), this.isVideoName(name), tree.getLastPathSegment(), 0L, 0L, "", "");
                           this.library.add(m);
                        }
                     }
                  }
               }
            } catch (Throwable var12) {
               if (c != null) {
                  try {
                     c.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (c != null) {
               c.close();
            }

            return;
         }

         if (c != null) {
            c.close();
         }
      } catch (Exception var13) {
         Log.w("SoloPlayer", "Folder scan failed", var13);
      }

   }

   private boolean isMediaName(String n) {
      return n != null && n.toLowerCase(Locale.ROOT).matches(".*\\.(mkv|mp4|m4v|mov|avi|wmv|webm|mpg|mpeg|ts|m2ts|mp3|flac|wav|m4a|aac|ogg|opus|wma|aiff|3gp|ac3|mka|ape|alac)$");
   }

   private boolean isVideoName(String n) {
      return n.toLowerCase(Locale.ROOT).matches(".*\\.(mkv|mp4|m4v|mov|avi|wmv|webm|mpg|mpeg|ts|m2ts|3gp)$");
   }

   private boolean matchesSearch(MediaItem m) {
      return this.searchQuery.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(this.searchQuery) || m.artist != null && m.artist.toLowerCase(Locale.ROOT).contains(this.searchQuery) || m.album != null && m.album.toLowerCase(Locale.ROOT).contains(this.searchQuery);
   }

   private void createPlaylistDialog() {
      EditText e = this.edit("Playlist name");
      (new AlertDialog.Builder(this)).setTitle("Create playlist").setView(e).setNegativeButton("Cancel", (DialogInterface.OnClickListener)null).setPositiveButton("Create", (d, w) -> {
         String n = e.getText().toString().trim();
         if (!n.isEmpty()) {
            this.playlists.put(n, new ArrayList());
            this.invalidatePageCachePrefix("Playlists");
            this.saveData();
            this.render();
         }

      }).show();
   }

   private boolean queueHas(String uri) {
      Iterator var2 = this.queue.iterator();

      MediaItem m;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         m = (MediaItem)var2.next();
      } while(!m.uri.equals(uri));

      return true;
   }

   private void requestStreamDialog() {
      this.page = "Network Stream";
      this.render();
   }

   private void toast(String s) {
      Toast.makeText(this, s, 1).show();
   }

   private void handleBack() {
      if (this.exitPromptPending) return;
      if (this.fullscreen && this.fullscreenDrawer != null && this.fullscreenDrawer.getVisibility() == 0) {
         this.toggleFullscreenDrawer();
      } else if (!this.fullscreen && !this.page.equals("Player")) {
         if (this.page.equals("Settings")) {
            this.page = this.previousPage.equals("Settings") ? "Home" : this.previousPage;
            this.render();
         } else if (this.page.equals("Folder")) {
            this.page = "Folders";
            this.render();
         } else if (this.page.equals("Folders")) {
            this.page = "More";
            this.render();
         } else if (this.page.equals("Playlist")) {
            this.page = "Playlists";
            this.render();
         } else if (this.page.equals("Channels")) {
            this.page = "Network Stream";
            this.render();
         } else if (this.page.equals("Network Stream")) {
            this.stopPlaybackSession();
            this.page = "Home";
            this.render();
         } else if (!this.page.equals("Home")) {
            this.page = "Home";
            this.render();
         } else {
            if (this.isBackgroundAudioSession()) this.syncAudioService(); else this.stopPlaybackSession();
            this.finish();
         }
      } else {
         this.closePlayerToHome();
      }
   }

   public void onBackPressed() {
      if (VERSION.SDK_INT < 33) {
         this.handleBack();
      } else {
         super.onBackPressed();
      }

   }

   private void exitFullscreenUi() {
      if (this.fullscreen) {
         this.fullscreen = false;
         this.handler.removeCallbacks(this.hideFullscreenControls);
         this.handler.removeCallbacks(this.hideGestureHud);
         this.getWindow().clearFlags(128);
         this.getWindow().getDecorView().setSystemUiVisibility(0);
         this.setRequestedOrientation(-1);
         if (this.player != null) {
            try {
               this.player.detachViews();
               this.videoSurfaceAttached = false;
            } catch (Exception var2) {
            }
         }

         if (this.videoSurface != null && this.videoSurface.getParent() instanceof ViewGroup) {
            ((ViewGroup)this.videoSurface.getParent()).removeView(this.videoSurface);
         }

         this.videoSurface = null;
         this.fullscreenContainer = null;
         this.fullscreenDrawer = null;
         this.fullscreenQueue = null;
         this.fullscreenOverlay = null;
         this.fullscreenTopOverlay = null;
         this.fullscreenChapters = null;
         this.fullscreenTools = null;
         this.fullscreenSeek = null;
         this.fullscreenTime = null;
         this.fullscreenTitle = null;
         this.fullscreenArtwork = null;
         this.fullscreenArtworkEffect = null;
         this.fullscreenAudioTools = null;
         this.fullscreenVideoTools = null;
         this.fullscreenSubtitleTools = null;
         this.fullscreenPlaybackTools = null;
         this.fullscreenSettingCategoryButtons = null;
         this.screenLockOverlay = null;
         this.screenUnlockButton = null;
         this.screenTouchesLocked = false;
         this.brightnessHud = null;
         this.volumeHud = null;
         if (this.root != null) {
            this.setContentView(this.root);
         }

      }
   }

   private void closePlayerToHome() {
      this.handler.removeCallbacks(this.hidePortraitControls);
      this.saveCurrentResumePosition();
      if (this.isBackgroundAudioSession()) {
         this.exitFullscreenUi();
         this.page = "Home";
         this.render();
         this.updateMini();
         this.syncAudioService();
         return;
      }
      if (this.exitPromptPending) return;
      boolean isBookmarked = this.current != null && this.current.network && this.streamBookmarks.stream().anyMatch(stream -> stream.uri.equalsIgnoreCase(this.current.uri));
      if (this.current != null && this.current.network && !isBookmarked) {
         this.exitPromptPending = true;
         new AlertDialog.Builder(this)
            .setTitle("Bookmark this stream?")
            .setMessage("Save " + this.current.name + " for quick access later?")
            .setPositiveButton("Bookmark", (dialog, which) -> this.finishPlayerExit(true))
            .setNegativeButton("Skip", (dialog, which) -> this.finishPlayerExit(false))
            .setCancelable(false)
            .show();
      } else {
         this.finishPlayerExit(false);
      }
   }

   private void closePlayerToHomeImmediately() {
      this.finishPlayerExit(false);
   }

   private void finishPlayerExit(boolean bookmark) {
      if (bookmark && this.current != null && this.current.network) {
         boolean alreadySaved = this.streamBookmarks.stream().anyMatch(stream -> stream.uri.equalsIgnoreCase(this.current.uri));
         if (!alreadySaved) this.saveStreamBookmark(this.current, null);
         this.recentStreams.removeIf(stream -> stream.uri.equals(this.current.uri));
         this.recentStreams.add(0, this.current);
         while (this.recentStreams.size() > 20) this.recentStreams.remove(this.recentStreams.size() - 1);
         this.saveData();
      }
      this.exitPromptPending = false;
      this.exitFullscreenUi();
      this.stopPlaybackSession();
      this.page = "Home";
      this.render();
   }

   private void stopPlaybackSession() {
      this.saveCurrentResumePosition();
      this.resumeLocalAfterCast = false;
      if (this.castContext != null && this.isCasting()) {
         try { this.castContext.getSessionManager().endCurrentSession(true); } catch (Exception ignored) {}
      }
      this.stopLocalCastServer();
      AudioPlaybackService.stop(this.getApplicationContext());
      playbackRequested = false;
      this.handler.removeCallbacks(this.hideFullscreenControls);
      this.handler.removeCallbacks(this.hideGestureHud);
      boolean wasNetwork = this.current != null && this.current.network;
      if (this.player != null) {
         try {
            if (this.player.hasMedia()) {
               this.player.stop();
            }
         } catch (Exception var5) {
         }

         try {
            this.player.setMedia((IMedia)null);
         } catch (Exception var4) {
         }
      }

      if (this.activeMediaFd != null) {
         try {
            this.activeMediaFd.close();
         } catch (Exception var3) {
         }

         this.activeMediaFd = null;
      }

      if (wasNetwork) {
         this.channels.clear();
      }

      this.queue.clear();
      if (this.current != null && this.current.privateVault) this.deleteActiveVaultTemp();
      current = null;
      this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
      playbackRequested = false;
      playbackStartPending = false;
      this.resumeAfterFocusLoss = false;
      this.pausedForInterruption = false;
      this.abandonPlaybackFocus();
      if (this.miniPlayer != null) {
         this.miniPlayer.setVisibility(8);
      }

      if (this.miniTitle != null) {
         this.miniTitle.setText("Nothing playing");
      }

      if (this.miniPlay != null) {
         this.setPlaybackButtonIcon(this.miniPlay, false);
      }

   }

   protected void onStart() {
      super.onStart();
      this.activityStarted = true;
      activeActivity = this;
      if (this.castContext != null) {
         try {
            SessionManager manager = this.castContext.getSessionManager();
            manager.addSessionManagerListener(this.castSessionListener, CastSession.class);
            this.activeCastSession = manager.getCurrentCastSession();
         } catch (Exception error) { Log.w("SoloPlayer", "Cast session listener unavailable", error); }
      }
      this.updateMini();
      this.syncPlaybackUi();
      this.scheduleVideoSurfaceRecovery(100L);
   }

   protected void onStop() {
      super.onStop();
      this.stopCastRouteScan();
      if (this.castPickerDialog != null && this.castPickerDialog.isShowing()) this.castPickerDialog.dismiss();
      this.saveCurrentResumePosition();
      boolean inPip = VERSION.SDK_INT >= 26 && this.isInPictureInPictureMode();
      boolean preserveVideoForReturn = inPip || this.isCasting() || (!this.isChangingConfigurations() && this.page.equals("Player") && this.currentIsVideo() && this.player != null && this.player.hasMedia());
      if (preserveVideoForReturn) {
         if (!inPip && !this.isCasting() && this.player != null) {
            this.pendingSubtitleSeek = this.pickerPausedVideo ? this.pickerVideoPosition : Math.max(0L, this.player.getTime());
            this.pendingSubtitleResume = this.pickerPausedVideo ? this.pickerVideoWasPlaying : this.isPlaybackActive();
            this.deferPlaybackUntilVideoSurface = true;
            playbackRequested = false;
            this.player.pause();
            this.videoSurfaceAttached = false;
            this.videoRebindPending = true;
         }
      }
      this.activityStarted = false;
      if (activeActivity == this) activeActivity = null;
      if (this.castContext != null) {
         try { this.castContext.getSessionManager().removeSessionManagerListener(this.castSessionListener, CastSession.class); } catch (Exception ignored) {}
      }
      if (this.isBackgroundAudioSession()) {
         this.syncAudioService();
      } else if (!preserveVideoForReturn && !this.pickerOpen && !this.isChangingConfigurations() && this.player != null && this.player.hasMedia()) {
         this.closePlayerToHomeImmediately();
      }

   }

   private boolean requestPlaybackFocus() {
      if (this.audioManager == null) {
         this.audioManager = (AudioManager)this.getSystemService("audio");
      }

      if (this.audioManager == null) {
         return true;
      } else {
         int result;
         if (VERSION.SDK_INT >= 26) {
            if (this.audioFocusRequest == null) {
               AudioAttributes attrs = (new AudioAttributes.Builder()).setUsage(1).setContentType(3).build();
               this.audioFocusRequest = (new AudioFocusRequest.Builder(1)).setAudioAttributes(attrs).setOnAudioFocusChangeListener(this.focusListener, this.handler).setWillPauseWhenDucked(true).build();
            }

            result = this.audioManager.requestAudioFocus(this.audioFocusRequest);
         } else {
            result = this.audioManager.requestAudioFocus(this.focusListener, 3, 1);
         }

         return result == 1;
      }
   }

   private void abandonPlaybackFocus() {
      if (this.audioManager != null) {
         if (VERSION.SDK_INT >= 26 && this.audioFocusRequest != null) {
            this.audioManager.abandonAudioFocusRequest(this.audioFocusRequest);
         } else {
            this.audioManager.abandonAudioFocus(this.focusListener);
         }
      }

      this.resumeAfterFocusLoss = false;
   }

   private void handleAudioFocus(int focus) {
      if (this.player != null) {
         if (focus != -2 && focus != -3) {
            if (focus == -1) {
               this.pausedForInterruption = false;
               this.resumeAfterFocusLoss = false;
               if (this.player.isPlaying()) {
                  this.player.pause();
               }
            } else if (focus == 1) {
               this.pausedForInterruption = false;
               if (this.resumeAfterFocusLoss && this.player.hasMedia() && this.activityStarted) {
                  this.player.play();
               }

               this.resumeAfterFocusLoss = false;
            }
         } else {
            this.pausedForInterruption = true;
            this.resumeAfterFocusLoss = this.player.isPlaying();
            if (this.resumeAfterFocusLoss) {
               this.player.pause();
            }
         }

         this.updateMini();
      }
   }

   private void styleThemedDialog(AlertDialog dialog) {
      if (dialog == null || dialog.getWindow() == null) return;
      GradientDrawable surface = new GradientDrawable();
      surface.setColor(Color.argb(248, 20, 16, 31));
      surface.setCornerRadius(this.dp(18));
      surface.setStroke(this.dp(1), Color.argb(190, 111, 42, 190));
      dialog.getWindow().setBackgroundDrawable(surface);
      int[] ids = new int[]{-1, -2, -3};
      for (int id : ids) {
         TextView button = dialog.getButton(id);
         if (button != null) button.setTextColor(LILAC);
      }
   }

   private void aspectRatioDialog() {
      if (this.player != null && this.player.hasMedia()) {
         String[] labels = new String[]{"Fit screen", "Center", "16:9", "Fill", "Best fit", "4:3", "16:10", "1:1", "2.35:1", "Original", "Crop", "Maximize"};
         AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Aspect ratio")
            .setSingleChoiceItems(labels, Math.max(0, Arrays.asList(labels).indexOf(this.aspectMode)), (d, w) -> {
               this.setAspectMode(labels[w], false);
               d.dismiss();
            }).setNegativeButton("Close", null).create();
         dialog.setOnShowListener(d -> this.styleThemedDialog(dialog));
         dialog.show();
      } else {
         this.toast("Start a video first");
      }
   }

   private void bindAspectRatioControl(TextView button) {
      button.setOnClickListener(v -> this.cycleAspectMode());
      button.setOnLongClickListener(v -> { this.aspectRatioDialog(); return true; });
      button.setContentDescription("Aspect ratio: " + this.aspectMode + ". Tap to cycle; long press for options");
   }

   private void cycleAspectMode() {
      String[] cycle = new String[]{"Fit screen", "Center", "16:9", "Fill", "Best fit"};
      int index = Arrays.asList(cycle).indexOf(this.aspectMode);
      if (index < 0) index = -1;
      this.setAspectMode(cycle[(index + 1) % cycle.length], true);
   }

   private void setAspectMode(String mode, boolean notify) {
      this.aspectMode = mode;
      this.prefs().edit().putString("aspect_mode", mode).apply();
      if (this.landscapeRatioButton != null) this.landscapeRatioButton.setText("▣ " + mode);
      if (this.portraitRatioButton != null) this.portraitRatioButton.setText("▭  " + mode);
      if (this.landscapeRatioButton != null) this.landscapeRatioButton.setContentDescription("Aspect ratio: " + mode + ". Tap to cycle; long press for options");
      if (this.portraitRatioButton != null) this.portraitRatioButton.setContentDescription("Aspect ratio: " + mode + ". Tap to cycle; long press for options");
      this.applyAspectRatio();
      if (notify) this.showAspectRatioFeedback();
      if (this.fullscreen) this.showFullscreenControls();
   }

   private void showAspectRatioFeedback() {
      FrameLayout host = this.fullscreen ? this.fullscreenContainer : this.videoFrame;
      if (host == null) return;
      this.clearSeekFeedback();
      LinearLayout card = this.col();
      card.setGravity(Gravity.CENTER);
      card.setPadding(this.dp(17), this.dp(9), this.dp(17), this.dp(9));
      GradientDrawable background = this.shape(Color.argb(205, 14, 8, 24), 16);
      background.setStroke(this.dp(1), Color.argb(210, 157, 65, 255));
      card.setBackground(background);
      TextView value = this.text(this.aspectMode, 16, Color.WHITE, true);
      value.setGravity(Gravity.CENTER);
      TextView hint = this.text("Long press for more options", 10, LILAC, false);
      hint.setGravity(Gravity.CENTER);
      card.addView(value);
      card.addView(hint);
      this.seekFeedback = card;
      host.addView(card, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
      card.setAlpha(0.0F);
      card.animate().alpha(1.0F).setDuration(110L).start();
      this.hideSeekFeedback = () -> card.animate().alpha(0.0F).setDuration(170L).withEndAction(() -> {
         if (card.getParent() instanceof ViewGroup) ((ViewGroup)card.getParent()).removeView(card);
         if (this.seekFeedback == card) this.seekFeedback = null;
      }).start();
      this.handler.postDelayed(this.hideSeekFeedback, 1000L);
   }

   private void applyAspectRatio() {
      if (this.player != null && this.player.hasMedia()) {
         this.player.setAspectRatio((String)null);
         this.player.setScale(0.0F);
         if (this.aspectMode.equals("Fit screen")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FIT_SCREEN);
         } else if (this.aspectMode.equals("Center") || this.aspectMode.equals("Original")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_ORIGINAL);
         } else if (this.aspectMode.equals("Best fit") || this.aspectMode.equals("Fit")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
         } else if (this.aspectMode.equals("Fill") || this.aspectMode.equals("Maximize")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
         } else if (this.aspectMode.equals("Crop")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL);
            this.player.setScale(1.25F);
         } else if (this.aspectMode.equals("16:9")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_16_9);
         } else if (this.aspectMode.equals("4:3")) {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_4_3);
         } else {
            this.player.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
            this.player.setAspectRatio(this.aspectMode);
         }

      }
   }

   private void toggleFullscreen() {
      if (this.current == null) return;
      if (this.fullscreen) {
         this.closePlayerToHome();
         return;
      }
      if(this.currentIsVideo()&&!this.isPlaybackActive()&&!this.playbackStartPending&&this.pausedFrameBitmap==null&&!this.pausedFrameTransitionInProgress){this.capturePausedFrameThen(()->this.toggleFullscreen());return;}
         if (!this.fullscreen) {
            this.handler.removeCallbacks(this.hidePortraitControls);
            if (this.videoSurface == null || this.player == null || !this.player.hasMedia()) {
               this.toast("Player is not ready for fullscreen");
               return;
            }

            this.fullscreen = true;
            this.getWindow().addFlags(128);
            this.enterCutoutImmersiveMode();
            if (this.videoSurface == null) {
               return;
            }

            VLCVideoLayout surface = this.videoSurface;

            try {
               this.player.detachViews();
               this.videoSurfaceAttached = false;
            } catch (Exception error) {
               Log.w("SoloPlayer", "Could not detach video output before moving it to fullscreen", error);
            }

            ViewParent parent = surface.getParent();
            if (parent instanceof ViewGroup) {
               ((ViewGroup)parent).removeView(surface);
            }

            this.buildLandscapePlayerUi(surface);
            if (this.currentIsVideo()) this.attachVideoSurface(surface, true, 0);
            this.showFullscreenControls();
            if (this.currentIsVideo()) this.applyOrientationMode();
         }

   }

   private void enterCutoutImmersiveMode() {
      WindowManager.LayoutParams attrs = this.getWindow().getAttributes();
      if (VERSION.SDK_INT >= 30) {
         attrs.layoutInDisplayCutoutMode = 3; // LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
      } else if (VERSION.SDK_INT >= 28) {
         attrs.layoutInDisplayCutoutMode = 1; // SHORT_EDGES
      }
      this.getWindow().setAttributes(attrs);
      this.getWindow().getDecorView().setSystemUiVisibility(5894);
   }

   private void exitCutoutImmersiveMode() {
      this.getWindow().getDecorView().setSystemUiVisibility(0);
      if (VERSION.SDK_INT >= 28) {
         WindowManager.LayoutParams attrs = this.getWindow().getAttributes();
         attrs.layoutInDisplayCutoutMode = 0; // DEFAULT
         this.getWindow().setAttributes(attrs);
      }
   }

   private void applyFullscreenSafeInsets(android.view.WindowInsets insets) {
      int left = this.dp(6), top = this.dp(5), right = this.dp(6), bottom = this.dp(5);
      if (VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
         android.view.DisplayCutout cutout = insets.getDisplayCutout();
         left = Math.max(left, cutout.getSafeInsetLeft());
         top = Math.max(top, cutout.getSafeInsetTop());
         right = Math.max(right, cutout.getSafeInsetRight());
         bottom = Math.max(bottom, cutout.getSafeInsetBottom());
      }
      if (this.fullscreenTopOverlay != null && this.fullscreenTopOverlay.getLayoutParams() instanceof FrameLayout.LayoutParams) {
         FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)this.fullscreenTopOverlay.getLayoutParams();
         lp.setMargins(left, top, right, 0);
         this.fullscreenTopOverlay.setLayoutParams(lp);
      }
      if (this.fullscreenOverlay != null && this.fullscreenOverlay.getLayoutParams() instanceof FrameLayout.LayoutParams) {
         FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)this.fullscreenOverlay.getLayoutParams();
         lp.setMargins(left, 0, right, bottom);
         this.fullscreenOverlay.setLayoutParams(lp);
      }
      if (this.brightnessHud != null && this.brightnessHud.getLayoutParams() instanceof FrameLayout.LayoutParams) {
         FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)this.brightnessHud.getLayoutParams();
         lp.leftMargin = left + this.dp(10);
         this.brightnessHud.setLayoutParams(lp);
      }
      if (this.volumeHud != null && this.volumeHud.getLayoutParams() instanceof FrameLayout.LayoutParams) {
         FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)this.volumeHud.getLayoutParams();
         lp.rightMargin = right + this.dp(10);
         this.volumeHud.setLayoutParams(lp);
      }
   }

   private boolean currentIsVideo() {
      return this.current != null && this.current.video;
   }

   private void attachVideoSurface(VLCVideoLayout surface, boolean landscape, int attempt) {
      if (surface != null) {
         surface.post(() -> {
            if (this.player != null && this.player.hasMedia() && surface == this.videoSurface && landscape == this.fullscreen) {
               if (surface.getWidth() > 0 && surface.getHeight() > 0) {
                  try {
                     this.player.attachViews(surface, (DisplayManager)null, true, false);
                     this.videoSurfaceAttached = true;
                     this.applyAspectRatio();
                     if(this.pendingAutoFullscreen&&this.currentIsVideo()&&!this.fullscreen){this.pendingAutoFullscreen=false;this.toggleFullscreen();return;}
                     if (this.deferPlaybackUntilVideoSurface && this.currentIsVideo()) {
                        this.deferPlaybackUntilVideoSurface = false;
                        if (this.pendingSubtitleSeek >= 0L) this.player.setTime(this.pendingSubtitleSeek);
                        this.pendingSubtitleSeek = -1L;
                        this.waitingSubtitleRestore = false;
                        if (this.pendingSubtitleResume && this.requestPlaybackFocus()) {
                           playbackRequested = true;
                           this.player.play();
                           if(this.isCasting()&&this.activeCastSession!=null)this.startCastPlayback(this.activeCastSession);
                        } else {
                           playbackRequested = false;
                        }
                        this.syncPlaybackUi();
                     }
                  } catch (Exception var5) {
                     Log.w("SoloPlayer", "Could not attach video surface", var5);
                     if (attempt < 4) {
                        this.handler.postDelayed(() -> {
                           this.attachVideoSurface(surface, landscape, attempt + 1);
                        }, 120L);
                     }
                  }

               } else {
                  if (attempt < 8) {
                     this.handler.postDelayed(() -> {
                        this.attachVideoSurface(surface, landscape, attempt + 1);
                     }, 80L);
                  }

               }
            }
         });
      }
   }

   private void buildLandscapePlayerUi(VLCVideoLayout surface) {
      this.fullscreenContainer = new FrameLayout(this);
      this.fullscreenContainer.setBackgroundColor(-16777216);
      this.setContentView(this.fullscreenContainer);
      this.videoSurface = surface;
      this.fullscreenContainer.addView(this.videoSurface, new FrameLayout.LayoutParams(-1, -1));
      if (!this.current.video) {
         FrameLayout audioStage = this.createAudioArtworkStage(this.current, true);
         this.fullscreenContainer.addView(audioStage, new FrameLayout.LayoutParams(this.dp(270), this.dp(280), Gravity.CENTER));
      }
      View touchLayer = new View(this);
      touchLayer.setBackgroundColor(0);
      touchLayer.setOnTouchListener((v, e) -> {
         return this.handleFullscreenTouch(e);
      });
      this.fullscreenContainer.addView(touchLayer, new FrameLayout.LayoutParams(-1, -1));
      this.addGestureHuds(this.fullscreenContainer);
      if (this.current.network && playbackStartPending) this.addStreamLoadingOverlay(this.fullscreenContainer);
      boolean portraitHeader = this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
      this.fullscreenTopOverlay = portraitHeader ? this.col() : this.row();
      this.fullscreenTopOverlay.setPadding(this.dp(12.0F), this.dp(5.0F), this.dp(12.0F), this.dp(5.0F));
      this.fullscreenTopOverlay.setBackground(this.shape(Color.argb(112, 12, 12, 12), 12));
      this.fullscreenTitle = this.text(this.current.name, 12, -1, true);
      this.fullscreenTitle.setSingleLine(!portraitHeader);
      this.fullscreenTitle.setGravity(Gravity.CENTER_VERTICAL);
      this.fullscreenTitle.setPadding(this.dp(6.0F), 0, this.dp(4.0F), 0);
      LinearLayout titleRow = portraitHeader ? this.row() : this.fullscreenTopOverlay;
      TextView playerBack = this.compactButton("‹", false);
      titleRow.addView(playerBack);
      playerBack.setOnClickListener((v) -> this.toggleFullscreen());
      titleRow.addView(this.fullscreenTitle, new LinearLayout.LayoutParams(0, -2, 1.0F));
      LinearLayout actionBar = portraitHeader ? this.row() : this.fullscreenTopOverlay;
      if (portraitHeader) this.fullscreenTopOverlay.addView(titleRow, this.lp(-1, -2));
      TextView audio = this.compactButton("♫", false);
      TextView subtitle = this.compactButton("CC", false);
      TextView ratio = this.compactButton("▣ " + this.aspectMode, false); this.landscapeRatioButton=ratio;
      this.bindAspectRatioControl(ratio);
      TextView tools = this.compactButton("⋯", false);
      TextView playlist = this.compactButton("☷", false);
      TextView search = this.compactButton("⌕", false);
      TextView bookmarkStream = this.compactButton("☆", false); bookmarkStream.setContentDescription("Bookmark stream");
      TextView pip = this.compactButton("▣", false);
      pip.setContentDescription("Open floating video window");
      TextView exit = this.compactButton("×", false);
      exit.setContentDescription("Close player");
      search.setVisibility(this.current.network ? 0 : 8);
      bookmarkStream.setVisibility(this.current.network ? 0 : 8);
      subtitle.setVisibility(this.current.video ? 0 : 8);
      ratio.setVisibility(this.current.video ? 0 : 8);
      TextView[] var12 = new TextView[]{audio, subtitle, ratio, tools, playlist, search, bookmarkStream, pip, exit};
      int var13 = var12.length;

      for(int var14 = 0; var14 < var13; ++var14) {
         TextView option = var12[var14];
         LinearLayout.LayoutParams optionLp = this.lp(-2, -2);
         optionLp.setMargins(this.dp(2.0F), 0, this.dp(2.0F), 0);
         actionBar.addView(option, optionLp);
      }

      audio.setOnClickListener((v) -> {
         this.showFullscreenControls();
         this.showTracks(false);
      });
      subtitle.setOnClickListener((v) -> {
         this.showFullscreenControls();
         this.showTracks(true);
      });
      tools.setOnClickListener((v) -> {
         this.showFullscreenTools();
      });
      playlist.setOnClickListener((v) -> {
         this.showFullscreenPlaylist();
      });
      search.setOnClickListener((v) -> {
         this.showFullscreenPlaylist();
         if (this.fullscreenChannelSearch != null) {
            this.fullscreenChannelSearch.requestFocus();
         }

      });
      bookmarkStream.setOnClickListener(v -> { this.editOrAddCurrentStreamBookmark(); this.showFullscreenControls(); });
      exit.setOnClickListener((v) -> {
         this.toggleFullscreen();
      });
      pip.setOnClickListener(v -> this.enterPlayerPip());
      MediaRouteButton fullscreenCast = this.createCastButton();
      LinearLayout.LayoutParams castLp = this.lp(this.dp(42), this.dp(42));
      castLp.setMargins(this.dp(2), 0, this.dp(2), 0);
      actionBar.addView(fullscreenCast, castLp);
      if (portraitHeader) {
         HorizontalScrollView actionScroller = new HorizontalScrollView(this);
         actionScroller.setHorizontalScrollBarEnabled(false);
         actionScroller.setFillViewport(false);
         actionScroller.addView(actionBar, new HorizontalScrollView.LayoutParams(-2, -2));
         this.fullscreenTopOverlay.addView(actionScroller, this.lp(-1, -2));
      }
      FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(-1, -2, 48);
      topLp.setMargins(this.dp(6.0F), this.dp(5.0F), this.dp(6.0F), 0);
      this.fullscreenContainer.addView(this.fullscreenTopOverlay, topLp);
      this.applyTranslucentControlBackgrounds(this.fullscreenTopOverlay, 170);
      this.fullscreenOverlay = this.col();
      this.fullscreenOverlay.setPadding(this.dp(12.0F), this.dp(5.0F), this.dp(12.0F), this.dp(7.0F));
      this.fullscreenOverlay.setBackground(this.shape(Color.argb(120, 10, 10, 10), 12));
      FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(-1, -2, 80);
      panelLp.setMargins(this.dp(6.0F), 0, this.dp(6.0F), this.dp(5.0F));
      this.fullscreenContainer.addView(this.fullscreenOverlay, panelLp);
      this.fullscreenTime = this.text(this.clock(0L) + "  /  " + this.clock(this.player.getLength()), 10, -1, false);
      this.fullscreenTime.setGravity(17);
      this.fullscreenOverlay.addView(this.fullscreenTime);
      this.fullscreenSeek = new SeekBar(this);
      this.fullscreenSeek.setMax(1000);
      this.fullscreenSeek.setProgressTintList(ColorStateList.valueOf(PURPLE));
      this.fullscreenSeek.setThumbTintList(ColorStateList.valueOf(LILAC));
      this.fullscreenOverlay.addView(this.fullscreenSeek, this.lp(-1, this.dp(24.0F)));
      this.fullscreenSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){seeking=true;showFullscreenControls();}public void onStopTrackingTouch(SeekBar s){long length=activePlaybackLength();if(length>0){if(isCasting())seekBy(length*s.getProgress()/1000L-activePlaybackTime());else player.setTime(length*s.getProgress()/1000L);}seeking=false;showFullscreenControls();}public void onProgressChanged(SeekBar s,int p,boolean u){}});
      LinearLayout controls = this.row();
      controls.setGravity(17);
      TextView back10 = this.compactButton("↶ 10", false);
      TextView previous = this.compactButton("|◀", false);
      TextView play = this.compactButton(this.isPlaybackActive() ? "❚❚" : "▶", true);
      this.fullscreenPlayButton = play;
      this.setPlaybackButtonIcon(play, this.isPlaybackActive());
      TextView next = this.compactButton("▶|", false);
      TextView forward10 = this.compactButton("10 ↷", false);
      TextView speed = this.compactButton("1.0×", false);
      TextView sleep = this.compactButton("◷", false);
      TextView orientationButton = this.compactButton(this.orientationModeGlyph(), !this.orientationMode.equals("Adaptive"));
      this.fullscreenOrientationButton = orientationButton;
      orientationButton.setVisibility(this.current.video ? View.VISIBLE : View.GONE);
      orientationButton.setContentDescription("Orientation mode: " + this.orientationMode);
      TextView[] var24 = new TextView[]{orientationButton, back10, previous, play, next, forward10, speed, sleep};
      int var25 = var24.length;

      for(int var26 = 0; var26 < var25; ++var26) {
         TextView b = var24[var26];
         LinearLayout.LayoutParams bp = this.lp(-2, -2);
         bp.setMargins(this.dp(2.0F), this.dp(2.0F), this.dp(2.0F), this.dp(2.0F));
         controls.addView(b, bp);
      }

      HorizontalScrollView controlsScroll = new HorizontalScrollView(this);
      controlsScroll.setHorizontalScrollBarEnabled(false);
      controlsScroll.setFillViewport(true);
      controlsScroll.addView(controls, new ViewGroup.LayoutParams(-2, -2));
      this.fullscreenOverlay.addView(controlsScroll, this.lp(-1, -2));
      orientationButton.setOnClickListener((v) -> this.openOrientationModeMenu(orientationButton));
      play.setOnClickListener((v) -> {
         this.togglePlayback();
         this.showFullscreenControls();
      });
      back10.setOnClickListener((v) -> {
         this.seekBy(-10000L);
         this.showFullscreenControls();
      });
      forward10.setOnClickListener((v) -> {
         this.seekBy(10000L);
         this.showFullscreenControls();
      });
      previous.setOnClickListener((v) -> {
         this.playAdjacent(-1);
         this.showFullscreenControls();
      });
      next.setOnClickListener((v) -> {
         this.playAdjacent(1);
         this.showFullscreenControls();
      });
      speed.setOnClickListener((v) -> {
         this.speedDialog();
         this.showFullscreenControls();
      });
      sleep.setOnClickListener((v) -> {
         this.showSleepTimerDialog();
         this.showFullscreenControls();
      });
      this.fullscreenOverlay.setOnTouchListener((v, e) -> {
         this.showFullscreenControls();
         return false;
      });
      this.applyTranslucentControlBackgrounds(this.fullscreenOverlay, 170);
      this.fullscreenTopOverlay.setVisibility(8);
      this.fullscreenOverlay.setVisibility(8);
      this.buildFullscreenDrawer();
      this.fullscreenContainer.setOnApplyWindowInsetsListener((view, insets) -> {
         this.applyFullscreenSafeInsets(insets);
         return insets;
      });
      this.fullscreenContainer.requestApplyInsets();
      this.screenLockOverlay = null;
      this.ensureScreenLockOverlay();
      if (this.screenTouchesLocked && this.screenLockOverlay != null) this.screenLockOverlay.setVisibility(View.VISIBLE);
   }

   private void addGestureHuds(FrameLayout host) {
      if (this.brightnessHud != null && this.brightnessHud.getParent() instanceof ViewGroup) ((ViewGroup)this.brightnessHud.getParent()).removeView(this.brightnessHud);
      if (this.volumeHud != null && this.volumeHud.getParent() instanceof ViewGroup) ((ViewGroup)this.volumeHud.getParent()).removeView(this.volumeHud);
      this.volumeHud = this.gestureHud("♫", false);
      FrameLayout.LayoutParams vlp = new FrameLayout.LayoutParams(this.dp(70.0F), this.dp(190.0F), Gravity.CENTER_VERTICAL | Gravity.LEFT);
      vlp.leftMargin = this.dp(12.0F);
      host.addView(this.volumeHud, vlp);
      this.volumeHud.setVisibility(View.GONE);
      this.brightnessHud = this.gestureHud("☼", true);
      FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(this.dp(70.0F), this.dp(190.0F), Gravity.CENTER_VERTICAL | Gravity.RIGHT);
      blp.rightMargin = this.dp(12.0F);
      host.addView(this.brightnessHud, blp);
      this.brightnessHud.setVisibility(View.GONE);
   }

   private void addStreamLoadingOverlay(FrameLayout host) {
      if (this.streamLoadingOverlay != null && this.streamLoadingOverlay.getParent() instanceof ViewGroup) ((ViewGroup)this.streamLoadingOverlay.getParent()).removeView(this.streamLoadingOverlay);
      LinearLayout card = this.col(); card.setGravity(Gravity.CENTER); card.setPadding(this.dp(18), this.dp(14), this.dp(18), this.dp(14)); card.setBackground(this.shape(Color.argb(210, 20, 12, 30), 18)); card.setClickable(false);
      ProgressBar spinner = new ProgressBar(this); spinner.setIndeterminate(true); spinner.setIndeterminateTintList(ColorStateList.valueOf(LILAC)); card.addView(spinner, this.lp(this.dp(44), this.dp(44)));
      this.streamLoadingText = this.text("Connecting to stream…", 13, Color.WHITE, true); this.streamLoadingText.setGravity(Gravity.CENTER); LinearLayout.LayoutParams textLp = this.lp(-2, -2); textLp.topMargin = this.dp(8); card.addView(this.streamLoadingText, textLp);
      FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER); host.addView(card, overlayLp); this.streamLoadingOverlay = card;
      card.setVisibility(playbackStartPending ? View.VISIBLE : View.GONE);
   }

   private void setStreamLoading(boolean visible, String message) {
      if (this.streamLoadingText != null && message != null) this.streamLoadingText.setText(message);
      if (this.streamLoadingOverlay != null) this.streamLoadingOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
   }

   private void startStreamConnectionTimeout() {
      this.handler.removeCallbacks(this.streamConnectionTimeout);
      this.handler.postDelayed(this.streamConnectionTimeout, 35000L);
   }

   private void showNetworkStreamFailure(String message) {
      this.handler.removeCallbacks(this.streamConnectionTimeout); this.setStreamLoading(false, null);
      playbackRequested = false; playbackStartPending = false; this.syncPlaybackUi();
      if (this.isFinishing() || this.isDestroyed()) return;
      new AlertDialog.Builder(this).setTitle("Stream could not start").setMessage(message)
         .setPositiveButton("Retry", (dialog, which) -> { if (this.current != null && this.current.network) this.play(this.current); })
         .setNegativeButton("Back to streams", (dialog, which) -> { this.stopPlaybackSession(); this.page = "Network Stream"; this.render(); })
         .setNeutralButton("Close", null).show();
   }

   private LinearLayout gestureHud(String icon, boolean brightness) {
      LinearLayout hud = this.col();
      hud.setGravity(17);
      hud.setPadding(this.dp(8.0F), this.dp(7.0F), this.dp(8.0F), this.dp(7.0F));
      hud.setBackground(this.shape(Color.argb(225, 20, 18, 28), 14));
      hud.setClipChildren(false);
      hud.setClipToPadding(false);
      TextView glyph = this.text(icon, 17, LILAC, true);
      glyph.setGravity(17);
      hud.addView(glyph, this.lp(this.dp(40.0F), this.dp(24.0F)));
      FrameLayout rail = new FrameLayout(this);
      rail.setClipChildren(false);
      rail.setClipToPadding(false);
      SeekBar slider = new SeekBar(this);
      slider.setMax(100);
      slider.setProgress(brightness ? this.getWindowBrightnessPercent() : this.getPlaybackVolumePercent());
      slider.setProgressTintList(ColorStateList.valueOf(PURPLE));
      slider.setThumbTintList(ColorStateList.valueOf(LILAC));
      slider.setRotation(-90.0F);
      slider.setEnabled(true);
      slider.setClickable(true);
      slider.setFocusable(true);
      rail.addView(slider, new FrameLayout.LayoutParams(this.dp(126.0F), this.dp(36.0F), 17));
      hud.addView(rail, this.lp(this.dp(46.0F), this.dp(132.0F)));
      TextView value = this.text((brightness ? this.getWindowBrightnessPercent() : this.getPlaybackVolumePercent()) + "%", 10, -1, true);
      value.setGravity(17);
      hud.addView(value, this.lp(this.dp(44.0F), this.dp(20.0F)));
      slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
         public void onStartTrackingTouch(SeekBar bar) {
            handler.removeCallbacks(hideGestureHud);
            showGestureHud(hud);
            if (brightness) volumeHud.setVisibility(View.GONE); else brightnessHud.setVisibility(View.GONE);
         }
         public void onStopTrackingTouch(SeekBar bar) {
            handler.removeCallbacks(hideGestureHud);
            handler.postDelayed(hideGestureHud, 1400L);
         }
         public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) return;
            if (brightness) {
               setWindowBrightnessPercent(progress);
               if (brightnessValue != null) brightnessValue.setText(progress + "%");
            } else {
               setPlaybackVolumePercent(progress);
               if (volumeValue != null) volumeValue.setText(getPlaybackVolumePercent() + "%");
            }
            handler.removeCallbacks(hideGestureHud);
         }
      });
      if (brightness) {
         this.brightnessSlider = slider;
         this.brightnessValue = value;
      } else {
         this.volumeSlider = slider;
         this.volumeValue = value;
      }

      return hud;
   }

   private int getWindowBrightnessPercent() {
      float value = this.getWindow().getAttributes().screenBrightness;
      return Math.round((value < 0.0F ? 0.5F : value) * 100.0F);
   }

   private void setWindowBrightnessPercent(int percent) {
      this.gestureBrightness = Math.max(0.01F, Math.min(1.0F, percent / 100.0F));
      WindowManager.LayoutParams lp = this.getWindow().getAttributes();
      lp.screenBrightness = this.gestureBrightness;
      this.getWindow().setAttributes(lp);
      if (this.brightnessSlider != null && this.brightnessSlider.getProgress() != percent) this.brightnessSlider.setProgress(percent);
   }

   private int getPlaybackVolumePercent() {
      if (this.audioManager == null) this.audioManager = (AudioManager)this.getSystemService("audio");
      if (this.audioManager == null) return 50;
      int max = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      return max <= 0 ? 50 : Math.round(100.0F * this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / max);
   }

   private void setPlaybackVolumePercent(int percent) {
      if (this.audioManager == null) this.audioManager = (AudioManager)this.getSystemService("audio");
      int value = Math.max(0, Math.min(100, percent));
      if (this.audioManager != null) {
         int max = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
         int level = Math.round(max * value / 100.0F);
         if (this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != level) this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0);
      }
      // Keep VLC's software gain at unity so Android media volume is the single
      // volume control and the gesture HUD always matches the audible level.

   }

   private void showGestureHud(LinearLayout hud) {
      if (hud == null) return;
      if (hud.getVisibility() != View.VISIBLE) {
         hud.setAlpha(0.0F);
         hud.setScaleX(0.96F);
         hud.setScaleY(0.96F);
         hud.setVisibility(View.VISIBLE);
         hud.animate().alpha(1.0F).scaleX(1.0F).scaleY(1.0F).setDuration(120L).start();
      }
   }
   private void animateHudSlider(SeekBar slider, int progress) {
      if (slider != null) {
         slider.setProgress(Math.max(0, Math.min(slider.getMax(), progress)));
      }
   }

   private GestureDetector createVideoTapDetector() {
      return new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
         @Override public boolean onDown(MotionEvent e) { return true; }
         @Override public boolean onSingleTapConfirmed(MotionEvent e) {
            MainActivity.this.togglePlayerControls();
            return true;
         }
         @Override public boolean onDoubleTap(MotionEvent e) {
            float width = MainActivity.this.fullscreenContainer != null ? MainActivity.this.fullscreenContainer.getWidth() : (MainActivity.this.videoSurface == null ? 0 : MainActivity.this.videoSurface.getWidth());
            if (width <= 0.0F) return true;
            float x = e.getX();
            float sideZone = width * 0.35F;
            if (x < sideZone) {
               MainActivity.this.seekBy(-10000L);
               MainActivity.this.showVideoSeekFeedback(false);
            } else if (x > width - sideZone) {
               MainActivity.this.seekBy(10000L);
               MainActivity.this.showVideoSeekFeedback(true);
            } else {
               MainActivity.this.togglePlayback();
               MainActivity.this.showVideoPlaybackFeedback(MainActivity.this.isPlaybackActive());
            }
            if (MainActivity.this.fullscreen) MainActivity.this.showFullscreenControls();
            return true;
         }
      });
   }

   private void showVideoPlaybackFeedback(boolean playing) {
      this.showVideoFeedback(playing ? "▶   Playing" : "❚❚   Paused");
   }

   private void showVideoSeekFeedback(boolean forward) {
      long position = this.player == null ? 0L : this.player.getTime();
      this.showSideSeekFeedback(forward, 10L, position);
   }

   private void showSideSeekFeedback(boolean forward, long seconds, long positionMs) {
      FrameLayout host = this.fullscreen ? this.fullscreenContainer : this.videoFrame;
      if (host == null) return;
      this.clearSeekFeedback();
      LinearLayout card = this.row();
      card.setGravity(17);
      card.setPadding(this.dp(15), this.dp(9), this.dp(17), this.dp(9));
      card.setBackground(this.shape(Color.argb(185, 8, 7, 14), 20));
      TextView arrows = this.text(forward ? "»" : "«", 32, LILAC, true);
      arrows.setGravity(17);
      card.addView(arrows, this.lp(this.dp(38), this.dp(44)));
      LinearLayout labels = this.col();
      labels.setGravity(forward ? Gravity.RIGHT : Gravity.LEFT);
      labels.addView(this.text((forward ? "+" : "−") + seconds + "s", 15, Color.WHITE, true));
      labels.addView(this.text("(" + this.playbackClock(positionMs, this.player == null ? 0L : this.player.getLength()) + ")", 12, Color.WHITE, true));
      card.addView(labels);
      this.addSeekFeedback(host, card, new FrameLayout.LayoutParams(-2, -2, forward ? (Gravity.RIGHT | Gravity.CENTER_VERTICAL) : (Gravity.LEFT | Gravity.CENTER_VERTICAL)), forward ? this.dp(24) : -this.dp(24));
   }

   private void showSwipeSeekFeedback(long offsetMs, long positionMs) {
      FrameLayout host = this.fullscreen ? this.fullscreenContainer : this.videoFrame;
      if (host == null) return;
      this.clearSeekFeedback();
      long offsetSeconds = Math.max(0L, Math.abs(offsetMs) / 1000L);
      String offset = String.format(Locale.US, "%s%d:%02d", offsetMs >= 0L ? "+" : "−", offsetSeconds / 60L, offsetSeconds % 60L);
      float rate = this.player == null ? 1.0F : this.player.getRate();
      TextView card = this.text(offset + "  (" + this.clock(positionMs) + ")  ×" + String.format(Locale.US, "%.1f", rate), 20, Color.WHITE, true);
      card.setGravity(17);
      card.setPadding(this.dp(18), this.dp(9), this.dp(18), this.dp(9));
      card.setBackground(this.shape(Color.argb(175, 8, 7, 14), 13));
      this.addSeekFeedback(host, card, new FrameLayout.LayoutParams(-2, -2, 17), 0);
   }

   private void clearSeekFeedback() {
      if (this.hideSeekFeedback != null) this.handler.removeCallbacks(this.hideSeekFeedback);
      if (this.seekFeedback != null && this.seekFeedback.getParent() instanceof ViewGroup) ((ViewGroup)this.seekFeedback.getParent()).removeView(this.seekFeedback);
      this.seekFeedback = null;
   }

   private void addSeekFeedback(FrameLayout host, View feedback, FrameLayout.LayoutParams layout, float startTranslationX) {
      this.seekFeedback = feedback;
      host.addView(feedback, layout);
      feedback.setAlpha(0.0F);
      feedback.setTranslationX(startTranslationX);
      feedback.animate().alpha(1.0F).translationX(0.0F).setDuration(130L).start();
      this.hideSeekFeedback = () -> {
         feedback.animate().alpha(0.0F).translationX(startTranslationX * 0.35F).setDuration(180L).withEndAction(() -> {
            if (feedback.getParent() instanceof ViewGroup) ((ViewGroup)feedback.getParent()).removeView(feedback);
            if (this.seekFeedback == feedback) this.seekFeedback = null;
         }).start();
      };
      this.handler.postDelayed(this.hideSeekFeedback, 780L);
   }

   private void showVideoFeedback(String message) {
      FrameLayout host = this.fullscreen ? this.fullscreenContainer : this.videoFrame;
      if (host == null) return;
      this.clearSeekFeedback();
      TextView indicator = this.text(message, 14, Color.WHITE, true);
      indicator.setGravity(17);
      indicator.setPadding(this.dp(18), this.dp(10), this.dp(18), this.dp(10));
      indicator.setBackground(this.shape(Color.argb(220, 26, 10, 40), 22));
      this.seekFeedback = indicator;
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, 17);
      host.addView(indicator, lp);
      indicator.setAlpha(0.0F);
      indicator.animate().alpha(1.0F).setDuration(100L).start();
      this.hideSeekFeedback = () -> {
         if (indicator.getParent() instanceof ViewGroup) ((ViewGroup)indicator.getParent()).removeView(indicator);
         if (this.seekFeedback == indicator) this.seekFeedback = null;
      };
      this.handler.postDelayed(this.hideSeekFeedback, 650L);
   }

private boolean handleFullscreenTouch(MotionEvent e) { return this.handleVideoTouch(e, this.fullscreenContainer); }

private boolean handleVideoTouch(MotionEvent e, FrameLayout host) {
      if (host == null) return false;
      if (this.screenTouchesLocked && host == this.fullscreenContainer) return true;
      int action = e.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN) this.suppressTapGestures = false;
      if (action == MotionEvent.ACTION_POINTER_DOWN) {
         this.suppressTapGestures = true;
         if (this.videoTapDetector != null) {
            MotionEvent cancel = MotionEvent.obtain(e);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            this.videoTapDetector.onTouchEvent(cancel);
            cancel.recycle();
         }
      } else if (!this.suppressTapGestures && e.getPointerCount() == 1 && this.videoTapDetector != null) {
         this.videoTapDetector.onTouchEvent(e);
      }
      float x = e.getX();
      float y = e.getY();
      float change;
      switch (e.getActionMasked()) {
         case 0:
            this.touchStartX = x;
            this.touchStartY = y;
            this.touchMoved = false;
            float w = (float)host.getWidth();
            this.touchZone = x < w * 0.25F ? 1 : (x > w * 0.75F ? 2 : 0);
            WindowManager.LayoutParams a = this.getWindow().getAttributes();
            this.touchStartBrightness = a.screenBrightness < 0.0F ? 0.5F : a.screenBrightness;
            this.touchStartVolume = this.getPlaybackVolumePercent();
            return true;
         case 5:
             return true;
          case 1:
            if (this.suppressTapGestures) { this.suppressTapGestures = false; return true; }
            change = e.getX() - this.touchStartX;
            if (this.touchZone == 0 && Math.abs(change) > (float)this.dp(45.0F)) {
               this.seekByGesture(change);
               this.showFullscreenControls();
            }

            return true;
         case 2:
            if (this.suppressTapGestures || e.getPointerCount() > 1) return true;
             float dy = this.touchStartY - y;
            float dx = x - this.touchStartX;
            if (Math.abs(dx) > (float)this.dp(36.0F) && Math.abs(dx) > Math.abs(dy) * 1.2F) this.touchZone = 0;
            if (this.touchZone == 0 && Math.abs(dx) > (float)this.dp(45.0F)) {
               this.touchMoved = true;
            }

            if (this.touchZone != 0 && Math.abs(dy) > (float)this.dp(2.0F)) {
               this.touchMoved = true;
               change = dy / (float)Math.max(1, host.getHeight());
               if (this.touchZone == 1) {
                  this.gestureVolume = Math.max(0, Math.min(100, Math.round((float)this.touchStartVolume + change * 100.0F)));
                  this.setPlaybackVolumePercent(this.gestureVolume);
                  this.volumeValue.setText(this.gestureVolume + "%");
                  this.animateHudSlider(this.volumeSlider, this.gestureVolume);
                  this.showGestureHud(this.volumeHud);
                  this.brightnessHud.setVisibility(8);
               } else {
                  int brightnessPercent = Math.max(1, Math.min(100, Math.round((this.touchStartBrightness + change) * 100.0F)));
                  this.setWindowBrightnessPercent(brightnessPercent);
                  this.brightnessValue.setText(brightnessPercent + "%");
                  this.animateHudSlider(this.brightnessSlider, Math.round(this.gestureBrightness * 100.0F));
                  this.showGestureHud(this.brightnessHud);
                  this.volumeHud.setVisibility(8);
               }

               this.handler.removeCallbacks(this.hideGestureHud);
               this.handler.postDelayed(this.hideGestureHud, 850L);
            }

            return true;
         case 6:
            this.showFullscreenControls();
            return true;
          case 3:
            return true;
         default:
            return true;
      }
   }

   private void buildFullscreenDrawer() {
      this.fullscreenDrawer = new FrameLayout(this);
      this.fullscreenDrawer.setBackgroundColor(Color.argb(155, 0, 0, 0));
      this.fullscreenDrawer.setOnClickListener((v) -> {
         this.toggleFullscreenDrawer();
      });
      FrameLayout.LayoutParams all = new FrameLayout.LayoutParams(-1, -1);
      this.fullscreenContainer.addView(this.fullscreenDrawer, all);
      LinearLayout pane = this.col();
      pane.setPadding(this.dp(10.0F), this.dp(10.0F), this.dp(10.0F), this.dp(10.0F));
      pane.setBackgroundColor(PANEL);
      FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(this.dp(290.0F), -1, 5);
      this.fullscreenDrawer.addView(pane, pp);
      pane.setOnClickListener((v) -> {
      });
      LinearLayout head = this.row();
      head.addView(this.text("Player options", 13, LILAC, true), new LinearLayout.LayoutParams(0, -2, 1.0F));
      TextView close = this.compactButton("×", false);
      head.addView(close);
      close.setOnClickListener((v) -> {
         this.toggleFullscreenDrawer();
      });
      pane.addView(head);
      LinearLayout tabs = this.row();
      TextView playlistTab = this.compactButton(this.current.network ? "Channels" : "Playlist", false);
      TextView chaptersTab = this.compactButton("Chapters", false);
      TextView toolsTab = this.compactButton("Controls", false);
      this.fullscreenPlaylistTab = playlistTab;
      this.fullscreenChaptersTab = chaptersTab;
      this.fullscreenControlsTab = toolsTab;
      TextView[] var10 = new TextView[]{playlistTab, chaptersTab, toolsTab};
      int var11 = var10.length;

      for(int var12 = 0; var12 < var11; ++var12) {
         TextView b = var10[var12];
         LinearLayout.LayoutParams tp = this.lp(0, -2);
         tp.weight = 1.0F;
         tabs.addView(b, tp);
      }

      pane.addView(tabs);
      this.fullscreenChannelSearch = this.edit("Search channels");
      this.fullscreenChannelSearch.setVisibility(this.current != null && this.current.network ? 0 : 8);
      pane.addView(this.fullscreenChannelSearch, this.lp(-1, this.dp(38.0F)));
      this.fullscreenChannelSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence x,int a,int c,int f){}public void onTextChanged(CharSequence x,int a,int before,int count){refreshFullscreenQueue();}public void afterTextChanged(Editable x){}});
      ScrollView listScroll = new ScrollView(this);
      this.fullscreenQueue = this.col();
      listScroll.addView(this.fullscreenQueue);
      pane.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1.0F));
      this.fullscreenChapters = this.col();
      this.fullscreenChapters.setPadding(this.dp(5.0F), this.dp(10.0F), this.dp(5.0F), this.dp(5.0F));
      this.fullscreenChapters.addView(this.hint("Chapter markers are available when the video file contains chapter metadata."));
      pane.addView(this.fullscreenChapters, new LinearLayout.LayoutParams(-1, 0, 1.0F));
      this.fullscreenTools = this.col();
      this.fullscreenTools.setPadding(this.dp(2.0F), this.dp(8.0F), this.dp(2.0F), this.dp(5.0F));
      this.fullscreenSettingCategoryButtons = this.row();
      String[] categories = new String[]{"Audio", "Video", "Subtitles", "Playback"};
      TextView[] categoryButtons = new TextView[categories.length];
      for (int i = 0; i < categories.length; i++) {
         final int index = i;
         TextView button = this.compactButton(categories[i], false);
         LinearLayout.LayoutParams categoryLp = this.lp(0, -2);
         categoryLp.weight = 1.0F;
         categoryLp.setMargins(this.dp(1.0F), 0, this.dp(1.0F), 0);
         this.fullscreenSettingCategoryButtons.addView(button, categoryLp);
         categoryButtons[i] = button;
      }
      this.fullscreenTools.addView(this.fullscreenSettingCategoryButtons);
      LinearLayout settingsPages = this.col();
      ScrollView settingsScroll = new ScrollView(this);
      settingsScroll.setFillViewport(true);
      settingsScroll.addView(settingsPages);
      this.fullscreenAudioTools = this.col();
      this.fullscreenVideoTools = this.col();
      this.fullscreenSubtitleTools = this.col();
      this.fullscreenPlaybackTools = this.col();
      settingsPages.addView(this.fullscreenAudioTools);
      settingsPages.addView(this.fullscreenVideoTools);
      settingsPages.addView(this.fullscreenSubtitleTools);
      settingsPages.addView(this.fullscreenPlaybackTools);
      this.fullscreenAudioTools.addView(this.fullscreenToolRow("Audio track", "Select an embedded audio track", () -> this.showTracks(false)));
      LinearLayout audioPresetRow = this.fullscreenToolRow("Audio preset", this.audioPreset, () -> this.showPlaybackPresets(false));
      this.audioPresetStatus = (TextView)audioPresetRow.getChildAt(1);
      this.fullscreenAudioTools.addView(audioPresetRow);
      this.fullscreenAudioTools.addView(this.fullscreenToolRow("Equalizer", "Frequency bands · custom sound curve", () -> {
         this.audioPreset = "Custom";
         this.prefs().edit().putString("audio_preset", "Custom").apply();
         this.applyPlaybackPresetForCurrentMedia();
         this.showCustomEqualizer(false);
      }));
      this.fullscreenAudioTools.addView(this.fullscreenToolRow("Audio boost", this.audioBoostDb + " dB · equalizer preamp", () -> this.showAudioEnhancementDialog(false)));
      this.fullscreenAudioTools.addView(this.fullscreenToolRow("Bass boost", this.bassBoostDb + " dB · low frequency bands", () -> this.showAudioEnhancementDialog(true)));
      this.fullscreenAudioTools.addView(this.fullscreenToolRow("Audio delay", this.audioDelayMs + " ms", () -> this.showSyncDelayDialog(false)));
      LinearLayout videoPresetRow = this.fullscreenToolRow("Video preset", this.videoPreset, () -> this.showPlaybackPresets(true));
      this.videoPresetStatus = (TextView)videoPresetRow.getChildAt(1);
      this.fullscreenVideoTools.addView(videoPresetRow);
      this.fullscreenVideoTools.addView(this.fullscreenToolRow("Video decoder", this.decoderName() + " · HW / SW / HW+SW", () -> this.decoderDialog()));
      this.fullscreenVideoTools.addView(this.fullscreenToolRow("Screen brightness", this.getWindowBrightnessPercent() + "% · gesture: right side", () -> this.showBrightnessDialog()));
      this.fullscreenVideoTools.addView(this.fullscreenToolRow("Aspect ratio / screen fit", this.aspectMode, () -> this.aspectRatioDialog()));
      this.fullscreenSubtitleTools.addView(this.fullscreenToolRow("Subtitle track", "Select, disable, or load an external file", () -> this.showTracks(true)));
      this.fullscreenSubtitleTools.addView(this.fullscreenToolRow("Subtitle delay", this.subtitleDelayMs + " ms", () -> this.showSyncDelayDialog(true)));
      this.fullscreenSubtitleTools.addView(this.hint("Subtitle size, color, and position controls are unavailable in the bundled VLC Android binding."));
      this.fullscreenPlaybackTools.addView(this.fullscreenToolRow("Playback speed", "Adjust without restarting playback", () -> this.speedDialog()));
      this.fullscreenPlaybackTools.addView(this.fullscreenToolRow("Sleep timer", this.sleepTimerDeadlineMs > 0L ? "Timer active" : "Off", () -> this.showSleepTimerDialog()));
      this.fullscreenPlaybackTools.addView(this.fullscreenToolRow("A–B repeat", this.abRepeatDescription(), () -> this.showAbRepeatDialog()));
      this.fullscreenPlaybackTools.addView(this.fullscreenToolRow("Screen touch lock", this.screenTouchesLocked ? "Locked" : "Unlocked", () -> this.setScreenTouchesLocked(!this.screenTouchesLocked)));
      this.fullscreenPlaybackTools.addView(this.fullscreenToolRow("Background audio", this.backgroundAudioEnabled ? "Enabled for audio files" : "Disabled", () -> this.setBackgroundAudioEnabled(!this.backgroundAudioEnabled)));
      this.fullscreenPlaybackTools.addView(this.hint("Last-position resume is saved automatically for recently played media."));
      this.playbackPresetStatus = this.currentIsVideo() ? this.videoPresetStatus : this.audioPresetStatus;
      this.fullscreenTools.addView(settingsScroll, new LinearLayout.LayoutParams(-1, 0, 1.0F));
      pane.addView(this.fullscreenTools, new LinearLayout.LayoutParams(-1, 0, 1.0F));
      this.fullscreenChapters.setVisibility(8);
      this.fullscreenTools.setVisibility(0);
      this.selectSettingsCategory(categoryButtons, this.currentIsVideo() ? 1 : 0);
      for (int i = 0; i < categoryButtons.length; i++) {
         final int index = i;
         categoryButtons[i].setOnClickListener(v -> this.selectSettingsCategory(categoryButtons, index));
      }
      this.selectFullscreenDrawerTab(toolsTab);
      playlistTab.setOnClickListener((v) -> {
         this.selectFullscreenDrawerTab(playlistTab);
         this.fullscreenQueue.setVisibility(0);
         this.fullscreenChapters.setVisibility(8);
         this.fullscreenTools.setVisibility(8);
         this.fullscreenChannelSearch.setVisibility(this.current != null && this.current.network ? 0 : 8);
      });
      chaptersTab.setOnClickListener((v) -> {
         this.selectFullscreenDrawerTab(chaptersTab);
         this.fullscreenQueue.setVisibility(8);
         this.fullscreenChapters.setVisibility(0);
         this.fullscreenTools.setVisibility(8);
         this.fullscreenChannelSearch.setVisibility(8);
      });
      toolsTab.setOnClickListener((v) -> {
         this.selectFullscreenDrawerTab(toolsTab);
         this.fullscreenQueue.setVisibility(8);
         this.fullscreenChapters.setVisibility(8);
         this.fullscreenTools.setVisibility(0);
         this.fullscreenChannelSearch.setVisibility(8);
      });
      this.fullscreenDrawer.setVisibility(8);
      this.refreshFullscreenQueue();
   }

   private LinearLayout fullscreenToolRow(String label, String value, Runnable action) {
      LinearLayout r = this.col();
      r.setPadding(this.dp(12.0F), this.dp(11.0F), this.dp(12.0F), this.dp(11.0F));
      r.setBackground(this.shape(CARD, 12));
      r.addView(this.text(label, 13, -1, true));
      TextView sub = this.text(value, 10, MUTED, false);
      sub.setPadding(0, this.dp(3.0F), 0, 0);
      r.addView(sub);
      r.setOnClickListener((v) -> {
         action.run();
      });
      return r;
   }

   private void selectFullscreenDrawerTab(TextView selected) {
      TextView[] tabs = new TextView[]{this.fullscreenPlaylistTab, this.fullscreenChaptersTab, this.fullscreenControlsTab};
      for (TextView tab : tabs) if (tab != null) {
         boolean active = tab == selected;
         tab.setBackground(this.shape(active ? PURPLE : CARD, 13));
         tab.setTextColor(active ? Color.WHITE : MUTED);
      }
   }

   private void selectSettingsCategory(TextView[] buttons, int selected) {
      LinearLayout[] pages = new LinearLayout[]{this.fullscreenAudioTools, this.fullscreenVideoTools, this.fullscreenSubtitleTools, this.fullscreenPlaybackTools};
      for (int i = 0; i < pages.length; i++) {
         if (pages[i] != null) pages[i].setVisibility(i == selected ? View.VISIBLE : View.GONE);
         if (buttons[i] != null) {
            buttons[i].setBackground(this.shape(i == selected ? PURPLE : CARD, 12));
            buttons[i].setTextColor(i == selected ? Color.WHITE : MUTED);
         }
      }
   }

   private void showFullscreenPlaylist() {
      if (this.fullscreenDrawer != null) {
         this.fullscreenDrawer.setVisibility(0);
         this.selectFullscreenDrawerTab(this.fullscreenPlaylistTab);
         this.fullscreenQueue.setVisibility(0);
         this.fullscreenChapters.setVisibility(8);
         this.fullscreenTools.setVisibility(8);
         if (this.fullscreenChannelSearch != null) this.fullscreenChannelSearch.setVisibility(this.current != null && this.current.network ? View.VISIBLE : View.GONE);
         this.showFullscreenControls();
      }
   }

   private void showFullscreenTools() {
      if (this.fullscreenDrawer != null) {
         this.fullscreenDrawer.setVisibility(0);
         this.selectFullscreenDrawerTab(this.fullscreenControlsTab);
         this.fullscreenQueue.setVisibility(8);
         this.fullscreenChapters.setVisibility(8);
         this.fullscreenTools.setVisibility(0);
         this.showFullscreenControls();
      }
   }

   private void toggleFullscreenDrawer() {
      if (this.fullscreenDrawer != null) {
         boolean open = this.fullscreenDrawer.getVisibility() != 0;
         this.fullscreenDrawer.setVisibility(open ? 0 : 8);
         this.showFullscreenControls();
      }
   }

   private void togglePlayerControls() {
      if (this.fullscreen) {
         boolean visible = this.fullscreenOverlay != null && this.fullscreenOverlay.getVisibility() == View.VISIBLE;
         if (visible) this.hideFullscreenControlsNow(); else this.showFullscreenControls();
      } else {
         this.setPlayerControlsVisible(!this.portraitControlsVisible, true);
      }
   }

   private void setPlayerControlsVisible(boolean visible, boolean animate) {
      this.portraitControlsVisible = visible;
      View[] views = new View[]{this.portraitControlsPanel, this.portraitVideoTools};
      this.handler.removeCallbacks(this.hidePortraitControls);
      for (View view : views) if (view != null) {
         view.animate().cancel();
         if (visible) {
            view.setVisibility(View.VISIBLE);
            if (animate) { view.setAlpha(0.0F); view.animate().alpha(1.0F).setDuration(180L).start(); } else view.setAlpha(1.0F);
         } else if (animate) {
            view.animate().alpha(0.0F).setDuration(180L).withEndAction(() -> { if (!this.portraitControlsVisible) view.setVisibility(View.GONE); }).start();
         } else view.setVisibility(View.GONE);
      }
      if (visible) this.handler.postDelayed(this.hidePortraitControls, 4000L);
   }

   private void hideFullscreenControlsNow() {
      this.handler.removeCallbacks(this.hideFullscreenControls);
      View[] views = new View[]{this.fullscreenOverlay, this.fullscreenTopOverlay};
      for (View view : views) if (view != null && view.getVisibility() == View.VISIBLE) {
         view.animate().cancel();
         view.animate().alpha(0.0F).setDuration(180L).withEndAction(() -> { if (view.getAlpha() == 0.0F) view.setVisibility(View.GONE); }).start();
      }
   }

   private void applyTranslucentControlBackgrounds(View view, int alpha) {
      if (view == null) return;
      if (view instanceof TextView && view.getBackground() != null) view.getBackground().mutate().setAlpha(alpha);
      if (view instanceof ViewGroup) {
         ViewGroup group = (ViewGroup)view;
         for (int i = 0; i < group.getChildCount(); i++) this.applyTranslucentControlBackgrounds(group.getChildAt(i), alpha);
      }
   }

   private void showFullscreenControls() {
      if (this.fullscreen && this.fullscreenOverlay != null) {
         this.fullscreenOverlay.setAlpha(0.0F);
         this.fullscreenOverlay.setVisibility(0);
         this.fullscreenOverlay.animate().alpha(1.0F).setDuration(150L);
         if (this.fullscreenTopOverlay != null) {
            this.fullscreenTopOverlay.setAlpha(0.0F);
            this.fullscreenTopOverlay.setVisibility(0);
            this.fullscreenTopOverlay.animate().alpha(1.0F).setDuration(150L);
         }

         this.handler.removeCallbacks(this.hideFullscreenControls);
         this.handler.postDelayed(this.hideFullscreenControls, 4000L);
      }
   }

   private String extension(String n) {
      int i = n.lastIndexOf(46);
      return i < 0 ? "VIDEO" : n.substring(i + 1).toUpperCase(Locale.ROOT);
   }

   private void refreshFullscreenQueue() {
      if (this.fullscreenQueue == null || this.current == null) return;
      this.fullscreenQueue.removeAllViews();
      ArrayList<MediaItem> entries = this.current.network && !this.channels.isEmpty() ? this.channels : this.queue;
      String channelQuery = this.fullscreenChannelSearch == null ? ""
              : this.fullscreenChannelSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
      if (this.fullscreenQueueQuery == null || !this.fullscreenQueueQuery.equals(channelQuery)) {
         this.fullscreenQueueQuery = channelQuery;
         this.fullscreenQueuePageStart = 0;
      }
      int matches = 0;
      for (MediaItem item : entries) if (this.matchesStream(item, channelQuery)) matches++;
      int pageEnd = Math.min(this.fullscreenQueuePageStart + CHANNEL_PAGE_SIZE, matches);
      int matched = 0;
      for (MediaItem m : entries) {
         if (!this.matchesStream(m, channelQuery)) continue;
         if (matched >= this.fullscreenQueuePageStart && matched < pageEnd) {
            LinearLayout item = this.row();
            item.setPadding(this.dp(4.0F), this.dp(5.0F), this.dp(3.0F), this.dp(5.0F));
            boolean selected = m.uri.equals(this.current.uri);
            item.setBackground(this.shape(selected ? Color.rgb(47, 15, 72) : CARD, 9));
            ImageView art = new ImageView(this);
            art.setImageResource(R.drawable.solo_player_logo);
            art.setScaleType(ScaleType.CENTER_CROP);
            item.addView(art, this.lp(this.dp(38.0F), this.dp(38.0F)));
            if (m.network) this.loadChannelLogo(m, art); else this.loadArtwork(m, art);
            LinearLayout meta = this.col();
            meta.setPadding(this.dp(5.0F), 0, 0, 0);
            meta.addView(this.text(m.name, this.swTextSize(), -1, true));
            meta.addView(this.text(this.clock(m.duration), 9, MUTED, false));
            item.addView(meta, new LinearLayout.LayoutParams(0, -2, 1.0F));
            LinearLayout.LayoutParams ip = this.lp(-1, -2);
            ip.bottomMargin = this.dp(4.0F);
            this.fullscreenQueue.addView(item, ip);
            item.setOnClickListener(v -> {
               if (this.current == null) { this.toast("Playback has already ended"); return; }
               this.playFullscreen(m);
            });
         }
         matched++;
         if (matched >= pageEnd) break;
      }
      if (matches == 0 && this.current.network) this.fullscreenQueue.addView(this.hint("No channels match your search."));
      this.addChannelPageNavigation(this.fullscreenQueue, matches, this.fullscreenQueuePageStart, value -> {
         this.fullscreenQueuePageStart = value;
         this.refreshFullscreenQueue();
      });
   }

   private int swTextSize() {
      return (float)this.getResources().getDisplayMetrics().widthPixels / this.getResources().getDisplayMetrics().density > 1100.0F ? 10 : 9;
   }

   private void playFullscreen(MediaItem item) {
      if (item == null || item.uri == null || item.uri.trim().isEmpty()) {
         this.toast("This channel has no playable stream URL");
         return;
      }
      if (this.player == null || this.vlc == null) {
         this.toast("The media player is not ready. Please try again.");
         return;
      }
      boolean waitForVideoSurface = this.waitingSubtitleRestore && item.video && this.videoSurface != null;
      long resumeAt = waitForVideoSurface ? 0L : this.savedPositionFor(item.uri);
      this.pendingResumePosition = resumeAt > 0L ? resumeAt : -1L;
      this.pendingResumeUri = item.uri;
      this.current = item;
      if (item.network) { this.streamHasPlayed = false; this.setStreamLoading(true, "Connecting to stream…"); this.startStreamConnectionTimeout(); }
      else { this.handler.removeCallbacks(this.streamConnectionTimeout); this.setStreamLoading(false, null); }
      this.updatePlaybackPresetStatus();
      playbackRequested = true;
      playbackStartPending = true;
      this.ensureQueueForItem(item);
      this.recordRecent(item);

      try {
         if (this.activeMediaFd != null) {
            try {
               this.activeMediaFd.close();
            } catch (Exception var4) {
            }

            this.activeMediaFd = null;
         }

         Uri source = Uri.parse(item.uri);
         Media media;
         if ("content".equalsIgnoreCase(source.getScheme()) && !item.network) {
            this.activeMediaFd = this.getContentResolver().openAssetFileDescriptor(source, "r");
            if (this.activeMediaFd == null) {
               throw new IOException("Android could not open this media file");
            }

            media = new Media(this.vlc, this.activeMediaFd);
         } else {
            media = new Media(this.vlc, source);
         }

         media.setHWDecoderEnabled(this.decoderMode != 1, this.decoderMode == 2);
         if (item.video && item.uri.equals(externalSubtitleMediaUri) && !externalSubtitlePath.isEmpty()) media.addOption(":sub-file=" + externalSubtitlePath);
         media.addOption(":network-caching=" + this.networkCache);
         if (item.network) {
            if (!item.agent.isEmpty()) {
               media.addOption(":http-user-agent=" + this.clean(item.agent));
            }

            if (!item.referer.isEmpty()) {
               media.addOption(":http-referrer=" + this.clean(item.referer));
            }

            if (item.uri.toLowerCase(Locale.ROOT).startsWith("rtsp:")) {
               media.addOption(":rtsp-tcp");
            }
         }

         this.player.setMedia(media);
         media.release();
         this.applyStoredSyncDelays();
         this.applyPlaybackPresetForCurrentMedia();
         if (waitForVideoSurface) {
            if (this.pendingSubtitleResume && this.requestPlaybackFocus()) {
               this.player.play();
               playbackRequested = true;
            } else {
               playbackRequested = false;
               playbackStartPending = false;
            }
         } else if (this.requestPlaybackFocus()) {
            this.player.play();
            playbackRequested = true;
         } else {
            playbackRequested = false;
            playbackStartPending = false;
         }

         this.applyAspectRatio();
         if (this.fullscreenTitle != null) {
            this.fullscreenTitle.setText(item.name);
         }
         if (!item.video && this.fullscreenArtwork != null) this.loadArtwork(item, this.fullscreenArtwork);

         this.refreshFullscreenQueue();
         this.syncPlaybackUi();
      } catch (Exception var5) {
         this.handler.removeCallbacks(this.streamConnectionTimeout);
         playbackRequested = false;
         playbackStartPending = false;
         this.waitingSubtitleRestore = false;
         this.pendingSubtitleSeek = -1L;
         this.syncPlaybackUi();
         if (item.network) this.showNetworkStreamFailure("Could not open stream: " + var5.getMessage()); else this.toast("Could not open media: " + var5.getMessage());
      }

   }

   protected void onDestroy() {
      this.saveCurrentResumePosition();
      this.clearPausedFrameOverlay();
      boolean preserveAudio = this.isBackgroundAudioSession();
      this.handler.removeCallbacks(this.ticker);
      if (VERSION.SDK_INT >= 33 && this.backCallback != null) {
         com.soloplayer.android.MainActivity.Api33BackHelper.unregister(this, this.backCallback);
      }

      if (!preserveAudio) this.stopPlaybackSession();
      if (activeActivity == this) activeActivity = null;
      if (!this.isCasting()) this.stopLocalCastServer();
      if (!(VERSION.SDK_INT >= 26 && this.isInPictureInPictureMode())) this.deleteActiveVaultTemp();
      this.worker.shutdownNow();
      if (this.player != null) {
         this.player.setEventListener(null);
         this.player.detachViews();
         this.videoSurfaceAttached = false;
         if (!preserveAudio) { this.player.release(); player = null; }
      }

      this.thumbnailWorker.shutdownNow();
      if (!preserveAudio && activeMediaFd != null) {
         try { activeMediaFd.close(); } catch (Exception ignored) {}
         activeMediaFd = null;
      }

      if (!preserveAudio && this.vlc != null) {
         this.vlc.release();
         vlc = null;
      }

      super.onDestroy();
   }

   private static final class LocalMediaHttpServer {
      private final Context context;
      private final MediaItem item;
      private final String token = UUID.randomUUID().toString().replace("-", "");
      private final ServerSocket socket;
      private volatile boolean running;

      LocalMediaHttpServer(Context context, MediaItem item) throws IOException {
         this.context = context.getApplicationContext();
         this.item = item;
         this.socket = new ServerSocket(0, 8, InetAddress.getByName("0.0.0.0"));
      }

      void start() {
         if (this.running) return;
         this.running = true;
         Thread acceptor = new Thread(() -> {
            while (this.running) {
               try {
                  Socket client = this.socket.accept();
                  Thread worker = new Thread(() -> this.serve(client), "solo-cast-http-client");
                  worker.setDaemon(true); worker.start();
               } catch (IOException error) { if (this.running) Log.w("SoloPlayer", "Cast HTTP accept failed", error); }
            }
         }, "solo-cast-http");
         acceptor.setDaemon(true); acceptor.start();
      }

      String url() throws IOException {
         InetAddress address = null;
         try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
               NetworkInterface network = interfaces.nextElement();
               if (!network.isUp() || network.isLoopback()) continue;
               Enumeration<InetAddress> addresses = network.getInetAddresses();
               while (addresses.hasMoreElements()) {
                  InetAddress candidate = addresses.nextElement();
                  if (!candidate.isLoopbackAddress() && candidate.isSiteLocalAddress() && candidate.getHostAddress().indexOf(':') < 0) {
                     address = candidate;
                     if (network.getName().startsWith("wlan")) break;
                  }
               }
               if (address != null && network.getName().startsWith("wlan")) break;
            }
         } catch (Exception error) { throw new IOException("Could not find the phone's Wi-Fi address", error); }
         if (address == null) throw new IOException("Connect both devices to the same Wi-Fi network");
         return "http://" + address.getHostAddress() + ":" + this.socket.getLocalPort() + "/" + this.token;
      }

      void stop() {
         this.running = false;
         try { this.socket.close(); } catch (Exception ignored) {}
      }

      private void serve(Socket client) {
         try (Socket peer = client) {
            peer.setSoTimeout(12000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(peer.getInputStream(), StandardCharsets.US_ASCII));
            String request = reader.readLine();
            if (request == null) return;
            String[] parts = request.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1].split("\\?", 2)[0];
            long rangeStart = 0L, rangeEnd = -1L;
            String header;
            while ((header = reader.readLine()) != null && !header.isEmpty()) {
               int colon = header.indexOf(':'); if (colon < 0) continue;
               String key = header.substring(0, colon).trim();
               String value = header.substring(colon + 1).trim();
               if (key.equalsIgnoreCase("Range") && value.startsWith("bytes=")) {
                  String[] range = value.substring(6).split("-", 2);
                  try { rangeStart = Long.parseLong(range[0]); if (range.length > 1 && !range[1].isEmpty()) rangeEnd = Long.parseLong(range[1]); } catch (Exception ignored) {}
               }
            }
            if (!path.equals("/" + this.token) || (!method.equals("GET") && !method.equals("HEAD"))) {
               this.writeHeaders(peer, "404 Not Found", "text/plain", 0L, null); return;
            }
            long total = this.mediaLength();
            if (total <= 0L || rangeStart >= total) { this.writeHeaders(peer, "416 Range Not Satisfiable", "text/plain", 0L, "Content-Range: bytes */" + Math.max(0L, total) + "\r\n"); return; }
            long end = rangeEnd < rangeStart || rangeEnd >= total ? total - 1L : rangeEnd;
            long bodyLength = end - rangeStart + 1L;
            boolean partial = rangeStart > 0L || end < total - 1L;
            String status = partial ? "206 Partial Content" : "200 OK";
            String extra = partial ? "Content-Range: bytes " + rangeStart + "-" + end + "/" + total + "\r\n" : "";
            this.writeHeaders(peer, status, this.mimeType(), bodyLength, extra);
            if (method.equals("HEAD")) return;
            try (InputStream input = this.openStream()) {
               long skipped = 0L;
               while (skipped < rangeStart) { long n = input.skip(rangeStart - skipped); if (n <= 0L) { if (input.read() < 0) break; n = 1L; } skipped += n; }
               byte[] buffer = new byte[64 * 1024]; long remaining = bodyLength; int read;
               OutputStream output = peer.getOutputStream();
               while (remaining > 0L && (read = input.read(buffer, 0, (int)Math.min((long)buffer.length, remaining))) >= 0) {
                  output.write(buffer, 0, read); remaining -= read;
               }
               output.flush();
            }
         } catch (Exception error) { Log.d("SoloPlayer", "Cast HTTP client closed", error); }
      }

      private void writeHeaders(Socket peer, String status, String mime, long length, String extra) throws IOException {
         OutputStream output = peer.getOutputStream();
         String header = "HTTP/1.1 " + status + "\r\n" +
            "Content-Type: " + mime + "\r\n" +
            "Content-Length: " + length + "\r\n" +
            "Accept-Ranges: bytes\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Connection: close\r\n" + (extra == null ? "" : extra) + "\r\n";
         output.write(header.getBytes(StandardCharsets.US_ASCII)); output.flush();
      }

      private InputStream openStream() throws IOException {
         Uri uri = Uri.parse(this.item.uri);
         if ("file".equalsIgnoreCase(uri.getScheme())) return new FileInputStream(new File(uri.getPath()));
         InputStream input = this.context.getContentResolver().openInputStream(uri);
         if (input == null) throw new FileNotFoundException("Media source unavailable");
         return input;
      }

      private long mediaLength() {
         Uri uri = Uri.parse(this.item.uri);
         if ("file".equalsIgnoreCase(uri.getScheme())) return new File(uri.getPath()).length();
         try (AssetFileDescriptor afd = this.context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (afd != null && afd.getLength() > 0L) return afd.getLength();
         } catch (Exception ignored) {}
         try (Cursor cursor = this.context.getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getLong(0);
         } catch (Exception ignored) {}
         return this.item.size;
      }

      private String mimeType() {
         String extension = this.item.name.contains(".") ? this.item.name.substring(this.item.name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
         String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
         return mime == null ? (this.item.video ? "video/mp4" : "audio/mpeg") : mime;
      }
   }

   private static final class Api33BackHelper {
      static Object register(MainActivity a,Runnable action){android.window.OnBackInvokedCallback cb=action::run;a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,cb);return cb;}
      static void unregister(MainActivity a,Object callback){a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)callback);}
   }}





