package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class VisualRealismApp {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    private long window;
    private Camera3D camera;
    private PerspectiveProjection projection;
    private ShaderProgram sceneShader;
    private ShaderProgram depthShader;
    private ShadowMapFBO shadowMapFBO;
    private Texture2D sphereTexture;
    private Mesh3D sphereMesh;
    private Mesh3D groundMesh;

    private boolean useBlinnPhong = true;      // Key 'B'
    private boolean useFlatShading = false;   // Key 'F'
    private boolean enableZBuffer = true;     // Key 'Z'
    private boolean enableCulling = true;     // Key 'C'
    private boolean useBilinearMipmap = true; // Key 'T'
    private boolean useTexture = true;        // Key 'M'

    private float azimuth = 35.0f;
    private float elevation = 25.0f;
    private float rotationAngle = 0.0f;

    private final Vector3f lightPos = new Vector3f(3.0f, 5.0f, 3.0f);

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "3D Visual Realism Suite - Minggu 11 & 12", 0, 0);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        setupKeyCallbacks();

        camera = new Camera3D(new Vector3f(0, 2, 6), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        projection = new PerspectiveProjection(45.0f, (float) WIDTH / HEIGHT, 0.1f, 50.0f);

        sceneShader = new ShaderProgram("/shaders/scene.vert", "/shaders/scene.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowMapFBO = new ShadowMapFBO();
        sphereTexture = new Texture2D("textures/brick.png");

        sphereMesh = Mesh3D.createSphere(1.0f, 32, 24);
        groundMesh = Mesh3D.createGroundPlane(6.0f, -1.2f);
    }

    private void setupKeyCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                if (key == GLFW_KEY_LEFT) azimuth -= 4.0f;
                if (key == GLFW_KEY_RIGHT) azimuth += 4.0f;
                if (key == GLFW_KEY_UP) elevation = Math.min(85.0f, elevation + 4.0f);
                if (key == GLFW_KEY_DOWN) elevation = Math.max(-20.0f, elevation - 4.0f);
            }
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_B) useBlinnPhong = !useBlinnPhong;
                if (key == GLFW_KEY_F) useFlatShading = !useFlatShading;
                if (key == GLFW_KEY_Z) enableZBuffer = !enableZBuffer;
                if (key == GLFW_KEY_C) enableCulling = !enableCulling;
                if (key == GLFW_KEY_M) useTexture = !useTexture;
                if (key == GLFW_KEY_T) {
                    useBilinearMipmap = !useBilinearMipmap;
                    sphereTexture.setMipmapFiltering(useBilinearMipmap);
                }
                updateWindowTitle();
            }
        });
        updateWindowTitle();
    }

    private void updateWindowTitle() {
        String title = String.format(
            "Visual Realism | [B] %s | [F] %s | [Z] Z-Buf:%s | [C] Cull:%s | [T] Filter:%s",
            useBlinnPhong ? "Blinn-Phong" : "Phong",
            useFlatShading ? "Flat" : "Smooth",
            enableZBuffer ? "ON" : "OFF",
            enableCulling ? "ON" : "OFF",
            useBilinearMipmap ? "Mipmap" : "Nearest"
        );
        glfwSetWindowTitle(window, title);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            rotationAngle += 0.4f;
            camera.updateOrbitalPosition(6.0f, azimuth, elevation);

            HSRManager.setupDepthTest(enableZBuffer);
            HSRManager.setupBackFaceCulling(enableCulling);

            Matrix4f lightProj = new Matrix4f().ortho(-7.0f, 7.0f, -7.0f, 7.0f, 1.0f, 20.0f);
            Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
            Matrix4f lightSpaceMatrix = new Matrix4f(lightProj).mul(lightView);

            Matrix4f sphereModel = new Matrix4f()
                .translate(0.0f, 0.2f, 0.0f)
                .rotateY((float) Math.toRadians(rotationAngle));
            Matrix4f groundModel = new Matrix4f();

            // PASS 1: Render Depth Map dari sudut pandang cahaya
            shadowMapFBO.bindForWriting();
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
            depthShader.setMat4("model", sphereModel);
            sphereMesh.draw();
            depthShader.setMat4("model", groundModel);
            groundMesh.draw();
            shadowMapFBO.unbind(WIDTH, HEIGHT);

            // PASS 2: Render Scene Utama dengan Shading, Texture & Shadow
            glClearColor(0.12f, 0.14f, 0.18f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            sceneShader.use();
            sceneShader.setMat4("view", camera.calculateLookAtMatrix());
            sceneShader.setMat4("projection", projection.calculateProjectionMatrix());
            sceneShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
            sceneShader.setVec3("lightPos", lightPos);
            sceneShader.setVec3("viewPos", camera.getEye());
            sceneShader.setVec3("lightColor", new Vector3f(1.0f, 0.98f, 0.95f));
            sceneShader.setBool("useBlinnPhong", useBlinnPhong);
            sceneShader.setBool("useFlatShading", useFlatShading);

            sphereTexture.bind(0);
            shadowMapFBO.bindDepthTexture(1);
            sceneShader.setInt("diffuseTexture", 0);
            sceneShader.setInt("shadowMap", 1);

            sceneShader.setMat4("model", sphereModel);
            sceneShader.setBool("useTexture", useTexture);
            sceneShader.setVec3("objectColor", new Vector3f(0.85f, 0.35f, 0.25f));
            sphereMesh.draw();

            sceneShader.setMat4("model", groundModel);
            sceneShader.setBool("useTexture", false);
            sceneShader.setVec3("objectColor", new Vector3f(0.45f, 0.48f, 0.52f));
            groundMesh.draw();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new VisualRealismApp().run();
    }
}