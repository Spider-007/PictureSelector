package com.luck.picture.lib.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class LocalMediaFolder implements Parcelable {
    /**文件目录名字*/
    private String name;
    /**文件夹路径*/
    private String path;
    /**文件首选图片路径*/
    private String firstImagePath;
    /**imageNum的图片数量*/
    private int imageNum;
    /**选中数量*/
    private int checkedNum;
    /**是否选中*/
    private boolean isChecked;
    /**得到本地所有图片*/
    private List<LocalMedia> images = new ArrayList<LocalMedia>();


    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFirstImagePath() {
        return firstImagePath;
    }

    public void setFirstImagePath(String firstImagePath) {
        this.firstImagePath = firstImagePath;
    }

    public int getImageNum() {
        return imageNum;
    }

    public void setImageNum(int imageNum) {
        this.imageNum = imageNum;
    }

    public List<LocalMedia> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    public void setImages(List<LocalMedia> images) {
        this.images = images;
    }

    public int getCheckedNum() {
        return checkedNum;
    }

    public void setCheckedNum(int checkedNum) {
        this.checkedNum = checkedNum;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** writeTo parcel->是序列化写入文件，序列化存储较为复杂，可以用系统插件代替-> plugin ->parcel</> */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        /**写入文件，name,path,firstImagePath......*/
        dest.writeString(this.name);
        dest.writeString(this.path);
        dest.writeString(this.firstImagePath);
        dest.writeInt(this.imageNum);
        dest.writeInt(this.checkedNum);
        dest.writeByte(this.isChecked ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.images);
    }

    public LocalMediaFolder() {
    }

    /**读取文件，name,path,firstImagePath,imageNum,checkedNum,isChecked,images.....*/
    protected LocalMediaFolder(Parcel in) {
        this.name = in.readString();
        this.path = in.readString();
        this.firstImagePath = in.readString();
        this.imageNum = in.readInt();
        this.checkedNum = in.readInt();
        this.isChecked = in.readByte() != 0;
        this.images = in.createTypedArrayList(LocalMedia.CREATOR);
    }

    public static final Parcelable.Creator<LocalMediaFolder> CREATOR = new Parcelable.Creator<LocalMediaFolder>() {
        @Override
        public LocalMediaFolder createFromParcel(Parcel source) {
            return new LocalMediaFolder(source);
        }

        @Override
        public LocalMediaFolder[] newArray(int size) {
            return new LocalMediaFolder[size];
        }
    };




}
