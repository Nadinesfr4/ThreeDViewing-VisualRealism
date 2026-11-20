package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class StereoAnaglyphRenderer {
    private float ipd = 0.15f;          // Interpupillary Distance

    public void renderAnaglyph(Runnable renderSceneCallback) {
        // PASS 1: Mata Kiri (Merah)
        glColorMask(true, false, false, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        glPushMatrix();
        glTranslatef(ipd / 2.0f, 0, 0);
        renderSceneCallback.run();
        glPopMatrix();

        // PASS 2: Mata Kanan (Cyan / Hijau-Biru)
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);
        
        glPushMatrix();
        glTranslatef(-ipd / 2.0f, 0, 0);
        renderSceneCallback.run();
        glPopMatrix();

        // Reset Color Mask
        glColorMask(true, true, true, true);
    }
}