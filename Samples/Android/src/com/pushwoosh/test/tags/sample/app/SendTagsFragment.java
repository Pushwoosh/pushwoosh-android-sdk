package com.pushwoosh.test.tags.sample.app;

import java.util.HashMap;
import java.util.Map;

import com.pushwoosh.PushManager;
import com.pushwoosh.SendPushTagsCallBack;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;

/**
 * Date: 01.11.12
 * Time: 11:37
 *
 * @author MiG35
 */
public class SendTagsFragment extends Fragment implements SendPushTagsCallBack
{
	private final Object mSyncObject = new Object();

	private int mSendTagsStatus = R.string.status_ready;
	private AsyncTask<Void, Void, Void> mTask;

	public SendTagsFragment()
	{
	}

	public boolean canSendTags()
	{
		synchronized (mSyncObject)
		{
			return mTask == null;
		}
	}

	public void submitTags(final Context context, String tagInt, String tagString)
	{
		synchronized (mSyncObject)
		{
			if (!canSendTags())
			{
				return;
			}

			if (!goodAllInputData(tagInt, tagString))
			{
				return;
			}

			mSendTagsStatus = R.string.status_started;
			transfareTaskStartsToActivity();

			final Map<String, Object> tags = generateTags(tagInt, tagString);

			mTask = new AsyncTask<Void, Void, Void>()
			{
				@Override
				protected Void doInBackground(Void... params)
				{
					PushManager.sendTags(context, tags, SendTagsFragment.this);
					return null;
				}
			};
			mTask.execute((Void) null);
		}
	}

	public int getSendTagsStatus()
	{
		synchronized (mSyncObject)
		{
			return mSendTagsStatus;
		}
	}

	@Override
	public void taskStarted()
	{
		synchronized (mSyncObject)
		{
			mSendTagsStatus = R.string.status_started;
			transfareStatusToActivity();
		}
	}

	@Override
	public void onSentTagsSuccess(Map<String, String> stringStringMap)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable() {
		    @Override
		    public void run() {
				synchronized (mSyncObject)
				{
					mSendTagsStatus = R.string.status_success;
					mTask = null;
					transfareStatusToActivity();
					transfareTaskEndsToActivity();
				}
		    }
		});
	}

	@Override
	public void onSentTagsError(final Exception e)
	{
		new Handler(Looper.getMainLooper()).post(new Runnable() {
		    @Override
		    public void run() {
				synchronized (mSyncObject)
				{
					mSendTagsStatus = R.string.status_error;
					if (null != e)
					{
						e.printStackTrace();
					}
					mTask = null;
					transfareStatusToActivity();
					transfareTaskEndsToActivity();
				}
		    }
		});
	}

	private boolean goodAllInputData(String tagInt, String tagString)
	{
		if (tagInt.length() == 0 && tagString.length() == 0)
		{
			mSendTagsStatus = R.string.status_init_error;
			transfareStatusToActivity();
			transfareTaskEndsToActivity();
			return false;
		}
		if (tagInt.length() != 0)
		{
			try
			{
				Integer.parseInt(tagInt);
			}
			catch (Exception e)
			{
				mSendTagsStatus = R.string.status_int_parse_error;
				transfareStatusToActivity();
				transfareTaskEndsToActivity();
				return false;
			}
		}
		return true;
	}

	private void transfareTaskStartsToActivity()
	{
		SendTagsCallBack sendTagsCallBack = (SendTagsCallBack) getActivity();
		if (null != sendTagsCallBack)
		{
			sendTagsCallBack.onTaskStarts();
		}
	}

	private void transfareTaskEndsToActivity()
	{
		SendTagsCallBack sendTagsCallBack = (SendTagsCallBack) getActivity();
		if (null != sendTagsCallBack)
		{
			sendTagsCallBack.onTaskEnds();
		}
	}

	private void transfareStatusToActivity()
	{
		SendTagsCallBack sendTagsCallBack = (SendTagsCallBack) getActivity();
		if (null != sendTagsCallBack)
		{
			sendTagsCallBack.onStatusChange(mSendTagsStatus);
		}
	}

	private Map<String, Object> generateTags(String tagInt, String tagString)
	{
		Map<String, Object> tags = new HashMap<String, Object>();

		if (tagInt.length() != 0)
		{
			tags.put("FavNumber", Integer.parseInt(tagInt));
		}
		
		//incremental tag using string syntax
		//tags.put("price", "#pwinc#-5");
		
		if (tagString.length() != 0)
		{
			tags.put("Alias", tagString);
		}
		
		//Java style incremental tag
		tags.put("price", PushManager.incrementalTag(5));

		return tags;
	}
}
