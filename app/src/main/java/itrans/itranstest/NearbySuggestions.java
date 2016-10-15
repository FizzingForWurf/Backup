package itrans.itranstest;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

public class NearbySuggestions implements SearchSuggestion{

    private String mPlaceName;
    private String mSecondaryPlaceName;
    private String mPlaceID;

    private String busStopName;
    private String busStopID;
    private Double busStopLat;
    private Double busStopLng;

    public NearbySuggestions(){

    }

    public NearbySuggestions(String suggestion) {
        this.mPlaceName = suggestion.toLowerCase();
    }

    public NearbySuggestions(Parcel source) {
        this.mPlaceName = source.readString();
    }

    @Override
    public String getBody() {
        return mPlaceName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Creator<NearbySuggestions> CREATOR = new Creator<NearbySuggestions>() {
        @Override
        public NearbySuggestions createFromParcel(Parcel in) {
            return new NearbySuggestions(in);
        }

        @Override
        public NearbySuggestions[] newArray(int size) {
            return new NearbySuggestions[size];
        }
    };

    public String getmPlaceName() {
        return mPlaceName;
    }

    public void setmPlaceName(String mPlaceName) {
        this.mPlaceName = mPlaceName;
    }

    public String getmSecondaryPlaceName() {
        return mSecondaryPlaceName;
    }

    public void setmSecondaryPlaceName(String mSecondaryPlaceName) {
        this.mSecondaryPlaceName = mSecondaryPlaceName;
    }

    public String getmPlaceID() {
        return mPlaceID;
    }

    public void setmPlaceID(String mPlaceID) {
        this.mPlaceID = mPlaceID;
    }

    public String getBusStopName() {
        return busStopName;
    }

    public void setBusStopName(String busStopName) {
        this.busStopName = busStopName;
    }

    public Double getBusStopLat() {
        return busStopLat;
    }

    public void setBusStopLat(Double busStopLat) {
        this.busStopLat = busStopLat;
    }

    public Double getBusStopLng() {
        return busStopLng;
    }

    public void setBusStopLng(Double busStopLng) {
        this.busStopLng = busStopLng;
    }

    public String getBusStopID() {
        return busStopID;
    }

    public void setBusStopID(String busStopID) {
        this.busStopID = busStopID;
    }
}
