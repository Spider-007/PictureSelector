package com.luck.picture.lib.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.rxbus2.RxUtils;
import com.luck.picture.lib.tools.MediaUtils;
import com.luck.picture.lib.tools.SdkVersionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * author：luck
 * project：LocalMediaLoader
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 */

public class LocalMediaLoader {
    /**
     * 类型
     */
    private int type;
    /**
     * 获取外部查询-->得到上下文内容提供器
     * 组合：媒体商店 + 所需文件夹 + 文件 ("external")//外部
     */
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    /**
     * 行的唯一id，得到行的desc-> 进行排序
     * 对id 进行排序， desc 指定排序方式
     */
    private static final String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
    /**
     * 没有gif
     * 拼接的字符串格式 "!='image/gif'" 代表无 gif
     */
    private static final String NOT_GIF = "!='image/gif'";
    /**
     * 过滤掉小于500毫秒的录音
     * 打开音频可以对音频进行控制
     */
    private static final int AUDIO_DURATION = 500;
    private Context mContext;
    private boolean isGif;
    /**
     * 最大录像值
     */
    private long videoMaxS;
    /**
     * 最小录像值
     */
    private long videoMinS;
    /**
     * 是否android10
     * */
    private boolean isAndroidQ;

    /**
     * 媒体文件数据库字段
     */
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID, //图片 _id
            MediaStore.MediaColumns.DATA, //获取媒体商店 媒体元数据列 的data
            MediaStore.MediaColumns.MIME_TYPE,//获取原始数据的列的mime,是只读的
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION};

    /**
     * {
     * 图片
     */
    private static final String SELECTION = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
            + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    /**
     * 设置是否支持gif}
     */
    private static final String SELECTION_NOT_GIF = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
            + " AND " + MediaStore.MediaColumns.SIZE + ">0"
            + " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF;

    /**
     * 查询条件(音视频)
     *
     * @param time_condition
     * @return
     */
    private static String getSelectionArgsForSingleMediaCondition(String time_condition) {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                + " AND " + time_condition;
    }

    /**
     * 查询条件(视频)
     *
     * @return
     */
    private static String getSelectionArgsForSingleMediaCondition() {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    }

    /**
     * 全部模式下条件
     *
     * @param time_condition
     * @param isGif
     * @return
     */
    private static String getSelectionArgsForAllMediaCondition(String time_condition, boolean isGif) {
        String condition = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + (isGif ? "" : " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                + " OR "
                + (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + time_condition) + ")"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0";
        return condition;
    }

    /**
     * 获取图片or视频
     */
    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };

    /**
     * 获取指定类型的文件
     *
     * @param mediaType
     * @return
     */
    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }

    public LocalMediaLoader(Context context, int type, boolean isGif, long videoMaxS, long videoMinS) {
        this.mContext = context.getApplicationContext();
        this.type = type;
        this.isGif = isGif;
        this.videoMaxS = videoMaxS;
        this.videoMinS = videoMinS;
        this.isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
    }

    public void loadAllMedia(final LocalMediaLoadListener imageLoadListener) {
        //抽象类方法回调
        RxUtils.io(new RxUtils.RxSimpleTask<List<LocalMediaFolder>>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @NonNull
            @Override
            public List<LocalMediaFolder> doSth(Object... objects) {
                String selection = null;
                String[] selectionArgs = null;
                switch (type) {
                    case PictureConfig.TYPE_ALL:
                        selection = getSelectionArgsForAllMediaCondition(getDurationCondition(0, 0), isGif);
                        selectionArgs = SELECTION_ALL_ARGS;
                        break;
                    case PictureConfig.TYPE_IMAGE:
                        // 只获取图片
                        selection = isGif ? SELECTION : SELECTION_NOT_GIF;
                        /**得到指定系统图库里面的图片 */
                        String[] MEDIA_TYPE_IMAGE = getSelectionArgsForSingleMediaType
                                (MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE); //得到pic 类型的数据
                        selectionArgs = MEDIA_TYPE_IMAGE;
                        break;
                    case PictureConfig.TYPE_VIDEO:
                        // 只获取视频
                        selection = getSelectionArgsForSingleMediaCondition();
                        selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
                        break;
                    case PictureConfig.TYPE_AUDIO:
                        selection = getSelectionArgsForSingleMediaCondition(getDurationCondition(0, AUDIO_DURATION));
                        String[] MEDIA_TYPE_AUDIO = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO);
                        selectionArgs = MEDIA_TYPE_AUDIO;
                        break;
                    default:
                        break;
                }
                Cursor data = mContext.getContentResolver().query(QUERY_URI, PROJECTION, selection, selectionArgs, ORDER_BY);
                try {
                    List<LocalMediaFolder> imageFolders = new ArrayList<>();
                    /**本地文件加载器*/
                    LocalMediaFolder allImageFolder = new LocalMediaFolder();
                    /**添加图片到系统目录*/
                    List<LocalMedia> latelyImages = new ArrayList<>();
                    /**当查询到的游标未关闭*/
                    if (data != null) {
                        int count = data.getCount();
                        if (count > 0) {
                            /**移动到 头一行*/
                            data.moveToFirst();
                            /**此处do while() 相等于 while(custor.moveTonext())*/
                            do {
                                /**返回给定列名从零开始的索引*/
                                long id = data.getLong
                                        (data.getColumnIndexOrThrow(PROJECTION[0]));

                                String path = isAndroidQ ? getRealPathAndroid_Q(id) : data.getString
                                        (data.getColumnIndexOrThrow(PROJECTION[1]));

                                String pictureType = data.getString
                                        (data.getColumnIndexOrThrow(PROJECTION[2]));

                                int w = data.getInt
                                        (data.getColumnIndexOrThrow(PROJECTION[3]));

                                int h = data.getInt
                                        (data.getColumnIndexOrThrow(PROJECTION[4]));

                                long duration = data.getLong
                                        (data.getColumnIndexOrThrow(PROJECTION[5]));

                                if (type == PictureConfig.TYPE_VIDEO) {
                                    if (duration == 0) {
                                        duration = MediaUtils.extractVideoDuration(mContext, isAndroidQ, path);
                                    }
                                    if (videoMinS > 0 && duration < videoMinS) {
                                        // 如果设置了最小显示多少秒的视频
                                        continue;
                                    }
                                    if (videoMaxS > 0 && duration > videoMaxS) {
                                        // 如果设置了最大显示多少秒的视频
                                        continue;
                                    }
                                    if (duration == 0) {
                                        // 时长如果为0，就当做损坏的视频处理过滤掉
                                        continue;
                                    }
                                }
                                /**拿到系统相册数据放到集合*/
                                LocalMedia image = new LocalMedia
                                        (path, duration, type, pictureType, w, h);
                                /**创建文件夹，不让系统相册一张一张的显示*/
                                LocalMediaFolder folder = getImageFolder(path, imageFolders);
                                /**得到文件夹，存放集合数据*/
                                List<LocalMedia> images = folder.getImages();
                                images.add(image);
                                /**设置显示的图片的数量*/
                                folder.setImageNum(folder.getImageNum() + 1);
                                /**加入相册，数据使用 arrayList 是基于数组，优点查询快，增删快，更改慢；链表则是更改快，查询慢!*/
                                latelyImages.add(image);
                                /**得到图片num,*/
                                int imageNum = allImageFolder.getImageNum();
                                allImageFolder.setImageNum(imageNum + 1);
                            } while (data.moveToNext());

                            if (latelyImages.size() > 0) {
                                sortFolder(imageFolders);
                                imageFolders.add(0, allImageFolder);
                                allImageFolder.setFirstImagePath
                                        (latelyImages.get(0).getPath());
                                String title = type == PictureMimeType.ofAudio() ?
                                        mContext.getString(R.string.picture_all_audio)
                                        : mContext.getString(R.string.picture_camera_roll);
                                allImageFolder.setName(title);
                                allImageFolder.setImages(latelyImages);
                            }
                            return imageFolders;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return getDefault();
            }

            @Override
            public List<LocalMediaFolder> getDefault() {
                return new ArrayList<>();
            }

            @Override
            public void onNext(List<LocalMediaFolder> imageFolders) {
                super.onNext(imageFolders);
                if (imageLoadListener != null) {
                    imageLoadListener.loadComplete(imageFolders);
                }
            }
        });
    }

    /**
     * 文件夹数量进行排序
     *
     * @param imageFolders
     */
    private void sortFolder(List<LocalMediaFolder> imageFolders) {
        // 文件夹按图片数量排序
        Collections.sort(imageFolders, (lhs, rhs) -> {
            if (lhs.getImages() == null || rhs.getImages() == null) {
                return 0;
            }
            int lsize = lhs.getImageNum();
            int rsize = rhs.getImageNum();
            return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
        });
    }

    /**
     * 适配Android Q
     *
     * @param id
     * @return
     */
    private String getRealPathAndroid_Q(long id) {
        return QUERY_URI.buildUpon().appendPath(String.valueOf(id)).build().toString();
    }

    /**
     * 创建相应文件夹
     *
     * @param path
     * @param imageFolders
     * @return
     */
    private LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path); //创建单个 pasth 图片
        File folderFile = imageFile.getParentFile();//得到每个图片的父级文件，创建文件夹文件
        for (LocalMediaFolder folder : imageFolders) { //遍历获取每个 folder
            // 同一个文件夹下，返回自己，否则创建新文件夹
            if (folder.getName().equals(folderFile.getName())) {//判断文件名称是否相同
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();//创建每一个显示 的本地文件夹存醋图片
        newFolder.setName(folderFile.getName());//获取文件夹名称赋初始值
        newFolder.setPath(folderFile.getAbsolutePath());//获取绝对路径 data/data/包名
        newFolder.setFirstImagePath(path); //获取第一张图片路径
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * 获取视频(最长或最小时间)
     *
     * @param exMaxLimit
     * @param exMinLimit
     * @return
     */
    private String getDurationCondition(long exMaxLimit, long exMinLimit) {
        long maxS = videoMaxS == 0 ? Long.MAX_VALUE : videoMaxS;
        if (exMaxLimit != 0) {
            maxS = Math.min(maxS, exMaxLimit);
        }

        return String.format(Locale.CHINA, "%d <%s " + MediaStore.MediaColumns.DURATION + " and " + MediaStore.MediaColumns.DURATION + " <= %d",
                Math.max(exMinLimit, videoMinS),
                Math.max(exMinLimit, videoMinS) == 0 ? "" : "=",
                maxS);
    }


    public interface LocalMediaLoadListener {
        void loadComplete(List<LocalMediaFolder> folders);
    }
}
