package edu.duke.compsci290.finalproject;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by isaac on 4/23/18.
 */
@Database(entities = {Moment.class}, version = 2)
public abstract class MomentDatabase extends RoomDatabase {
    private static MomentDatabase INSTANCE;

    public abstract MomentDao momentDao();

    public static MomentDatabase getInMemoryDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE =
                    Room.databaseBuilder(context.getApplicationContext(),
                            MomentDatabase.class, "moment.db")
                            .fallbackToDestructiveMigration()
                            .build();
        }
        return INSTANCE;
    }

}
