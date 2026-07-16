package app.wifibattleship.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

/**
 * WiFi Direct exige NEARBY_WIFI_DEVICES en Android 13+ y ACCESS_FINE_LOCATION
 * en Android 8-12 (donde además la ubicación del sistema debe estar encendida
 * para que el descubrimiento funcione).
 */
final class P2pPermissions {

    static final int REQUEST_CODE = 1001;

    private P2pPermissions() {
    }

    static String required() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Manifest.permission.NEARBY_WIFI_DEVICES;
        }
        return Manifest.permission.ACCESS_FINE_LOCATION;
    }

    static boolean granted(Context context) {
        return ContextCompat.checkSelfPermission(context, required())
                == PackageManager.PERMISSION_GRANTED;
    }

    static void request(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{required()}, REQUEST_CODE);
    }

    /** Solo relevante en Android 8-12: el descubrimiento P2P necesita ubicación activa. */
    static boolean locationRequiredAndOff(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return false;
        }
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm == null || !LocationManagerCompat.isLocationEnabled(lm);
    }
}
