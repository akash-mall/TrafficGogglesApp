package com.example.trafficgoggles;

import android.content.res.AssetManager;

import java.io.IOException;

public class Yolov3Classifier extends Classifier {

    protected float mObjThresh = 0.1f;

    public Yolov3Classifier(AssetManager assetManager) throws IOException {
        super(assetManager, "final_quantize.tflite", "label.txt", 416);
        mAnchors = new int[]{
                10,13,  16,30,  33,23,  30,61,  62,45,  59,119,  116,90,  156,198,  373,326
        };

        mMasks = new int[][]{{6,7,8},{3,4,5},{0,1,2}};
        //output dimensions for 416, change according to input size
        mOutWidth = new int[]{13,26,52};
        mObjThresh = 0.4f;
    }

    @Override
    protected float getObjThresh() {
        return mObjThresh;
    }
}
