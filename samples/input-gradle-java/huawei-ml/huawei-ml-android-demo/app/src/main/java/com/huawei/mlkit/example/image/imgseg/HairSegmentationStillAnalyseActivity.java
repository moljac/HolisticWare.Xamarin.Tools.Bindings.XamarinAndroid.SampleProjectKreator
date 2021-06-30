/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.huawei.mlkit.example.image.imgseg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentation;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationAnalyzer;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationScene;
import com.huawei.hms.mlsdk.imgseg.MLImageSegmentationSetting;
import com.huawei.mlkit.example.R;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

public class HairSegmentationStillAnalyseActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = HairSegmentationStillAnalyseActivity.class.getSimpleName();

    private MLImageSegmentationAnalyzer analyzer;

    private ImageView mImageView;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_segmentation_analyse);
        this.findViewById(R.id.segment_detect).setOnClickListener(this);
        this.mImageView = this.findViewById(R.id.image_result);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.segment_detect:
                this.analyzer();
                break;
            default:
                break;
        }
    }

    private void analyzer() {
        /**
         * Configure image segmentation analyzer with custom parameter MLImageSegmentationSetting.
         *
         * setExact(): Set the segmentation fine mode, true is the fine segmentation mode,
         *     and false is the speed priority segmentation mode.
         * setAnalyzerType(): Set the segmentation mode. When segmenting a static image, support setting
         *     MLImageSegmentationSetting.BODY_SEG (only segment human body and background)
         *     and MLImageSegmentationSetting.IMAGE_SEG (segment 10 categories of scenes, including human bodies)
         * setScene(): Set the type of the returned results. This configuration takes effect only in
         *     MLImageSegmentationSetting.BODY_SEG mode. In MLImageSegmentationSetting.IMAGE_SEG mode,
         *     only pixel-level tagging information is returned.
         *     Supports setting MLImageSegmentationScene.ALL (returns all segmentation results,
         *     including: pixel-level tag information, portrait images with transparent backgrounds
         *     and portraits are white, gray background with black background),
         *     MLImageSegmentationScene.MASK_ONLY (returns only pixel-level tag information),
         *     MLImageSegmentationScene .FOREGROUND_ONLY (returns only portrait images with transparent background),
         *     MLImageSegmentationScene.GRAYSCALE_ONLY (returns only grayscale images with white portrait and black background).
         */
        MLImageSegmentationSetting setting = new MLImageSegmentationSetting.Factory()
                .setExact(false)
                .setAnalyzerType(MLImageSegmentationSetting.HAIR_SEG)
                .create();
        this.analyzer = MLAnalyzerFactory.getInstance().getImageSegmentationAnalyzer(setting);
        // Create an MLFrame by using android.graphics.Bitmap. Recommended image size: large than 224*224.
        this.bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.imgseg_foreground);
        MLFrame mlFrame = new MLFrame.Creator().setBitmap(this.bitmap).create();
        Task<MLImageSegmentation> task = this.analyzer.asyncAnalyseFrame(mlFrame);
        task.addOnSuccessListener(new OnSuccessListener<MLImageSegmentation>() {
            @Override
            public void onSuccess(MLImageSegmentation imageSegmentationResult) {
                // Processing logic for recognition success.
                if (imageSegmentationResult != null) {
                    HairSegmentationStillAnalyseActivity.this.displaySuccess(imageSegmentationResult);
                } else {
                    HairSegmentationStillAnalyseActivity.this.displayFailure("imageSegmentationResult is null.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Processing logic for recognition failure.
                HairSegmentationStillAnalyseActivity.this.displayFailure(e.getMessage());
            }
        });
    }

    private void displaySuccess(MLImageSegmentation imageSegmentationResult) {
        if (this.bitmap == null) {
            this.displayFailure("bitmap is null.");
            return;
        }
        // Draw the portrait with a transparent background.
        if (imageSegmentationResult.getMasks() == null) {
            return;
        }
        Bitmap processedBitmap = null;
        int[] pixels = byteArrToIntArr(imageSegmentationResult.getMasks());
        processedBitmap = Bitmap.createBitmap(pixels, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        if (processedBitmap != null) {
            this.mImageView.setImageBitmap(processedBitmap);
        } else {
            this.displayFailure("bitmapFore is null.");
        }
    }

    private void displayFailure(String str) {
        Log.e(TAG, str);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.analyzer != null) {
            try {
                this.analyzer.stop();
            } catch (IOException e) {
                Log.e(TAG, "Stop failed: " + e.getMessage());
            }
        }
    }

    /**
     * 蒙版
     * @param masks
     * @return
     */
    private int[] byteArrToIntArr(byte[] masks) {
        int[] results = new int[masks.length];
        for (int i = 0; i < masks.length; i++) {
            if (masks[i] == 1) {
                results[i] = Color.WHITE;
            } else if (masks[i] == 2) {
                results[i] = Color.BLUE;
            } else if (masks[i] == 3) {
                results[i] = Color.DKGRAY;
            } else if (masks[i] == 4) {
                results[i] = Color.YELLOW;
            } else if (masks[i] == 5) {
                results[i] = Color.LTGRAY;
            } else if (masks[i] == 6) {
                results[i] = Color.CYAN;
            } else if (masks[i] == 7) {
                results[i] = Color.RED;
            } else if (masks[i] == 8) {
                results[i] = Color.GRAY;
            } else if (masks[i] == 9) {
                results[i] = Color.MAGENTA;
            } else if (masks[i] == 10) {
                results[i] = Color.GREEN;
            } else {
                results[i] = Color.BLACK;
            }
        }
        return results;
    }
}
