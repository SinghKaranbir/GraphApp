package com.spartan.karanbir.graphapp;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glUniform3fv;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.vrtoolkit.cardboard.audio.CardboardAudioEngine;
import com.spartan.karanbir.graphapp.util.*;


public class MyRenderer implements CardboardView.StereoRenderer {

    private static final String TAG = MyRenderer.class.getSimpleName();
    private static final String A_COLOR = "a_Color";
    private static final String A_POSITION = "a_Position";
    private static final String U_MATRIX = "u_Matrix";
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int BYTES_PER_FLOAT = 4;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;
    private static final float CAMERA_Z = 0.01f;
    private final FloatBuffer vertexData;
    private final Context context;
    private int program;
    private int aPositionLocation, aColorLocation, uMatrixLocation, cubeModelParam, cubeModelViewParam, cubeModelViewProjectionParam,cubeLightPosParam, cubeNormalParam;
    private int STRIDE = (POSITION_COMPONENT_COUNT+COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private static final float MAX_MODEL_DISTANCE = 7.0f;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private float[] modelCube = new float[16];
    private float[] camera = new float[16];
    private float[] view = new float[16];
    private float[] headView = new float[16];
    private float[] modelPosition = new float[]{0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 1.0f};
    private final float[] lightPosInEyeSpace = new float[4];
    public MyRenderer(Context context) {
        this.context = context;

        float[] lineVertices = {

                // Line 1
                -0.5f, 0f,0.0f,0.0f,1.0f,
                0.5f, 0f,0.0f,0.0f,1.0f,

                //Line2
                -0.5f,0f,0.0f,0.0f,1.0f,
                -0.5f,0.5f,0.0f,0.0f,1.0f,

        };

        vertexData = ByteBuffer
                .allocateDirect(lineVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        vertexData.put(lineVertices);
    }


    @Override
    public void onSurfaceCreated(EGLConfig config) {
        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.passthrough_vertex);
        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.fragment);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aColorLocation = glGetAttribLocation(program, A_COLOR);
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        //cubeNormalParam = GLES20.glGetAttribLocation(program, "a_Normal");
        cubeModelParam = GLES20.glGetUniformLocation(program, "u_Model");
        cubeModelViewParam = GLES20.glGetUniformLocation(program, "u_MVMatrix");
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(program, "u_MVP");
        cubeLightPosParam = GLES20.glGetUniformLocation(program, "u_LightPos");
        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION_LOCATION.
       //vertexData.position(0);
      //  glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
       //         false, STRIDE, vertexData);
      //  vertexData.position(POSITION_COMPONENT_COUNT);
      //  glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);
        glEnableVertexAttribArray(aPositionLocation);
        glEnableVertexAttribArray(aColorLocation);
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, 0, -2.5f, 0);
      updateModelPosition();
    }

    /**
     * onSurfaceChanged is called whenever the surface has changed. This is
     * called at least once when the surface is initialized. Keep in mind that
     * Android normally restarts an Activity on rotation, and in that case, the
     * renderer will be destroyed and a new one created.
     *
     * @param width
     *            The new width, in pixels.
     * @param height
     *            The new height, in pixels.
     */
    @Override
    public void onSurfaceChanged(int width, int height) {
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    public void drawLines() {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        //glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        glUseProgram(program);
        glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        glUniformMatrix4fv(cubeModelParam, 1, false, modelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        glUniformMatrix4fv(cubeModelViewParam, 1, false, modelMatrix, 0);
        vertexData.position(0);
        // Set the position of the cube
        glVertexAttribPointer(
                aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT, false, STRIDE, vertexData);
        vertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_COUNT, GL_FLOAT,false,STRIDE,vertexData);
        // Set the ModelViewProjection matrix in the shader.
        glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, projectionMatrix, 0);

        // Draw the center dividing line.
        glDrawArrays(GL_LINES, 0, 2);
        glDrawArrays(GL_LINES,2,2);


    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        Log.i(TAG, "onNewFrame");
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        headTransform.getHeadView(headView, 0);


    }

    @Override
    public void onDrawEye(Eye eye) {
        Log.i(TAG, "onDrawEye");
        glEnable(GLES20.GL_DEPTH_TEST);
        glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

// Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);
        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelMatrix, 0, view, 0, modelCube, 0);
        Matrix.multiplyMM(projectionMatrix, 0, perspective, 0, modelMatrix, 0);
        drawLines();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
        Log.i(TAG, "onFinishFrame");
    }



    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    /**
     * Updates the cube model position.
     */
    private void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);


    }
}
