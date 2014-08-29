package com.braintreepayments.api.dropin;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.test.FlakyTest;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeApi;
import com.braintreepayments.api.BraintreeTestUtils;
import com.braintreepayments.api.FixturesHelper;
import com.braintreepayments.api.TestClientTokenBuilder;
import com.braintreepayments.api.dropin.view.BraintreeEditText;
import com.braintreepayments.api.dropin.view.LoadingHeader;
import com.braintreepayments.api.dropin.view.LoadingHeader.HeaderState;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PaymentMethod;
import com.braintreepayments.api.models.PaymentMethod.Builder;
import com.google.android.apps.common.testing.ui.espresso.NoMatchingViewException;
import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;
import com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.Visibility;
import com.google.common.base.Optional;

import java.util.Map;

import static com.braintreepayments.api.BraintreeTestUtils.assertBitmapsEqual;
import static com.braintreepayments.api.BraintreeTestUtils.assertSelectedPaymentMethodIs;
import static com.braintreepayments.api.BraintreeTestUtils.injectBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.injectBraintreeApi;
import static com.braintreepayments.api.BraintreeTestUtils.injectGeneric422ErrorOnCardCreateBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.injectSlowBraintree;
import static com.braintreepayments.api.BraintreeTestUtils.setUpActivityTest;
import static com.braintreepayments.api.ui.Matchers.withHint;
import static com.braintreepayments.api.ui.RotationHelper.rotateToLandscape;
import static com.braintreepayments.api.ui.RotationHelper.rotateToPortrait;
import static com.braintreepayments.api.ui.ViewHelper.TEN_SECONDS;
import static com.braintreepayments.api.ui.ViewHelper.waitForKeyboardToClose;
import static com.braintreepayments.api.ui.ViewHelper.waitForView;
import static com.braintreepayments.api.ui.WaitForActivityHelper.waitForActivity;
import static com.braintreepayments.api.utils.PaymentFormHelpers.fillInPayPal;
import static com.braintreepayments.api.utils.PaymentFormHelpers.onAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForAddPaymentFormHeader;
import static com.braintreepayments.api.utils.PaymentFormHelpers.waitForPaymentMethodList;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.clearText;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.closeSoftKeyboard;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDescendantOfA;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isEnabled;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreatePaymentMethodTest extends BraintreePaymentActivityTestCase {

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
            rotateToPortrait(this);
        }
    }

    private void assertCardFormCreatesAPaymentMethod() {
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText("4111111111111111"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("CVV"))
                .perform(typeText("123"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withId(R.id.card_form_complete_button)).perform(click());

        waitForView(withId(R.id.header_status_icon));

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.SUCCESS, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));

        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    @FlakyTest
    public void testCardFormCreatesAPaymentMethodWithoutACustomer() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withoutCustomer().withPayPal().build());
        assertCardFormCreatesAPaymentMethod();
    }

    @FlakyTest
    public void testCardFormCreatesAPaymentMethodWithACustomer() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this);
        assertCardFormCreatesAPaymentMethod();
    }

    @FlakyTest
    public void testCardFormCreatesAPaymentMethodInLandscape() {
        BraintreeTestUtils.setUpActivityTest(this);
        rotateToLandscape(this);
        assertCardFormCreatesAPaymentMethod();
    }

    @FlakyTest
    public void testCardFormCreatesAPaymentMethodWithoutCvvOrPostalCode() {
        String clientToken = new TestClientTokenBuilder()
                .withoutCvvChallenge()
                .withoutPostalCodeChallenge()
                .build();
        injectBraintree(getInstrumentation().getContext(), clientToken);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(R.id.card_form_postal_code)).check(matches(not(isDisplayed())));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    @FlakyTest
    public void testCvvHintsShowAndDisappearOnClick() throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this);
        final BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("CVV"))
                .check(thereIsNoIconHint())
                .perform(click())
                .check(theIconHintIs(R.drawable.ic_cvv_highlighted))
                .perform(typeText("123"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withHint("CVV"))
                .check(thereIsNoIconHint()); // check that the hint is gone after defocusing
    }

    private void assertCreatePaymentMethodFromPayPal() {
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        fillInPayPal();

        onView(withId(R.id.payment_method_description)).check(
                matches(withText("bt_buyer_us@paypal.com")));
        onView(withId(R.id.select_payment_method_button)).perform(click());

        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        PaymentMethod paymentMethod =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(paymentMethod.getNonce());
    }

    @FlakyTest
    public void testPayPalCreatesAPaymentMethodWithACustomer() {
        BraintreeTestUtils.setUpActivityTest(this);
        assertCreatePaymentMethodFromPayPal();
    }

    @FlakyTest
    public void testPayPalCreatesAPaymentMethodWithoutACustomer() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withoutCustomer().withPayPal().build());
        assertCreatePaymentMethodFromPayPal();
    }

    @FlakyTest
    public void testReturnsToSelectPaymentMethodViewAfterAddingAPayPalAccount() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForView(withId(R.id.paypal_appswitch_button)).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForPaymentMethodList(TEN_SECONDS);
        assertSelectedPaymentMethodIs(R.string.descriptor_paypal);
    }

    @FlakyTest
    public void testDisplaysLoadingViewWhileCreatingAPayPalAccount() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 2000);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForView(withId(R.id.paypal_appswitch_button), 8000).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());

        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForView(allOf(withId(R.id.loading_spinner), isDescendantOfA(withId(R.id.inflated_loading_view))))
                .check(matches(isDisplayed()));
        onView(withId(R.id.form_header)).check(matches(not(isDisplayed())));

        waitForPaymentMethodList().check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testDisablesSubmitButtonWhileCreatingPaymentMethod() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 200);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        Button submitButton = (Button) activity.findViewById(R.id.card_form_complete_button);
        assertFalse(submitButton.isEnabled());

        onView(withId(R.id.card_form_card_number)).perform(click());
        assertFalse(submitButton.isEnabled());

        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        PaymentMethod response =
                (PaymentMethod) ((Intent) result.get("resultData")).getSerializableExtra(
                        BraintreePaymentActivity.EXTRA_PAYMENT_METHOD);

        assertEquals(Activity.RESULT_OK, result.get("resultCode"));
        assertNotNull(response.getNonce());
        assertEquals("11", ((Card) response).getLastTwo());
    }

    @FlakyTest
    public void testReEnablesSubmitButtonIfThereAreValidationErrorsForThePaymentMethod() {
        String clientToken = new TestClientTokenBuilder().withoutPostalCodeChallenge().withCvvVerification().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 1000);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();

        onView(withId(R.id.card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withId(R.id.card_form_expiration)).perform(typeText("12/19"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_cvv)).perform(typeText("200"));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        onView(withId(R.id.card_form_complete_button)).check(matches(not(isEnabled())));
        waitForView(withId(R.id.card_form_complete_button), isEnabled());
        onView(withId(R.id.card_form_complete_button)).check(matches(isEnabled()));
    }

    @FlakyTest
    public void testPayPalButtonIsNotShownIfPayPalIsNotSupported() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.paypal_appswitch_button))
                .check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    @FlakyTest
    public void testBackButtonExitsTheActivityIfThereAreNoPaymentMethodsToSelectFrom() {
        BraintreeTestUtils.setUpActivityTest(this);
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();
        onAddPaymentFormHeader().check(matches(isDisplayed()));
        assertFalse(activity.isFinishing());
        sendKeys(KeyEvent.KEYCODE_BACK);
        assertTrue(activity.isFinishing());
    }

    @FlakyTest
    public void testBackButtonInPayPalTakesYouBackToAddPaymentMethodView() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.paypal_appswitch_button)).perform(click());
        waitForView(withHint("Email")).check(matches(isDisplayed())).perform(closeSoftKeyboard(),
                waitForKeyboardToClose());
        sendKeys(KeyEvent.KEYCODE_BACK);

        waitForAddPaymentFormHeader().check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testBackButtonDuringCreditCardAddDoesNothing() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 2000);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")).perform(typeText("123"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testBackButtonDuringPayPalAddDoesNothing() {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        injectSlowBraintree(getInstrumentation().getContext(), clientToken, 2000);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.paypal_appswitch_button)).perform(click());

        waitForView(withHint("Email"));
        onView(withHint("Email")).perform(typeText("bt_buyer_us@paypal.com"));
        onView(withHint("Password")).perform(typeText("11111111"));
        onView(withText("Log In")).perform(click());
        waitForView(withText("Agree"));
        onView(withText("Agree")).perform(click());

        waitForView(allOf(withId(R.id.loading_spinner),
                isDescendantOfA(withId(R.id.inflated_loading_view))))
                .check(matches(isDisplayed()));
        sendKeys(KeyEvent.KEYCODE_BACK);
        onView(allOf(withId(R.id.loading_spinner),
                isDescendantOfA(withId(R.id.inflated_loading_view))))
                .check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testUpButtonIsNotShownIfThereAreNoPaymentMethods() {
        BraintreeTestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertFalse("Expected up not to be present on action bar", checkHomeAsUpEnabled(activity));
    }

    @FlakyTest
    public void testUpButtonIsShownAfterYouAddAPaymentMethod() {
        BraintreeTestUtils.setUpActivityTest(this);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        fillInPayPal();

        assertFalse("Expected up not to be present on action bar", checkHomeAsUpEnabled(activity));
        onView(withId(R.id.change_payment_method_link)).perform(click());

        assertTrue("Expected up to be present on action bar", checkHomeAsUpEnabled(activity));
    }

    @FlakyTest
    public void testCorrectCardHintsAreDisplayed() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();

        waitForView(withHint("Card Number"))
                .check(theIconHintIs(R.drawable.ic_card_highlighted))
                .perform(typeText("4")).check(theIconHintIs(R.drawable.ic_visa))
                .perform(clearText())
                .perform(typeText("51")).check(theIconHintIs(R.drawable.ic_mastercard))
                .perform(clearText())
                .perform(typeText("37")).check(theIconHintIs(R.drawable.ic_amex))
                .perform(clearText())
                .perform(typeText("35")).check(theIconHintIs(R.drawable.ic_jcb))
                .perform(clearText())
                .perform(typeText("5018")).check(theIconHintIs(R.drawable.ic_maestro))
                .perform(clearText())
                .perform(typeText("1234"))
                .check(theIconHintIs(R.drawable.ic_card_highlighted));
    }

    @FlakyTest
    public void testDisplaysAnErrorWhenCardNumberFailsOnServer()
            throws BraintreeException, ErrorWithResponse {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi expirationFailBraintreeApi = mock(BraintreeApi.class);
        when(expirationFailBraintreeApi.create((Builder) anyObject())).thenThrow(new ErrorWithResponse(422,
                FixturesHelper.stringFromFixture(getInstrumentation().getContext(), "errors/card_number_error_response.json")));
        injectBraintreeApi(clientToken, expirationFailBraintreeApi);

        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForAddPaymentFormHeader();
        onView(withId(R.id.card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withId(R.id.card_form_expiration)).perform(typeText("0719"));

        onView(withId(R.id.card_form_complete_button)).perform(click());

        waitForView(withId(R.id.card_form_complete_button), isEnabled());

        BraintreeEditText editText = (BraintreeEditText) activity.findViewById(R.id.card_form_card_number);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testDisplaysAnErrorWhenExpirationFailsOnServer()
            throws ErrorWithResponse, BraintreeException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeApi expirationFailBraintreeApi = mock(BraintreeApi.class);
        when(expirationFailBraintreeApi.create((Builder) anyObject())).thenThrow(new ErrorWithResponse(422,
                FixturesHelper.stringFromFixture(getInstrumentation().getContext(), "errors/expiration_date_error_response.json")));
        injectBraintreeApi(clientToken, expirationFailBraintreeApi);

        BraintreeTestUtils.setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withId(R.id.card_form_expiration)).perform(typeText("08/15"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_complete_button)).perform(click());

        waitForView(withId(R.id.card_form_complete_button), isEnabled());

        BraintreeEditText editText = (BraintreeEditText) activity.findViewById(R.id.card_form_expiration);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testDisplaysAnErrorWhenPostalCodeFailsOnServer() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withPostalCodeVerification().build());
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withId(R.id.card_form_expiration)).perform(typeText("08/15"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_cvv)).perform(typeText("123"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withId(R.id.card_form_postal_code)).perform(typeText("20000"));
        onView(withId(R.id.card_form_complete_button)).perform(click());

        waitForView(withId(R.id.card_form_complete_button), isEnabled());

        BraintreeEditText editText = (BraintreeEditText) activity.findViewById(R.id.card_form_postal_code);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testReturnsUnexpectedErrorWhenServerReturnsNonCreditCardError() {
        String clientToken = new TestClientTokenBuilder().build();
        injectGeneric422ErrorOnCardCreateBraintree(getInstrumentation().getContext(), clientToken);
        setUpActivityTest(this, clientToken);
        BraintreePaymentActivity activity = getActivity();

        waitForView(withId(R.id.card_form_card_number)).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")) .perform(typeText("123"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("12345"));

        onView(withId(R.id.card_form_complete_button)).perform(click());
        waitForActivity(activity);

        Map<String, Object> result = BraintreeTestUtils.getActivityResult(activity);
        assertEquals(BraintreePaymentActivity.BRAINTREE_RESULT_SERVER_ERROR,
                result.get("resultCode"));
    }

    @FlakyTest
    public void testSubmitButtonIsEnabledWhenCardFormIsEntered() {
        BraintreeTestUtils.setUpActivityTest(this);
        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.card_form_card_number)).perform(typeText("1"));
        onView(withId(R.id.card_form_complete_button)).check(matches(isEnabled()));
    }

    @FlakyTest
    public void testCvvAndPostalCodeFieldsAreNotShownIfChallengesAreNotPresent() {
        String clientToken = new TestClientTokenBuilder()
                .withoutCvvChallenge()
                .withoutPostalCodeChallenge()
                .build();
        injectBraintree(getInstrumentation().getContext(), clientToken);
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.card_form_cvv)).check(matches(not(isDisplayed())));
        onView(withId(R.id.card_form_postal_code)).check(matches(not(isDisplayed())));
    }

    @FlakyTest
    public void testCvvAndPostalCodeFieldsAreShownIfChallengesArePresent() {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeTestUtils.setUpActivityTest(this, clientToken);

        getActivity();
        waitForAddPaymentFormHeader();

        onView(withId(R.id.card_form_cvv)).check(matches(isDisplayed()));
        onView(withId(R.id.card_form_postal_code)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testErrorIsShownWhenCvvDoesNotMatchForCvvVerificationMerchants() {
        BraintreeTestUtils
                .setUpActivityTest(this, new TestClientTokenBuilder().withCvvVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.card_form_complete_button)).perform(click());
        waitForView(withId(R.id.header_message));

        BraintreeEditText editText = (BraintreeEditText) activity.findViewById(R.id.card_form_cvv);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testErrorIsShownWhenPostalCodeDoesNotMatchForPostalCodeVerificationMerchants() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withPostalCodeVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard(), waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.card_form_complete_button)).perform(click());
        waitForView(withId(R.id.header_message));

        BraintreeEditText editText = (BraintreeEditText) activity.findViewById(R.id.card_form_postal_code);
        assertTrue(editText.isError());
        assertTrue(editText.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testErrorIsShownWhenCvvAndPostalCodeDoesNotMatchForCvvAndPostalCodeVerificationMerchants() {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());

        BraintreePaymentActivity activity = getActivity();
        waitForAddPaymentFormHeader();

        onView(withHint("Card Number")).perform(typeText("4111111111111111"));
        onView(withHint("Expiration")).perform(typeText("0619"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("CVV")) .perform(typeText("200"), closeSoftKeyboard(),
                waitForKeyboardToClose());
        onView(withHint("Postal Code")).perform(typeText("20000"));

        onView(withId(R.id.card_form_complete_button)).perform(click());
        waitForView(withId(R.id.header_message));

        BraintreeEditText cvvCode = (BraintreeEditText) activity.findViewById(R.id.card_form_cvv);
        BraintreeEditText postalCode = (BraintreeEditText) activity.findViewById(R.id.card_form_postal_code);

        assertTrue(cvvCode.isError());
        assertTrue(postalCode.isError());
        assertTrue(cvvCode.isFocused());

        LoadingHeader loadingHeader = (LoadingHeader) activity.findViewById(R.id.header_container);
        assertEquals(HeaderState.ERROR, loadingHeader.getCurrentState());
        onView(withId(R.id.header_container)).check(matches(isDisplayed()));
    }

    @FlakyTest
    public void testIsSuccessfulWhenCvvAndPostalCodeMatchForCvvAndPostalCodeVerificationMerchants()
            throws InterruptedException {
        BraintreeTestUtils.setUpActivityTest(this,
                new TestClientTokenBuilder().withCvvAndPostalCodeVerification().build());
        assertCardFormCreatesAPaymentMethod();
    }

    @FlakyTest
    public void testSetsIMEActionAsGoForExpirationIfCvvAndPostalAreNotPresent() {
        BraintreeTestUtils
                .setUpActivityTest(this, new TestClientTokenBuilder().withoutCvvChallenge()
                        .withoutPostalCodeChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.card_form_expiration)).getImeOptions());
    }

    @FlakyTest
    public void testSetsIMEActionAsGoForCvvIfCvvIsPresentAndPostalIsNot() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder()
                .withoutPostalCodeChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.card_form_cvv)).getImeOptions());
    }

    @FlakyTest
    public void testSetsIMEActionAsGoForPostalAndNextForExpirationIfCvvIsNotPresent() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder()
                .withoutCvvChallenge().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.card_form_postal_code)).getImeOptions());
    }

    @FlakyTest
    public void testSetsIMEActionAsGoForPostalCodeIfCvvAndPostalArePresent() {
        BraintreeTestUtils.setUpActivityTest(this, new TestClientTokenBuilder().build());
        Activity activity = getActivity();

        waitForAddPaymentFormHeader();

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) activity.findViewById(R.id.card_form_cvv)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) activity.findViewById(R.id.card_form_postal_code)).getImeOptions());
    }

    private ViewAssertion thereIsNoIconHint() {
        return assertHintsAre(null, null, null, null);
    }

    private ViewAssertion theIconHintIs(int resId) {
        Drawable right = getInstrumentation().getContext().getResources().getDrawable(resId);
        return assertHintsAre(null, null, right, null);
    }

    private ViewAssertion assertHintsAre(final Drawable left, final Drawable top,
            final Drawable right, final Drawable bottom) {
        return new ViewAssertion() {
            @Override
            public void check(Optional<View> viewOptional,
                    Optional<NoMatchingViewException> noMatchingViewExceptionOptional) {
                EditText editText = ((EditText) viewOptional.get());
                Drawable[] drawables = editText.getCompoundDrawables();
                assertBitmapsEqual(drawables[0], left);
                assertBitmapsEqual(drawables[1], top);
                assertBitmapsEqual(drawables[2], right);
                assertBitmapsEqual(drawables[3], bottom);
            }
        };
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    private boolean checkHomeAsUpEnabled(Activity activity) {
        return (activity.getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) ==
            ActionBar.DISPLAY_HOME_AS_UP;
    }
}
