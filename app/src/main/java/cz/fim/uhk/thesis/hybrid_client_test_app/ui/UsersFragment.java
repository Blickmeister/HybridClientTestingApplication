package cz.fim.uhk.thesis.hybrid_client_test_app.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Bc. Ondřej Schneider - FIM UHK
 * @version 1.0
 * @since 2021-04-06
 * Submodul hlavního (řídícího) modulu pro funkce IS
 * Konkrétně pro zobrazení všech klientů
 * (ať už z centrálního serveru či z jiného klienta v podobě P2P serveru)
 */
public class UsersFragment extends Fragment {

    private TableLayout tableLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_users, container, false);

        tableLayout = root.findViewById(R.id.table_layout_users);
        MainActivity mainActivity = (MainActivity) getActivity();

        // request na server
        Call<List<User>> call = Objects.requireNonNull(mainActivity).getIsCentralServerApi().getUsers();

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<List<User>>() {
            final TextView textViewError = new TextView(getContext());
            final TableRow tableRowError = new TableRow(getContext());

            // pokud dostaneme response (nemusí být úspěšný)
            @SuppressLint("SetTextI18n")
            // v tomto případě není žádoucí využití string resource souboru
            @Override
            public void onResponse(@NotNull Call<List<User>> call, @NotNull Response<List<User>> response) {
                // kontrola zda response je neúspěšný
                if (!response.isSuccessful()) {
                    // zobrazíme chybový HTTP kód a návrat z metody
                    textViewError.setText("HTTP kód: " + response.code());
                    tableRowError.addView(textViewError);
                    tableLayout.addView(tableRowError);
                    return;
                }

                // uložení dat a zobrazení
                List<User> users = response.body();
                generateTableUsersBody(users);
            }

            // pokud při spojení či zpracování požadavku došlo k chybě
            @Override
            public void onFailure(@NotNull Call<List<User>> call, @NotNull Throwable t) {
                textViewError.setText(t.getMessage());
                tableRowError.addView(textViewError);
                tableLayout.addView(tableRowError);
            }
        });
        return root;
    }

    @SuppressLint("SetTextI18n")
    private void generateTableUsersBody(List<User> users) {
        if (users != null && !users.isEmpty()) {
            float scale = getResources().getDisplayMetrics().density; // převod na dp
            for (User user : users) {
                TableRow tableRow = new TableRow(getContext());
                tableRow.setPadding(5, 5, 5, 5);
                tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT));

                TextView textViewId = new TextView(getContext());
                textViewId.setText(user.getSsid());
                textViewId.setTextColor(getResources().getColor(R.color.black));
                textViewId.setTextSize(15);
                textViewId.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewLatitude = new TextView(getContext());
                textViewLatitude.setText(String.valueOf(user.getLatitude()));
                textViewLatitude.setTextColor(getResources().getColor(R.color.black));
                textViewLatitude.setTextSize(15);
                textViewLatitude.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewLongitude = new TextView(getContext());
                textViewLongitude.setText(String.valueOf(user.getLongitude()));
                textViewLongitude.setTextColor(getResources().getColor(R.color.black));
                textViewLongitude.setTextSize(15);
                textViewLongitude.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewIsOnline = new TextView(getContext());
                textViewIsOnline.setText(Boolean.toString(user.isOnline()));
                textViewIsOnline.setTextColor(getResources().getColor(R.color.black));
                textViewIsOnline.setTextSize(15);
                textViewIsOnline.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewActState = new TextView(getContext());
                textViewActState.setText(user.getActualState());
                textViewActState.setTextColor(getResources().getColor(R.color.black));
                textViewActState.setTextSize(15);
                textViewActState.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewFutState = new TextView(getContext());
                textViewFutState.setText(user.getFutureState());
                textViewFutState.setTextColor(getResources().getColor(R.color.black));
                textViewFutState.setTextSize(15);
                textViewFutState.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm",
                        Locale.getDefault());
                TextView textViewFirstConn = new TextView(getContext());
                textViewFirstConn.setText(sdf.format(user.getFirstConnectionToServer()) + " UTC");
               /*if (user.getFirstConnectionToServer() == null) {
                    textViewFirstConn.setText("Čas není k dispozici");
                } else {
                    textViewFirstConn.setText(sdf.format(user.getFirstConnectionToServer()));
                }*/
                textViewFirstConn.setTextColor(getResources().getColor(R.color.black));
                textViewFirstConn.setTextSize(15);
                textViewFirstConn.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewLastConn = new TextView(getContext());
                textViewLastConn.setText(sdf.format(user.getLastConnectionToServer()) + " UTC");
                /*if (user.getLastConnectionToServer() == null) {
                    textViewLastConn.setText("Čas není k dispozici");
                } else {
                    textViewLastConn.setText(sdf.format(user.getLastConnectionToServer()));
                }*/
                textViewLastConn.setTextColor(getResources().getColor(R.color.black));
                textViewLastConn.setTextSize(15);
                textViewLastConn.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewTemperature = new TextView(getContext());
                if (user.getSensorInformation() == null) textViewTemperature.setText("0.0");
                else
                    textViewTemperature.setText(String.valueOf(user.getSensorInformation().getTemperature()));
                textViewTemperature.setTextColor(getResources().getColor(R.color.black));
                textViewTemperature.setTextSize(15);
                textViewTemperature.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                TextView textViewPressure = new TextView(getContext());
                if (user.getSensorInformation() == null) textViewPressure.setText("0.0");
                else
                    textViewPressure.setText(String.valueOf(user.getSensorInformation().getPressure()));
                textViewPressure.setTextColor(getResources().getColor(R.color.black));
                textViewPressure.setTextSize(15);
                textViewPressure.setPadding((int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f),
                        (int) (4 * scale + 0.5f), (int) (4 * scale + 0.5f));

                tableRow.addView(textViewId);
                tableRow.addView(textViewLatitude);
                tableRow.addView(textViewLongitude);
                tableRow.addView(textViewIsOnline);
                tableRow.addView(textViewActState);
                tableRow.addView(textViewFutState);
                tableRow.addView(textViewFirstConn);
                tableRow.addView(textViewLastConn);
                tableRow.addView(textViewTemperature);
                tableRow.addView(textViewPressure);
                tableLayout.addView(tableRow);
            }
        } else {
            TableRow tableRow = new TableRow(getContext());
            TextView textView = new TextView(getContext());
            textView.setText(R.string.txt_user_no_client);
            tableRow.addView(textView);
            tableLayout.addView(tableRow);
        }
    }
}
