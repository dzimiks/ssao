package ssao;

import org.lwjgl.BufferUtils;
import rafgl.RGL;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.*;

/**
 * FrameBufferObject class.
 *
 * @author dzimiks
 */
public class FrameBufferObject {

    private int bufferHandle;
    private int colorTex;
    private int normalTex;
    private int positionTex;

    public FrameBufferObject(boolean G, int colorTex) {
        this.colorTex = colorTex;

        bufferHandle = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, bufferHandle);

        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                colorTex,
                0);

        if (G) {
            normalTex = Utils.getTexture(
                    null,
                    RGL.getWidth(),
                    RGL.getHeight(),
                    GL_RGB16F,
                    GL_REPEAT);

            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT1,
                    GL_TEXTURE_2D,
                    normalTex,
                    0);

            positionTex = Utils.getTexture(
                    null,
                    RGL.getWidth(),
                    RGL.getHeight(),
                    GL_RGB16F,
                    GL_REPEAT);

            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT2,
                    GL_TEXTURE_2D,
                    positionTex,
                    0);

            // Notify
            int buffers[] = {
                    GL_COLOR_ATTACHMENT0,
                    GL_COLOR_ATTACHMENT1,
                    GL_COLOR_ATTACHMENT2
            };

            IntBuffer buf = BufferUtils.createIntBuffer(buffers.length);
            buf.put(buffers);
            buf.flip();
            glDrawBuffers(buf);
        }

        // Add depth buffer
        int depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);

        glRenderbufferStorage(
                GL_RENDERBUFFER,
                GL_DEPTH_COMPONENT24,
                RGL.getWidth(),
                RGL.getHeight());

        glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_RENDERBUFFER,
                depthBuffer);

        // Check for completeness
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if (status == GL_FRAMEBUFFER_COMPLETE) {
            RGL.log("GL_FRAMEBUFFER is OK!");
        } else {
            RGL.log("GL_FRAMEBUFFER failed!");
            throw new RuntimeException("GL_FRAMEBUFFER failed!");
        }

//        switch (status) {
//            case GL_FRAMEBUFFER_COMPLETE:
//                RGL.log("GL_FRAMEBUFFER is OK!");
//                break;
//            default:
//                RGL.log("GL_FRAMEBUFFER failed!");
//                throw new RuntimeException("GL_FRAMEBUFFER failed!");
//        }
    }

    public int getBufferHandle() {
        return bufferHandle;
    }

    public int getColorTex() {
        return colorTex;
    }

    public int getNormalTex() {
        return normalTex;
    }

    public int getPositionTex() {
        return positionTex;
    }
}
