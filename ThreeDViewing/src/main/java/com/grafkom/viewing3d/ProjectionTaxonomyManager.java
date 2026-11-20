package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.*;

public class ProjectionTaxonomyManager {
    public enum ProjectionType {
        ORTHOGRAPHIC, ISOMETRIC, OBLIQUE_CAVALIER, PERSPECTIVE_3D
    }

    public static void applyProjection(ProjectionType type, float aspect, PerspectiveProjection perspective) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        float size = 2.5f;
        switch (type) {
            case ORTHOGRAPHIC -> glOrtho(-size * aspect, size * aspect, -size, size, 0.1, 50.0);
            case ISOMETRIC -> {
                glOrtho(-size * aspect, size * aspect, -size, size, 0.1, 50.0);
                glRotatef(35.264f, 1.0f, 0.0f, 0.0f);
                glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
            }
            case OBLIQUE_CAVALIER -> {
                glOrtho(-size * aspect, size * aspect, -size, size, 0.1, 50.0);
                float[] shearMatrix = {
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0.5f * (float)Math.cos(Math.toRadians(45)), 0.5f * (float)Math.sin(Math.toRadians(45)), 1, 0,
                    0, 0, 0, 1
                };
                glMultMatrixf(shearMatrix);
            }
            case PERSPECTIVE_3D -> {
                perspective.setAspectRatio(aspect);
                Matrix4f proj = perspective.calculateProjectionMatrix();
                float[] buf = new float[16];
                proj.get(buf);
                glLoadMatrixf(buf);
            }
        }
    }
}