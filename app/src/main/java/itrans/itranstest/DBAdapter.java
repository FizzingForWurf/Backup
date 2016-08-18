package itrans.itranstest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBAdapter {

    private static final String DATABASE_NAME = "destinationDB.db";
    private static final String DATABASE_TABLE = "alarms";
    private static final int DATABASE_VERSION = 1;

    private final Context context;
    private MyDBOpenHelper dbHelper;
    private SQLiteDatabase _db;

    public static final String KEY_ID = "_id";
    public static final String ENTRY_TITLE = "entry_title";
    public static final String ENTRY_DESTINATION = "entry_destination";
    public static final String ENTRY_LATLNG = "entry_latlng";
    public static final String ENTRY_RINGTONE = "entry_ringtone";
    public static final String ENTRY_ALERTRADIUS = "entry_alertradius";

    protected static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ENTRY_TITLE + " TEXT NOT NULL, "
            + ENTRY_DESTINATION + " TEXT NOT NULL, " + ENTRY_LATLNG + " TEXT NOT NULL, " + ENTRY_RINGTONE + " TEXT NOT NULL, "+ ENTRY_ALERTRADIUS + " TEXT NOT NULL);";

    public DBAdapter(Context _context){
        this.context = _context;
    }

    public String getRadius(String position) {
        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null, null, null, null);
        int iRadius = c.getColumnIndex(ENTRY_ALERTRADIUS);
        if (c != null){
            c.moveToFirst();
            return c.getString(iRadius);
        }
        return null;
    }

    public String getLatLng(String position) {
        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null,null,null,null);
        int iLatLng = c.getColumnIndex(ENTRY_LATLNG);
        if (c != null){
            c.moveToFirst();
            return c.getString(iLatLng);
        }
        return null;
    }

    public String getDestination(String position) {
        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null,null,null,null);
        int iDestination = c.getColumnIndex(ENTRY_DESTINATION);
        if (c != null){
            c.moveToFirst();
            return c.getString(iDestination);
        }
        return null;
    }

    public String getTitle(String position) {
        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null,null,null,null);
        int iTitle = c.getColumnIndex(ENTRY_TITLE);
        if (c != null){
            c.moveToFirst();
            return c.getString(iTitle);
        }
        return null;
    }

    public String getRingTone(String position) {
        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor c = _db.query(DATABASE_TABLE, columns, KEY_ID + " = " + position, null,null,null,null);
        int iRingTone = c.getColumnIndex(ENTRY_RINGTONE);
        if (c != null){
            c.moveToFirst();
            return c.getString(iRingTone);
        }
        return null;
    }

    public void updateEntry(int rowNumber, String newTitle, String newDestination, String newLatLng, String newRadius, String newRingTone){
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(ENTRY_TITLE, newTitle);
        updatedValues.put(ENTRY_DESTINATION, newDestination);
        updatedValues.put(ENTRY_LATLNG, newLatLng);
        updatedValues.put(ENTRY_ALERTRADIUS, newRadius);
        updatedValues.put(ENTRY_RINGTONE, newRingTone);

        _db.update(DATABASE_TABLE, updatedValues, KEY_ID + "=" + rowNumber,null);
    }

    public class MyDBOpenHelper extends SQLiteOpenHelper {
        public MyDBOpenHelper(Context context)	{
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    public DBAdapter open() throws SQLiteException {
        try {
            dbHelper = new MyDBOpenHelper(context);
            _db = dbHelper.getWritableDatabase();
        }catch (SQLiteException e){
            _db = dbHelper.getReadableDatabase();
        }
        return this;
    }

    public void close(){
        dbHelper.close();
    }

    public long insertEntry(String entryTitle, String entryDestination, String entryLatLng, String entryAlertRadius, String entryRingTone) {
        ContentValues newEntryValues = new ContentValues();

        newEntryValues.put(ENTRY_TITLE, entryTitle);
        newEntryValues.put(ENTRY_DESTINATION, entryDestination);
        newEntryValues.put(ENTRY_LATLNG, entryLatLng);
        newEntryValues.put(ENTRY_ALERTRADIUS, entryAlertRadius);
        newEntryValues.put(ENTRY_RINGTONE, entryRingTone);

        return  _db.insert(DATABASE_TABLE, null, newEntryValues);
    }

    public Cursor retrieveAllEntriesCursor() {
        Cursor c = null;

        try {
            String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
            c = _db.query(true,DATABASE_TABLE, columns,null, null, null, null, null, null);

            if (c != null) {
                c.moveToFirst();
            }
        } catch (SQLiteException e){
            e.printStackTrace();
        }
        return c;
    }

    public void deleteAllEntries(){
        _db.delete(DATABASE_TABLE, null, null);
    }

    public void deleteEntry(int rowNumber){
        _db.delete(DATABASE_TABLE, KEY_ID + "=" + rowNumber, null);
    }

    public ArrayList<AlarmsDeleteHelper> getAllEntriesIntoArrayList(){
        ArrayList<AlarmsDeleteHelper> mArrayList = new ArrayList<AlarmsDeleteHelper>();

        String[] columns = {KEY_ID, ENTRY_TITLE, ENTRY_DESTINATION, ENTRY_LATLNG, ENTRY_ALERTRADIUS, ENTRY_RINGTONE};
        Cursor cursor = _db.query(true,DATABASE_TABLE, columns, null, null, null, null, null, null);
        if (cursor != null){
            if (cursor.moveToFirst()) {

                do {
                    AlarmsDeleteHelper helper = new AlarmsDeleteHelper();
                    helper.setTitle(cursor.getString(cursor.getColumnIndex(ENTRY_TITLE)));
                    helper.setDestination(cursor.getString(cursor.getColumnIndex(ENTRY_DESTINATION)));
                    helper.setLatLng(cursor.getString(cursor.getColumnIndex(ENTRY_LATLNG)));
                    helper.setRadius(cursor.getString(cursor.getColumnIndex(ENTRY_ALERTRADIUS)));
                    helper.setRingTone(cursor.getString(cursor.getColumnIndex(ENTRY_RINGTONE)));

                    mArrayList.add(helper);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return mArrayList;
    }
}
