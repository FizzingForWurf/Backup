package itrans.itranstest.Internet;

import android.content.Context;
import android.database.Cursor;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import java.util.ArrayList;
import java.util.List;

import itrans.itranstest.BusServiceDBAdapter;

public class MyApplication extends MultiDexApplication {

    private static MyApplication sInstance;

    private List<String> BusNumbers;
    private List<Integer> BusIDs;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static MyApplication getInstance(){
        return sInstance;
    }

    public static Context getAppContext(){
        return sInstance.getApplicationContext();
    }

    public MyApplication(){
        BusNumbers = new ArrayList<String>();
        BusIDs = new ArrayList<Integer>();
    }

    public long addToDatabase(String entryNo, Context c){
        BusServiceDBAdapter db = new BusServiceDBAdapter(c);
        db.open();

        long rowIDofInsertedEntry = db.insertEntry(entryNo);

        db.close();

        return rowIDofInsertedEntry;
    }

    public boolean deleteFromDatabase(int rowID, Context c){
        BusServiceDBAdapter db = new BusServiceDBAdapter(c);
        db.open();

        int id = BusIDs.get(rowID);

        boolean updateStatus = db.removeEntry(id);

        db.close();

        return updateStatus;
    }

    public void deleteAll(Context c){
        BusServiceDBAdapter db = new BusServiceDBAdapter(c);
        db.open();

        db.removeAllEntries();

        db.close();
    }

    public List<String> retrieveAll(Context c){

        Cursor myCursor;
        String myString = "";

        BusServiceDBAdapter db = new BusServiceDBAdapter(c);
        db.open();
        BusIDs.clear();
        BusNumbers.clear();
        myCursor = db.retrieveAllEntriesCursor();

        if(myCursor != null && myCursor.getCount()>0){
            myCursor.moveToFirst();

            do{
                BusIDs.add(myCursor.getInt(db.COLUMN_KEY_ID));

                myString = myCursor.getString(db.COLUMN_BN_ID);

                BusNumbers.add(myString);
            }while(myCursor.moveToNext());
        }
        db.close();

        return BusNumbers;
    }
}
