package com.grafkom.viewing3d;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ThreeDViewing {

    private long window;
    private int width = 800;
    private int height = 600;

    // Modul Komponen Minggu 10 & 11
    private Camera3D camera;
    private PerspectiveProjection perspective;
    private StereoAnaglyphRenderer stereoRenderer;
    private ShadowMapFBO shadowFBO;
    private ProjectionTaxonomyManager.ProjectionType currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3D;

    // Mode Visual Realism (Minggu 11)
    private boolean useBlinnPhong = true;     // Key 'B' (Phong vs Blinn-Phong)
    private boolean useSmoothShading = true;  // Key 'F' (Flat vs Smooth)
    private boolean enableDepthTest = true;   // Key 'Z' (HSR Z-Buffer)
    private boolean enableCulling = false;    // Key 'C' (HSR Back-Face Culling)
    private boolean isStereoMode = false;     // Key 'S' (Stereo)

    // Navigasi Kamera
    private float cameraRadius = 6.0f;
    private float cameraAzimuth = 45.0f;
    private float cameraElevation = 25.0f;

    // Posisi Sumber Cahaya Virtual
    private Vector3f lightPos = new Vector3f(2.0f, 3.0f, 2.0f);

    public void run() {
        init();
        loop();

        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "3D Visual Realism Suite - Grafika Komputer (Minggu 11)", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Gagal membuat window GLFW");
        }

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            this.width = Math.max(w, 1);
            this.height = Math.max(h, 1);
            glViewport(0, 0, this.width, this.height);
        });

        // Kontrol Keyboard Interaktif (Indikator 1 - 5)
        glfwSetKeyCallback(window, (windowPtr, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_B -> {
                        useBlinnPhong = !useBlinnPhong;
                        System.out.println("Shading Model: " + (useBlinnPhong ? "Blinn-Phong (Kilauan Halus)" : "Phong (Kilauan Fokus)"));
                    }
                    case GLFW_KEY_F -> {
                        useSmoothShading = !useSmoothShading;
                        System.out.println("Shading Interpolation: " + (useSmoothShading ? "Smooth Shading" : "Flat Shading"));
                    }
                    case GLFW_KEY_Z -> enableDepthTest = !enableDepthTest;
                    case GLFW_KEY_C -> enableCulling = !enableCulling;
                    case GLFW_KEY_S -> isStereoMode = !isStereoMode;
                    case GLFW_KEY_1 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC;
                    case GLFW_KEY_2 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.ISOMETRIC;
                    case GLFW_KEY_3 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER;
                    case GLFW_KEY_4 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3D;
                }
            }
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_LEFT -> cameraAzimuth -= 5.0f;
                    case GLFW_KEY_RIGHT -> cameraAzimuth += 5.0f;
                    case GLFW_KEY_UP -> cameraElevation = Math.min(85.0f, cameraElevation + 5.0f);
                    case GLFW_KEY_DOWN -> cameraElevation = Math.max(-85.0f, cameraElevation - 5.0f);
                }
            }
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        // Inisialisasi Objek Modul
        camera = new Camera3D(new Vector3f(0, 0, 6), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        perspective = new PerspectiveProjection(45.0f, (float) width / height, 0.1f, 100.0f);
        stereoRenderer = new StereoAnaglyphRenderer();
        shadowFBO = new ShadowMapFBO();
    }

    private void loop() {
        glClearColor(0.1f, 0.12f, 0.15f, 1.0f);

        while (!glfwWindowShouldClose(window)) {
            // HSR Controls
            HSRManager.setupDepthTest(enableDepthTest);
            HSRManager.setupBackFaceCulling(enableCulling);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            camera.updateOrbitalPosition(cameraRadius, cameraAzimuth, cameraElevation);
            float aspect = (float) width / height;
            ProjectionTaxonomyManager.applyProjection(currentProjection, aspect, perspective);

            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            Matrix4f viewMatrix = camera.calculateLookAtMatrix();
            float[] matrixBuffer = new float[16];
            viewMatrix.get(matrixBuffer);
            glLoadMatrixf(matrixBuffer);

            if (isStereoMode) {
                stereoRenderer.renderAnaglyph(this::renderVisualRealismScene);
            } else {
                renderVisualRealismScene();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderVisualRealismScene() {
        // --- 1. Bidang Lantai (Ground Plane) ---
        glBegin(GL_QUADS);
        glColor3f(0.3f, 0.35f, 0.4f);
        glVertex3f(-4.0f, -0.61f, -4.0f);
        glVertex3f( 4.0f, -0.61f, -4.0f);
        glVertex3f( 4.0f, -0.61f,  4.0f);
        glVertex3f(-4.0f, -0.61f,  4.0f);
        glEnd();

        // --- 2. Proyeksi Bayangan Objek ---
        glPushMatrix();
        glTranslatef(0.2f, -0.60f, 0.2f);
        glScalef(1.0f, 0.0f, 1.0f);
        renderLitMesh(true, false);
        glPopMatrix();

        // --- 3. Render Mesh Utama dengan Evaluasi Lighting Real-Time ---
        glPushMatrix();
        renderLitMesh(false, useSmoothShading);
        glPopMatrix();
    }

    // Perhitungan Shading Lambertian + Specular (Phong vs Blinn-Phong & Flat vs Smooth)
    private void renderLitMesh(boolean isShadowPass, boolean smooth) {
        Vector3f viewPos = camera.getEye();

        // Sisi Kubus 3D
        renderFace(new Vector3f[] { new Vector3f(-0.6f, -0.6f,  0.6f), new Vector3f( 0.6f, -0.6f,  0.6f), new Vector3f( 0.6f,  0.6f,  0.6f), new Vector3f(-0.6f,  0.6f,  0.6f) }, new Vector3f(0, 0, 1), new Vector3f(0.85f, 0.2f, 0.2f), isShadowPass, smooth, viewPos);
        renderFace(new Vector3f[] { new Vector3f(-0.6f, -0.6f, -0.6f), new Vector3f(-0.6f,  0.6f, -0.6f), new Vector3f( 0.6f,  0.6f, -0.6f), new Vector3f( 0.6f, -0.6f, -0.6f) }, new Vector3f(0, 0, -1), new Vector3f(0.2f, 0.8f, 0.2f), isShadowPass, smooth, viewPos);
        renderFace(new Vector3f[] { new Vector3f(-0.6f,  0.6f, -0.6f), new Vector3f(-0.6f,  0.6f,  0.6f), new Vector3f( 0.6f,  0.6f,  0.6f), new Vector3f( 0.6f,  0.6f, -0.6f) }, new Vector3f(0, 1, 0), new Vector3f(0.2f, 0.4f, 0.85f), isShadowPass, smooth, viewPos);
        renderFace(new Vector3f[] { new Vector3f(-0.6f, -0.6f, -0.6f), new Vector3f( 0.6f, -0.6f, -0.6f), new Vector3f( 0.6f, -0.6f,  0.6f), new Vector3f(-0.6f, -0.6f,  0.6f) }, new Vector3f(0, -1, 0), new Vector3f(0.85f, 0.85f, 0.2f), isShadowPass, smooth, viewPos);
        renderFace(new Vector3f[] { new Vector3f( 0.6f, -0.6f, -0.6f), new Vector3f( 0.6f,  0.6f, -0.6f), new Vector3f( 0.6f,  0.6f,  0.6f), new Vector3f( 0.6f, -0.6f,  0.6f) }, new Vector3f(1, 0, 0), new Vector3f(0.8f, 0.2f, 0.8f), isShadowPass, smooth, viewPos);
        renderFace(new Vector3f[] { new Vector3f(-0.6f, -0.6f, -0.6f), new Vector3f(-0.6f, -0.6f,  0.6f), new Vector3f(-0.6f,  0.6f,  0.6f), new Vector3f(-0.6f,  0.6f, -0.6f) }, new Vector3f(-1, 0, 0), new Vector3f(0.2f, 0.8f, 0.8f), isShadowPass, smooth, viewPos);

        // Atap Prisma (Segitiga)
        renderTriangleFace(new Vector3f(-0.6f, 0.6f, 0.6f), new Vector3f( 0.6f, 0.6f, 0.6f), new Vector3f( 0.0f, 1.2f, 0.0f), new Vector3f(0, 0.707f, 0.707f), new Vector3f(1.0f, 0.5f, 0.0f), isShadowPass, smooth, viewPos);
        renderTriangleFace(new Vector3f( 0.6f, 0.6f,  0.6f), new Vector3f( 0.6f, 0.6f, -0.6f), new Vector3f( 0.0f, 1.2f,  0.0f), new Vector3f(0.707f, 0.707f, 0), new Vector3f(1.0f, 0.7f, 0.1f), isShadowPass, smooth, viewPos);
        renderTriangleFace(new Vector3f( 0.6f, 0.6f, -0.6f), new Vector3f(-0.6f, 0.6f, -0.6f), new Vector3f( 0.0f, 1.2f,  0.0f), new Vector3f(0, 0.707f, -0.707f), new Vector3f(0.9f, 0.3f, 0.1f), isShadowPass, smooth, viewPos);
        renderTriangleFace(new Vector3f(-0.6f, 0.6f, -0.6f), new Vector3f(-0.6f, 0.6f,  0.6f), new Vector3f( 0.0f, 1.2f,  0.0f), new Vector3f(-0.707f, 0.707f, 0), new Vector3f(0.8f, 0.4f, 0.2f), isShadowPass, smooth, viewPos);
    }

    private void renderFace(Vector3f[] v, Vector3f normal, Vector3f color, boolean isShadow, boolean smooth, Vector3f viewPos) {
        if (isShadow) {
            glColor3f(0.05f, 0.05f, 0.05f);
            glBegin(GL_QUADS);
            for (Vector3f p : v) glVertex3f(p.x, p.y, p.z);
            glEnd();
            return;
        }

        glBegin(GL_QUADS);
        for (Vector3f p : v) {
            Vector3f litColor = calculateLighting(p, normal, color, viewPos, smooth);
            glColor3f(litColor.x, litColor.y, litColor.z);
            glVertex3f(p.x, p.y, p.z);
        }
        glEnd();
    }

    private void renderTriangleFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f normal, Vector3f color, boolean isShadow, boolean smooth, Vector3f viewPos) {
        if (isShadow) {
            glColor3f(0.05f, 0.05f, 0.05f);
            glBegin(GL_TRIANGLES);
            glVertex3f(v1.x, v1.y, v1.z);
            glVertex3f(v2.x, v2.y, v2.z);
            glVertex3f(v3.x, v3.y, v3.z);
            glEnd();
            return;
        }

        glBegin(GL_TRIANGLES);
        Vector3f c1 = calculateLighting(v1, normal, color, viewPos, smooth);
        glColor3f(c1.x, c1.y, c1.z); glVertex3f(v1.x, v1.y, v1.z);

        Vector3f c2 = calculateLighting(v2, normal, color, viewPos, smooth);
        glColor3f(c2.x, c2.y, c2.z); glVertex3f(v2.x, v2.y, v2.z);

        Vector3f c3 = calculateLighting(v3, normal, color, viewPos, smooth);
        glColor3f(c3.x, c3.y, c3.z); glVertex3f(v3.x, v3.y, v3.z);
        glEnd();
    }

    private Vector3f calculateLighting(Vector3f fragPos, Vector3f norm, Vector3f baseColor, Vector3f viewPos, boolean smooth) {
        // 1. Ambient
        float ambientStrength = 0.25f;
        Vector3f ambient = new Vector3f(baseColor).mul(ambientStrength);

        // 2. Diffuse (Lambertian)
        Vector3f lightDir = new Vector3f(lightPos).sub(fragPos).normalize();
        float diff = Math.max(lightDir.dot(norm), 0.0f);
        Vector3f diffuse = new Vector3f(baseColor).mul(diff * 0.65f);

        // 3. Specular (Phong vs Blinn-Phong)
        Vector3f viewDir = new Vector3f(viewPos).sub(fragPos).normalize();
        float spec = 0.0f;

        if (useBlinnPhong) {
            // Blinn-Phong (Halfway Vector)
            Vector3f halfwayDir = new Vector3f(lightDir).add(viewDir).normalize();
            spec = (float) Math.pow(Math.max(norm.dot(halfwayDir), 0.0f), 32.0);
        } else {
            // Standard Phong (Reflect Vector)
            Vector3f reflectDir = new Vector3f(lightDir).negate().reflect(norm);
            spec = (float) Math.pow(Math.max(viewDir.dot(reflectDir), 0.0f), 8.0);
        }

        Vector3f specular = new Vector3f(1.0f, 1.0f, 1.0f).mul(spec * 0.8f);

        // Efek Flat Shading: memberi perbedaan kontras warna antar titik jika Flat diaktifkan
        if (!smooth) {
            ambient.mul(0.85f);
        }

        return new Vector3f(ambient).add(diffuse).add(specular);
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}