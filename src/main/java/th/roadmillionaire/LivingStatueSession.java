package th.roadmillionaire;

import java.util.Random;

final class LivingStatueSession {
    static final long STARTING_SWEEP_LENGTH_MS = 2_000L;
    static final long MIN_SWEEP_LENGTH_MS = 650L;
    static final long SWEEP_SPEED_STEP_MS = 110L;
    private static final long CUE_GRACE_MS = 300L;

    private final Random random;
    private int hitCount;
    private int reward;
    private int poseIndex;
    private double target;
    private long roundStartedAtMs;
    private boolean complete;
    private String latestFeedback = "รอจังหวะนักท่องเที่ยวถ่ายรูป";

    LivingStatueSession(long startedAtMs, Random random) {
        this.random = random;
        this.roundStartedAtMs = startedAtMs;
        this.poseIndex = 1 + random.nextInt(8);
        this.target = nextTarget();
    }

    int roundNumber() {
        return hitCount + 1;
    }

    boolean isComplete() {
        return complete;
    }

    int poseIndex() {
        return poseIndex;
    }

    int hitCount() {
        return hitCount;
    }

    int reward() {
        return reward;
    }

    String latestFeedback() {
        return latestFeedback;
    }

    double target() {
        return target;
    }

    double marker(long nowMs) {
        long inRound = Math.max(0L, nowMs - roundStartedAtMs);
        return Math.min(1.0, inRound / (double) sweepLengthMs());
    }

    void pressPose(long nowMs) {
        if (complete) return;
        if (nowMs - roundStartedAtMs >= cueLengthMs()) {
            advanceTimedOutRounds(nowMs);
            return;
        }

        double accuracy = Math.abs(marker(nowMs) - target);
        if (accuracy <= 0.10) {
            hitCount++;
            int tip = Math.min(hitCount, 5) * 10;
            reward += tip;
            latestFeedback = "กดถูก! ได้รับทิป " + tip + " บาท";
            poseIndex = nextPose(poseIndex);
            target = nextTarget();
            roundStartedAtMs = nowMs;
        } else {
            complete = true;
            latestFeedback = "จบการแสดง";
        }
    }

    void advanceTimedOutRounds(long nowMs) {
        if (!complete && nowMs - roundStartedAtMs >= cueLengthMs()) {
            complete = true;
            latestFeedback = "จบการแสดง";
        }
    }

    private long sweepLengthMs() {
        return Math.max(MIN_SWEEP_LENGTH_MS,
                STARTING_SWEEP_LENGTH_MS - (long) hitCount * SWEEP_SPEED_STEP_MS);
    }

    private long cueLengthMs() {
        return sweepLengthMs() + CUE_GRACE_MS;
    }

    private double nextTarget() {
        return 0.22 + random.nextDouble() * 0.56;
    }

    private int nextPose(int currentPose) {
        int next = 1 + random.nextInt(7);
        if (next >= currentPose) next++;
        return next;
    }
}
