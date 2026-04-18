package com.example.trustpay.ui.liveness;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnLivenessDetectedListener {
        void onLivenessDetected();
    }

    public interface OnInstructionUpdateListener {
        void onInstructionUpdate(String message);
    }

    private final OnLivenessDetectedListener listener;
    private final OnInstructionUpdateListener instructionListener;
    private boolean isCompleted = false;

    public FaceAnalyzer(OnLivenessDetectedListener listener) {
        this(null, listener);
    }

    public FaceAnalyzer(OnInstructionUpdateListener instructionListener,
                        OnLivenessDetectedListener listener) {
        this.instructionListener = instructionListener;
        this.listener = listener;
    }

    private boolean isBlinkDetected = false;
    private boolean isHeadTurnDetected = false;
    private boolean isStraightFaceReady = false;

    private final FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    private final FaceDetector detector = FaceDetection.getClient(options);

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isCompleted) {
            imageProxy.close();
            return;
        }

        @SuppressLint("UnsafeOptInUsageError")
        Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage,
                            imageProxy.getImageInfo().getRotationDegrees());

            detector.process(image)
                    .addOnSuccessListener(faces -> {

                        if (faces.isEmpty() && instructionListener != null && !isCompleted) {
                            instructionListener.onInstructionUpdate("Show your face clearly");
                        }

                        if (faces.size() > 1 && instructionListener != null && !isCompleted) {
                            instructionListener.onInstructionUpdate("Only one face should be visible");
                            return;
                        }

                        for (Face face : faces) {

                            // 👁️ Blink Detection
                            Float leftEye = face.getLeftEyeOpenProbability();
                            Float rightEye = face.getRightEyeOpenProbability();

                            if (leftEye != null && rightEye != null && leftEye < 0.4 && rightEye < 0.4) {
                                isBlinkDetected = true;
                            }

                            // 🔄 Head Turn Detection
                            float rotY = face.getHeadEulerAngleY();

                            if (Math.abs(rotY) > 15) {
                                isHeadTurnDetected = true;
                            }

                            boolean eyesOpenNow = leftEye != null
                                    && rightEye != null
                                    && leftEye > 0.6
                                    && rightEye > 0.6;
                            boolean faceStraightNow = Math.abs(rotY) < 8;
                            isStraightFaceReady = isBlinkDetected
                                    && isHeadTurnDetected
                                    && eyesOpenNow
                                    && faceStraightNow;

                            if (!isCompleted && instructionListener != null) {
                                if (!isBlinkDetected) {
                                    instructionListener.onInstructionUpdate("Blink your eyes");
                                } else if (!isHeadTurnDetected) {
                                    instructionListener.onInstructionUpdate("Turn your face left or right");
                                } else if (!isStraightFaceReady) {
                                    instructionListener.onInstructionUpdate("Look straight at camera");
                                } else {
                                    instructionListener.onInstructionUpdate("Liveness verified");
                                }
                            }

                            // ✅ Liveness Check
                            if (isStraightFaceReady && !isCompleted) {
                                isCompleted = true;

                                Log.d("LIVENESS", "Liveness Verified ✅");

                                if (listener != null) {
                                    listener.onLivenessDetected();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (instructionListener != null && !isCompleted) {
                            instructionListener.onInstructionUpdate("Unable to analyze face. Try again");
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    public void close() {
        detector.close();
    }
}
