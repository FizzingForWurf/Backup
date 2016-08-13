package itrans.itranstest;

public class BusDetails {
    private String busNo, busT, bF, space;

    public BusDetails(){
    }

    public BusDetails(String busNo, String busT, String bF, String cap){
        this.busNo = busNo;
        this.busT = busT;
        this.bF = bF;
        this.space = cap;
    }

    public String getBusNo(){
        return busNo;
    }

    public void setBusNo(String name){
        this.busNo = name;
    }

    public String getBusT(){
        return busT;
    }

    public void setBusT(String time){
        this.busT = time;
    }

    public String getBF(){
        return bF;
    }

    public void setBF(String WCA){
        this.bF = WCA;
    }

    public String getSpace(){
        return space;
    }

    public void setSpace(String capacity){
        this.space = capacity;
    }
}

