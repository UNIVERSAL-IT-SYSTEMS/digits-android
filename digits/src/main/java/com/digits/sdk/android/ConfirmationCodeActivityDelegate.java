/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.digits.sdk.android;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ResultReceiver;
import android.widget.EditText;
import android.widget.TextView;

import io.fabric.sdk.android.services.common.CommonUtils;

class ConfirmationCodeActivityDelegate extends DigitsActivityDelegateImpl {
    EditText editText;
    LinkTextView editPhoneNumberLink;
    StateButton stateButton;
    InvertedStateButton resendButton, callMeButton;
    TextView termsText;
    TextView timerText;
    DigitsController controller;
    CountDownTimer timer;
    SmsBroadcastReceiver receiver;
    Activity activity;
    DigitsScribeService scribeService;
    AuthConfig config;

    public ConfirmationCodeActivityDelegate(DigitsScribeService scribeService) {
        this.scribeService = scribeService;
    }

    @Override
    public int getLayoutId() {
        return R.layout.dgts__activity_confirmation;
    }

    @Override
    public boolean isValid(Bundle bundle) {
        return BundleManager.assertContains(bundle, DigitsClient.EXTRA_RESULT_RECEIVER,
                DigitsClient.EXTRA_PHONE);
    }

    @Override
    public void init(Activity activity, Bundle bundle) {
        this.activity = activity;
        editText = (EditText) activity.findViewById(R.id.dgts__confirmationEditText);
        stateButton = (StateButton) activity.findViewById(R.id.dgts__createAccount);
        resendButton =  (InvertedStateButton) activity
                .findViewById(R.id.dgts__resendConfirmationButton);
        callMeButton =  (InvertedStateButton) activity.findViewById(R.id.dgts__callMeButton);
        editPhoneNumberLink = (LinkTextView) activity.findViewById(R.id.dgts__editPhoneNumber);
        termsText = (TextView) activity.findViewById(R.id.dgts__termsTextCreateAccount);
        timerText = (TextView) activity.findViewById(R.id.dgts__countdownTimer);
        config = bundle.getParcelable(DigitsClient.EXTRA_AUTH_CONFIG);

        controller = initController(bundle);
        timer = initCountDownTimer(controller, timerText, resendButton, callMeButton);


        setUpEditText(activity, controller, editText);
        setUpSendButton(activity, controller, stateButton);
        setupResendButton(activity, controller, scribeService, resendButton);
        setupCallMeButton(activity, controller, scribeService, callMeButton, config);
        setupCountDownTimer(timerText, timer, config);
        setUpEditPhoneNumberLink(activity, editPhoneNumberLink,
                bundle.getString(DigitsClient.EXTRA_PHONE));
        setUpTermsText(activity, controller, termsText);
        setUpSmsIntercept(activity, editText);

        CommonUtils.openKeyboard(activity, editText);
    }

    DigitsController initController(Bundle bundle) {
        return new ConfirmationCodeController(
                bundle.<ResultReceiver>getParcelable(DigitsClient.EXTRA_RESULT_RECEIVER),
                stateButton, resendButton, callMeButton, editText,
                bundle.getString(DigitsClient.EXTRA_PHONE), scribeService,
                bundle.getBoolean(DigitsClient.EXTRA_EMAIL));
    }

    @Override
    public void setUpTermsText(Activity activity, DigitsController controller, TextView termsText) {
        termsText.setText(getFormattedTerms(activity, R.string.dgts__terms_text_create));
        super.setUpTermsText(activity, controller, termsText);
    }

    @Override
    public void onResume() {
        scribeService.impression();
        controller.onResume();
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            activity.unregisterReceiver(receiver);
        }
        timer.cancel();
    }

    protected void setUpSmsIntercept(Activity activity, EditText editText) {
        if (CommonUtils.checkPermission(activity, "android.permission.RECEIVE_SMS")) {
            final IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
            receiver = new SmsBroadcastReceiver(editText);
            activity.registerReceiver(receiver, filter);
        }
    }
}
