package com.braintreepayments.api.dropin;

import android.app.Activity;
import android.test.FlakyTest;
import android.view.KeyEvent;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;

import java.util.Map;

import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.ui.WaitForActivityHelper.waitForActivity;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;

public class UnsuccessfulResultTest extends BraintreePaymentActivityTestCase {

    private Braintree mBraintree;
    private BraintreePaymentActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        mBraintree = injectBraintree(getInstrumentation().getContext(), clientToken);
        setUpActivityTest(this, clientToken);
        mActivity = getActivity();

        waitForView(withId(R.id.form_header));
    }

    @FlakyTest
    public void testReturnsDeveloperErrorOnAuthenticationException() {
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new AuthenticationException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsDeveloperErrorOnAuthorizationException() {
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new AuthorizationException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsDeveloperErrorOnUpgradeRequiredException() {
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new UpgradeRequiredException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_DEVELOPER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsServerErrorOnServerException() {
        BraintreeTestUtils.postUnrecoverableErrorFromBraintree(mBraintree, new ServerException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsServerUnavailableOnDownForMaintenanceException() {
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new DownForMaintenanceException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_UNAVAILABLE,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsServerErrorOnUnexpectedException() {
        BraintreeTestUtils
                .postUnrecoverableErrorFromBraintree(mBraintree, new UnexpectedException());

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testReturnsUserCanceledOnBackButtonPress() {
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForActivity(mActivity);
        Map<String, Object> result = BraintreeTestUtils.getActivityResult(mActivity);

        assertEquals(Activity.RESULT_CANCELED, result.get("resultCode"));

    }
}
