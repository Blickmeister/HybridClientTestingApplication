package cz.fim.uhk.thesis.hybrid_client_test_app.ui.samplefunctionality;

import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;

public class SampleFunctionalityFragment extends Fragment {

    // globální a pomocné proměnné
    //private GalleryViewModel galleryViewModel;
    private SensorManager sensorManager;
    private Sensor temperatureSensor;
    private Sensor pressureSensor;
    private SensorEventListener listenerTempSensor;
    private SensorEventListener listenerPresSensor;
    private float temperature;
    private float pressure;
    private Button btnShowMap;
    private TextView txtTemperature;
    private TextView txtPressure;
    private Switch switchTemperature;
    private Switch switchPressure;

    // konstanty
    private static final String TAG = "MainAct/SampleFuncFrag";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        /*galleryViewModel =
                ViewModelProviders.of(this).get(GalleryViewModel.class);*/
        View root = inflater.inflate(R.layout.fragment_func, container, false);
        final TextView textView = root.findViewById(R.id.text_func);
        txtTemperature = root.findViewById(R.id.txt_func_temperature_value);
        txtPressure = root.findViewById(R.id.txt_func_pressure_value);
        btnShowMap = root.findViewById(R.id.btn_func_show_map);
        switchTemperature = root.findViewById(R.id.switch_func_temperature);
        switchPressure = root.findViewById(R.id.switch_func_pressure);
        /*galleryViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });*/
        if (isServicesOk()) {
            initMap();
        }
        // inicializace sensorů a sensorManageru
        sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // sensor listener pro teplotní senzor
        listenerTempSensor = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // při každé změně hodnot senzorů
                if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    // pokud se jedná o teplotní senzor
                    temperature = event.values[0]; // TODO uložíme hodnotu teploty (nwm jak to budeme ukladat na server jiank by stacilo bez promenny)
                    txtTemperature.setText(temperature + " °C");
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {} // není potřeba
        };

        // sensor listener pro tlakový senzor
        listenerPresSensor = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                    // pokud se jedná o tlakový senzor
                    pressure = event.values[0]; // TODO to samy
                    txtPressure.setText(pressure + " hPa");
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        // switch listener teplota
        switchTemperature.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    // je-li temperature switch v ON stavu naslouchame senzoru
                    sensorManager.registerListener(listenerTempSensor, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    // jinak defaultní text a vypnout naslouchání senzoru
                    sensorManager.unregisterListener(listenerTempSensor);
                    txtTemperature.setText(R.string.txt_func_temperature_value);
                }
            }
        });
        // switch listener tlak
        switchPressure.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    // je-li pressure switch v ON stavu naslouchame senzoru
                    sensorManager.registerListener(listenerPresSensor, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    // jinak defaultní text a vypnout naslouchání senzoru
                    sensorManager.unregisterListener(listenerPresSensor);
                    txtPressure.setText(R.string.txt_func_pressure_value);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // vypneme naslouchání senzoru při opuštění fragmentu či aktivity
        sensorManager.unregisterListener(listenerTempSensor);
        sensorManager.unregisterListener(listenerPresSensor);
    }

    // metoda, jež nás přesune po kliknutí na btnShowMap na fragment map
    private void initMap() {
        btnShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putFloat("temperatureValue", temperature);
                bundle.putFloat("pressureValue", pressure);
                Navigation.findNavController(getActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.nav_map, bundle);
            }
        });
    }

    // metoda pro kontrolu dostupnosti Google play services na cílovém zařízení
    // TODO možna hodit oddus do MainAct, pokud ne, tak dat pryc public a misto toho private
    public boolean isServicesOk() {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());

        if(available == ConnectionResult.SUCCESS) {
            // Google play services je k dispozici - mapy mohou být načteny
            Log.d(TAG, "isServicesOK: google services is OK");
            return true;
        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // objevil se řešitelný problém (stará verze Google play services atp.)
            Log.d(TAG, "isServicesOK: an error occurred but it is solvable");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            // neřešitelný problém (například zcela chybí podpora Google play services atp.)
            Toast.makeText(getContext(), "Mapy nelze zobrazit.", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}