/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.testingapp.layout;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.pushwoosh.testingapp.LogsBuffer;
import com.pushwoosh.testingapp.PlaceholderFragment;
import com.pushwoosh.testingapp.R;

public class LogsFragment extends PlaceholderFragment {
	TextView logsTextView;

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		logsTextView = view.findViewById(R.id.logsTextView);
		view.findViewById(R.id.removeFab).setOnClickListener(view1 -> {
			LogsBuffer.instance.clear();
			logsTextView.setText("");
		});
		LogsBuffer.instance.setTextView(logsTextView);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		LogsBuffer.instance.setTextView(null);
	}
}
