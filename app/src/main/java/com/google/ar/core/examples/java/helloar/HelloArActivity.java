/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.common.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.common.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.google.ar.core.examples.java.helloar.BytesConverter.intToByte;
import static com.google.ar.core.examples.java.helloar.CoordinatesConversion.rotate;
import static com.google.ar.core.examples.java.helloar.CoordinatesConversion.shiftCameraBackward;
import static com.google.ar.core.examples.java.helloar.CoordinatesConversion.shiftCameraForward;
import static com.google.ar.core.examples.java.helloar.CoordinatesConversion.shiftCameraLeft;
import static com.google.ar.core.examples.java.helloar.CoordinatesConversion.shiftCameraRight;


public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = HelloArActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;

    private boolean installRequested;

    private Session session;
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
    private TapHelper tapHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private ArrayList<ObjectRenderer> virtualObject = new ArrayList<>();
    private float[] scales = new float[]{0.1f, 0.005f};

    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();




    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[16];

    private DBHelper dbHelper;

    private int mWidth;
    private int mHeight;
    private int savedWidth;
    private int savedHeight;
    private boolean capturePicture = false;
    private Pose cameraPose;
    private String photoPath;
    private int[] bitmapData;
    private int numObj = 2;
    private boolean tap = false;
    private boolean drawPlanes = false;

    private Button saveButton, undoButton, photoButton, leftButton, rightButton, rotateButton, forwardButton, backButton, nextButton, prevButton, okButton, upButton, downButton;
    private TextView photoText, planesText, editText, saveText;
    private boolean tutorial = false;


    private ArrayList<AnchorModel> anchors = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        setContentView(R.layout.activity_main);

        super.onCreate(savedInstanceState);

        File f = new File(this.getFilesDir(), "tutorial");
        if (!f.isFile()) {
            tutorial = true;
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveButton = findViewById(R.id.save);
        saveButton.setVisibility(View.GONE);
        undoButton = findViewById(R.id.undo);
        undoButton.setVisibility(View.GONE);
        photoButton = findViewById(R.id.photo);
        leftButton = findViewById(R.id.left);
        leftButton.setVisibility(View.GONE);
        rightButton = findViewById(R.id.right);
        rightButton.setVisibility(View.GONE);
        rotateButton = findViewById(R.id.rotate);
        rotateButton.setVisibility(View.GONE);
        nextButton = findViewById(R.id.next);
        nextButton.setVisibility(View.GONE);
        prevButton = findViewById(R.id.prev);
        prevButton.setVisibility(View.GONE);
        upButton = findViewById(R.id.up);
        upButton.setVisibility(View.GONE);
        forwardButton = findViewById(R.id.forward);
        forwardButton.setVisibility(View.GONE);
        backButton = findViewById(R.id.back);
        backButton.setVisibility(View.GONE);
        okButton = findViewById(R.id.ok);
        okButton.setVisibility(View.GONE);
        nextButton = findViewById(R.id.next);
        nextButton.setVisibility(View.GONE);
        downButton = findViewById(R.id.down);
        downButton.setVisibility(View.GONE);


        photoText = findViewById(R.id.text_photo);
        planesText = findViewById(R.id.text_planes);
        editText = findViewById(R.id.text_edit);
        saveText = findViewById(R.id.text_save);
        planesText.setVisibility(View.GONE);
        editText.setVisibility(View.GONE);
        saveText.setVisibility(View.GONE);
        photoText.setVisibility(View.GONE);
        if (tutorial) {
            photoText.setVisibility(View.VISIBLE);
        }



        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        tapHelper = new TapHelper(/*context=*/ this);
        surfaceView.setOnTouchListener(tapHelper);

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        surfaceView.setWillNotDraw(false);

        installRequested = false;

        dbHelper = new DBHelper(this);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }


                // Create the session.
                session = new Session(/* context= */ this);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            session = null;
            return;
        }

        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            for (int i = 0; i < numObj; i++) {
                virtualObject.add(new ObjectRenderer());
            }

            virtualObject.get(0).createOnGlThread(/*context=*/ this, "models/ArcticFox_Posed.obj", "models/ArcticFox_Diffuse.png");
            virtualObject.get(0).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObject.get(1).createOnGlThread(/*context=*/ this, "models/Mesh_Wolf.obj", "models/Tex_Wolf.png");
            virtualObject.get(1).setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (capturePicture) {
            capturePicture = false;
            try {
                SavePicture();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = session.update();
            Camera camera = frame.getCamera();

            // Handle one tap per frame.
            handleTap(frame, camera);

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            // If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            // Use try-with-resources to automatically release the point cloud.
            try (PointCloud pointCloud = frame.acquirePointCloud()) {
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);
            }

            // No tracking error at this point. If we detected any plane, then hide the
            // message UI, otherwise show searchingPlane message.

            // Visualize planes.
            if (drawPlanes) {
                planeRenderer.drawPlanes(
                        session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
            }

            // Visualize anchors created by touch.
            for (AnchorModel anchorModel : anchors) {
                if (anchorModel.anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchorModel.anchor.getPose().toMatrix(anchorMatrix, 0);

                // Update and draw the model and its shadow.

                virtualObject.get(anchorModel.model).updateModelMatrix(anchorMatrix, scales[anchorModel.model]);
                virtualObject.get(anchorModel.model).draw(viewmtx, projmtx, colorCorrectionRgba);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Frame frame, Camera camera) {
        if (!tap) {
            return;
        }
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon
                Trackable trackable = hit.getTrackable();
                // Creates an anchor if a plane or an oriented point was hit.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size() >= 20) {
                        anchors.get(0).anchor.detach();
                        anchors.remove(0);
                    }


                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(new AnchorModel(hit.createAnchor(), 0));
                    this.tap = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            upButton.setVisibility(View.VISIBLE);
                            downButton.setVisibility(View.VISIBLE);
                            forwardButton.setVisibility(View.VISIBLE);
                            backButton.setVisibility(View.VISIBLE);
                            leftButton.setVisibility(View.VISIBLE);
                            rightButton.setVisibility(View.VISIBLE);
                            rotateButton.setVisibility(View.VISIBLE);
                            prevButton.setVisibility(View.VISIBLE);
                            nextButton.setVisibility(View.VISIBLE);
                            okButton.setVisibility(View.VISIBLE);
                            if (tutorial) {
                                if (planesText.getVisibility() == View.VISIBLE) {
                                    planesText.setVisibility(View.GONE);
                                    editText.setVisibility(View.VISIBLE);
                                } else {
                                    saveText.setVisibility(View.GONE);
                                    tutorial = false;
                                }
                            }
                        }
                    });
                    break;
                }
            }
        }
    }

    public void onSavePicture(View view) {
        this.capturePicture = true;
        tap = true;
        drawPlanes = true;
    }


    public void SavePicture() throws IOException {

        int pixelData[] = new int[mWidth * mHeight];
        savedHeight = mHeight;
        savedWidth = mWidth;


        // Read the pixels from the current GL frame.
        IntBuffer buf = IntBuffer.wrap(pixelData);
        buf.position(0);
        GLES20.glReadPixels(0, 0, mWidth, mHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

        bitmapData = new int[pixelData.length];
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                int p = pixelData[i * mWidth + j];
                int b = (p & 0x00ff0000) >> 16;
                int r = (p & 0x000000ff) << 16;
                int ga = p & 0xff00ff00;
                bitmapData[(mHeight - i - 1) * mWidth + j] = ga | r | b;
            }
        }

        File file = new File(this.getFilesDir(), "filename" + dbHelper.getNextRoomID());
        try (FileOutputStream fos = new FileOutputStream(file.getPath())) {
            fos.write(intToByte(bitmapData));
        }

        try {
            Frame frame = session.update();
            cameraPose = frame.getCamera().getDisplayOrientedPose();
            photoPath = file.getPath();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photoButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
                undoButton.setVisibility(View.VISIBLE);
                if (tutorial) {
                    photoText.setVisibility(View.GONE);
                    planesText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void onSaveClick(View view) {
        LayoutInflater li = LayoutInflater.from(HelloArActivity.this);
        View promptsView = li.inflate(R.layout.dialog_signin, null);

        //Создаем AlertDialog
        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(HelloArActivity.this);

        //Настраиваем prompt.xml для нашего AlertDialog:
        mDialogBuilder.setView(promptsView);

        //Настраиваем отображение поля для ввода текста в открытом диалоге:
        final EditText userInput = promptsView.findViewById(R.id.input_text);

        //Настраиваем сообщение в диалоговом окне:
        mDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                long roomID = dbHelper.addRoom(userInput.getText().toString(), cameraPose, photoPath, savedWidth, savedHeight);
                                dbHelper.addObjects(roomID, anchors);

                                finish();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        //Создаем AlertDialog:
        AlertDialog alertDialog = mDialogBuilder.create();

        //и отображаем его:
        alertDialog.show();
    }

    public void onUndoClick(View view) {
        if (anchors.size() > 0) {
            anchors.get(anchors.size() - 1).anchor.detach();
            anchors.remove(anchors.size() - 1);
            onOKClick(view);
        }
    }

    public void onPrevClick(View view) {
        if (anchors.size() > 0) {
            anchors.get(anchors.size() - 1).model = (anchors.get(anchors.size() - 1).model + numObj - 1) % numObj;
        }
    }

    public void onNextClick(View view) {
        if (anchors.size() > 0) {
            anchors.get(anchors.size() - 1).model = (anchors.get(anchors.size() - 1).model + 1) % numObj;
        }
    }

    public void onForwardClick(View view) {
        if (anchors.size() > 0) {

            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = shiftCameraForward(pose);
            changeLastAnchorPose(np);
        }
    }

    public void onBackClick(View view) {
        if (anchors.size() > 0) {

            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = shiftCameraBackward(pose);
            changeLastAnchorPose(np);
        }
    }

    public void onLeftClick(View view) {
        if (anchors.size() > 0) {
            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = shiftCameraLeft(pose);
            changeLastAnchorPose(np);
        }
    }

    public void onRightClick(View view) {
        if (anchors.size() > 0) {
            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = shiftCameraRight(pose);
            changeLastAnchorPose(np);
        }
    }

    public void onUpClick(View view) {
        if (anchors.size() > 0) {
            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = new Pose(new float[]{pose.tx(), pose.ty() + 0.01f, pose.tz()}, pose.getRotationQuaternion());
            changeLastAnchorPose(np);
        }
    }

    public void onDownClick(View view) {
        if (anchors.size() > 0) {
            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = new Pose(new float[]{pose.tx(), pose.ty() - 0.01f, pose.tz()}, pose.getRotationQuaternion());
            changeLastAnchorPose(np);
        }
    }

    public void onRotateClick(View view) {
        if (anchors.size() > 0) {
            Pose pose = anchors.get(anchors.size() - 1).anchor.getPose();
            Pose np = rotate(pose);
            changeLastAnchorPose(np);
        }
    }

    private void changeLastAnchorPose(Pose newPose) {
        AnchorModel a = anchors.get(anchors.size() - 1);
        anchors.get(anchors.size() - 1).anchor.detach();
        anchors.remove(anchors.size() - 1);
        Anchor anchor = session.createAnchor(newPose);
        anchors.add(new AnchorModel(anchor, a.model));
    }

    public void onOKClick(View view) {
        upButton.setVisibility(View.GONE);
        downButton.setVisibility(View.GONE);
        forwardButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        leftButton.setVisibility(View.GONE);
        rightButton.setVisibility(View.GONE);
        rotateButton.setVisibility(View.GONE);
        prevButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        okButton.setVisibility(View.GONE);
        tap = true;
        if (tutorial) {
            editText.setVisibility(View.GONE);
            saveText.setVisibility(View.VISIBLE);
        }
    }
}