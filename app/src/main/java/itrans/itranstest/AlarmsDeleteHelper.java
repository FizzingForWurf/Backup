package itrans.itranstest;

public class AlarmsDeleteHelper {

    private String Title;
    private String Destination;
    private String LatLng;
    private String Radius;
    private String RingTone;

    public AlarmsDeleteHelper(){

    }

    public String getRingTone() {
        return RingTone;
    }

    public void setRingTone(String ringTone) {
        RingTone = ringTone;
    }

    public String getRadius() {
        return Radius;
    }

    public void setRadius(String radius) {
        Radius = radius;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getDestination() {
        return Destination;
    }

    public void setDestination(String destination) {
        Destination = destination;
    }

    public String getLatLng() {
        return LatLng;
    }

    public void setLatLng(String latLng) {
        LatLng = latLng;
    }
}
