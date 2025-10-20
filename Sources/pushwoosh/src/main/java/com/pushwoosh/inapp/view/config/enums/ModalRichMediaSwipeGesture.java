package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Defines swipe gestures that users can perform to dismiss modal in-app messages.
 * <p>
 * Swipe gestures provide intuitive ways for users to close modal messages without having to
 * find and tap a close button. Multiple gestures can be enabled simultaneously to give users
 * flexible dismissal options.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * // Enable swipe down for top-positioned modals
 * Set<ModalRichMediaSwipeGesture> topModalSwipes = new HashSet<>();
 * topModalSwipes.add(ModalRichMediaSwipeGesture.DOWN);
 * 
 * ModalRichmediaConfig topConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.TOP)
 *     .setSwipeGestures(topModalSwipes);
 * 
 * // Enable multiple swipe directions for maximum flexibility
 * Set<ModalRichMediaSwipeGesture> flexibleSwipes = new HashSet<>();
 * flexibleSwipes.add(ModalRichMediaSwipeGesture.UP);
 * flexibleSwipes.add(ModalRichMediaSwipeGesture.DOWN);
 * flexibleSwipes.add(ModalRichMediaSwipeGesture.LEFT);
 * flexibleSwipes.add(ModalRichMediaSwipeGesture.RIGHT);
 * 
 * ModalRichmediaConfig flexibleConfig = new ModalRichmediaConfig()
 *     .setSwipeGestures(flexibleSwipes);
 * }
 * </pre>
 *
 * @see ModalRichmediaConfig#setSwipeGestures(Set)
 */
public enum ModalRichMediaSwipeGesture {
    /** Swipe upward to dismiss modal, commonly used for bottom-positioned modals */
    UP(0, 1 << 2),
    
    /** Swipe left to dismiss modal, useful for side-by-side content or pagination */
    LEFT(1, 1 << 0),
    
    /** Swipe right to dismiss modal, useful for side-by-side content or pagination */
    RIGHT(2, 1 << 1),
    
    /** Swipe downward to dismiss modal, commonly used for top or center-positioned modals */
    DOWN(3, 1 << 3),
    
    /** No swipe gesture enabled - used internally, not for configuration */
    NONE(4, 0);

    private final int code;
    private final int bit;

    ModalRichMediaSwipeGesture(int code, int bit) {
        this.code = code;
        this.bit = bit;
    }

    public int getCode() {
        return code;
    }

    public int getBit() {
        return bit;
    }

    public int compare(ModalRichMediaSwipeGesture source){
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaSwipeGesture getByCode(int code) {
        for (ModalRichMediaSwipeGesture source : ModalRichMediaSwipeGesture.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }

    public static ModalRichMediaSwipeGesture fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "up": return UP;
            case "left": return LEFT;
            case "right": return RIGHT;
            case "down": return DOWN;
            case "none": return NONE;
            default:
                return null;
        }
    }
}
