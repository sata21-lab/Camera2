package com.example.camera2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Camera2Activity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {
    private TextureView mTextureView;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraDevice mCameraDevice;
    private Surface mPreviewSurface;
    private CameraManager manager;
    private int WIDTH = 0;
    private int HEIGHT = 0;
    private int ScreenWidth = 0;
    private int ScreenHeight = 0;
    private String currentCamera = "0";
    private Button switch_camera;
    private ImageReader mImageReader;
    CameraCharacteristics characteristics;
    private Button capture;
    private Button ratio;
    Size previewSizes[];
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    private void front() {
        //前置时，照片竖直显示
        ORIENTATION.append(Surface.ROTATION_0, 270);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 90);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private void rear() {
        //后置时，照片竖直显示
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private CameraDevice.StateCallback mCameraStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            openPreview(HEIGHT, WIDTH);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };
    private CameraCaptureSession.StateCallback mCameraCaptureCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                mCameraCaptureSession = session;
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(mPreviewSurface);
                mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            showImage(reader);
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera2);
        initView();
        initCamera();
    }
    public void initView(){
        capture = findViewById(R.id.capture);
        capture.setOnClickListener(this);
        mTextureView = findViewById(R.id.texture_view);
        switch_camera = findViewById(R.id.switch_camera);
        switch_camera.setOnClickListener(this);
        ratio=findViewById(R.id.ratio);
        ratio.setOnClickListener(this);
    }
    public void initCamera(){
        rear();
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        ScreenWidth = metrics.widthPixels;
        ScreenHeight = metrics.heightPixels;
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(currentCamera);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap scmap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        previewSizes = scmap.getOutputSizes(ImageReader.class);
        for (int i = 0; i < previewSizes.length; i++) {
            if (previewSizes[i].getHeight() == ScreenWidth) {
                if ((previewSizes[i].getWidth() * 1.0 / previewSizes[i].getHeight() * 1.0) == 4.0 / 3.0) {
                    WIDTH = previewSizes[i].getHeight();
                    HEIGHT = previewSizes[i].getWidth();
                }
            }
        }
        mImageReader = ImageReader.newInstance(previewSizes[0].getWidth(), previewSizes[0].getHeight(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WIDTH, HEIGHT);
        mTextureView.setLayoutParams(layoutParams);
        mTextureView.setSurfaceTextureListener(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_camera:
                switch (currentCamera) {
                    case "0":
                        try {
                            mCameraCaptureSession.close();
                            mCameraDevice.close();
                            currentCamera = "1";
                            initCamera();
                            manager.openCamera(currentCamera, mCameraStateCallBack, null);
                            front();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "1":
                        try {
                            mCameraCaptureSession.close();
                            mCameraDevice.close();
                            currentCamera = "0";
                            initCamera();
                            manager.openCamera(currentCamera, mCameraStateCallBack, null);
                            rear();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                break;
            case R.id.capture:
                capture();
                break;
            case R.id.ratio:
                try {
                    WIDTH=1080;
                    HEIGHT=1920;
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WIDTH, HEIGHT);
                    mTextureView.setLayoutParams(layoutParams);
                    manager.openCamera(currentCamera, mCameraStateCallBack, null);
                    rear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    @SuppressLint({"MissingPermission", "CheckResult"})
    @Override
    protected void onResume() {//修复切换相机bug
        super.onResume();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(WIDTH, HEIGHT);
        mTextureView.setLayoutParams(layoutParams);
        if (mTextureView.isAvailable()) {
            initPermission();
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }


    private void capture() {
        try {
            //首先我们创建请求拍照的CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //设置拍照方向
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            //停止预览
            mCameraCaptureSession.stopRepeating();
            //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，所以会自动回调ImageReader的onImageAvailable()方法保存图片
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Toast.makeText(Camera2Activity.this, "拍照成功!", Toast.LENGTH_SHORT).show();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };
            mCameraCaptureSession.capture(mCaptureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showImage(ImageReader reader) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        matrix.postRotate(ORIENTATION.get(rotation));
        if (currentCamera.equals("1")) {
            matrix.postScale(-1, 1);
        }
        // 拿到图片数据
        Image image = reader.acquireLatestImage();
        // 获取字节缓冲
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        // 创建与缓冲区相同的字节数组
        byte[] bytes = new byte[buffer.remaining()];
        // 将数据读取字节数组
        buffer.get(bytes);
        // 创建图片
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap newBM = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        saveBitmap(newBM);
        image.close();
    }

    public void saveBitmap(final Bitmap bm) {
        final File f = new File(getExternalFilesDir(null), "111.png");
        if (f.exists()) {
            f.delete();
        }

        if (!f.exists()) {
            f.getParentFile().mkdir();
            try {
                //创建文件
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileOutputStream out = new FileOutputStream(f);
                        bm.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint({"MissingPermission", "CheckResult"})
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        surface.setDefaultBufferSize(HEIGHT, WIDTH);
        mPreviewSurface = new Surface(surface);
        initPermission();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void openPreview(int height, int width) {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(height, width);
        Surface surface = new Surface(texture);
        try {
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mCameraCaptureCallBack, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initPermission() {
        PermissionX.init(Camera2Activity.this)
                .permissions(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .explainReasonBeforeRequest()
                .onExplainRequestReason(new ExplainReasonCallback() {
                    @Override
                    public void onExplainReason(ExplainScope scope, List<String> deniedList) {
                        scope.showRequestReasonDialog(deniedList, "即将申请的权限是程序必须依赖的权限", "我已明白", "取消");
                    }
                })
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(ForwardScope scope, List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList, "您需要去应用程序设置当中手动开启权限", "我已明白", "取消");
                    }
                })
                .request(new RequestCallback() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onResult(boolean allGranted, List<String> grantedList, List<String> deniedList) {
                        if (allGranted) {
                            try {
                                manager.openCamera(currentCamera, mCameraStateCallBack, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(Camera2Activity.this, "拒绝授权", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
