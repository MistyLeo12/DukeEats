package edu.duke.compsci290.finalproject;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by isaac on 4/23/18.
 */

@Dao
public interface MomentDao {

    @Query("SELECT * FROM moments")
    List<Moment> getAll();

    @Insert
    void insert(Moment moment);

    @Delete
    void delete(Moment moment);

    @Query("SELECT * FROM moments WHERE firebaseid LIKE :id")
    List<Moment> getAllByFirebaseId(String id);

}
