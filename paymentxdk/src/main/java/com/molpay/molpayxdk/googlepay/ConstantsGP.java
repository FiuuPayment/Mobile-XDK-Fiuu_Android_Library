package com.molpay.molpayxdk.googlepay;

import com.google.android.gms.wallet.WalletConstants;

import java.util.Arrays;
import java.util.List;

/**
 * This file contains several constants you must edit before proceeding.
 * Please take a look at PaymentsUtil.java to see where the constants are used and to potentially
 * remove ones not relevant to your integration.
 *
 * <p>Required changes:
 * <ol>
 * <li> Update SUPPORTED_NETWORKS and SUPPORTED_METHODS if required (consult your processor if
 *      unsure)
 * <li> Update CURRENCY_CODE to the currency you use.
 * <li> Update SHIPPING_SUPPORTED_COUNTRIES to list the countries where you currently ship. If this
 *      is not applicable to your app, remove the relevant bits from PaymentsUtil.java.
 * <li> If you're integrating with your {@code PAYMENT_GATEWAY}, update
 *      PAYMENT_GATEWAY_TOKENIZATION_NAME and PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS per the
 *      instructions they provided. You don't need to update DIRECT_TOKENIZATION_PUBLIC_KEY.
 * <li> If you're using {@code DIRECT} integration, please edit protocol version and public key as
 *      per the instructions.
 */
public class ConstantsGP {

    /**
     * Changing this to ENVIRONMENT_PRODUCTION will make the API return chargeable card information.
     * Please refer to the documentation to read about the required steps needed to enable
     * ENVIRONMENT_PRODUCTION.
     *
     * @value #PAYMENTS_ENVIRONMENT
     */
    public static final int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
//  public static final int PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION;

    /**
     * TODO: List card networks supported by your app
     *
     * The allowed networks to be requested from the API. If the user has cards from networks not
     * specified here in their account, these will not be offered for them to choose in the popup.
     *
     * @value #SUPPORTED_NETWORKS
     */
    public static final List<String> SUPPORTED_NETWORKS = Arrays.asList(
            "AMEX",
            "DISCOVER",
            "JCB",
            "MASTERCARD",
            "VISA");

    /**
     * TODO: Confirm your processor supports Android device tokens on your supported card networks
     *
     * The Google Pay API may return cards on file on Google.com (PAN_ONLY) and/or a device token on
     * an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
     *
     * @value #SUPPORTED_METHODS
     */
    public static final List<String> SUPPORTED_METHODS = Arrays.asList(
            "PAN_ONLY",
            "CRYPTOGRAM_3DS");

}
