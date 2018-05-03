package io.branch.referral.validators;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.BranchUtil;
import io.branch.referral.PrefHelper;

/**
 * Created by sojanpr on 9/15/17.
 */

public class IntegrationValidator implements ServerRequestGetAppConfig.IGetAppConfigEvents {

    private static IntegrationValidator instance;
    private final BranchIntegrationModel integrationModel;
    private final String TAG = "BranchSDK_Doctor";

    private IntegrationValidator(Context context) {
        this.integrationModel = new BranchIntegrationModel(context);
    }

    public static void validate(Context context) {
        if (instance == null) {
            instance = new IntegrationValidator(context);
        }
        instance.validateSDKIntegration(context);
    }

    private void validateSDKIntegration(Context context) {
        logValidationProgress("\n\n------------------- Initiating Branch integration verification ---------------------------");
        // 1. Verify Branch Auto instance
        logValidationProgress("1. Verifying Branch instance creation");
        if (Branch.getInstance() == null) {
            logIntegrationError("Branch is not initialised from your Application class. Please add `Branch.getAutoInstance(this);` to your Application#onCreate() method.",
                    "https://docs.branch.io/pages/apps/android/#load-branch");
            return;
        }
        logValidationPassed();

        // 2. Verify Branch Keys
        logValidationProgress("2. Checking Branch keys");
        if (TextUtils.isEmpty(PrefHelper.getInstance(context).readBranchKey(!BranchUtil.isTestModeEnabled(context)))) {
            logIntegrationError("Unable to read Branch keys from your application. Did you forget to add Branch keys in your application?.",
                    "https://docs.branch.io/pages/apps/android/#configure-app");
            return;
        }
        logValidationPassed();

        Branch.getInstance().handleNewRequest(new ServerRequestGetAppConfig(context, this));

    }

    private void doValidateWithAppConfig(JSONObject branchAppConfig) {
        // 3. Verify the package name of app with Branch dash board settings
        logValidationProgress("3. Verifying application package name");
        if (!integrationModel.packageName.equals(branchAppConfig.optString("android_package_name"))) {
            logIntegrationError("Incorrect package name in Branch dashboard. Please correct your package name in dashboard -> link Settings page.",
                    "https://docs.branch.io/pages/dashboard/integrate/#android");
            return;
        }
        logValidationPassed();

        // 4. Verify the URI scheme filters are added on the app
        logValidationProgress("4. Checking Android Manifest for URI based deep link config");
        if (integrationModel.deeplinkUriScheme == null || integrationModel.deeplinkUriScheme.length() == 0) {
            logIntegrationError(String.format("No intent found for opening the app through uri Scheme '%s'." +
                            "Please add the intent with URI scheme to your Android manifest.", branchAppConfig.optString("android_uri_scheme")),
                    "https://docs.branch.io/pages/apps/android/#configure-app");
            return;
        }
        logValidationPassed();

        // 5. Check if URI Scheme is added in the Branch dashboard
        logValidationProgress("5. Verifying URI based deep link config with Branch dash board.");
        String branchAppUriScheme = branchAppConfig.optString("android_uri_scheme");
        if (TextUtils.isEmpty(branchAppUriScheme)) {
            logIntegrationError("Uri Scheme to open your app is not specified in Branch dashboard. Please add URI scheme in Branch dashboard.",
                    "https://docs.branch.io/pages/dashboard/integrate/#android");
            return;
        }
        logValidationPassed();

        // 6. Check if URI Scheme matches with the Branch app settings
        logValidationProgress("6. Verifying intent for receiving URI scheme.");
        if (!checkIfIntentAddedForURIScheme(branchAppUriScheme)) {
            logIntegrationError(String.format("Uri scheme '%s' specified in Branch dashboard doesn't match with the deep link intent in manifest file", branchAppUriScheme),
                    "https://docs.branch.io/pages/dashboard/integrate/#android");
            return;
        }
        logValidationPassed();

        // 5. Check if AppLinks are specified in the Manifest
        logValidationProgress("7. Checking AndroidManifest for AppLink config.");
        if (integrationModel.applinkScheme.isEmpty()) {
            logIntegrationError("Could not find any App Link hosts to support Android AppLinks. Please add intent filter for handling AppLinks in your Android Manifest file",
                    "https://docs.branch.io/pages/deep-linking/android-app-links/#add-intent-filter-to-manifest");
            return;
        }
        logValidationPassed();

        // 6. Look for any custom domains specified in the dash board and has matching intent filter
        {
            logValidationProgress("8. Verifying any supported custom link domains.");
            String customDomain = branchAppConfig.optString("short_url_domain");
            if (!TextUtils.isEmpty(customDomain) && !checkIfIntentAddedForLinkDomain(customDomain)) {
                logIntegrationError(String.format("Could not find intent filter to support custom link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", customDomain),
                        "https://docs.branch.io/pages/apps/android/#configure-app");
                return;
            }
        }
        logValidationPassed();

        // 7. Check for matching intent filter for default app link domains
        {
            logValidationProgress("9. Verifying default link domains integrations.");
            String defAppLinkDomain = branchAppConfig.optString("default_short_url_domain");
            if (!TextUtils.isEmpty(defAppLinkDomain) && !checkIfIntentAddedForLinkDomain(defAppLinkDomain)) {
                logIntegrationError(String.format("Could not find intent filter to support Branch default link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", defAppLinkDomain),
                        "https://docs.branch.io/pages/apps/android/#configure-app");
                return;
            }
        }
        logValidationPassed();

        // 8. Check for matching intent filter for alternative app link domains
        {
            logValidationProgress("10. Verifying alternate link domains integrations.");
            String alternateAppLinkDomain = branchAppConfig.optString("alternate_short_url_domain");
            if (!TextUtils.isEmpty(alternateAppLinkDomain) && !checkIfIntentAddedForLinkDomain(alternateAppLinkDomain)) {
                logIntegrationError(String.format("Could not find intent filter to support alternate link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", alternateAppLinkDomain),
                        "https://docs.branch.io/pages/apps/android/#configure-app");
                return;

            }
        }
        logValidationPassed();
        Log.d(TAG, "--------------------------------------------\nSuccessfully completed Branch integration validation. Everything looks good!");
    }

    private boolean checkIfIntentAddedForURIScheme(String uriScheme) {
        Uri branchDeepLinkURI = Uri.parse(uriScheme);
        String uriHost = branchDeepLinkURI.getScheme();
        String uriPath = branchDeepLinkURI.getHost();
        uriPath = TextUtils.isEmpty(uriPath) ? "open" : uriPath;
        boolean foundMatchingUri = false;
        for (Iterator<String> it = integrationModel.deeplinkUriScheme.keys(); it.hasNext(); ) {
            String key = it.next();
            if (uriHost.equals(key)) {
                JSONArray hosts = integrationModel.deeplinkUriScheme.optJSONArray(key);
                if (hosts != null && hosts.length() > 0) {
                    for (int i = 0; i < hosts.length(); ++i) {
                        if (uriPath.equals(hosts.optString(i))) {
                            foundMatchingUri = true;
                            break;
                        }
                    }
                } else {
                    foundMatchingUri = true;
                    break;
                }
            }
        }
        return foundMatchingUri;
    }

    private boolean checkIfIntentAddedForLinkDomain(String domainName) {
        boolean foundIntentFilterMatchingDomainName = false;
        if (!TextUtils.isEmpty(domainName)) {
            for (String host : integrationModel.applinkScheme) {
                if (domainName.equals(host)) {
                    foundIntentFilterMatchingDomainName = true;
                    break;
                }
            }
        }
        return foundIntentFilterMatchingDomainName;
    }

    @Override
    public void onAppConfigAvailable(JSONObject branchAppConfig) {
        if (branchAppConfig != null) {
            doValidateWithAppConfig(branchAppConfig);
        } else {
            logIntegrationError("Unable to read Dashboard config. Please confirm that your Branch key is properly added to the manifest. Please fix your Dashboard settings.",
                    "https://branch.app.link/link-settings-page");
        }
    }

    private void logIntegrationError(String message, String documentLink) {
        Log.d(TAG, "** ERROR ** : " + message + "\nPlease follow the link for more info " + documentLink);
    }

    private void logValidationProgress(String message) {
        Log.d(TAG, message + " ... ");
    }

    private void logValidationPassed() {
        Log.d(TAG, "Passed");
    }


}
