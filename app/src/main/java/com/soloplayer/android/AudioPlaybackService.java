package com.soloplayer.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import org.videolan.libvlc.MediaPlayer;

/** Keeps local audio alive while the Solo Player Activity is in the background. */
public final class AudioPlaybackService extends Service {
    private static final String CHANNEL_ID = "solo_audio_playback";
    private static final int NOTIFICATION_ID = 4107;
    private static final String ACTION_UPDATE = "com.soloplayer.android.audio.UPDATE";
    private static final String ACTION_TOGGLE = "com.soloplayer.android.audio.TOGGLE";
    private static final String ACTION_STOP = "com.soloplayer.android.audio.STOP";
    private MediaSession mediaSession;
    private Bitmap notificationIcon;
    private String title = "Solo Player";
    private boolean requestedPlaying;

    static void startOrUpdate(Context context, String title, boolean playing) {
        Intent intent = new Intent(context, AudioPlaybackService.class).setAction(ACTION_UPDATE)
                .putExtra("title", title).putExtra("playing", playing);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, AudioPlaybackService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        mediaSession = new MediaSession(this, "SoloPlayerAudio");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { setPlaying(true); }
            @Override public void onPause() { setPlaying(false); }
            @Override public void onStop() { stopAudio(); }
        });
        mediaSession.setActive(true);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAudio();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            MediaPlayer player = MainActivity.retainedPlayer();
            boolean playing = player != null && (player.isPlaying() || MainActivity.retainedPlaybackRequested());
            setPlaying(!playing);
        } else if (intent != null) {
            title = intent.getStringExtra("title");
            requestedPlaying = intent.getBooleanExtra("playing", false);
        }
        if (title == null || title.trim().isEmpty()) title = "Solo Player";
        startForeground(NOTIFICATION_ID, buildNotification());
        publishSessionState();
        return START_STICKY;
    }

    private void setPlaying(boolean playing) {
        MediaPlayer player = MainActivity.retainedPlayer();
        if (player != null && player.hasMedia()) {
            requestedPlaying = playing;
            MainActivity.setRetainedPlaybackRequested(playing);
            if (playing) player.play(); else player.pause();
        } else {
            requestedPlaying = false;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        publishSessionState();
        MainActivity.refreshRetainedPlaybackUi();
    }

    private void stopAudio() {
        requestedPlaying = false;
        MainActivity.stopRetainedAudio();
        publishSessionState();
    }

    private void publishSessionState() {
        if (mediaSession == null) return;
        mediaSession.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Solo Player")
                .putBitmap(MediaMetadata.METADATA_KEY_ART, getNotificationIcon())
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, getNotificationIcon()).build());
        MediaPlayer player = MainActivity.retainedPlayer();
        boolean playing = player != null && (player.isPlaying() || requestedPlaying);
        long position = player != null && player.hasMedia() ? player.getTime() : 0L;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP;
        mediaSession.setPlaybackState(new PlaybackState.Builder().setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, position, 1.0f).build());
    }

    private Notification buildNotification() {
        MediaPlayer player = MainActivity.retainedPlayer();
        boolean playing = player != null && (player.isPlaying() || requestedPlaying);
        Intent toggleIntent = new Intent(this, AudioPlaybackService.class).setAction(ACTION_TOGGLE);
        PendingIntent toggle = PendingIntent.getService(this, 1, toggleIntent, pendingFlags());
        Intent openIntent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(this, 2, openIntent, pendingFlags());
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_media_play).setLargeIcon(getNotificationIcon()).setContentTitle(title).setContentText("Solo Player · Audio")
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(playing ? R.drawable.pause_icon : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play", toggle);
        if (mediaSession != null) builder.setStyle(new Notification.MediaStyle().setMediaSession(mediaSession.getSessionToken()).setShowActionsInCompactView(0));
        return builder.build();
    }

    private Bitmap getNotificationIcon() {
        if (notificationIcon == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            notificationIcon = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon, options);
        }
        return notificationIcon;
    }

    private int pendingFlags() {
        return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Audio playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Controls for audio playing in Solo Player");
            channel.setShowBadge(false);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        if (mediaSession != null) { mediaSession.setActive(false); mediaSession.release(); mediaSession = null; }
        super.onDestroy();
    }
}
