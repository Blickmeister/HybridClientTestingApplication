package cz.fim.uhk.thesis.hybrid_client_test_app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.fim.uhk.thesis.hybrid_client_test_app.MainActivity;
import cz.fim.uhk.thesis.hybrid_client_test_app.R;
import cz.fim.uhk.thesis.hybrid_client_test_app.model.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsersFragment extends Fragment {

    private TableLayout tableLayout;
    private MainActivity mainActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_users, container, false);

        tableLayout = root.findViewById(R.id.table_layout_users);

        // request na server
        mainActivity = (MainActivity) getActivity();
        Call<List<User>> call = mainActivity.getIsCentralServerApi().getUsers();

        // zpracování response ze serveru
        // metoda enqueue zajistí, aby zpracovaní proběhlo na nově vytvořeném background vlákně
        call.enqueue(new Callback<List<User>>() {
            TextView textViewError = new TextView(getContext());
            TableRow tableRowError = new TableRow(getContext());
            // pokud dostaneme response (nemusí být úspěšný)
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                // kontrola zda response je neúspěšný
                System.out.println("sem tu");
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
            public void onFailure(Call<List<User>> call, Throwable t) {
                textViewError.setText(t.getMessage());
                tableRowError.addView(textViewError);
                tableLayout.addView(tableRowError);
            }
        });
        return root;
    }

    private void generateTableUsersBody(List<User> users) {
        if(users != null) {
            float scale = getResources().getDisplayMetrics().density; // prevod na dp
            for(User user : users) {
                System.out.println("ID: " + user.getSsid());
                TableRow tableRow = new TableRow(getContext());
                tableRow.setPadding(5,5,5,5);
                tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT));
                TextView textViewId = new TextView(getContext());
                textViewId.setText(user.getSsid());
                textViewId.setTextColor(getResources().getColor(R.color.black));
                textViewId.setTextSize(15);
                textViewId.setPadding((int)(4 * scale + 0.5f),(int)(4 * scale + 0.5f),
                        (int)(4 * scale + 0.5f),(int)(4 * scale + 0.5f));
                TextView textViewActState = new TextView(getContext());
                textViewActState.setText(user.getActualState());
                textViewActState.setTextColor(getResources().getColor(R.color.black));
                textViewActState.setTextSize(15);
                textViewActState.setPadding((int)(4 * scale + 0.5f),(int)(4 * scale + 0.5f),
                        (int)(4 * scale + 0.5f),(int)(4 * scale + 0.5f));
                tableRow.addView(textViewId);
                tableRow.addView(textViewActState);
                tableLayout.addView(tableRow);
            }
        } else {
            TableRow tableRow = new TableRow(getContext());
            TextView textView = new TextView(getContext());
            textView.setText("Žádný klient k zobrazení");
            tableRow.addView(textView);
            tableLayout.addView(tableRow);
        }
    }
}
