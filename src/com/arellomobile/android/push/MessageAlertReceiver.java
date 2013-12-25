package com.arellomobile.android.push;

import com.amazon.device.messaging.ADMMessageReceiver;

/**
 * The MessageAlertReceiver class listens for messages from ADM and forwards them to the
 * SampleADMMessageHandler class.
 *
 * Date: 14.08.13
 * Time: 15:10
 *
 * @author Yuri Shmakov
 */
public class MessageAlertReceiver extends ADMMessageReceiver
{
	/** {@inheritDoc} */
	public MessageAlertReceiver()
	{
		super(PushAmazonIntentService.class);
	}
}
