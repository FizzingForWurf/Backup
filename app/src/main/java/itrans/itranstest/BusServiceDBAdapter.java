package itrans.itranstest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BusServiceDBAdapter {
    private static final String DATABASE_NAME = "buses.db";
    private static final String DATABASE_TABLE = "busServices";
    private static final int DATABASE_VERSION =1;
    private SQLiteDatabase _db;
    private final Context context;
    private MyDBOpenHelper dbHelper;

    public static final String KEY_ID ="_id";
    public static final int COLUMN_KEY_ID = 0;
    public static final String BUS_NO = "bus_no";
    public static final int COLUMN_BN_ID = 1;
    public static final String BUS_DIRECTION_ONE = "bus_one";
    public static final int COLUMN_BN_ONE = 2;
    public static final String BUS_DIRECTION_TWO = "bus_two";
    public static final int COLUMN_BN_TWO = 3;
    protected static final String DATABASE_CREATE = "create table " + DATABASE_TABLE + " " + "(" + KEY_ID +
            " integer primary key autoincrement, " + BUS_NO + " text not null, " + BUS_DIRECTION_ONE +
            " text not null, " + BUS_DIRECTION_TWO + " text not null);";

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

    public BusServiceDBAdapter(Context _context){
        this.context = _context;

        dbHelper = new MyDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void close(){
        _db.close();
    }

    public void open() throws SQLiteException{
        try{
            _db = dbHelper.getWritableDatabase();
        }
        catch(SQLiteException e){
            _db = dbHelper.getReadableDatabase();
        }
    }

    public long insertEntry(String entryNo, String directionOne, String directionTwo){
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(BUS_NO, entryNo);
        newEntryValues.put(BUS_DIRECTION_ONE, directionOne);
        newEntryValues.put(BUS_DIRECTION_TWO, directionTwo);

        return _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public boolean removeEntry(long _rowIndex){
        if(_db.delete(DATABASE_TABLE, KEY_ID + "-" + _rowIndex, null) <= 0){
            return false;
        }
        return true;
    }

    public void removeAllEntries(){
        _db.delete(DATABASE_TABLE, null, null);
    }

    public Cursor retrieveAllEntriesCursor(){
        Cursor c = null;

        try{
            c = _db.query(DATABASE_TABLE, new String[] {KEY_ID, BUS_NO},null, null, null, null, null);
        }
        catch(SQLiteException e){
            Log.e("DBADAPTER","Retrieval error");
        }

        return c;
    }

    public String getdirectionone(int position){
        String[] columns = {KEY_ID, BUS_NO, BUS_DIRECTION_ONE, BUS_DIRECTION_TWO};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iONE = c.getColumnIndex(BUS_DIRECTION_ONE);
        if (c != null){
            c.moveToFirst();
            return c.getString(iONE);
        }
        return null;
    }

    public String getdirectiontwo(int position){
        String[] columns = {KEY_ID, BUS_NO, BUS_DIRECTION_ONE, BUS_DIRECTION_TWO};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iTWO = c.getColumnIndex(BUS_DIRECTION_TWO);
        if (c != null){
            c.moveToFirst();
            return c.getString(iTWO);
        }
        return null;
    }
}
