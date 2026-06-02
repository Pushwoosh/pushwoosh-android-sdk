package com.pushwoosh.liveupdates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.notification.Action;

import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of a Live Update, parsed from a single push payload.
 * <p>
 * This is the input handed to {@link LiveUpdateProgressStyleProvider#createStyle(LiveUpdateState)}
 * on every {@code start} / {@code update} render, and the value type an integrator reads to drive
 * a custom progress style. It carries the identity and lifecycle of the update
 * ({@link #getActivityId()}, {@link #getOperation()}), its textual content
 * ({@link #getTitle()}, {@link #getSubtitle()}), progress data ({@link #getProgress()},
 * {@link #isProgressIndeterminate()}, {@link #getSegments()}), the large-icon URL, notification
 * actions, the header-time configuration (see the mode tree below), and an arbitrary
 * {@link #getExtras() JSON extras} object that is the primary channel for custom business data.
 * <p>
 * Instances are immutable and built via {@link Builder}; all reference getters are non-null
 * except where annotated {@code @Nullable}.
 *
 * <p>Header-time mode tree — how the four time fields interact. The platform silently
 * ignores meaningless combinations; this is the integrator's contract, not enforced here:
 *
 * <pre>
 * showWhen=false ──────────────► time hidden, the other three are irrelevant
 * showWhen=true (default)
 *   └─ when = anchor (epoch ms)
 *        ├─ chronometer=false (default) ──► static stamp ("14:42" / "5 min ago")
 *        └─ chronometer=true ──► live counter from `when`
 *             ├─ chronometerCountDown=false (default) ──► counts up ("12:34" rising)
 *             └─ chronometerCountDown=true ───────────► counts down ("5:00" falling)
 * </pre>
 */
public final class LiveUpdateState {

    @NonNull private final String activityId;

    @NonNull private final LiveUpdateOperation operation;

    @Nullable private final String title;

    @Nullable private final String subtitle;

    @Nullable private final Integer progress;

    private final boolean progressIndeterminate;

    @NonNull private final List<LiveUpdateSegment> segments;

    @Nullable private final String iconUrl;

    @NonNull private final List<Action> actions;

    @Nullable private final JSONObject extras;

    @Nullable private final Long when;

    private final boolean chronometer;

    private final boolean chronometerCountDown;

    private final boolean showWhen;

    private LiveUpdateState(Builder b) {
        this.activityId = b.activityId;
        this.operation = b.operation;
        this.title = b.title;
        this.subtitle = b.subtitle;
        this.progress = b.progress;
        this.progressIndeterminate = b.progressIndeterminate;
        this.segments = b.segments == null ? Collections.emptyList() : b.segments;
        this.iconUrl = b.iconUrl;
        this.actions = b.actions == null ? Collections.emptyList() : b.actions;
        this.extras = b.extras;
        this.when = b.when;
        this.chronometer = b.chronometer;
        this.chronometerCountDown = b.chronometerCountDown;
        this.showWhen = b.showWhen;
    }

    /** Stable id that ties together all pushes of one Live Update; the notification tag. */
    @NonNull public String getActivityId() {
        return activityId;
    }

    /** Lifecycle operation this push represents ({@code START} / {@code UPDATE} / {@code END}). */
    @NonNull public LiveUpdateOperation getOperation() {
        return operation;
    }

    /** Notification title, or {@code null} if the payload carried none. */
    @Nullable public String getTitle() {
        return title;
    }

    /** Notification body text, or {@code null} if the payload carried none. */
    @Nullable public String getSubtitle() {
        return subtitle;
    }

    /** Progress value against the summed segment lengths, or {@code null} if unspecified. */
    @Nullable public Integer getProgress() {
        return progress;
    }

    /** Whether the progress bar animates indeterminately rather than showing a concrete value. */
    public boolean isProgressIndeterminate() {
        return progressIndeterminate;
    }

    /** Ordered progress segments; empty if none were supplied. See {@link LiveUpdateSegment}. */
    @NonNull public List<LiveUpdateSegment> getSegments() {
        return segments;
    }

    /** URL of the large icon to download and display, or {@code null} if none. */
    @Nullable public String getIconUrl() {
        return iconUrl;
    }

    /** Notification action buttons; empty if none were supplied. */
    @NonNull public List<Action> getActions() {
        return actions;
    }

    /** Arbitrary JSON from the payload, the primary channel for custom data; {@code null} if absent. */
    @Nullable public JSONObject getExtras() {
        return extras;
    }

    /** Header time anchor in epoch ms, or {@code null} to let the system show "now". See the mode tree on {@link LiveUpdateState}. */
    @Nullable public Long getWhen() {
        return when;
    }

    /** Whether the header time ticks as a live chronometer. See the mode tree on {@link LiveUpdateState}. */
    public boolean isChronometer() {
        return chronometer;
    }

    /** Whether a ticking chronometer counts down rather than up. See the mode tree on {@link LiveUpdateState}. */
    public boolean isChronometerCountDown() {
        return chronometerCountDown;
    }

    /** Whether the header time column is shown at all. See the mode tree on {@link LiveUpdateState}. */
    public boolean isShowWhen() {
        return showWhen;
    }

    /**
     * Fluent builder for {@link LiveUpdateState}. {@code activityId} and {@code operation} are
     * required (constructor arguments); every other field is optional and defaults to absent
     * ({@code showWhen} defaults to {@code true}, matching the platform default).
     */
    public static final class Builder {
        @NonNull private final String activityId;

        @NonNull private final LiveUpdateOperation operation;

        @Nullable private String title;

        @Nullable private String subtitle;

        @Nullable private Integer progress;

        private boolean progressIndeterminate;

        @Nullable private List<LiveUpdateSegment> segments;

        @Nullable private String iconUrl;

        @Nullable private List<Action> actions;

        @Nullable private JSONObject extras;

        @Nullable private Long when;

        private boolean chronometer;

        private boolean chronometerCountDown;

        private boolean showWhen = true;

        /**
         * @param activityId stable id tying together all pushes of one Live Update
         * @param operation the lifecycle operation this state represents
         */
        public Builder(@NonNull String activityId, @NonNull LiveUpdateOperation operation) {
            this.activityId = activityId;
            this.operation = operation;
        }

        public Builder title(@Nullable String v) {
            this.title = v;
            return this;
        }

        public Builder subtitle(@Nullable String v) {
            this.subtitle = v;
            return this;
        }

        public Builder progress(@Nullable Integer v) {
            this.progress = v;
            return this;
        }

        public Builder progressIndeterminate(boolean v) {
            this.progressIndeterminate = v;
            return this;
        }

        public Builder segments(@Nullable List<LiveUpdateSegment> v) {
            this.segments = v;
            return this;
        }

        public Builder iconUrl(@Nullable String v) {
            this.iconUrl = v;
            return this;
        }

        public Builder actions(@Nullable List<Action> v) {
            this.actions = v;
            return this;
        }

        public Builder extras(@Nullable JSONObject v) {
            this.extras = v;
            return this;
        }

        public Builder when(@Nullable Long v) {
            this.when = v;
            return this;
        }

        public Builder chronometer(boolean v) {
            this.chronometer = v;
            return this;
        }

        public Builder chronometerCountDown(boolean v) {
            this.chronometerCountDown = v;
            return this;
        }

        public Builder showWhen(boolean v) {
            this.showWhen = v;
            return this;
        }

        /** Builds the immutable {@link LiveUpdateState} from the configured fields. */
        public LiveUpdateState build() {
            return new LiveUpdateState(this);
        }
    }
}
