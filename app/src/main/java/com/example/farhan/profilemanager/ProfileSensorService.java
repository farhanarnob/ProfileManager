package com.example.farhan.profilemanager;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by ${farhanarnob} on ${06-Oct-16}.
 */

public class ProfileSensorService extends Service implements SensorEventListener {

    //Important Variables
    private Double[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    public AudioManager audioManager;
    public SensorManager sensorManager;
    public Sensor accelerometerSensor,proximitySensor,lightSensor,accelerometerSensorLiner;


    //Shaking Part Variables
    public long lastUpdate = 0;
    public float last_x,last_y,last_z;
    public static final int SHAKE_THRESHOLD = 200;

    //For Condition Flag
    boolean _faceUp=false,_inFrontHas=false,_lightOn=false,_shacking=false;

    @Override
    public void onCreate() {
        super.onCreate();
        //getting audio services
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        //Getting Sensor Service
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //Now Getting required Sensors
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accelerometerSensorLiner = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        /*
        Now Check Sensor Availability
        As We are working on three sensors so checking this three sensors are available or not
        If not available then this profile manager is not going to work
        */
        if (accelerometerSensor != null){
            sensorManager.registerListener(this,accelerometerSensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (proximitySensor != null){
            sensorManager.registerListener(this,proximitySensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (lightSensor != null){
            sensorManager.registerListener(this,lightSensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(accelerometerSensorLiner!=null){
            sensorManager.registerListener(this,lightSensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
    }



    //Every Time Service Started this command execute
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Service has been started",Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    //When Service Destroyed
    //Sensor Unregister
    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this,"Service Disable",Toast.LENGTH_SHORT).show();
        sensorManager.unregisterListener(this);
    }

    //For Binding The service to mainActivity
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //Where Sensor Works
    @Override
    public void onSensorChanged(SensorEvent event) {

        //For Light
        if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            if (event.values[0] < 15){
                _lightOn = false;
            } else if (event.values[0] >= 15){
                _lightOn = true;
            }
        }

        //For Proximity
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            if (event.values[0] == 0){
                _inFrontHas = true;
            } else {
                _inFrontHas = false;

            }
        }


        //For Accelerometer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x;
            float y;
            float z = event.values[2];

            //For Phone Face Up And Face Down

            if (z > 2.0){
                //Face Up
                _faceUp = true;
                //Log.d("Accelerometer", "Face Up");
                //audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else if (z <= 2){
                _faceUp = false;
                //Log.d("Accelerometer", "Face Down");
                //audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    Log.d("sensor", "shake detected w/ speed: " + speed);
                    _shacking=true;
                    Toast.makeText(this, "shake detected w/ speed: " + speed, Toast.LENGTH_SHORT).show();
                }else {
                    _shacking = false;
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }

        }

        //Profile Manager

        if (_faceUp && !_inFrontHas && audioManager.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL){
            Log.d("Profile", "home");
            // For Home Profile
            //No Vibration, Ringer Loud
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            audioManager.setStreamVolume(AudioManager.STREAM_RING,audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),0);
//            SystemClock.sleep(20);

        }
        else if (_shacking && audioManager.getRingerMode()!=AudioManager.RINGER_MODE_VIBRATE){
            Log.d("Profile", "Pocket");
            //Pocket Profile
            //Vibration On, Ringer Medium
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_RING,20,0);
//            SystemClock.sleep(20);

        }
        else if (!_faceUp && _inFrontHas && !_lightOn && audioManager.getRingerMode()!=AudioManager.RINGER_MODE_SILENT){
            Log.d("Profile", "Silent");
            //Silent Profile
            //Only Vibration
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
//            audioManager.setStreamVolume(AudioManager.STREAM_RING,0,0);
//            SystemClock.sleep(20);
        }

    }


    //Not In Use For Now
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
