package com.braintreepayments.api.dropin.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.braintreepayments.api.dropin.R;
import com.braintreepayments.api.models.Card;
import com.braintreepayments.api.models.PaymentMethod;

/**
 * Layout that includes an image and text representation of a PaymentMethod
 */
public class PaymentMethodView extends RelativeLayout {

    private static final int UNKNOWN_IMAGE_ID = -1;

    public static enum PaymentType {
        VISA(R.drawable.ic_visa, R.string.descriptor_visa, "Visa"),
        MASTERCARD(R.drawable.ic_mastercard, R.string.descriptor_mastercard, "MasterCard"),
        DISCOVER(R.drawable.ic_discover, R.string.descriptor_discover, "Discover"),
        AMEX(R.drawable.ic_amex, R.string.descriptor_amex, "American Express"),
        JCB(R.drawable.ic_jcb, R.string.descriptor_jcb, "JCB"),
        DINERS(R.drawable.ic_diners, R.string.descriptor_diners, "Diners"),
        MAESTRO(R.drawable.ic_maestro, R.string.descriptor_maestro, "Maestro"),
        PAYPAL(R.drawable.ic_paypal, R.string.descriptor_paypal, "PayPal"),
        UNKNOWN(UNKNOWN_IMAGE_ID, R.string.descriptor_unknown, "unknown");

        private final int mPictureResId;

        /** Resource ID to a localized/user-facing version of the string. */
        private final int mDescriptorStringId;

        /** Name of the card known by Braintree. Only ever needed in English. */
        private String mCanonicalName;

        PaymentType(int pictureResId, int descriptorStringId, String canonicalName) {
            mPictureResId = pictureResId;
            mDescriptorStringId = descriptorStringId;
            mCanonicalName = canonicalName;
        }

        static PaymentType forType(String paymentMethodType) {
            for (PaymentType type : values()) {
                if (type.mCanonicalName.equals(paymentMethodType)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    private PaymentType mType = PaymentType.UNKNOWN;
    private ImageView mIconView;
    private TextView mDescriptorView;
    private TextView mDescriptionView;
    private CharSequence mDescription;

    public PaymentMethodView(Context context) {
        super(context);
        init(context);
    }

    public PaymentMethodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PaymentMethodView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.payment_method, this);
        mIconView = findView(R.id.payment_method_icon);
        mDescriptorView = findView(R.id.payment_method_type);
        mDescriptionView = findView(R.id.payment_method_description);
        refresh();
    }

    public void setPaymentMethodDetails(PaymentMethod paymentMethod) {
        mType = PaymentType.forType(paymentMethod.getTypeLabel());
        if (paymentMethod instanceof Card) {
            mDescription = String.format(getResources().getString(R.string.card_descriptor),
                    ((Card) paymentMethod).getLastTwo());
        } else {
            mDescription = paymentMethod.getDescription();
        }
        refresh();
    }

    private void refresh() {
        if (mType.mPictureResId != UNKNOWN_IMAGE_ID) {
            mIconView.setImageResource(mType.mPictureResId);
            mIconView.setVisibility(View.VISIBLE);
        } else {
            mIconView.setVisibility(View.GONE);
        }
        mDescriptorView.setText(getContext().getString(mType.mDescriptorStringId));
        mDescriptionView.setText(mDescription);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }
}
