package io.branch.referral;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.JarFile;


/**
 * Class for Branch utility methods
 */
public class BranchUtil {

    static boolean isCustomDebugEnabled_ = false; /* For setting debug mode using Branch#setDebug api */

    /**
     * Get the value of "io.branch.sdk.TestMode" entry in application manifest or from String res.
     *
     * @return value of "io.branch.sdk.TestMode" entry in application manifest or String res.
     * false if "io.branch.sdk.TestMode" is not added in the manifest or String res.
     */
    public static boolean isTestModeEnabled(Context context) {
        if (isCustomDebugEnabled_) {
            return true;
        }
        boolean isTestMode_ = false;
        String testModeKey = "io.branch.sdk.TestMode";
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null && ai.metaData.containsKey(testModeKey)) {
                isTestMode_ = ai.metaData.getBoolean(testModeKey, false);
            } else {
                Resources resources = context.getResources();
                isTestMode_ = Boolean.parseBoolean(resources.getString(resources.getIdentifier(testModeKey, "string", context.getPackageName())));
            }
        } catch (Exception ignore) { // Extending catch to trap any exception to handle a rare dead object scenario
        }

        return isTestMode_;
    }

    /**
     * Converts a given link param as {@link JSONObject} to string after adding the source param and removes replaces any illegal characters.
     *
     * @param params Link param JSONObject.
     * @return A {@link String} representation of link params.
     */
    static String formatAndStringifyLinkParam(JSONObject params) {
        return stringifyAndAddSource(filterOutBadCharacters(params));
    }

    /**
     * Convert the given JSONObject to string and adds source value as
     *
     * @param params JSONObject to convert to string
     * @return A {@link String} value representing the JSONObject
     */
    private static String stringifyAndAddSource(JSONObject params) {
        if (params == null) {
            params = new JSONObject();
        }
        try {
            params.put("source", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params.toString();
    }

    /**
     * Replaces illegal characters in the given JSONObject.
     *
     * @param inputObj JSONObject to remove illegal characters.
     * @return A {@link JSONObject} with illegal characters replaced.
     */
    static JSONObject filterOutBadCharacters(JSONObject inputObj) {
        JSONObject filteredObj = new JSONObject();
        if (inputObj != null) {
            Iterator<String> keys = inputObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    if (inputObj.has(key) && inputObj.get(key).getClass().equals(String.class)) {
                        filteredObj.put(key, inputObj.getString(key).replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\""));
                    } else if (inputObj.has(key)) {
                        filteredObj.put(key, inputObj.get(key));
                    }
                } catch (JSONException ignore) {

                }
            }
        }
        return filteredObj;
    }

    public static class JsonReader {
        private final JSONObject jsonObject;

        public JsonReader(JSONObject jsonObject) {
            JSONObject tempJsonObj = new JSONObject();
            try {
                tempJsonObj = new JSONObject(jsonObject.toString());
            } catch (JSONException ignore) {
            }
            this.jsonObject = tempJsonObj;
        }

        public JSONObject getJsonObject() {
            return jsonObject;
        }

        public int readOutInt(String key) {
            int val = jsonObject.optInt(key);
            jsonObject.remove(key);
            return val;
        }

        public Integer readOutInt(String key, Integer fallback) {
            Integer val = fallback;
            if (jsonObject.has(key)) {
                val = jsonObject.optInt(key);
                jsonObject.remove(key);
            }
            return val;
        }

        public String readOutString(String key) {
            String val = jsonObject.optString(key);
            jsonObject.remove(key);
            return val;
        }

        public String readOutString(String key, String fallback) {
            String val = jsonObject.optString(key, fallback);
            jsonObject.remove(key);
            return val;
        }

        public long readOutLong(String key) {
            long val = jsonObject.optLong(key);
            jsonObject.remove(key);
            return val;
        }

        public double readOutDouble(String key) {
            double val = jsonObject.optDouble(key);
            jsonObject.remove(key);
            return val;
        }

        public Double readOutDouble(String key, Double fallback) {
            Double val = fallback;
            if (jsonObject.has(key)) {
                val = jsonObject.optDouble(key);
                jsonObject.remove(key);
            }
            return val;
        }

        public boolean readOutBoolean(String key) {
            boolean val = jsonObject.optBoolean(key);
            jsonObject.remove(key);
            return val;
        }

        public JSONArray readOutJsonArray(String key) {
            JSONArray val = jsonObject.optJSONArray(key);
            jsonObject.remove(key);
            return val;
        }

        public Object readOut(String key) {
            Object val = jsonObject.opt(key);
            jsonObject.remove(key);
            return val;
        }

        public boolean has(String key) {
            return jsonObject.has(key);
        }

        public Iterator<String> keys() {
            return jsonObject.keys();
        }

    }

    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int drawableID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(drawableID, context.getTheme());
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(drawableID);
        }
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }


    /**
     * <p>Checks the current device's {@link ActivityManager} system service and returns the value
     * of the lowMemory flag.</p>
     *
     * @return <p>A {@link Boolean} value representing the low memory flag of the current device.</p>
     * <ul>
     * <li><i>true</i> - the free memory on the current device is below the system-defined threshold
     * that triggers the low memory flag.</li>
     * <li><i>false</i> - the device has plenty of free memory.</li>
     * </ul>
     */
    public static boolean isLowOnMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    public static JSONObject getDeepLinkSchemes(Context context) {
        JSONObject obj = null;
        if (!isLowOnMemory(context)) {
            JarFile jf = null;
            InputStream is = null;
            byte[] xml;
            try {
                jf = new JarFile(context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).publicSourceDir);
                is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
                xml = new byte[is.available()];
                //noinspection ResultOfMethodCallIgnored
                is.read(xml);
                obj = new ApkParser().decompressXMLForValidator(xml);
            } catch (Exception ignored) {
            } finally {
                try {
                    if (is != null) {
                        is.close();
                        // noinspection unused
                        is = null;
                    }
                    if (jf != null) {
                        jf.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return obj;
    }

}
