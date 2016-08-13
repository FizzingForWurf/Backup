package itrans.itranstest;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BusArrivalAdapter extends RecyclerView.Adapter<BusArrivalAdapter.MyViewHolder>{

    String timeRemaining;
    private List<BusDetails> busServices;
    private List<Integer> Positions = new ArrayList<>();
    SharedPreferences.Editor editor;
    public static final String MY_PREFS_POS = "MyPrefsPos";
    CountDownTimer timing;

    public class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView ETA, busNum;
        public ImageView BusFeature;

        public MyViewHolder(View view){
            super(view);
            busNum = (TextView) view.findViewById(R.id.busNumber);
            ETA = (TextView) view.findViewById(R.id.busTiming);
            BusFeature = (ImageView) view.findViewById(R.id.wheelCA);
        }
    }

    public BusArrivalAdapter(List<BusDetails> busServices){
        this.busServices = busServices;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_bus_arrival_rv_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position){
        if(Positions.isEmpty()){
            for(int i =0;i<busServices.size();i++) {
                Positions.add(0);
            }
        }
        final BusDetails bus = busServices.get(position);
        holder.busNum.setText(bus.getBusNo());
        if(bus.getBF().isEmpty()){
            holder.BusFeature.setVisibility(View.INVISIBLE);
        }else{
            holder.BusFeature.setVisibility(View.VISIBLE);
        }

        if(bus.getSpace().equals("Seats Available")){
            holder.busNum.setTextColor(Color.parseColor("#000000"));
        }else if(bus.getSpace().equals("Standing Available")){
            holder.busNum.setTextColor(Color.parseColor("#FF808080"));
        }else if(bus.getSpace().equals("Limited Standing")){
            holder.busNum.setTextColor(Color.parseColor("#ef6b6b"));
        }

        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sst = format.format(c.getTime());
        Date eta;
        Date current;
        long diff = 0;
        try {
            String[] splitString = bus.getBusT().split("T");
            splitString[1].replace("+08:00","");
            eta = format.parse(splitString[0]+" "+splitString[1]);
            current = format.parse(sst);

            diff = eta.getTime() - current.getTime();

        }catch(Exception e){
            e.printStackTrace();
        }

        counter(holder, holder.getAdapterPosition(), diff);

    }

    public void counter(final MyViewHolder holder, int position, long diff){
        if(Positions.get(position)==0) {
            Positions.set(position, 1);
        }else if(timing!=null){
            timing.cancel();
        }
        timing = new CountDownTimer(diff, 10000) {
            public void onTick(long millisUntilFinished) {
                if(millisUntilFinished<60000) {
                    holder.ETA.setText("Arriving");
                }else {
                    holder.ETA.setText(String.valueOf(millisUntilFinished / 60000) + " min");
                }
            }
            public void onFinish() {
                holder.ETA.append(String.valueOf(holder.getAdapterPosition()));

            }

        };
        timing.start();

    }

    @Override
    public int getItemCount(){
        return busServices.size();
    }

}
