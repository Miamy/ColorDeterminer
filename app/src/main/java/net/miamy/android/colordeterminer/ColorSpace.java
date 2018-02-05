package net.miamy.android.colordeterminer;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Miamy on 03.02.2018.
 */
class ColorPair
{
    private int color;
    private String name;

    private int blue;
    private int red;
    private int green;

    public ColorPair(int aColor, String aName)
    {
        name = aName;
        setColor(aColor);
    }

    public ColorPair(String aLine)
    {
        String[] tokens = aLine.split("=");
        name = tokens[0];
        setColor(Color.parseColor(tokens[1]));
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

    int GetDifference(int aColor)
    {
        if (aColor == color)
            return 0;

        int blue1 = Color.blue(aColor);
        int red1 = Color.red(aColor);
        int green1 = Color.green(aColor);
        return Math.abs(blue1 - blue) + Math.abs(red1 - red) + Math.abs(green1 - green);
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

    ColorPair Find(int aColor, int aPrecision)
    {
        /*int currPrecision = 0;
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
        }*/

        //ArrayList<ColorPair, int> founded = new ArrayList<>();
        final List<HashMap<ColorPair, Integer>> colorMap = new ArrayList<>();
        ColorPair currPair = null;
        int currPrecision = 100000000;
        for (int i = 0; i < list.size(); i++)
        {
            ColorPair pair = list.get(i);
            int diff = pair.GetDifference(aColor);
            if (diff <= aPrecision)
            {
                //HashMap<ColorPair, Integer> h = new HashMap<>();
                //h.put(pair, diff);
                //colorMap.add(h);
                if (diff < currPrecision)
                {
                    currPair = pair;
                    currPrecision = diff;
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            colorMap.sort(new MyComparator());
        }
        //colorMap.sort()
        return currPair;
    }

    public ColorPair Find(int aColor)
    {
        return Find(aColor, 0);
    }

    int Length()
    {
        return list.size();
    }
}

class MyComparator implements Comparator
{
    @Override
    public int compare(Object o1, Object o2)
    {
        return 0;
    }
}
