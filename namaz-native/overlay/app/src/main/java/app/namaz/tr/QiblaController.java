package app.namaz.tr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.webkit.WebView;

import org.json.JSONObject;

public final class QiblaController implements SensorEventListener {
    private final SensorManager manager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final WebView webView;
    private float[] gravity;
    private float[] magnetic;
    private int magneticAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
    private long lastDispatch;

    public QiblaController(Context context, WebView webView) {
        this.webView = webView;
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        if (manager == null || accelerometer == null || magnetometer == null) {
            emitUnavailable("Bu cihazda gerekli pusula sensörleri yok");
            return;
        }
        manager.unregisterListener(this);
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        if (manager != null) manager.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) gravity = event.values.clone();
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values.clone();
            magneticAccuracy = event.accuracy;
        }
        if (gravity == null || magnetic == null) return;
        long now = System.currentTimeMillis();
        if (now - lastDispatch < 90) return;
        float[] rotation = new float[9];
        float[] inclination = new float[9];
        if (!SensorManager.getRotationMatrix(rotation, inclination, gravity, magnetic)) return;
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotation, orientation);
        double heading = PrayerMath.normalizeDegrees(Math.toDegrees(orientation[0]));
        lastDispatch = now;
        String js = "window.onNativeHeading&&window.onNativeHeading(" + heading + "," + magneticAccuracy + ")";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor != null && sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) magneticAccuracy = accuracy;
    }

    private void emitUnavailable(String reason) {
        String js = "window.onNativeQiblaUnavailable&&window.onNativeQiblaUnavailable(" + JSONObject.quote(reason) + ")";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }
}
