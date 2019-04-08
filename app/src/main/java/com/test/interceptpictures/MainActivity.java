package com.test.interceptpictures;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    Button bt_photographs;
    Button bt_albums;
    RoundImageView riv_;
    boolean have_permission = false;
    String imgName = "12345";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Initialization();
    }
    private void Initialization() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }else {
            have_permission = true;
        }

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        final File file = new File(path + "/plants");
        if (!file.exists()) {
            try {
                //按照指定的路径创建文件夹
                file.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        bt_photographs = findViewById(R.id.bt_photographs);
        bt_albums = findViewById(R.id.bt_albums);
        riv_ = findViewById(R.id.riv_);
        bt_photographs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(have_permission){
                    takeCameraOnly();
                }else {
                    Log.e("MainActivity","permission:"+have_permission);
                }
            }
        });
        bt_albums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(have_permission){
                    gotoAlbum();
                }else {
                    Log.e("MainActivity","permission:"+have_permission);
                }
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==123){
            boolean isAllGranted = true;
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    have_permission = false;
                    break;
                }
            }
            if (isAllGranted) {
                have_permission = true;
            }
        }
    }

    private Uri imageUri;
    private Uri imageCropUri;
    private File cropFile;
    private static final int RESULT_CAMERA_ONLY = 101;
    private static final int RESULT_ALBUM_ONLY_THROUGH_URI =102;
    private static final int CROP_PATH_RESULT = 103;

    void takeCameraOnly() {
        File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+ "/plants/temp"+imgName+".jpg");//创建拍照图片文件
        imageUri = Uri.fromFile(file2);
        File cropFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/plants/temp"+imgName+"_crop.jpg");//创建截图图片文件
        imageCropUri = Uri.fromFile(cropFile);
        Intent intent = null;
        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//action is capture
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, RESULT_CAMERA_ONLY);
    }
    void gotoAlbum(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        startActivityForResult(intent, RESULT_ALBUM_ONLY_THROUGH_URI);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("onActivityResult","requestCode："+requestCode+"resultCode:"+resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case RESULT_CAMERA_ONLY: {
                File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+ "/plants/temp"+ imgName+".jpg");// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                imageUri = Uri.fromFile(file2);
                cropFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/plants/temp"+ imgName+"_crop.jpg");
                imageCropUri = Uri.fromFile(cropFile);
                cropImg(imageUri);
            }
            break;
            case RESULT_ALBUM_ONLY_THROUGH_URI:{
                String picPath = parsePicturePath(MainActivity.this,data.getData());
                File file = new File(picPath);
                Uri uri = Uri.fromFile(file);
                cropFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/plants/temp"+ imgName+"_crop.jpg");
                imageCropUri = Uri.fromFile(cropFile);
                cropImg(uri);
            }
            break;
            case CROP_PATH_RESULT: {
                if (getTempFile() != null) {
                    try {
                        imageCropUri = Uri.fromFile(cropFile);
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageCropUri));
                        if (bitmap != null) {
                            Bitmap smallBmp = setScaleBitmap(bitmap, 2);
                            riv_.setImageBitmap(smallBmp);
                        }
                        if(!TextUtils.isEmpty(imageUri.toString())){
                            File file =  new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+ "/plants/temp"+ imgName+".jpg");
                            file.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    Log.e("CROP_PATH_RESULT","getTempFile=null");
                }
            }
            break;
        }
    }

    private File getTempFile() {
        try {
            return new File(imageCropUri.toString());
        } catch (Exception e) {
            Log.d("harvic", e.getMessage());
        }
        return null;
    }
    private Bitmap setScaleBitmap(Bitmap photo,int SCALE) {
        if (photo != null) {
            //为防止原始图片过大导致内存溢出，这里先缩小原图显示，然后释放原始Bitmap占用的内存
            //这里缩小了1/2,但图片过大时仍然会出现加载不了,但系统中一个BITMAP最大是在10M左右,我们可以根据BITMAP的大小
            //根据当前的比例缩小,即如果当前是15M,那如果定缩小后是6M,那么SCALE= 15/6
            Bitmap smallBitmap = zoomBitmap(photo, photo.getWidth() / SCALE, photo.getHeight() / SCALE);
            //释放原始图片占用的内存，防止out of memory异常发生
            photo.recycle();
            return smallBitmap;
        }
        return null;
    }
    public Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);// 利用矩阵进行缩放不会造成内存溢出
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        return newbmp;
    }
    public void cropImg(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 700);
        intent.putExtra("outputY", 700);
        intent.putExtra("return-data", false);
//        Log.i("TAG","imageCropUri;"+imageCropUri.toString());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCropUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, CROP_PATH_RESULT);
    }

    public static String parsePicturePath(Context context, Uri uri) {

        if (null == context || uri == null)
            return null;

        boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentUri
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageDocumentsUri
            if (isExternalStorageDocumentsUri(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] splits = docId.split(":");
                String type = splits[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + splits[1];
                }
            }
            // DownloadsDocumentsUri
            else if (isDownloadsDocumentsUri(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaDocumentsUri
            else if (isMediaDocumentsUri(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = "_id=?";
                String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosContentUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;

    }
    private static boolean isExternalStorageDocumentsUri(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocumentsUri(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocumentsUri(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosContentUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            try {
                if (cursor != null)
                    cursor.close();
            } catch (Exception e) {
                Log.e("harvic",e.getMessage());
            }
        }
        return null;

    }
}
