package net.miamy.android.colordeterminer;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Miamy on 03.02.2018.
 */
class ColorPair
{
    private int color;
    private final String name;
    private final String code;

    private int blue;
    private int red;
    private int green;

    public ColorPair(String aLine)
    {
//        Log.d("colordeterminer", "ColorPair: " + aLine);
        String[] tokens = aLine.split("=");
        name = tokens[0].trim();
        code = tokens[1].trim();
        setColor(Color.parseColor(code));
    }

    public void setColor(int aColor)
    {
        color = aColor;

        blue = Color.blue(color);
        red = Color.red(color);
        green = Color.green(color);
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }
    public String getCode() {
        return code;
    }

    boolean IsEqual(int aColor, int aPrecision)
    {
        if (aColor == color)
            return true;
        if (aPrecision == 0)
            return false;
        int blue1 = Color.blue(aColor);
        int red1 = Color.red(aColor);
        int green1 = Color.green(aColor);
        return Math.abs(blue1 - blue) <= aPrecision && Math.abs(red1 - red) <= aPrecision && Math.abs(green1 - green) <= aPrecision;
    }

    double GetDifference(int aColor)
    {
        if (aColor == color)
            return 0;

        int blue1 = Color.blue(aColor);
        int red1 = Color.red(aColor);
        int green1 = Color.green(aColor);
//        //return Math.abs(blue1 - blue) + Math.abs(red1 - red) + Math.abs(green1 - green);
//        int max1 = Math.max(Math.abs(blue1 - blue), Math.abs(red1 - red));
//        return Math.max(max1, Math.abs(green1 - green));
        // compute the Euclidean distance between the two colors
        // note, that the alpha-component is not used in this example
        double dbl_test_red = Math.pow(red - red1, 2.0);
        double dbl_test_green = Math.pow(green - green1, 2.0);
        double dbl_test_blue = Math.pow(blue - blue1, 2.0);

        return dbl_test_blue + dbl_test_green + dbl_test_red;
    }
}


class ColorSpace
{
    private static final ColorSpace ourInstance = new ColorSpace();

    static ColorSpace getInstance()
    {
        return ourInstance;
    }

    private ColorSpace()
    {
        list = new ArrayList<>();
    }

    private static ArrayList<ColorPair> list;

    public void load(Context context, String filename) throws FileNotFoundException
    {
        File dataDir = Environment.//getDataDirectory();
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dataDir, filename);
        try
        {
            FileInputStream fIn = context.openFileInput ( file.getName() ) ;
            load(context, fIn);
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace ( ) ;
            throw  new FileNotFoundException();
        }
    }

    void load(Context context, InputStream stream) throws IOException
    {
        list.clear();
        try
        {
            InputStreamReader isr = new InputStreamReader ( stream ) ;
            BufferedReader buffReader = new BufferedReader ( isr ) ;

            String readString = buffReader.readLine ( ) ;
            while ( readString != null )
            {
                boolean skip = (readString.trim().isEmpty() || readString.startsWith(";") || readString.startsWith("//"));
                if (!skip)
                {
                    ColorPair pair = new ColorPair(readString);
                    list.add(pair);
                }
                readString = buffReader.readLine ( ) ;
            }

            isr.close ( ) ;
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace ( ) ;
            throw new IOException();
        }
    }

    ColorPair Find(int aColor)
    {
        ColorPair currPair = null;
        double currPrecision = 100000000;

        for (int i = 0; i < list.size(); i++)
        {
            ColorPair pair = list.get(i);
            double diff = pair.GetDifference(aColor);
            if(diff == 0.0)
            {
                currPair = pair;
                break;
            }
            else if (diff < currPrecision)
            {
                currPrecision = diff;
                currPair = pair;
            }
        }
        return currPair;
    }

    int Length()
    {
        return list.size();
    }
}

