package ch.srg.mediaplayer.internal.cast;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.WebImage;

import java.io.IOException;

import ch.srg.mediaplayer.PlayerDelegate;
import ch.srg.mediaplayer.SRGMediaPlayerException;
import ch.srg.mediaplayer.SRGMediaPlayerView;

/**
 * Created by npietri on 19.10.15.
 */
public class GoogleCastDelegate implements PlayerDelegate {

    private static final String TAG = "GoogleCastDelegate";
    private final OnPlayerDelegateListener controller;

    private RemoteMediaPlayer remoteMediaPlayer;
    private GoogleApiClient apiClient;
    private String sessionId;

    private boolean mApplicationStarted;

    private MediaInfo mediaInfo;

    private int internalStatus;
    private boolean delegateReady;
    private boolean playIfReady;
    private long pendingSeekTo;
    private String title;
    private String mediaThumbnailUrl;

    public GoogleCastDelegate(RemoteMediaPlayer remoteMediaPlayer, String sessionId, GoogleApiClient apiClient, OnPlayerDelegateListener controller) {
        this.sessionId = sessionId;
        this.apiClient = apiClient;
        this.controller = controller;
        this.remoteMediaPlayer = remoteMediaPlayer;
        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient,
                    this.remoteMediaPlayer.getNamespace(), this.remoteMediaPlayer);
            this.remoteMediaPlayer.setOnStatusUpdatedListener(onStatusUpdateListener);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        }
    }

    @Override
    public boolean canRenderInView(View view) {
        return true;
    }

    @Override
    public View createRenderingView(Context parentContext) {
        return new View(parentContext);
    }

    @Override
    public void bindRenderingViewInUiThread(SRGMediaPlayerView mediaPlayerView) throws SRGMediaPlayerException {

    }

    @Override
    public void unbindRenderingView() {

    }


    @Override
    public void prepare(Uri videoUri) throws SRGMediaPlayerException {
        remoteMediaPlayer.requestStatus(apiClient);
        Log.d(TAG, "Prepare: " + videoUri);
        controller.onPlayerDelegatePreparing(this);

        String metadataTitle;
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        if (title != null) {
            metadataTitle = title;
        } else {
            Context context = controller.getContext();
            metadataTitle = context.getString(context.getApplicationInfo().labelRes);
        }
        if (mediaThumbnailUrl != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(mediaThumbnailUrl)));
        }
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, metadataTitle);
        mediaInfo = new MediaInfo.Builder(String.valueOf(videoUri))
                .setContentType("application/vnd.apple.mpegurl")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        remoteMediaPlayer
                .load(apiClient, mediaInfo, this.playIfReady, pendingSeekTo)
                .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                        Log.d(TAG, "onPlayerDelegatePlayWhenReadyCommited");
                        delegateReady = true;
                        controller.onPlayerDelegateReady(GoogleCastDelegate.this);
                    }
                });
    }

    @Override
    public void playIfReady(boolean playIfReady) throws IllegalStateException {
        Log.d(TAG, "PlayIfReady: " + playIfReady);

        remoteMediaPlayer.requestStatus(apiClient);
        controller.onPlayerDelegatePlayWhenReadyCommited(GoogleCastDelegate.this);

        if (remoteMediaPlayer != null && mediaInfo != null && this.playIfReady != playIfReady) {
            if (playIfReady && internalStatus == MediaStatus.PLAYER_STATE_PAUSED) {
                Log.d(TAG, "remoteMediaPlayer.play");
                remoteMediaPlayer.play(apiClient);
            } else {
                Log.d(TAG, "remoteMediaPlayer.pause");
                remoteMediaPlayer.pause(apiClient);
            }
        }
        this.playIfReady = playIfReady;
    }

    @Override
    public void seekTo(long positionInMillis) throws IllegalStateException {
        Log.d(TAG, "seekTo: " + positionInMillis);
        if (delegateReady && remoteMediaPlayer != null && sessionId != null && apiClient != null) {
            controller.onPlayerDelegateBuffering(this);
            Log.d(TAG, "remoteMediaPlayer.seek");
            remoteMediaPlayer.seek(apiClient, positionInMillis).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    controller.onPlayerDelegateReady(GoogleCastDelegate.this);
                }
            });
        } else {
            pendingSeekTo = positionInMillis;
        }
    }

    @Override
    public boolean isPlaying() {
        if (remoteMediaPlayer != null) {
            return internalStatus == MediaStatus.PLAYER_STATE_PLAYING;
        }
        return false;
    }

    @Override
    public void setMuted(boolean muted) {
        if (remoteMediaPlayer != null) {
            remoteMediaPlayer.setStreamMute(apiClient, muted);
        }
    }

    @Override
    public long getCurrentPosition() {
        return remoteMediaPlayer != null ? remoteMediaPlayer.getApproximateStreamPosition() : 0;
    }

    @Override
    public long getDuration() {
        return remoteMediaPlayer != null ? remoteMediaPlayer.getStreamDuration() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public long getBufferPosition() {
        return 0;
    }

    @Override
    public int getVideoSourceHeight() {
        return 0;
    }

    @Override
    public void release() throws IllegalStateException {
        teardown();
        remoteMediaPlayer = null;
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public long getPlaylistStartTime() {
        return 0;
    }

    RemoteMediaPlayer.OnStatusUpdatedListener onStatusUpdateListener = new RemoteMediaPlayer.OnStatusUpdatedListener() {
        @Override
        public void onStatusUpdated() {
            if (remoteMediaPlayer != null) {
                MediaStatus status = remoteMediaPlayer.getMediaStatus();
                if (status != null) {
                    internalStatus = status.getPlayerState();
                }
            }
        }
    };

    private void teardown() {
        Log.d(TAG, "teardown");
        if (apiClient != null) {
            if (mApplicationStarted) {
                if (apiClient.isConnected() || apiClient.isConnecting()) {
                    Cast.CastApi.stopApplication(apiClient, sessionId);
                    apiClient.disconnect();
                }
                mApplicationStarted = false;
                delegateReady = false;
            }
            apiClient = null;
        }
        sessionId = null;
    }

    public void setMediaTitle(String title) {
        this.title = title;
    }

    public void setMediaThumbnailUrl(String mediaThumbnailUrl) {
        this.mediaThumbnailUrl = mediaThumbnailUrl;
    }
}