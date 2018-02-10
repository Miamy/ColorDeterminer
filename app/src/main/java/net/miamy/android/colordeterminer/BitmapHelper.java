package net.miamy.android.colordeterminer;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BitmapHelper
{

    public static int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height)
    {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), x, y, width, height);
        final int[] subsetPixels = new int[width * height];
        for (int row = 0; row < height; row++)
        {
            System.arraycopy(pixels, (row * bitmap.getWidth()), subsetPixels, row * width, width);
        }
        return subsetPixels;
    }

    public static int getAveragedColor(int[] bitmap)
    {
        long redBucket = 0;
        long greenBucket = 0;
        long blueBucket = 0;
        long pixelCount = 0;

        for (int i = 0; i < bitmap.length; i++)
        {
                int c = bitmap[i];
                pixelCount++;
            redBucket += (c >> 16) & 0xFF; // Color.red
            greenBucket += (c >> 8) & 0xFF; // Color.greed
            blueBucket += (c & 0xFF); // Color.blue
            // does alpha matter?
        }

        return getIntFromColor(redBucket / pixelCount,
                greenBucket / pixelCount,
                blueBucket / pixelCount);
    }

    private static int getIntFromColor(float Red, float Green, float Blue)
    {
        int R = Math.round(1 * Red);
        int G = Math.round(1 * Green);
        int B = Math.round(1 * Blue);
        return 0xff000000 | (R << 16) | (G << 8) | B;
    }

    public static int getDominantColor(int[]  pixels)
    {
        final List<HashMap<Integer, Integer>> colorMap = new ArrayList<>();
        colorMap.add(new HashMap<Integer, Integer>());
        colorMap.add(new HashMap<Integer, Integer>());
        colorMap.add(new HashMap<Integer, Integer>());

        int color;
        int r, g, b;
        Integer rC, gC, bC;
        for (int i = 0; i < pixels.length; i++) {
            color = pixels[i];

            r = Color.red(color);
            g = Color.green(color);
            b = Color.blue(color);

            rC = colorMap.get(0).get(r);
            if (rC == null)
                rC = 0;
            colorMap.get(0).put(r, ++rC);

            gC = colorMap.get(1).get(g);
            if (gC == null)
                gC = 0;
            colorMap.get(1).put(g, ++gC);

            bC = colorMap.get(2).get(b);
            if (bC == null)
                bC = 0;
            colorMap.get(2).put(b, ++bC);
        }

        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++)
        {
            int max = 0;
            int val = 0;
            for (Map.Entry<Integer, Integer> entry : colorMap.get(i).entrySet())
            {
                if (entry.getValue() > max)
                {
                    max = entry.getValue();
                    val = entry.getKey();
                }
            }
            rgb[i] = val;
        }

        return getIntFromColor(rgb[0], rgb[1], rgb[2]);
    }
}