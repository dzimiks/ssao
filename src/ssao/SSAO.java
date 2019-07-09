package ssao;

import org.lwjgl.BufferUtils;
import rafgl.jglm.Vec3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

public class SSAO {

    private int numSamples;

    public SSAO(int numSamples) {
        this.numSamples = numSamples;
    }

    // Linear interpolation
    private float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    private ArrayList<Vec3> makeKernel(int numSamples) {
        ArrayList<Vec3> ssaoKernel = new ArrayList<>();
        Random r = new Random(255);
        for (int i = 0; i < numSamples; i++) {
            // Generate a kernel in tangent space
            // Random vector in hemispehere: [-1, 1] x [-1, 1] x [0, 1]
            float x = (float) (r.nextDouble() * 2.0 - 1.0);
            float y = (float) (r.nextDouble() * 2.0 - 1.0);
            float z = (float) (r.nextDouble()); // hemisphere
            Vec3 sample = (new Vec3(x, y, z)).getUnitVector();
            // Scale it but make sure it's closer to the origin
            float ratio = i / (float) numSamples;
            float scale = lerp(0.1f, 1.0f, ratio * ratio);
            sample = sample.multiply(scale);
            ssaoKernel.add(sample);
        }
        // convert to floatbuffer
        return ssaoKernel;
    }

    public FloatBuffer getKernelBuf() {
        ArrayList<Vec3> kernel = makeKernel(numSamples);
        FloatBuffer kernelBuf = BufferUtils.createFloatBuffer(8 * 8 * 3);
        for (int i = 0; i < numSamples; i++) {
            kernelBuf.put(kernel.get(i).x);
            kernelBuf.put(kernel.get(i).y);
            kernelBuf.put(kernel.get(i).z);
        }
        kernelBuf.flip();
        return kernelBuf;
    }

    private ArrayList<Vec3> makeNoise() {
        // Noise in XY [-1, 1] [-1, 1]
        ArrayList<Vec3> ssaoNoise = new ArrayList<>();
        Random r = new Random(255);
        for (int i = 0; i < 16; i++) {
            float x = (float) (r.nextDouble() * 2.0 - 1.0);
            float y = (float) (r.nextDouble() * 2.0 - 1.0);
            float z = (float) (0);
            Vec3 noise = (new Vec3(x, y, z));
            ssaoNoise.add(noise);
        }
        return ssaoNoise;
    }

    public FloatBuffer getNoiseBuf() {
        ArrayList<Vec3> noise = makeNoise();
        FloatBuffer noiseBuf = BufferUtils.createFloatBuffer(4 * 4 * 3);
        for (int i = 0; i < 16; i++) {
            noiseBuf.put(noise.get(i).x);
            noiseBuf.put(noise.get(i).y);
            noiseBuf.put(noise.get(i).z);
        }
        noiseBuf.flip();
        return noiseBuf;
    }
}