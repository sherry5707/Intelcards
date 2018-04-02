package com.ragentek.intelcards;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ragentek.intelcards.xy.MySdkDoAction;

import java.util.HashMap;

import cn.com.xy.sms.util.ParseManager;

import static com.ragentek.intelcards.SmsContentObserver.FROM;
import static com.ragentek.intelcards.SmsContentObserver.RESET_ALRAM;
import static com.ragentek.intelcards.ui.IntelCardListActivity.getICCID;

public class IntelcardsApplication extends Application {
    private static final String TAG = "IntelcardsApplication";
    private static final String CARD_SDK_CHANNEL = "yG2Qg5GwRAGENTEKCARD";
    private static final String CARD_SDK_SECRETKEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKYsuizdRx3mBTspkBEp11ZBgITsZ+ki+KvcH+56SGRBzjeoNPBVQyV33flLtYH55PNpeTYczJ3oizsXlfXH3AHKys0q8d0eZNZZ+NeZoN/qRv/i3Vm3ClyjQaGDc5mTTfIjdf99cX+vwcfPifu1XRMUhsWj59pgns7tXIbZCDkbAgMBAAECgYB1qKetyoNnPQ2pJTWXoXuR3FEmTMkKPrO54+NMFJMbQajEqbnKDWS8q9GkiSGcRRcB7kVftiZ/DD9A+OM6Ime4TNAtMQgjHXN06J0kK5pAF3x+tp+QOJCcdkeJU0cEqhnZ6Fu7uKnOkPq4NyPSeQtDNf+H4qwxbiBw0OzBIycjoQJBANbCgI2URVZF91HlJ8mp8erwsJggyMEWflyn1FyRfixob0hOOzhnLIAyO0W1MeOyZZ6PfKAU1jAIbwPo4RTKyAsCQQDGFc35NXkqT/3XzGHZYpUT5TT8nbBpbAPL9d/1wOPj8Hv0C0Kv2Y3uRbiQXOzV3dQnoKu3ZgCNakuJOoZ96i0xAkAqCVBYZhlcVb2fTheHpbgwoIQwgtpI0TpSsJckt2XXE2oU4Rs+YxOW1D492sW1KAo0Cyn9u5ZhOIViYocMJtUZAkAvuBkCrCsgUlzom8gGwoT/Yfw9zw8slmTjwbvOTEWaJ9j0lbHfAx36BLnNrVUwwFvXoBE+AUioyK6hxIOZ2cxBAkEAsieeCqT7xiZiXQmC8M/oc/QrgzUdBMJS0Uk/Yne8v7rL7Q0f0oiB2vOtqd37t0kZNaaBI0yTmqu8BTR+p8PbGQ==";
    private static final String CARD_SDK_RSAPRVKEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCM9HUsidEDjoynu57z3vU26P4y5lpIkJWgxm/kIKgO2sWF0AQjJFvtkUwCldQgocM4Er7VTsJD2iZiiRnlEojfrvXM8rn5d9PkShkTe1qGslPHMitfYI7aAXNE1H0OlQSVb5fn/rxZopiAnQJE9ueqiqbI8O207qz+bfH+gDzTUoIGNjys/tWqFSHZA1yecdOZQx6A4hal67x+M4lrgZyficj0BkzbbVsK6GbW6JzaQ3+jw8D1NRrDn5WWB7gFbYf0LZzGrxbJEef75nRFpFO8acNLj1toa6x1oLDdJfJ596Zn6J0+wHcgQxWZUU17Tdr5VRTehChMMgJ5Hdn/QFFTAgMBAAECggEAGmg6A4wWQmpUY6A82Udt/UmNRg/t3iZoQeSrVbvggFfwJW+hNsw7BLjx2pIjWbG0ly673c6RipC2HEO/PDFKv01R84GN4fTXF6/iHso/XXjPRURa59V5LdKTu9NrUm4Mm8p6VRZeRxS1KXNA15bCNCshsAejIQCOHGSQr7NqsdQ+ENdxgMcxKLNPj3bv6Gd913dgDhSQlr0kVhN1RAE+8WioeMWil3Mm7Se4e3VRmkB1r2Ze4utzenvObRd3LzgluBPMuIHH4zsM2izeZlEMysnwNo519WsqGbCdXiWv/armFSq9DzAnbyiBLwkQ8fL4BS8gbouxngnTr5iVLgNVEQKBgQDHeW38q/zkz3ErS1g//vEgaME8LjfTTzFXnuOKP6MbwKWFz8M4oXYnJnYBNXWiWrUt4IDIhiNzA+aVs9T0w65HkYKcjVADGpthDkMH+5wZqC8hWZUvEXry9QwCYgq1RJ/5hkaVnyO0y4rT+nGsZIf3GdklR6k4UFBQXGPMyGbfJQKBgQC05dLAWpDkTe7PbAwe2M/XswE/aPZE8BvIwvXakcZWx/byVneikdWK35iaMMc7uhovPNFUkLbdYJOoleNxQlhF7UpyhOF3aILzvcWbAsb76CLiUhVEkfcIDGNT5Ct/3ABRWOEo5u/QUKJl6OMMkmrGYzFCFHRvF8p5aVM+UzyhFwKBgQC8NBvMDFG9aOPz31DSgK3s0CmRqGHPo6aAb09sfwJcCvWhPReKdPzPj4BDP7dPiZnsQSgBCl6kBAgpMtU4YvAqYmYDY2kcpJv5hMVF4OW1Z3OgWa9iC3IfjYjZLru/r5HokgJC5TWBPZIs0t0xtGB8igzRGb3VfiFo0OLwshwtaQKBgQCmi2TR5U+6camo2+/4wmHwU4SVjZwV8f2SgzP4a/dv1CAeEs+II723Yo5LfxcefQM4dETBF47UF+M59e11S8CbeJDvCDnQRq8xmokAYrkyGRFszmd4Pu5xQX59MPd/etvsCOkbUCp/3oz1SEkpPPADADXMgaqE0SF/UCHsopOSNwKBgDXnD9s7GqQajZJO4Qz27lHYyVRnIXE7inHt1ohW7XpbqDkh4VAK9dJgmEFSdkH0OvdyDSZNlhjzCtXKDzNlhlckgjNdYqA9STa00uYGjBIqqenZXy9B0RF6nxwAdh7ZwfPf26xUX3DY8MeDY+bLt9k/pyudXaGKqLPeZw1+NpYV";

    @Override
    public void onCreate() {
        super.onCreate();
        Intent updateWidgetService = new Intent(getApplicationContext(), IntelcardService.class);
        getApplicationContext().startService(updateWidgetService);
        initCardHolder(this);
    }

    public static void initCardHolder(Context context) {
        Log.i(TAG,"initCardHolder");
        try {
            HashMap<String, String> extend = new HashMap<>();
            extend.put("ONLINE_UPDATE_SDK", "1");
            extend.put("SUPPORT_NETWORK _TYPE", "2");
            extend.put("SECRETKEY", CARD_SDK_SECRETKEY);
            extend.put("RSAPRVKEY", CARD_SDK_RSAPRVKEY);
            //extend.put("smartsms_enhance","true");
            String iccid = getICCID(context);
            ParseManager.initSdk(context, CARD_SDK_CHANNEL, iccid, true, true, extend);
            MySdkDoAction mySdkDoAction = new MySdkDoAction();
            ParseManager.setSdkDoAction(mySdkDoAction);
        } catch (Exception e) {
            Log.e(TAG, "cn.com.xy.sms.sdk.SmartSmsSdkUtil.init error", e);
        }
    }
}
