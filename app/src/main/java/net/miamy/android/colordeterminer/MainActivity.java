package net.miamy.android.colordeterminer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.RelativeLayout.ALIGN_PARENT_END;
import static android.widget.RelativeLayout.BELOW;

public class MainActivity extends Activity implements Camera.PreviewCallback, SurfaceHolder.Callback
        {
    private ColorSpace colorSpace;
    private SurfaceView sv;
    private SurfaceHolder holder;
    private LayoutView transparentView;

    private Camera camera;
    private ImageView previewImage;

    private boolean lightOn = false;
    private int currCamera = 0;

    private AbsoluteLayout surfaceParent;
    private LinearLayout controlsParent;

    private Button flashButton;
    private Button camerasButton;

    private TextView foundColor;
    private TextView foundColorName;
    private TextView averagedColor;

    private RadioButton rbAveraged;
    private RadioButton rbDominant;

    private int counter = 0;
    private int oldAvgColor = 0;
    final int MaxPrecision = 30;
    final int clipSize = 10;

    private SharedPreferences preferences;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 555;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera)
        {
            showDialog(getString(R.string.camera_not_found));
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        InputStream raw = getResources().openRawResource(R.raw.colors_wiki);
        colorSpace = ColorSpace.getInstance();
        try
        {
            //colorSpace.load(this, "colors.txt");
            colorSpace.load(this, raw);
        }
        catch (IOException e)
        {
            //showDialog("File with color definitions not found.");
            //finish();
        }

        if (colorSpace.Length() == 0)
        {
            showDialog("Color definitions not found.");
            finish();
        }

        InitControls();

        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);

        LoadSettings();
        camerasButton.setText(currCamera == CAMERA_FACING_FRONT ? R.string.toBackCamera: R.string.toFrontCamera);
        transparentView.setDelta(clipSize);

    }

    @Override
    protected void onDestroy() {
        SaveSettings();
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        camera = Camera.open(currCamera);
        setPreviewSize();

        setControlsEnabled();

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        setupLayout(metrics.widthPixels, metrics.heightPixels);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_CAMERA:
            {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED)
                {
                    showDialog(getString(R.string.camera_not_accessible));
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (camera != null)
        {
            lightOff();
            camera.release();
        }
        camera = null;
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        setupLayout(newConfig.screenWidthDp, newConfig.screenHeightDp);

        setCameraDisplayOrientation(currCamera);
        setPreviewSize();
    }


    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        //currCamera = savedInstanceState.getInt("currCamera");
        LoadSettings();
        changeCameraClick(null);
    }


    protected void onSaveInstanceState(Bundle outState)
    {
        //super.onSaveInstanceState(outState);
        SaveSettings();
        outState.putInt("currCamera", currCamera);
    }

    private void setupLayout(int width, int height)
    {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        int where = (height > width) ? BELOW : ALIGN_PARENT_END;
        params.addRule(where, R.id.surfaceParent);
        controlsParent.setLayoutParams(params);
    }


    private void InitControls() {
        sv = findViewById(R.id.surfaceView);

        flashButton = findViewById(R.id.turnLight);
        camerasButton = findViewById(R.id.changeCamera);

        previewImage = findViewById(R.id.previewImage);
        foundColor = findViewById(R.id.foundColor);
        foundColorName = findViewById(R.id.foundColorName);
        averagedColor = findViewById(R.id.averagedColor);

        rbAveraged = findViewById(R.id.rbAveraged);
        rbDominant = findViewById(R.id.rbDominant);

        surfaceParent = findViewById(R.id.surfaceParent);
        controlsParent = findViewById(R.id.controlsParent);
        transparentView = (LayoutView) findViewById(R.id.TransparentView);
    }


    private void LoadSettings()
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currCamera = preferences.getInt("currCamera", CAMERA_FACING_BACK);
        boolean method = preferences.getBoolean("method", true);
        rbAveraged.setChecked(method);
        rbDominant.setChecked(!method);
    }
    private void SaveSettings()
    {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt("currCamera", currCamera );
        ed.putBoolean("method", rbAveraged.isChecked());
        ed.commit();
    }


    private void showDialog(String message)
    {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        }
        else
        {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setControlsEnabled()
    {
        final List<String> flashModes = camera.getParameters().getSupportedFlashModes();

        boolean lightDisabled = true;
        for (int i = 0; i < flashModes.size(); i++)
        {
            String mode = flashModes.get(i);
            if (mode.compareTo("torch") == 0)
            {
                lightDisabled = false;
                break;
            }
        }

        flashButton.setEnabled(!lightDisabled);

        int camerasNumber = Camera.getNumberOfCameras();
        if (camerasButton != null)
        {
            camerasButton.setEnabled(camerasNumber > 1);
        }
    }

    public void turnLightClick(View view)
    {
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(lightOn ? "off" : "torch");
        camera.setParameters(params);

        lightOn = !lightOn;
        flashButton.setText(lightOn ? R.string.lightOff : R.string.lightOn);
    }

    public void changeCameraClick(View view)
    {
        lightOff();
        if (camera != null)
        {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
        }
        currCamera = currCamera == CAMERA_FACING_BACK ? CAMERA_FACING_FRONT : CAMERA_FACING_BACK;
        camera = Camera.open(currCamera);
        camera.setPreviewCallback(this);
        setCameraDisplayOrientation(currCamera);
        try
        {
            camera.setPreviewDisplay(holder);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        setPreviewSize();
        camera.startPreview();

        camerasButton.setText(currCamera == CAMERA_FACING_FRONT ? R.string.toBackCamera: R.string.toFrontCamera);
    }

    private void lightOff()
    {
        if (lightOn)
        {
            turnLightClick(null);
        }
    }

    public void dominantMethodClick(View view)
    {
        rbAveraged.setChecked(false);
    }

    public void averagedMethodClick(View view)
    {
        rbDominant.setChecked(false);
    }

    private void setPreviewSize()
    {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        int width;
        int height;
        final int Dimension;

        if (metrics.heightPixels > metrics.widthPixels)
        {
            width = metrics.widthPixels;
            Dimension = 850;
            height = metrics.heightPixels - Dimension;
            //height = metrics.heightPixels / 2;
        }
        else
        {
            height = metrics.heightPixels;
            Dimension = 1000;
            width = metrics.widthPixels - Dimension;
            //width = metrics.widthPixels / 2;
        }
        surfaceParent.getLayoutParams().height = height;
        surfaceParent.getLayoutParams().width = width;
        //transparentView.getLayoutParams().height = height;
        //transparentView.getLayoutParams().width = width;

//        transparentView.getLayoutParams().height = height;
//        transparentView.getLayoutParams().width = width;

        boolean widthIsMax = width > height;

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        rectDisplay.set(0, 0, width, height);

        if (widthIsMax)
        {
            //noinspection SuspiciousNameCombination
            rectPreview.set(0, 0, size.height, size.width);
        }
        else
        {
            rectPreview.set(0, 0, size.width, size.height);
        }

        Matrix matrix = new Matrix();
        matrix.setRectToRect(rectDisplay, rectPreview, Matrix.ScaleToFit.CENTER);
        matrix.invert(matrix);
        matrix.mapRect(rectPreview);

        sv.getLayoutParams().height = (int) rectPreview.height();
        sv.getLayoutParams().width = (int) rectPreview.width();
        sv.setX(rectPreview.left);
        sv.setY(rectPreview.top);
        //transparentView.getLayoutParams().height = (int) (rectPreview.bottom);
        //transparentView.getLayoutParams().width = (int) (rectPreview.right);
        //transparentView.setX(rectPreview.left);
        //transparentView.setY(rectPreview.top);
    }

    private void setCameraDisplayOrientation(int cameraId)
    {
        int result = getRotationAngle(cameraId);
        camera.setDisplayOrientation(result);
    }

    private int getRotationAngle(int cameraId)
    {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == CAMERA_FACING_FRONT)
            {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }

        return result % 360;
    }

    //region Camera.PreviewCallback
            @Override
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                counter++;
                int maxCounter = 5;
                if (counter != maxCounter)
                    return;
                try
                {
                    counter = 0;
                    Camera.Parameters parameters = camera.getParameters();
                    int width = parameters.getPreviewSize().width;
                    int height = parameters.getPreviewSize().height;

                    YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

                    byte[] bytes = out.toByteArray();
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp == null)
                        return;

                    width = bmp.getWidth();
                    height = bmp.getHeight();
                    int angle = getRotationAngle(currCamera);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(angle);
                    Bitmap rotated = Bitmap.createBitmap (bmp,(width) / 2 - clipSize, (height ) / 2  - clipSize, 2 * clipSize, 2 * clipSize, matrix, true);

                    previewImage.setImageBitmap(rotated);

                    width = 2 * clipSize;
                    height = 2 * clipSize;
                    int[] centerPixels = new int[width * height];
                    rotated.getPixels(centerPixels, 0, width, 0, 0, width, height);

                    int avgColor;
                    if (rbAveraged.isChecked())
                    {
                        avgColor = BitmapHelper.getAveragedColor(centerPixels);
                    }
                    else
                    {
                        avgColor = BitmapHelper.getDominantColor(centerPixels);
                    }
                    if (oldAvgColor == avgColor)
                        return;

                    oldAvgColor = avgColor;
                    averagedColor.setBackgroundColor(avgColor);

                    foundColor.setBackgroundColor(0);
                    foundColorName.setText(getString(R.string.no_color));
                    ColorPair foundedColor = colorSpace.Find(avgColor);
                    if (foundedColor == null)
                        return;
                    foundColor.setBackgroundColor(foundedColor.getColor());
                    foundColorName.setText(foundedColor.getName());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            //endregion

    //region SurfaceHolder.Callback
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
                camera.stopPreview();
                setCameraDisplayOrientation(currCamera);
                try
                {
                    camera.setPreviewDisplay(holder);
                    //Camera.Parameters params = camera.getParameters();
                    //params.setPictureFormat(ImageFormat.JPEG);
                    //List<Integer> formats = params.getSupportedPictureFormats();
                    //for (int i = 0; i<formats.size(); i++)
                    // {
                    //    Log.d("colordeterminer", String.valueOf(formats.get(i)));
                    //}
                    //camera.setParameters(params);
                    camera.setPreviewCallback(this);
                    camera.startPreview();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
            }
            //endregion
}