package com.pushwoosh.inapp.view.config;

import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration class for customizing the presentation and behavior of modal in-app messages.
 * <p>
 * This class allows you to control how modal in-app messages are displayed, including their position,
 * animations, swipe gestures, and visual layout. The configuration can be applied globally using
 * {@link com.pushwoosh.richmedia.RichMediaManager#setDefaultRichMediaConfig(ModalRichmediaConfig)}
 * or specified for individual messages.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Position Control - TOP, CENTER, BOTTOM, or FULLSCREEN presentation</li>
 * <li>Custom Animations - Fade, slide, or drop animations for show/hide</li>
 * <li>Swipe Gestures - Enable swipe-to-dismiss in any direction</li>
 * <li>Layout Options - Control window width and status bar behavior</li>
 * <li>Edge-to-Edge Support - Modern Android layout compatibility</li>
 * </ul>
 * <p>
 * <b>Basic Usage:</b>
 * <pre>
 * {@code
 * // Configure modal presentation globally
 * ModalRichmediaConfig config = new ModalRichmediaConfig()
 *     .setAnimationDuration(500);
 * 
 * // Apply global configuration
 * RichMediaManager.setDefaultRichMediaConfig(config);
 * }
 * </pre>
 * <p>
 * <b>Advanced Configuration:</b>
 * <pre>
 * {@code
 * // E-commerce app with bottom-positioned modals and swipe gestures
 * Set<ModalRichMediaSwipeGesture> swipeGestures = new HashSet<>();
 * swipeGestures.add(ModalRichMediaSwipeGesture.DOWN);
 * swipeGestures.add(ModalRichMediaSwipeGesture.RIGHT);
 * 
 * ModalRichmediaConfig ecommerceConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
 *     .setSwipeGestures(swipeGestures)
 *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
 *     .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
 * 
 * RichMediaManager.setDefaultRichMediaConfig(ecommerceConfig);
 * }
 * </pre>
 * <p>
 * <b>Default Values:</b>
 * <p>When configuration values are not specified (null), the following system defaults are used:
 * <ul>
 * <li><b>View Position:</b> {@link ModalRichMediaViewPosition#FULLSCREEN FULLSCREEN} - modals cover the entire screen</li>
 * <li><b>Present Animation:</b> {@link ModalRichMediaPresentAnimationType#FADE_IN FADE_IN} - smooth fade-in presentation</li>
 * <li><b>Dismiss Animation:</b> {@link ModalRichMediaDismissAnimationType#FADE_OUT FADE_OUT} - smooth fade-out dismissal</li>
 * <li><b>Window Width:</b> {@link ModalRichMediaWindowWidth#FULL_SCREEN FULL_SCREEN} - modals span full screen width</li>
 * <li><b>Animation Duration:</b> 1000ms - balanced speed for smooth animations</li>
 * <li><b>Swipe Gestures:</b> Empty set - no swipe-to-dismiss enabled by default</li>
 * <li><b>Status Bar Covered:</b> false - respects status bar area (applies to API 34 and below where edge-to-edge behavior is not enforced by default)</li>
 * <li><b>Edge-to-Edge Layout:</b> true - uses modern edge-to-edge layout by default (applies to API 35+ where edge-to-edge is the standard)</li>
 * </ul>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>All configuration properties are optional - null values will use system defaults</li>
 * <li>Method chaining is supported for fluent configuration</li>
 * <li>Global configuration affects all subsequent in-app messages</li>
 * <li>Individual messages can override global settings via server-side configuration</li>
 * <li>Status bar and edge-to-edge settings require Android API level considerations</li>
 * </ul>
 *
 * @see com.pushwoosh.richmedia.RichMediaManager#setDefaultRichMediaConfig(ModalRichmediaConfig)
 * @see ModalRichMediaViewPosition
 * @see ModalRichMediaPresentAnimationType
 * @see ModalRichMediaDismissAnimationType
 * @see ModalRichMediaSwipeGesture
 * @see ModalRichMediaWindowWidth
 */
public class ModalRichmediaConfig {
    private ModalRichMediaViewPosition viewPosition = null;
    private Set<ModalRichMediaSwipeGesture> swipeGestures = null;
    private ModalRichMediaPresentAnimationType presentAnimationType = null;
    private ModalRichMediaDismissAnimationType dismissAnimationType = null;
    private ModalRichMediaWindowWidth windowWidth = null;
    private Integer animationDuration = null;
    private Boolean statusBarCovered = null;
    private Boolean respectEdgeToEdgeLayout = null;

    public Boolean isStatusBarCovered() {
        return statusBarCovered;
    }

    /**
     * Sets whether modal in-app messages should cover the status bar area.
     * <p>
     * When enabled, modals can extend into the status bar area for a more immersive
     * experience. This is particularly effective for fullscreen modals or when you
     * want maximum visual impact. Consider the status bar content visibility when using this option.
     * <p>
     * <b>API Level Considerations:</b> This setting primarily applies to API 34 and below, where
     * edge-to-edge behavior is not enforced by default. For API 35+, use 
     * {@link #setRespectEdgeToEdgeLayout(Boolean)} instead.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Immersive fullscreen experience for video or rich content
     * ModalRichmediaConfig immersiveConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.FULLSCREEN)
     *     .setStatusBarCovered(true)
     *     .setRespectEdgeToEdgeLayout(true);
     * 
     * // Standard modal that respects system UI (uses defaults)
     * ModalRichmediaConfig standardConfig = new ModalRichmediaConfig();
     * 
     * // Top-positioned modal that integrates with status bar
     * ModalRichmediaConfig integratedConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.TOP)
     *     .setStatusBarCovered(true);
     * }
     * </pre>
     *
     * @param statusBarCovered true to cover the status bar, false to respect it, defaults to false if not specified
     * @return this configuration instance for method chaining
     */
    public ModalRichmediaConfig setStatusBarCovered(Boolean statusBarCovered) {
        this.statusBarCovered = statusBarCovered;
        return this;
    }

    public Boolean shouldRespectEdgeToEdgeLayout() {
        return respectEdgeToEdgeLayout;
    }

    /**
     * Sets whether modal in-app messages should respect edge-to-edge layout in modern Android versions.
     * <p>
     * Edge-to-edge layout extends content to the edges of the screen, including areas behind
     * the status bar and navigation bar. When enabled, modals integrate better
     * with modern Android design guidelines and provide more immersive experiences.
     * <p>
     * <b>API Level Considerations:</b> This setting is particularly relevant for API 35+ where
     * edge-to-edge layout is enforced by default. For API 34 and below, consider using
     * {@link #setStatusBarCovered(Boolean)} for similar visual control.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Modern edge-to-edge layout for API 35+ (uses defaults: FULLSCREEN + edge-to-edge)
     * ModalRichmediaConfig modernConfig = new ModalRichmediaConfig();
     * 
     * // Legacy approach for API 34 and below using statusBarCovered
     * ModalRichmediaConfig legacyConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.FULLSCREEN)
     *     .setStatusBarCovered(true)
     *     .setRespectEdgeToEdgeLayout(false);
     * 
     * // Adaptive approach - works across API levels (uses defaults)
     * ModalRichmediaConfig adaptiveConfig = new ModalRichmediaConfig();
     * }
     * </pre>
     *
     * @param respectEdgeToEdgeLayout true to enable edge-to-edge layout, false to use traditional layout, defaults to true if not specified
     * @return this configuration instance for method chaining
     */
    public ModalRichmediaConfig setRespectEdgeToEdgeLayout(Boolean respectEdgeToEdgeLayout) {
        this.respectEdgeToEdgeLayout = respectEdgeToEdgeLayout;
        return this;
    }

    public ModalRichmediaConfig() {
    }

    /**
     * Sets the animation type used when dismissing modal in-app messages.
     * <p>
     * The dismissal animation provides visual feedback and smooth transition when users
     * close modals. Matching the dismiss animation with the present animation creates
     * a cohesive user experience.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Matching fade animations for smooth experience (uses defaults)
     * ModalRichmediaConfig fadeConfig = new ModalRichmediaConfig();
     * 
     * // Slide animations for directional feedback
     * ModalRichmediaConfig slideConfig = new ModalRichmediaConfig()
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
     *     .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN);
     * 
     * // Quick dismissal for urgent messages
     * ModalRichmediaConfig quickConfig = new ModalRichmediaConfig()
     *     .setDismissAnimationType(ModalRichMediaDismissAnimationType.NONE)
     *     .setAnimationDuration(0);
     * }
     * </pre>
     *
     * @param dismissAnimationType the animation type for modal dismissal, defaults to FADE_OUT if not specified
     * @return this configuration instance for method chaining
     * @see ModalRichMediaDismissAnimationType
     */
    public ModalRichmediaConfig setDismissAnimationType(ModalRichMediaDismissAnimationType dismissAnimationType) {
        this.dismissAnimationType = dismissAnimationType;
        return this;
    }

    /**
     * Sets the animation type used when presenting modal in-app messages.
     * <p>
     * The presentation animation creates visual continuity and draws attention to the modal
     * when it appears. Different animations work better with different modal positions and
     * app contexts.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Subtle fade-in for professional/business apps (uses default FADE_IN)
     * ModalRichmediaConfig businessConfig = new ModalRichmediaConfig()
     *     .setAnimationDuration(300);
     * 
     * // Dynamic slide-up for bottom-positioned modals
     * ModalRichmediaConfig dynamicConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP);
     * 
     * // Eye-catching drop-down for notifications/alerts
     * ModalRichmediaConfig alertConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.TOP)
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.DROP_DOWN);
     * 
     * // No animation for immediate display
     * ModalRichmediaConfig instantConfig = new ModalRichmediaConfig()
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.NONE);
     * }
     * </pre>
     *
     * @param presentAnimationType the animation type for modal presentation, defaults to FADE_IN if not specified
     * @return this configuration instance for method chaining
     * @see ModalRichMediaPresentAnimationType
     */
    public ModalRichmediaConfig setPresentAnimationType(ModalRichMediaPresentAnimationType presentAnimationType) {
        this.presentAnimationType = presentAnimationType;
        return this;
    }


    /**
     * Sets the swipe gestures that users can perform to dismiss modal in-app messages.
     * <p>
     * This enables users to dismiss modals by swiping in the specified directions, improving
     * the user experience and making dismissal more intuitive. Multiple swipe directions can
     * be enabled simultaneously.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Enable swipe down to dismiss (common for top/center modals)
     * Set<ModalRichMediaSwipeGesture> basicSwipes = new HashSet<>();
     * basicSwipes.add(ModalRichMediaSwipeGesture.DOWN);
     * 
     * ModalRichmediaConfig config = new ModalRichmediaConfig()
     *     .setSwipeGestures(basicSwipes);
     * 
     * // Enable multiple swipe directions for flexible dismissal
     * Set<ModalRichMediaSwipeGesture> allSwipes = new HashSet<>();
     * allSwipes.add(ModalRichMediaSwipeGesture.UP);
     * allSwipes.add(ModalRichMediaSwipeGesture.DOWN);
     * 
     * ModalRichmediaConfig flexibleConfig = new ModalRichmediaConfig()
     *     .setSwipeGestures(allSwipes);
     * 
     * // Disable all swipe gestures (users must tap close button)
     * ModalRichmediaConfig noSwipeConfig = new ModalRichmediaConfig()
     *     .setSwipeGestures(null);
     * }
     * </pre>
     *
     * @param gestures set of swipe gestures to enable, defaults to no swipe gestures if not specified (users must use close button)
     * @return this configuration instance for method chaining
     * @see ModalRichMediaSwipeGesture
     */
    public ModalRichmediaConfig setSwipeGestures(Set<ModalRichMediaSwipeGesture> gestures) {
        if (gestures == null) {
            this.swipeGestures = null;
        } else {
            this.swipeGestures = new HashSet<>(gestures);
            this.swipeGestures.remove(ModalRichMediaSwipeGesture.NONE);
        }
        return this;
    }

    /**
     * Sets the position where modal in-app messages will be displayed on screen.
     * <p>
     * This controls the vertical placement and sizing behavior of modal messages.
     * Position affects how users interact with the modal and the overall user experience.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Shopping app with promotional modals at the top
     * ModalRichmediaConfig config = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.TOP);
     * 
     * // News app with full-screen article promotions (uses default FULLSCREEN)
     * ModalRichmediaConfig newsConfig = new ModalRichmediaConfig();
     * 
     * // Social app with centered engagement modals
     * ModalRichmediaConfig socialConfig = new ModalRichmediaConfig()
     *     .setViewPosition(ModalRichMediaViewPosition.CENTER);
     * }
     * </pre>
     *
     * @param viewPosition the position where modals should appear (TOP, CENTER, BOTTOM, FULLSCREEN), defaults to FULLSCREEN if not specified
     * @return this configuration instance for method chaining
     * @see ModalRichMediaViewPosition
     */
    public ModalRichmediaConfig setViewPosition(ModalRichMediaViewPosition viewPosition) {
        this.viewPosition = viewPosition;
        return this;
    }

    /**
     * Sets the window width behavior for modal in-app messages.
     * <p>
     * This controls how the modal's width is calculated, affecting its visual impact
     * and integration with your app's layout. The choice between full screen and
     * wrap content affects readability and user attention.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Full-screen modals for maximum impact (e.commerce promotions, uses default FULLSCREEN)
     * ModalRichmediaConfig promoConfig = new ModalRichmediaConfig();
     * 
     * // Wrap content for compact messages (quick tips, alerts)
     * ModalRichmediaConfig compactConfig = new ModalRichmediaConfig()
     *     .setWindowWidth(ModalRichMediaWindowWidth.WRAP_CONTENT)
     *     .setViewPosition(ModalRichMediaViewPosition.CENTER);
     * }
     * </pre>
     *
     * @param windowWidth the width behavior for modal windows, defaults to FULL_SCREEN if not specified
     * @return this configuration instance for method chaining
     * @see ModalRichMediaWindowWidth
     */
    public ModalRichmediaConfig setWindowWidth(ModalRichMediaWindowWidth windowWidth) {
        this.windowWidth = windowWidth;
        return this;
    }

    public ModalRichMediaViewPosition getViewPosition() {
        return viewPosition;
    }


    public Set<ModalRichMediaSwipeGesture> getSwipeGestures() {
        return swipeGestures == null ? Collections.emptySet() : Collections.unmodifiableSet(swipeGestures);
    }

    public ModalRichMediaPresentAnimationType getPresentAnimationType() {
        return presentAnimationType;
    }

    public ModalRichMediaDismissAnimationType getDismissAnimationType() {
        return dismissAnimationType;
    }

    public ModalRichMediaWindowWidth getWindowWidth() {
        return windowWidth;
    }

    public Integer getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Sets the duration in milliseconds for modal presentation and dismissal animations.
     * <p>
     * Animation duration affects the perceived responsiveness and polish of modal interactions.
     * Shorter durations feel snappier, while longer durations can be more noticeable but may
     * feel slow if overused.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * // Quick animations for frequent interactions (300ms, uses default FADE animations)
     * ModalRichmediaConfig snappyConfig = new ModalRichmediaConfig()
     *     .setAnimationDuration(300);
     * 
     * // Smooth animations for important messages (500ms)
     * ModalRichmediaConfig smoothConfig = new ModalRichmediaConfig()
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
     *     .setAnimationDuration(500);
     * 
     * // Dramatic animations for special promotions (800ms)
     * ModalRichmediaConfig dramaticConfig = new ModalRichmediaConfig()
     *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.DROP_DOWN)
     *     .setAnimationDuration(800);
     * 
     * // Instant display with no animation delay
     * ModalRichmediaConfig instantConfig = new ModalRichmediaConfig()
     *     .setAnimationDuration(0);
     * }
     * </pre>
     *
     * @param animationDuration animation duration in milliseconds, defaults to 1000ms if not specified
     * @return this configuration instance for method chaining
     */
    public ModalRichmediaConfig setAnimationDuration(Integer animationDuration) {
        this.animationDuration = animationDuration;
        return this;
    }
}
