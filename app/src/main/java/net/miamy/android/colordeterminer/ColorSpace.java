package net.miamy.android.colordeterminer;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Miamy on 03.02.2018.
 */
class ColorPair
{
    int color;
    String name;

    int blue;
    int red;
    int green;

    public ColorPair(int aColor, String aName)
    {
        color = aColor;
        name = aName;
    }
    public ColorPair(String aLine)
    {
        String[] tokens = aLine.split("=");
        name = tokens[0];
        color = Color.parseColor(tokens[1]);

        blue = Color.blue(color);
        red = Color.red(color);
        green = Color.green(color);
    }

    public boolean IsEqual(int aColor, int aPrecision)
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
        list = new ArrayList<ColorPair>();
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

    public void load(Context context, InputStream stream) throws IOException
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

    public ColorPair Find(int aColor, int aPrecision)
    {
        int currPrecision = 0;
        while (currPrecision <= aPrecision)
        {
            for (int i = 0; i < list.size(); i++)
            {
                ColorPair pair = list.get(i);
                if (pair.IsEqual(aColor, currPrecision))
                {
                    return pair;
                }
            }
            currPrecision++;
        }

        return null;
    }

    public ColorPair Find(int aColor)
    {
        return Find(aColor, 0);
    }
}
