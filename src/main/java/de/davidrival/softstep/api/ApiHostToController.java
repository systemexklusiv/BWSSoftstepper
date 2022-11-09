package de.davidrival.softstep.api;

import com.bitwig.extension.controller.api.*;
import de.davidrival.softstep.controller.Page;
import lombok.Setter;

import java.util.Timer;
import java.util.TimerTask;

import static de.davidrival.softstep.api.ApiManager.*;
import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.*;
import static de.davidrival.softstep.api.ApiManager.PLAYBACK_EVENT.PLAYBACK_STATE_NOT_KNOWN;
import static de.davidrival.softstep.controller.Page.CLIP_LED_STATES.*;

@Setter
public class ApiHostToController extends SimpleConsolePrinter{

    private final ApiManager api;

    private Timer timer;

    public ApiHostToController(ApiManager apiManager) {
        super(apiManager.getHost());
        this.api = apiManager;

        this.api.getTrackCurser().arm().addValueObserver(this::onArmChanged);
        this.api.getTrackCurser().arm().markInterested();

        this.api.getTrackCurser().mute().addValueObserver(this::onMuteChanged);
        this.api.getTrackCurser().mute().markInterested();

        /* Import or some content updates are not correct */
        runClipCleanupTaskEach(CLIPS_CONTENT_CLEANUP_PERIOD);
    }

    /**
     * checks for content in clip slots infinitly ond if absents sends explicitly a
     * OFF LED at the specific point. This is a fix or sometimes LED get Stuck
     *
     * @param millis time the task repeats
     */
    private void runClipCleanupTaskEach(int millis) {
        timer = new Timer();
        int size = api.getSlotBank().getSizeOfBank();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                p(">>> running cleanup :-)");
                for (int i = 0; i < size; i++) {
                    ClipLauncherSlot clipLauncherSlot = api.getSlotBank().getItemAt(i);
                    if ( !clipLauncherSlot.hasContent().get() ){
                        api.getSoftstepController().updateLedStates(Page.CLIP, i, OFF);
                    }
                }
            }
        }, 5000, millis);
    }

    private void onMuteChanged(boolean onOff) {
//        p("! onMuteChanged: " + onOff);
        api.getSoftstepController().updateLedStates(Page.CLIP, Page.PAD_INDICES.MUTE_PAD, onOff ?
                Page.CHANNEL_LED_STATES.MUTED
                : Page.CHANNEL_LED_STATES.UNMUTED
        );
    }
    private void onArmChanged(boolean onOff) {
//        p("! onArmChanged: " + onOff);
        api.getSoftstepController().updateLedStates(Page.CLIP, Page.PAD_INDICES.ARM_PAD, onOff ?
                Page.CHANNEL_LED_STATES.ARMED
                : Page.CHANNEL_LED_STATES.UNARMED
        );

    }

    public void onContentInSlotBankChanged(int idx, boolean onOff) {
//        p("! content ! slotIdx" + idx + " clip? " + onOff);
        api.getSoftstepController().updateLedStates(Page.CLIP, idx, onOff ? STOP : OFF);
    }

    public void onPlaybackStateChanged(int slotIndex, int index, boolean isQueued) {
//        p("! playbackStateChanged ! slotIndex " + slotIndex + " playbackState " + playbackEvent.toString() + " isQueued " + isQueued);
        ApiManager.PLAYBACK_EVENT playbackEvent = getApiEventByCallbackIndex(index);
        switch (playbackEvent) {
            case STOPPED:
                api.getSoftstepController().updateLedStates(Page.CLIP, slotIndex, isQueued ? STOP_QUE : STOP);
                break;
            case PLAYING:
                api.getSoftstepController().updateLedStates(Page.CLIP, slotIndex, isQueued ? PLAY_QUE : PLAY);
                break;
            case RECORDING:
                api.getSoftstepController().updateLedStates(Page.CLIP, slotIndex, isQueued ? REC_QUE : REC);
                break;
        }
    }

    private PLAYBACK_EVENT getApiEventByCallbackIndex(int playbackState) {
        switch (playbackState) {
            case 0:
                return STOPPED;
            case 1:
                return PLAYING;
            case 2:
                return RECORDING;
            default:
//                e("Unknown state from PlaybackStateObserver with idx: " + playbackState);
                return PLAYBACK_STATE_NOT_KNOWN;
        }
    }

}

