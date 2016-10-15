package itrans.itranstest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class BusNumberDBAdapter {
    private static final String DATABASE_NAME = "busNumber.db";
    private static final String DATABASE_TABLE = "busServiceNumber";
    private static final int DATABASE_VERSION =1;
    private SQLiteDatabase _db;
    private final Context context;
    private MyDBOpenHelper dbHelper;

    public static final String KEY_ID ="_id";
    public static final String BUS_STOP_NO = "busStop_id";
    public static final String BUS_STOP_NAME = "bus_stop_name";
    public static final String BUS_STOP_LATITUDE = "bus_stop_name_encode";
    public static final String BUS_STOP_LATLNG = "bus_stop_latlng";
    public static final String BUS_STOP_DESCRIPTION = "bus_stop_description";

    protected static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " " + "(" + KEY_ID +
            " integer primary key autoincrement, " + BUS_STOP_NO + " text not null, " + BUS_STOP_NAME + " text not null, "
            + BUS_STOP_LATITUDE + " text not null, " +  BUS_STOP_DESCRIPTION + " text not null, " + BUS_STOP_LATLNG +
            " text not null);";

    public class MyDBOpenHelper extends SQLiteOpenHelper {
        public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVerison){

        }
    }

    public BusNumberDBAdapter(Context _context){
        this.context = _context;

        dbHelper = new MyDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void close(){
        _db.close();
    }

    public void open() throws SQLiteException {
        try{
            _db = dbHelper.getWritableDatabase();
        }
        catch(SQLiteException e){
            _db = dbHelper.getReadableDatabase();
        }
    }

    public long insertBusStop(String id, String name, String latitude, String description, String latlng){
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(BUS_STOP_NO, id);
        newEntryValues.put(BUS_STOP_NAME, name);
        newEntryValues.put(BUS_STOP_LATITUDE, latitude);
        newEntryValues.put(BUS_STOP_DESCRIPTION, description);
        newEntryValues.put(BUS_STOP_LATLNG, latlng);

        return _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public String getBusStopLatLng(String stopID) {
        String[] columns = {KEY_ID, BUS_STOP_NO, BUS_STOP_NAME, BUS_STOP_DESCRIPTION, BUS_STOP_LATLNG};
        Cursor c = _db.query(DATABASE_TABLE, columns, BUS_STOP_NO + " = " + stopID, null, null, null, null);
        int iLATLNG = c.getColumnIndex(BUS_STOP_LATLNG);
        if (c != null){
            c.moveToFirst();
            return c.getString(iLATLNG);
        }
        return null;
    }

    public String getBusStopName(String stopID) {
        String[] columns = {KEY_ID, BUS_STOP_NO, BUS_STOP_NAME, BUS_STOP_DESCRIPTION, BUS_STOP_LATLNG};
        Cursor c = _db.query(DATABASE_TABLE, columns, BUS_STOP_NO + " = " + stopID, null, null, null, null);
        int iNAME = c.getColumnIndex(BUS_STOP_NAME);
        if (c != null){
            c.moveToFirst();
            return c.getString(iNAME);
        }
        return null;
    }

    public int getNumberOfRows(){
        return (int) DatabaseUtils.queryNumEntries(_db, DATABASE_TABLE);
    }

    public String getCoordinates(int rowNum) {
        String[] columns = {KEY_ID, BUS_STOP_NO, BUS_STOP_NAME, BUS_STOP_LATITUDE, BUS_STOP_DESCRIPTION, BUS_STOP_LATLNG};
        Cursor c = null;
        try {
            c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + rowNum, null, null, null, null);
            int iId = c.getColumnIndex(BUS_STOP_LATLNG);
            if (c != null){
                c.moveToFirst();
                return c.getString(iId);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public String getName(int rowNum) {
        String[] columns = {KEY_ID, BUS_STOP_NO, BUS_STOP_NAME, BUS_STOP_LATITUDE, BUS_STOP_DESCRIPTION, BUS_STOP_LATLNG};
        Cursor c = null;
        try {
            c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + rowNum, null, null, null, null);
            int iId = c.getColumnIndex(BUS_STOP_NAME);
            if (c != null){
                c.moveToFirst();
                return c.getString(iId);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    public String getID(int rowNum) {
        String[] columns = {KEY_ID, BUS_STOP_NO, BUS_STOP_NAME, BUS_STOP_LATITUDE, BUS_STOP_DESCRIPTION, BUS_STOP_LATLNG};
        Cursor c = null;
        try {
            c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + rowNum, null, null, null, null);
            int iId = c.getColumnIndex(BUS_STOP_NO);
            if (c != null){
                c.moveToFirst();
                return c.getString(iId);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }
}
