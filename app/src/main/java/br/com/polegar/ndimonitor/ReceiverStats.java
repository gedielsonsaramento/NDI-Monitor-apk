package br.com.polegar.ndimonitor;

import org.json.JSONObject;

/** Captured-frame rate is distinct from the source's advertised format rate. */
final class ReceiverStats {
    private long previousFrames = -1, previousAt, lastFrameAt, received, dropped;
    private Double fps;
    private int queue;
    void reset() { previousFrames = -1; previousAt = 0; lastFrameAt = 0; received = 0; dropped = 0; fps = null; queue = 0; }
    void accept(long frames, long drops, int queued, long now) {
        if (frames < 0 || drops < 0 || queued < 0) return;
        if (previousFrames >= 0 && frames >= previousFrames && now > previousAt)
            fps = Math.round((frames - previousFrames) * 100000d / (now - previousAt)) / 100d;
        else fps = null;
        if (frames > 0 && (previousFrames < 0 || frames > previousFrames)) lastFrameAt = now;
        previousFrames = received = frames; previousAt = now; dropped = drops; queue = queued;
    }
    JSONObject json(long now) throws Exception {
        return new JSONObject().put("captured_frames", previousFrames < 0 ? JSONObject.NULL : received)
                .put("dropped_frames", previousFrames < 0 ? JSONObject.NULL : dropped)
                .put("capture_fps", fps == null ? JSONObject.NULL : fps)
                .put("queue_frames", previousFrames < 0 ? JSONObject.NULL : queue)
                .put("last_frame_age_ms", lastFrameAt == 0 ? JSONObject.NULL : Math.max(0, now-lastFrameAt))
                .put("state", lastFrameAt != 0 && now - lastFrameAt < 3500 ? "receiving" : "waiting");
    }
}
