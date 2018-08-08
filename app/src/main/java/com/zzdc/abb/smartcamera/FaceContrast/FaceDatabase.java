package com.zzdc.abb.smartcamera.FaceContrast;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = CameraDatabase.class)
public class FaceDatabase extends BaseModel {
    @PrimaryKey(autoincrement = true)
    public int id;//通知唯一id
    @Column
    public long times;//设置通知时间，long类型
    @Column
    public byte[] face;
    @Column
    public String name;
    @Column
    public boolean focus;

}
