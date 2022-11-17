package com.pushwoosh.testingapp;

import android.app.Activity;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.core.internal.deps.dagger.internal.Preconditions.checkNotNull;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.core.AllOf.allOf;

/**
 * Custom Espresso methods for android applications
 **/

public class EspressoUtils {

    public static final String SWIPE_RIGHT = "right";
    public static final String SWIPE_LEFT = "left";
    public static final String SWIPE_UP = "up";
    public static final String SWIPE_DOWN = "down";


    /***************************************************************************************
     * Method Name : getListView
     * Arguments :  matcher -  view which you want to match
     * Returns: View - ListView corresponding to the matcher
     * Method Description : Use this method to get a listview corresponding to the matcher
     ***********************************x*****************************************************/
    private static View getListView(Matcher matcher) {
        final ListView[] listView = new ListView[1];
        onView(allOf(matcher, isEnabled(), isClickable())).check(matches(new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                listView[0] = (ListView) view;
                return true;
            }

            @Override
            public void describeTo(Description description) {

            }
        }));
        return listView[0];
    }


    /***************************************************************************************
     * Method Name : getListCount
     * Arguments :  matcher -  view which you want to match
     * Returns: count - Item count of the listview
     * Method Description : Use this method to get the item count of a listview
     ***********************************x*****************************************************/
    public static int getListCount(Matcher matcher) {

        int length;
        View view = getListView(matcher);

        Adapter adapter = ((AdapterView) view).getAdapter();
        length = adapter.getCount();
        Log.i("Size of the List", Integer.toString(length));
        return length;
    }


    /***************************************************************************************
     * Method Name : getRecyclerViewCount
     * Arguments :  matcher -  view which you want to match
     * Returns: count - Item count of the recyclerview
     * Method Description : Use this method to get the item count of a recyclerview
     ***********************************x*****************************************************/
    public static int getRecyclerViewCount(Matcher matcher) {
        final int[] num = new int[1];
        onView(allOf(matcher, isEnabled())).check(matches(new TypeSafeMatcher<View>() {
            RecyclerView recyclerView = null;

            @Override
            public boolean matchesSafely(View view) {
                recyclerView = (RecyclerView) view;
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                Log.d("Booking Recycler Cnt", adapter.getItemCount() + "");
                num[0] = adapter.getItemCount();
                return true;
            }

            @Override
            public void describeTo(Description description) {

            }
        }));
        return num[0];
    }


    /***************************************************************************************
     * Method Name : tapContentText
     * Arguments :  matcher -  view which you want to match
     *              st - text of the view you want to tap
     * Method Description : Use this method to tap a view with the text specified
     ***********************************x*****************************************************/
    public static void tapContentText(Matcher matcher, String St) {
        ViewInteraction treeboButtonClick = onView(
                allOf(matcher,
                        withContentDescription(containsString(St)),
                        isDisplayed()));
        treeboButtonClick.perform(customClick());
    }


    /***************************************************************************************
     * Method Name : isViewEnabled
     * Arguments :  matcher -  view which you want to match
     * Method Description : Use this method to check if a view is enabled
     ***********************************x*****************************************************/
    public static ViewInteraction isViewEnabled(Matcher matcher) {
        return onView(matcher).
                check(ViewAssertions.matches(
                        ViewMatchers.isEnabled())
                );
    }


    /***************************************************************************************
     * Method Name : isTextVisible
     * Arguments :  matcher -  view which you want to match
     *              text - text which you want to check
     * Method Description : Use this method to check if a view with given text is visible
     ***********************************x*****************************************************/
    public static ViewInteraction isTextVisible(Matcher matcher, String text) {
        return onView(matcher).
                check(ViewAssertions.matches(
                        allOf(withText(containsString(text)), isEnabled()))
                );
    }


    /***************************************************************************************
     * Method Name : isViewDisplayed
     * Arguments :  matcher -  view which you want to match
     * Method Description : Use this method to check if a view is displayed
     ***********************************x*****************************************************/
    public static ViewInteraction isViewDisplayed(Matcher matcher) {
        return onView(matcher).
                check(ViewAssertions.matches(
                        ViewMatchers.isDisplayed())
                );
    }


    /***************************************************************************************
     * Method Name : isCheckboxEnabled
     * Arguments :  matcher -  view which you want to match
     * Method Description : Use this method to check if a checkbox is enabled
     ***********************************x*****************************************************/
    public static ViewInteraction isCheckboxEnabled(Matcher matcher) {
        String TAG = "Is Checkbox Enabled?";
        ViewInteraction isCheckboxEnabled = null;
        try {
            isCheckboxEnabled = onView(matcher).
                    check(ViewAssertions.matches(
                            ViewMatchers.isChecked())
                    );
            Log.i(TAG, "True");

        } catch (Exception e) {
            Log.e(TAG, "No");
            throw e;
        }
        return isCheckboxEnabled;
    }


    /***************************************************************************************
     * Method Name : viewWithText
     * Arguments :   matcher -  view which you want to match
     *              String -  text to be searched
     * Returns: ViewInteraction - viewInteraction of the view with the given text
     * Method Description : Use this method to get the ViewInteraction of a view with the given text
     ***********************************x*****************************************************/
    public static ViewInteraction viewWithText(Matcher m, String str) {
        Boolean status = false;
        String TAG = "Text enabled with Matcher?";
        ViewInteraction textView;
        try {
            textView = onView(allOf(m, withText(str), isDisplayed()));
            Log.i(TAG, "True");
            status = true;
            return textView;
        } catch (Exception e) {
            Log.e(TAG, "No");
            throw e;
        } finally {
            if (!status) {
                textView = onView(allOf(m, withText(equalToIgnoringCase(str)), isDisplayed()));
                Log.i(TAG, "True");
                return textView;
            }
        }
    }


    /***************************************************************************************
     * Method Name : tapButtonText
     * Arguments :  matcher -  view which you want to match
     *              text - String which you want to match the view against
     * Method Description : Use this method to get the tap a button with the text specified
     ***********************************x*****************************************************/
    public static ViewInteraction tapButtonText(Matcher matcher, String text) {
        ViewInteraction treeboButtonClick;
        try {
            treeboButtonClick = viewWithText(matcher, text);
            treeboButtonClick.perform(click());
            return treeboButtonClick;
        } catch (Throwable T) {
            T.printStackTrace();
        } finally {
            treeboButtonClick = viewWithText(matcher, text);
            treeboButtonClick.perform(customClick());
            return treeboButtonClick;
        }
    }


    /***************************************************************************************
     * Method Name : tapButton
     * Arguments :  matcher -  view which you want to match
     * Method Description : Use this method to get the tap a button
     ***********************************x*****************************************************/
    public static void tapButton(Matcher matcher) {
        String TAG = "Tap on button?";
        Boolean status = false;
        try {
            EspressoUtils.isViewEnabled(matcher);
            onView(matcher).perform(EspressoUtils.customClick());
            status = true;
            Log.i(TAG, "Tapped button: " + status.toString());
        } catch (Exception e) {
            Log.e(TAG, "Unable to tap on button");
        } finally {
            if (!status) {
                onView(allOf(matcher)).perform(click());
                Log.i(TAG, "Tapped button: " + status.toString());
            }
        }
    }


    /***************************************************************************************
     * Method Name : enterText
     * Arguments :   matcher -  view which you want to match
     *              input - String you want to enter
     * Method Description : Use this method to enter text into a view
     ***********************************x*****************************************************/
    public static void enterText(Matcher matcher, String input) {
        onView(matcher).perform(typeText(input),
                closeSoftKeyboard());
    }


    /***************************************************************************************
     * Method Name : getText
     * Arguments : matcher -  view which you want to match
     * Returns: String: Text of the view passed
     * Method Description : Use this method to get the text from a view
     ***********************************x*****************************************************/
    public static String getText(final Matcher<View> matcher) {
        final String[] stringHolder = {null};
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView) view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    public static ViewAction swipeUpSlow() {
        return new GeneralSwipeAction(Swipe.SLOW, GeneralLocation.BOTTOM_CENTER,
                GeneralLocation.TOP_CENTER, Press.FINGER);
    }

    //Method to enter text
    public static void scroll(Matcher matcher) {
        onView(matcher).perform(scrollTo());
    }

    //Method to swipe right
    public static void swipeToRight(Matcher matcher) {
        onView(matcher).perform(swipeRight());
    }

    //Method to swipe left
    public static void swipeToLeft(Matcher matcher) {
        onView(matcher).perform(swipeLeft());
    }

    //Method to swipe down
    public static void swipeToDown(Matcher matcher) {
        onView(matcher).perform(swipeDown());
    }

    //Method to swipe up
    public static void swipeToTop(Matcher matcher, String... options) {
        if (options == null || options.length == 0)
            onView(matcher).perform(swipeUp());
        else if (options[0] != null && options[0] == "SLOW")
            onView(matcher).perform(swipeUpSlow());
    }

    //Methods for force sleep
    public static void forceWait(long ms) {
        SystemClock.sleep(ms);
    }

    //Custom Click Method
    public static ViewAction customClick() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return any(View.class); // no constraints, they are checked above
            }

            @Override
            public String getDescription() {
                return "custom button click";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
            }
        };
    }


    public static Matcher<View> nthChildOf(final Matcher<View> parentMatcher, final int childPosition) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with " + childPosition + " child view of type parentMatcher");
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof ViewGroup)) {
                    return parentMatcher.matches(view.getParent());
                }

                ViewGroup group = (ViewGroup) view.getParent();
                return parentMatcher.matches(view.getParent()) && group.getChildAt(childPosition).equals(view);
            }
        };
    }

    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }


    /***************************************************************************************
     * Method Name : clickChildViewWithId
     * Arguments :  viewID -  Id of the view you want to click
     * Method Description : Use this method to click a view given its id
     ***********************************x*****************************************************/
    public static ViewAction clickChildViewWithId(final int viewID) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(viewID);
                v.performClick();
            }
        };
    }


    /***************************************************************************************
     * Method Name : clickChildViewWithText
     * Arguments :  listOfViews -  List of Views in which you want to search
     *              str - String you want the view to be checked against with
     * Method Description : Use this method to check if a list of views has a view with the given
     *                  text
     ***********************************x*****************************************************/
    public static ViewAction clickChildViewWithText(final ArrayList<View> listOfViews,
                                                    final CharSequence str) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified text.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.findViewsWithText(listOfViews, str, View.FIND_VIEWS_WITH_TEXT);
                View v = listOfViews.get(0);
                v.performClick();
            }
        };
    }


    /***************************************************************************************
     * Method Name : assertText
     * Arguments :  matcher -  view which you want to match
     *              st - String you want the view to be checked against with
     * Method Description : Use this method to check the text of a view with the required text
     ***********************************x*****************************************************/
    public static void assertText(Matcher matcher, String St) {
        onView(matcher).check(matches(withText(St)));
    }


    /***************************************************************************************
     * Method Name : checkElementAtPosition
     * Arguments :  position - position of the element in the recyclerview
     *              matcher - view which you want to match
     * Method Description : Use this method to check for a particular element in a recyclerview
     ***********************************x*****************************************************/
    public static Matcher<View> checkElementAtPosition(final int position,
                                                       @NonNull final Matcher<View> itemMatcher) {
        checkNotNull(itemMatcher);
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                if (viewHolder == null) {
                    // has no item on such position
                    return false;
                }
                return itemMatcher.matches(viewHolder.itemView);
            }
        };
    }


    /***************************************************************************************
     * Method Name : isViewVisible
     * Arguments :  matcher -  view which you want to match
     * Method Description : Use this method to check if a view is visible
     ***********************************x*****************************************************/
    private static boolean isViewVisible(Matcher matcher) {
        try {
            isViewEnabled(matcher);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /***************************************************************************************
     * Method Name : swipeToElement
     * Arguments :  matcher - view on which swipe is performed (e.g. RecyclerView)
     *              elementToFind - elementToFind is view till which swipe will be performed
     *              swipeDirection - direction in which swipe will be performed (top/down/left/right)
     *              maxNoOfSwipes - no of maximum swipe attempts
     * Method Description : This method swipes to find the final element
     * Returns: true - If the element was found after swiping in the direction specified,
     *          false - if not found
     ****************************************************************************************/
    public static boolean swipeToElement(Matcher matcher, Matcher elementToFind, String swipeDirection, int maxNoOfSwipes) {
        for (int i = 0; i < maxNoOfSwipes; i++) {
            if (isViewVisible(elementToFind)) {
                return true;
            }
            switch (swipeDirection) {
                case SWIPE_LEFT:
                    swipeToLeft(matcher);
                    break;

                case SWIPE_RIGHT:
                    swipeToRight(matcher);
                    break;

                case SWIPE_UP:
                    swipeToTop(matcher, "SLOW");
                    break;

                case SWIPE_DOWN:
                    swipeToDown(matcher);
                    break;
            }
        }
        return false;
    }

    /*
       getActivity() will return the current screen activity
       This method is being used while taking screenshots with spoon plugin or with any other plugin
    */
    public static Activity getActivity() {
        final Activity[] currentActivity = new Activity[1];
        onView(allOf(withId(android.R.id.content), isDisplayed())).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "Retrieving text from view";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view.getContext() instanceof Activity) {
                    Activity finalActivity = ((Activity) view.getContext());
                    currentActivity[0] = finalActivity;
                }
            }
        });
        return currentActivity[0];
    }


    /***************************************************************************************
     * Method Name : waitForElementToLoad
     * Arguments : matcher -  view which you want to match
     *             retryInterval - Frequency of retry
     *             maxWaitTime -  Maximum wait time
     * Method Description : This method waits for an element to load.
     * It will keep on trying to find element after every (retryInterval) ms and
     * will wait for maximum time of (maxWaitTime) ms
     ****************************************************************************************/
    public static void waitForElementToLoad(Matcher matcher, int retryInterval, int maxWaitTime) {
        int count = 0;
        while (true) {
            try {
                EspressoUtils.isViewEnabled(matcher);
                break;
            } catch (Exception e) {
                count += retryInterval;
                if (count >= maxWaitTime) throw e;
                EspressoUtils.forceWait(retryInterval);
            }
        }
    }


    /***************************************************************************************
     * Method Name : replaceText
     * Arguments :  matcher - view which you want to match
     *              replaceString - String you want the view to be updated with
     * Method Description : Use this method to replace the text of a view with the required text
     ***********************************x*****************************************************/
    public static void replaceText(Matcher matcher, String replaceString) {
        isViewEnabled(matcher);
        onView(matcher).
                check(ViewAssertions.matches(
                        ViewMatchers.isEnabled())
                ).perform(ViewActions.replaceText(replaceString));
    }


    /***************************************************************************************
     * Method Name : verifyTextFieldErrorMessage
     * Arguments :  View - edittext to be checked
     *              errorText - Expected error text string
     * Method Description : This method checks if the error text on an edittext is as expected
     * Returns: true - if error text matches expected value,
     *          false - if error text does not match
     ***********************************x*****************************************************/
    public static boolean verifyTextFieldErrorMessage(EditText view,
                                                      final String expectedErrorText) {

        if ((view == null)) {
            return false;
        }
        CharSequence error = view.getError();
        if (error == null) {
            return false;
        }
        String actualErrorText = error.toString();
        return expectedErrorText.equals(actualErrorText);

    }

    /***************************************************************************************
     * Method Name : getErrorText
     * Arguments :  matcher -  view which you want to match
     * Method Description : This method returns the error text of an edittext
     * Returns: String - error text
     ***********************************x*****************************************************/
    public static String getErrorText(final Matcher<View> matcher) {
        final String[] errorText = {null};
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting error message from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                CharSequence error = ((EditText) view).getError();
                String actualErrorText = error.toString();
                errorText[0] = actualErrorText;
            }
        });
        return errorText[0];
    }
}
