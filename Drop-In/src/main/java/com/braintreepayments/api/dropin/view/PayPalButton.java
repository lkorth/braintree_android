package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.braintreepayments.api.dropin.R;

/**
 * A skinned {@link android.widget.ImageButton} intended for launching the Pay With PayPal flow.
 * No behavior outside of appearance is included by default.
 */
public class PayPalButton extends ImageButton {

    public PayPalButton(Context context) {
        super(context);
        init();
    }

    public PayPalButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PayPalButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setBackgroundResource(R.drawable.paypal_button_selector);
        setImageResource(R.drawable.ic_paypal_button);
    }
}
