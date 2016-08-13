package itrans.itranstest;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

public class BusRoutes implements SearchSuggestion {

    private String BusServiceNumber;

    public BusRoutes(String suggestion) {
        this.BusServiceNumber = suggestion.toLowerCase();
    }

    public BusRoutes(Parcel in) {
        this.BusServiceNumber = in.readString();
    }

    @Override
    public String getBody() {
        return BusServiceNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Creator<BusRoutes> CREATOR = new Creator<BusRoutes>() {
        @Override
        public BusRoutes createFromParcel(Parcel in) {
            return new BusRoutes(in);
        }

        @Override
        public BusRoutes[] newArray(int size) {
            return new BusRoutes[size];
        }
    };
}
