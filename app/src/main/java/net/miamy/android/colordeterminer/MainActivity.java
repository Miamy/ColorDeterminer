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
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class MainActivity extends Activity
{
    private ColorSpace colorSpace;
    private SurfaceView sv;
    private SurfaceHolder holder;
    private LayoutView transparentView;
    private Camera.PreviewCallback previewCallback;

    private Camera camera;
    private ImageView previewImage;

    private boolean lightOn = false;
    private int currCamera = 0;

    private final boolean FULL_SCREEN = true;

    private AbsoluteLayout surfaceParent;
    private LinearLayout controlsParent;

    private Button flashButton;
    private Button camerasButton;

    private TextView foundColor;
    private TextView foundColorName;
    private TextView averagedColor;

    private RadioButton rbAveraged;
    private RadioButton rbDominant;

    private SeekBar sbTolerance;

    private int counter = 0;
    final int MaxPrecision = 30;
    final int clipSize = 10;

    private SharedPreferences preferences;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 555;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        boolean hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (!hasCamera)
        {
            showDialog("Camera not found.");
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else
        {
        }
        /*String[] files =new String[1];
        try {
            files = getAssets().list("/");
            InputStream is = getAssets().open("colors.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("colordeterminer2", files.toString());*/

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

        HolderCallback holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);

        previewCallback = new PreviewCallback();

        LoadSettings();
        //setDrawable();
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
        setPreviewSize(FULL_SCREEN);

        setControlsEnabled();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_CAMERA:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                }
                else
                {
                    showDialog("Camera not accessible.");
                    finish();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            lightOff();
            camera.release();
        }
        camera = null;
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (newConfig.screenHeightDp > newConfig.screenWidthDp)
        {
            //params.removeRule(RelativeLayout.RIGHT_OF);
            params.addRule(RelativeLayout.BELOW, R.id.surfaceParent);
        }
        else
        {
            //params.removeRule(RelativeLayout.BELOW);
            params.addRule(RelativeLayout.ALIGN_PARENT_END, R.id.surfaceParent);
        }
        controlsParent.setLayoutParams(params);
        setCameraDisplayOrientation(currCamera);
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
        sbTolerance = findViewById(R.id.sbTolerance);
        transparentView = (LayoutView) findViewById(R.id.TransparentView);

    }


    private void LoadSettings()
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        currCamera = preferences.getInt("currCamera", CAMERA_FACING_BACK);
        boolean method = preferences.getBoolean("method", true);
        rbAveraged.setChecked(method);
        rbDominant.setChecked(!method);
        sbTolerance.setProgress(preferences.getInt("Tolerance", 10));
    }
    private void SaveSettings()
    {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt("currCamera", currCamera );
        ed.putBoolean("method", rbAveraged.isChecked());
        ed.putInt("Tolerance", sbTolerance.getProgress());
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
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        currCamera = currCamera == CAMERA_FACING_BACK ? CAMERA_FACING_FRONT : CAMERA_FACING_BACK;
        camera = Camera.open(currCamera);
        camera.setPreviewCallback(previewCallback);
        setCameraDisplayOrientation(currCamera);
        try
        {
            camera.setPreviewDisplay(holder);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        setPreviewSize(FULL_SCREEN);
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

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
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
                camera.setPreviewCallback(previewCallback);
                camera.startPreview();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    class PreviewCallback implements Camera.PreviewCallback
    {
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
//                int angle = getRotationAngle(currCamera);
//                Matrix matrix = new Matrix();
//                matrix.postRotate(angle);
                //Bitmap rotated/*bmp*/ =
                 bmp =
                        Bitmap.createBitmap (bmp,(width) / 2 - clipSize, (height ) / 2  - clipSize, 2 * clipSize, 2 * clipSize);
                        //Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                int angle = getRotationAngle(currCamera);
                Matrix matrix = new Matrix();
                matrix.postRotate(angle);
                Bitmap
                        rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                previewImage.setImageBitmap(rotated);

                width = rotated.getWidth();
                height = rotated.getHeight();
                int[] centerPixels = new int[4 * clipSize * clipSize];
                rotated.getPixels(centerPixels, 0, width, 0, 0, width, height);
//                BitmapHelper.getBitmapPixels(bmp, width / 2 - DeltaPixels, height / 2  - DeltaPixels, 2 * DeltaPixels, 2 * DeltaPixels);

                int avgColor;
                if (rbAveraged.isChecked())
                {
                    avgColor = BitmapHelper.getAveragedColor(centerPixels);
                }
                else
                {
                    avgColor = BitmapHelper.getDominantColor(centerPixels);
                }
                averagedColor.setBackgroundColor(avgColor);

                foundColor.setBackgroundColor(0);
                foundColorName.setText("no color");
                ColorPair foundedColor = colorSpace.Find(avgColor, sbTolerance.getProgress());
                if (foundedColor == null)
                    return;
                foundColor.setBackgroundColor(foundedColor.getColor());
                foundColorName.setText(foundedColor.getName());
//                Log.d("colordeterminer", "onPreviewFrame: avg = " + avgColor + ", founded = " + foundedColor.getColor() + ",  " +
//                        foundedColor.getName());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void setPreviewSize(boolean fullScreen)
    {
        Display display = getWindowManager().getDefaultDisplay();
//        boolean widthIsMax = display.getWidth() > display.getHeight();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        int width;
        int height;
        final int Dimension = 950;

        if (metrics.heightPixels > metrics.widthPixels)
        {
            width = metrics.widthPixels;
            //height = metrics.heightPixels - Dimension;
            height = metrics.heightPixels / 2;
        }
        else
        {
            height = metrics.heightPixels;
            //width = metrics.widthPixels - Dimension;
            width = metrics.widthPixels / 2;
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
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.CENTER);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.CENTER);
            matrix.invert(matrix);
        }
        matrix.mapRect(rectPreview);


        sv.getLayoutParams().height = (int) rectPreview.height(); //(int) rectPreview.bottom;
        sv.getLayoutParams().width = (int) rectPreview.right;
        //sv.setX(rectPreview.left);
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
}