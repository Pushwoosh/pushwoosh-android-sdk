package com.pushwoosh.sample;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pushwoosh.PushManager;

import java.util.Map;


public class SetTagsActivity extends FragmentActivity implements SendTagsCallBack
{
    private static final String SEND_TAGS_STATUS_FRAGMENT_TAG = "send_tags_status_fragment_tag";

    private TextView mTagsStatus;
    private EditText mIntTags;
    private EditText mStringTags;
    private Button mSubmitTagsButton;

    public class GetTagsListenerImpl implements PushManager.GetTagsListener
    {
        @Override
        public void onTagsReceived(Map<String, Object> tags)
        {
            Log.e("Pushwoosh", "Success: get Tags " + tags.toString());
        }

        @Override
        public void onError(Exception e)
        {
            Log.e("Pushwoosh", "ERROR: get Tags " + e.getMessage());
        }
    }

    GetTagsListenerImpl tagsListener = new GetTagsListenerImpl();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_set_tags);

        mTagsStatus = (TextView) findViewById(R.id.status);
        mIntTags = (EditText) findViewById(R.id.tag_int);
        mStringTags = (EditText) findViewById(R.id.tag_string);

        mSubmitTagsButton = (Button) findViewById(R.id.submit_tags);
        mSubmitTagsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //PushManager.getTagsAsync(SetTagsActivity.this, tagsListener);
                checkAndSendTagsIfWeCan();
            }
        });

        SendTagsFragment sendTagsFragment = getSendTagsFragment();
        mTagsStatus.setText(sendTagsFragment.getSendTagsStatus());
        mSubmitTagsButton.setEnabled(sendTagsFragment.canSendTags());
    }

    @Override
    public void onStatusChange(int sendTagsStatus)
    {
        mTagsStatus.setText(sendTagsStatus);
    }

    @Override
    public void onTaskEnds()
    {
        mSubmitTagsButton.setEnabled(true);
    }

    @Override
    public void onTaskStarts()
    {
        mSubmitTagsButton.setEnabled(false);
    }

    private void checkAndSendTagsIfWeCan()
    {
        SendTagsFragment sendTagsFragment = getSendTagsFragment();

        if (sendTagsFragment.canSendTags())
        {
            sendTagsFragment.submitTags(this, mIntTags.getText().toString().trim(), mStringTags.getText().toString().trim());
        }
    }

    private SendTagsFragment getSendTagsFragment()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        SendTagsFragment sendTagsFragment =
                (SendTagsFragment) fragmentManager.findFragmentByTag(SEND_TAGS_STATUS_FRAGMENT_TAG);

        if (null == sendTagsFragment)
        {
            sendTagsFragment = new SendTagsFragment();
            sendTagsFragment.setRetainInstance(true);
            fragmentManager.beginTransaction().add(sendTagsFragment, SEND_TAGS_STATUS_FRAGMENT_TAG).commit();
            fragmentManager.executePendingTransactions();
        }

        return sendTagsFragment;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        mIntTags = null;
        mStringTags = null;
        mTagsStatus = null;
        mSubmitTagsButton = null;
    }
}
