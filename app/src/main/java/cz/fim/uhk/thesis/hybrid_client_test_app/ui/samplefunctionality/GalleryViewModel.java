package cz.fim.uhk.thesis.hybrid_client_test_app.ui.samplefunctionality;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GalleryViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public GalleryViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Vzorová funkcionalita");
    }

    public LiveData<String> getText() {
        return mText;
    }
}