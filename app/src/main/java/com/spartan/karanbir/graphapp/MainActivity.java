package com.spartan.karanbir.graphapp;

import android.opengl.Matrix;
import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.vrtoolkit.cardboard.audio.CardboardAudioEngine;
import android.opengl.GLES20;
import android.os.Vibrator;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    //Declaring the vertex positions  of X_LINE_COORD
    private static final float[] X_LINE_COORD = new float[]{0.0f, 0.0f, -3.0f,
                                                            10.0f, 0.0f, -3.0f,
                                                            };

    //Declaring the Color for each Vertex of X_LINE_COLOR
    private static final float[] X_LINE_COLOR = new float[]{0.0f, 0.0f, 0.0f, 0.0f,
                                                            0.0f, 0.0f, 0.0f, 0.0f};

    //Declaring the vertex positions of Y_Line_COORD
    private static final float[] Y_LINE_COORD = new float[]{ 0.0f, 0.0f, -3.0f,
                                                            0.0f, 10.0f, -3.0f,
                                                            };

    //Declaring the Color for each Vertex of Y_LINE_COLOR
    private static final float[] Y_LINE_COLOR = new float[]{ 0.0f, 0.0f, 0.0f, 0.0f,
                                                            0.0f, 0.0f, 0.0f, 0.0f};

    private FloatBuffer xLineVertices, yLineVertices, xLineColor, yLineColor;

    private int lineProgram;
    private final static int BYTES_PER_FLOAT = 4;


    // I don't know about these.
    private int cubePositionParam;
    private int cubeNormalParam;
    private int cubeColorParam;
    private int cubeModelParam;
    private int cubeModelViewParam;
    private int cubeModelViewProjectionParam;
    private int cubeLightPosParam;

    private float[] modelCube;
    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelFloor;

    private float[] modelPosition;
    private float[] headRotation;

    private int score = 0;
    private float objectDistance = MAX_MODEL_DISTANCE / 2.0f;
    private float floorDepth = 20f;

    private Vibrator vibrator;

    private static final String TAG = "MainActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private static final int COORDS_PER_VERTEX = 3;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 1.0f};

    private static final float MIN_MODEL_DISTANCE = 3.0f;
    private static final float MAX_MODEL_DISTANCE = 7.0f;

    private static final String SOUND_FILE = "cube_sound.wav";

    private final float[] lightPosInEyeSpace = new float[4];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRestoreGLStateEnabled(false);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        modelCube = new float[16];
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelFloor = new float[16];
        // Model first appears directly in front of user.
        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
        headRotation = new float[4];
        headView = new float[16];

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the Model part of the ModelView matrix.
        Matrix.rotateM(modelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(headView, 0);



    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawXLine();


    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        // Not much useful
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {


        ByteBuffer bbVertices = ByteBuffer.allocateDirect(X_LINE_COORD.length * BYTES_PER_FLOAT);
        bbVertices.order(ByteOrder.nativeOrder());
        xLineVertices = bbVertices.asFloatBuffer();
        xLineVertices.put(X_LINE_COORD);
        xLineVertices.position(0);

        ByteBuffer xLineColors = ByteBuffer.allocateDirect(X_LINE_COLOR.length * BYTES_PER_FLOAT);
        xLineColors.order(ByteOrder.nativeOrder());
        xLineColor = xLineColors.asFloatBuffer();
        xLineColor.put(X_LINE_COLOR);
        xLineColor.position(0);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Y_LINE_COORD.length * BYTES_PER_FLOAT);
        byteBuffer.order(ByteOrder.nativeOrder());
        yLineVertices = byteBuffer.asFloatBuffer();
        yLineVertices.put(Y_LINE_COORD);
        yLineVertices.position(0);

        ByteBuffer yLineColors = ByteBuffer.allocateDirect(Y_LINE_COLOR.length * BYTES_PER_FLOAT);
        yLineColors.order(ByteOrder.nativeOrder());
        yLineColor = yLineColors.asFloatBuffer();
        yLineColor.put(Y_LINE_COLOR);
        yLineColor.position(0);


        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader);

        lineProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(lineProgram, vertexShader);
        GLES20.glAttachShader(lineProgram,fragmentShader);
        GLES20.glLinkProgram(lineProgram);
        GLES20.glUseProgram(lineProgram);


        cubePositionParam = GLES20.glGetAttribLocation(lineProgram, "a_Position");
        //cubeNormalParam = GLES20.glGetAttribLocation(lineProgram, "a_Normal");
        cubeColorParam = GLES20.glGetAttribLocation(lineProgram, "u_Color");

        //cubeModelParam = GLES20.glGetUniformLocation(lineProgram, "u_Model");
        //cubeModelViewParam = GLES20.glGetUniformLocation(lineProgram, "u_MVMatrix");
        //cubeModelViewProjectionParam = GLES20.glGetUniformLocation(lineProgram, "u_MVP");
        //cubeLightPosParam = GLES20.glGetUniformLocation(lineProgram, "u_LightPos");

        GLES20.glEnableVertexAttribArray(cubePositionParam);
        //GLES20.glEnableVertexAttribArray(cubeNormalParam);
        GLES20.glEnableVertexAttribArray(cubeColorParam);

        updateModelPosition();


    }

    /**
     * Updates the cube model position.
     */
    private void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);
    }
    @Override
    public void onRendererShutdown() {

    }

    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void drawXLine() {

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(lineProgram);
;

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(cubePositionParam, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                0, xLineVertices);







        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelViewProjection, 0);


        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);


    }
}
