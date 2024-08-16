package br.edu.leonardo.apps.sensors;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.measure.Quantity;

import br.edu.leonardo.jaf.Agent;
import br.edu.leonardo.jaf.AgentException;
import br.edu.leonardo.jaf.Behaviour;
import br.edu.leonardo.jaf.R;
import br.edu.leonardo.jaf.sensors.Sensor;
import br.edu.leonardo.jaf.sensors.SensorNotification;
import br.edu.leonardo.jaf.sensors.SensorValue;
import br.edu.leonardo.jaf.android.AndroidDeviceSensorFactory;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.MetricPrefix;
import tec.units.ri.unit.Units;

/**
 * The main activity class.
 */
public class MainActivity extends AppCompatActivity {

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // P R O T E C T E D   M E T H O D S
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check permissions to access location information and request them, if needed.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }

        // Create the fragments for each sensor category and organize them in a view pager.
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new MotionSensorsFragment());
        fragmentList.add(new PositionSensorsFragment());
        fragmentList.add(new AmbientSensorsFragment());

        ViewPager2 vp2Sensors = (ViewPager2) findViewById(R.id.vp2Sensors);
        SensorFragmentAdapter sensorFragAdpt = new SensorFragmentAdapter(this, fragmentList);
        vp2Sensors.setAdapter(sensorFragAdpt);

        // Related the view pager and the tab layout.
        String[] tabTitles = {
                getString(R.string.motion_tab_title),
                getString(R.string.position_tab_title),
                getString(R.string.environment_tab_title)
        };
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tlSensors);
        new TabLayoutMediator(tabLayout, vp2Sensors,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        // Create the agent and its sensors.
        Agent agent = new Agent();
        createMotionSensors(agent);
        createPositionSensors(agent);
        createAmbientSensors(agent);

        // Initialize the agent.
        try {
            agent.init();
        } catch (AgentException e) {
            showMessage(e.getMessage());
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // P R I V A T E   C O N S T A N T S
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The sensor sampling period used in this application.
     */
    private static final int SENSOR_SAMPLING_PERIOD = SensorManager.SENSOR_DELAY_NORMAL;

    /**
     * The sensor report latency used in this application.
     */
    private static final int SENSOR_REPORT_LATENCY = 0;

    /**
     * The minimum time interval between location updates.
     */
    private static final Quantity LOCATION_MIN_TIME = Quantities.getQuantity(500, MetricPrefix.MILLI(Units.SECOND));

    /**
     * The minimum distance between location updates.
     */
    private static final Quantity LOCATION_MIN_DISTANCE = Quantities.getQuantity(1, Units.METRE);

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // P R I V A T E   M E T H O D S
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * This method shows a message in an alert dialog.
     *
     * @param msg The message.
     */
    private void showMessage(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.create().show();
    }

    /**
     * This message adds the given sensor to the given agent and relates some of sensor values
     * properties to view elements.
     *
     * @param a The desired agent.
     * @param sensor The desired sensor.
     * @param props The relation between properties and view elements.
     */
    private void createSensor(Agent a, Sensor sensor, DisplayedProperty[] props) {
        if(sensor != null) {
            // Add sensor to the agent.
            a.addSensor(sensor);

            // Create and add a behaviour to be executed when the sensor notifies values to the agent.
            a.addBehaviour(new Behaviour() {
                @Override
                public void execute(SensorNotification notification) {
                    // Obtain the value notified by the sensor.
                    SensorValue value = notification.getValue();

                    // Get the value class.
                    Class valClass = value.getClass();

                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void run() {
                            // For each value property informed to the method, the text of a related
                            // view is changed to show the new property value.
                            TextView textView;
                            for(DisplayedProperty dp : props) {
                                textView = (TextView) findViewById(dp.viewId);
                                if(textView != null) {
                                    try {
                                        Method getter = valClass.getMethod(dp.generateGetterName());
                                        textView.setText(getter.invoke(value).toString());
                                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                    }
                                }
                            }
                        }
                    });
                }
            }, sensor);
        }
    }

    /**
     * THis method creates all motion sensors used by this application.
     *
     * @param a The desired agent.
     */
    private void createMotionSensors(Agent a) {
        // ACCELEROMETER
        createSensor(
                a,
                AndroidDeviceSensorFactory.createAccelerometer(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvAccXValue, "x"),
                        new DisplayedProperty(R.id.tvAccYValue, "y"),
                        new DisplayedProperty(R.id.tvAccZValue, "z")
                }
        );

        // GRAVITY
        createSensor(
                a,
                AndroidDeviceSensorFactory.createGravity(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvGravXValue, "x"),
                        new DisplayedProperty(R.id.tvGravYValue, "y"),
                        new DisplayedProperty(R.id.tvGravZValue, "z")
                }
        );

        // GYROSCOPE
        createSensor(
                a,
                AndroidDeviceSensorFactory.createGyroscope(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvGyrXValue, "x"),
                        new DisplayedProperty(R.id.tvGyrYValue, "y"),
                        new DisplayedProperty(R.id.tvGyrZValue, "z")
                }
        );

        // LINEAR ACCELERATION
        createSensor(
                a,
                AndroidDeviceSensorFactory.createLinearAcceleration(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvLinAccXValue, "x"),
                        new DisplayedProperty(R.id.tvLinAccYValue, "y"),
                        new DisplayedProperty(R.id.tvLinAccZValue, "z")
                }
        );

        // STEP COUNTER
        createSensor(
                a,
                AndroidDeviceSensorFactory.createStepCounter(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvStepCounterValue, "value")
                }
        );
    }

    /**
     * THis method creates all position sensors used by this application.
     *
     * @param a The desired agent.
     */
    private void createPositionSensors(Agent a) {
        // MAGNETIC FIELD
        createSensor(
                a,
                AndroidDeviceSensorFactory.createMagneticField(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvMagFXValue, "x"),
                        new DisplayedProperty(R.id.tvMagFYValue, "y"),
                        new DisplayedProperty(R.id.tvMagFZValue, "z")
                }
        );

        // PROXIMITY
        createSensor(
                a,
                AndroidDeviceSensorFactory.createProximity(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvProximityValue, "value")
                }
        );

        // LOCATION
        createSensor(
                a,
                AndroidDeviceSensorFactory.createLocationSensor(this, LOCATION_MIN_TIME, LOCATION_MIN_DISTANCE),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvLatitudeValue, "latitude"),
                        new DisplayedProperty(R.id.tvLongitudeValue, "longitude")
                }
        );
    }

    /**
     * THis method creates all ambient sensors used by this application.
     *
     * @param a The desired agent.
     */
    private void createAmbientSensors(Agent a) {
        // AMBIENT TEMPERATURE
        createSensor(
                a,
                AndroidDeviceSensorFactory.createAmbientTemperatureSensor(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvAmbientTempSensorValue, "value")
                }
        );

        // LIGHT
        createSensor(
                a,
                AndroidDeviceSensorFactory.createLight(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvLightSensorValue, "value")
                }
        );

        // PRESSURE
        createSensor(
                a,
                AndroidDeviceSensorFactory.createPressure(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvPressureSensorValue, "value")
                }
        );

        // RELATIVE HUMIDITY
        createSensor(
                a,
                AndroidDeviceSensorFactory.createRelativeHumidity(this, SENSOR_SAMPLING_PERIOD, SENSOR_REPORT_LATENCY),
                new DisplayedProperty[]{
                        new DisplayedProperty(R.id.tvRelHumiditySensorValue, "value")
                }
        );
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // P R I V A T E   I N T E R N A L   C L A S S E S
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal class used by create sensor methods to relate sensor values properties and view
     * elements.
     */
    private class DisplayedProperty {

        //////////////////////////////////////////////////////////////////////////////////////////
        // P U B L I C   C O N S T R U C T O R S
        //////////////////////////////////////////////////////////////////////////////////////////

        /**
         * This constructor builds a new DisplayedProperty with the given view element id and the
         * sensor value property name.
         *
         * @param viewId The id of the desired view element.
         * @param property The property name.
         */
        public DisplayedProperty(int viewId, String property) {
            this.viewId = viewId;
            this.property = property;
        }

        //////////////////////////////////////////////////////////////////////////////////////////
        // P U B L I C   M E T H O D S
        //////////////////////////////////////////////////////////////////////////////////////////

        /**
         * This method generates the name of the getter related to the property in this object.
         *
         * @return The getter method name.
         */
        public String generateGetterName() {
            return "get"+Character.toUpperCase(property.charAt(0))+property.substring(1);
        }

        //////////////////////////////////////////////////////////////////////////////////////////
        // P R I V A T E   A T T R I B U T E S
        //////////////////////////////////////////////////////////////////////////////////////////

        /**
         * The id of the view element.
         */
        private int viewId;

        /**
         * The name of the related sensor value property.
         */
        private String property;
    }
}