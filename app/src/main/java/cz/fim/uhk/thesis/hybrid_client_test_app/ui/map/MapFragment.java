package cz.fim.uhk.thesis.hybrid_client_test_app.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Submodul hlavního (řídícího) modulu pro funkce IS, konrétně vykreslení mapy s ostatními klienty
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    // globální a pomocné proměnné
    private GoogleMap map;
    private boolean locationPermissionGranted = false;

    // konstanty
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String TAG = "MainAct/MapFrag";
    private static final float DEFAULT_ZOOM = 15f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // kontrola povolení získání aktuální pozice a případné načtení map
        getLocationPermission();
    }

    // metoda obdržení povolení pro získání aktuální polohy zařízení
    private void getLocationPermission() {
        String[] locationPermission = {Manifest.permission.ACCESS_FINE_LOCATION};

        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()).getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // povolení již bylo uděleno
            locationPermissionGranted = true;
            initMap();
        } else {
            // zažádání o povolení
            ActivityCompat.requestPermissions(getActivity(),
                    locationPermission,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;

        // kontrola výsledku zažádání o povolení
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                // cyklus pro případ zvýšení počtu povolení (v tuhle chvíli pouze jedno)
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                // vše v pořádku lze načíst mapu
                locationPermissionGranted = true;
                initMap();
            }
        }
    }

    // metoda pro inicializaci map
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) this
                .getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    // metoda rozhraní OnMapReadyCallback při načtení map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // aktivování možnosti zoomu
        map.getUiSettings().setZoomControlsEnabled(true);

        // získání polohy
        if (locationPermissionGranted) {
            getDeviceLocation();
            if (getContext() != null && ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            map.setMyLocationEnabled(true); // nastavení zobrazení aktuální polohy
        }

        // vytvoření markerů všech ostatních uživatelů
        MainActivity mainActivity = (MainActivity) getActivity();
        List<User> users = null;
        if (mainActivity != null) users = mainActivity.getClients();

        if (users != null) {
            for (User user : users) {
                System.out.println("USER FOR VIEW: " + user.getSsid());
                // chceme zobrazit jen uživatele s informací ze senzorů
                if (user.getSensorInformation() != null) {
                    System.out.println("LAT VAL: " + user.getLatitude());
                    map.addMarker(new MarkerOptions()
                            .position(new LatLng(user.getLatitude(), user.getLongitude()))
                            .title(user.getSsid())
                            .snippet("Teplota: " + Math.round(user.getSensorInformation().getTemperature() * 100.0) / 100.0 + " °C "
                                    + " Tlak: " + Math.round(user.getSensorInformation().getPressure() * 100.0) / 100.0 + " hPa"));
                }
            }
        }
    }

    // metoda pro získání aktuální pozice zařízení
    private void getDeviceLocation() {
        FusedLocationProviderClient locationProviderClient = LocationServices
                .getFusedLocationProviderClient(Objects.requireNonNull(getActivity()));

        try {
            if (locationPermissionGranted) {
                // je-li uděleno povolení pro zisk polohy
                // požadavek na získání pozice
                Task<Location> location = locationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Pozice úspěšně nalezena");
                            // uložení pozice
                            Location currentLocation = (Location) task.getResult();
                            if (currentLocation != null) {
                                // uložení pozice do objektu aktuálního hybridního klienta
                                // mohlo by být již v MainActivity při startu, nicméně předpokládá se,
                                // že každý uživatel si funkce aplikace nejprve vyzkouší, čímž se doplní
                                // zbylé důležité údaje pro fungování konceptu hybridního klienta (a IS)
                                // především tedy poloha klienta
                                MainActivity mainActivity = (MainActivity) getActivity();
                                if (mainActivity != null) {
                                    mainActivity.getCurrentUser().setLatitude(currentLocation.getLatitude());
                                    mainActivity.getCurrentUser().setLongitude(currentLocation.getLongitude());
                                } else {
                                    Log.e(TAG, "Instance MainActivity je null");
                                }
                                // přidání markeru s pozicí včetně dat získaných ze senzorů
                                if (getArguments() != null) {
                                    float temperature = getArguments().getFloat("temperatureValue");
                                    float pressure = getArguments().getFloat("pressureValue");
                                    map.addMarker(new MarkerOptions()
                                            .position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                                            .title("My SSID")
                                            .snippet("Teplota: " + temperature + " °C " + " Tlak: " + pressure + " hPa"));
                                } else {
                                    Log.e(TAG, "Předané argumenty v Bundle objektu jsou null");
                                }
                                // zoom kamery na aktuální pozici
                                moveCamera(new LatLng(currentLocation.getLatitude(),
                                        currentLocation.getLongitude())
                                );
                            } else {
                                // v emulátoru často nutné nejprve získat polohu třeba skrze aplikaci Google Maps
                                Log.e(TAG, "Nepodařilo se získat aktuální polohu");
                            }
                        } else {
                            Log.d(TAG, "Aktuální pozice je null");
                            Toast.makeText(getContext(), "Nelze získat aktuální pozici",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Získání pozice selhalo: " + e.getMessage());
        }
    }

    // metoda pro zaměření kamery na danou pozici
    private void moveCamera(LatLng latLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapFragment.DEFAULT_ZOOM));
    }
}
