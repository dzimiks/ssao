package ssao;

import org.lwjgl.input.Keyboard;
import rafgl.RGL;
import rafgl.jglm.Vec3;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private HashMap<String, GameShader> shaders;
    private GameCamera camera;
    private int[] skybox;
    private GameObject building;
    private int ssaoKernelTex, ssaoNoiseTex;
    private FrameBufferObject gFbo, ssaoFbo, blurFbo, lightFbo;

    private float diffSpeed, ssaoRadius, ssaoBias, ssaoPower;
    private int ssaoMode;

    public Renderer() {
        initShaders();
        initCamera();
        loadObjects();
        initFbos();

        diffSpeed = 0.01f;
        ssaoRadius = 0.6f;
        ssaoBias = 0.003f;
        ssaoPower = 0.8f;
        ssaoMode = 0;
    }

    public void drawFrame() {
        // Updates
        building.updateMatrix();

        // Start
        RGL.beginFrame();

        // Render
        drawPrep();
        drawSsao();
        drawBlur();
        drawLight();
        drawPostprocess();

        // End
        RGL.endFrame();
    }

    public void handleEvents() {
        // Controls

        // SSAO mode
        if (RGL.wasMouseButtonJustPressed(0))
            ssaoMode = (ssaoMode + 1) % 4;
        if (RGL.wasMouseButtonJustPressed(1))
            ssaoMode = (ssaoMode + 3) % 4;

        // Camera
        if (RGL.isKeyDown(Keyboard.KEY_W))
            camera.setPosition(camera.getPosition().add(
                    camera.getForward().multiply(0.5f)));
        if (RGL.isKeyDown(Keyboard.KEY_S))
            camera.setPosition(camera.getPosition().subtract(
                    camera.getForward().multiply(0.5f)));
        if (RGL.isKeyDown(Keyboard.KEY_A))
            camera.setPosition(camera.getPosition().subtract(
                    camera.getRight().multiply(0.5f)));
        if (RGL.isKeyDown(Keyboard.KEY_D))
            camera.setPosition(camera.getPosition().add(
                    camera.getRight().multiply(0.5f)));
        if (RGL.isKeyDown(Keyboard.KEY_Q))
            camera.setYaw(camera.getYaw() - 0.05f);
        if (RGL.isKeyDown(Keyboard.KEY_E))
            camera.setYaw(camera.getYaw() + 0.05f);
        if (RGL.isKeyDown(Keyboard.KEY_SPACE))
            camera.setPosition(camera.getPosition().add(0.0f, 0.5f, 0.0f));
        if (RGL.isKeyDown(Keyboard.KEY_LCONTROL))
            camera.setPosition(camera.getPosition().add(0.0f, -0.5f, 0.0f));
        camera.update();

        // Ssao controls
        if (RGL.isKeyDown(Keyboard.KEY_T)) {
            ssaoRadius += diffSpeed;
            System.out.println(ssaoRadius);
        }
        if (RGL.isKeyDown(Keyboard.KEY_G)) {
            ssaoRadius -= diffSpeed;
            System.out.println(ssaoRadius);
        }

        if (RGL.isKeyDown(Keyboard.KEY_Y)) {
            ssaoBias += diffSpeed / 1000.0;
            System.out.println(ssaoBias);
        }
        if (RGL.isKeyDown(Keyboard.KEY_H)) {
            ssaoBias -= diffSpeed / 1000.0;
            System.out.println(ssaoBias);
        }
        if (RGL.isKeyDown(Keyboard.KEY_U)) {
            ssaoPower += diffSpeed;
            System.out.println(ssaoPower);
        }
        if (RGL.isKeyDown(Keyboard.KEY_J)) {
            ssaoPower -= diffSpeed;
            System.out.println(ssaoPower);
        }
    }

    private void drawPrep() {
        // 1. G buffer - prep
        glBindFramebuffer(GL_FRAMEBUFFER, gFbo.getBufferHandle());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        building.setShader(shaders.get("prep"));
        glUseProgram(building.getShader().getShaderID());
        camera.uploadUniforms(building.getShader());

        building.draw(building.getShader(), camera);
    }

    private void drawSsao() {
        // 2. SSAO

        glBindFramebuffer(GL_FRAMEBUFFER, ssaoFbo.getBufferHandle());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaders.get("ssao").getShaderID());
        camera.uploadUniforms(shaders.get("ssao"));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gFbo.getColorTex());
        glUniform1i(shaders.get("ssao").uni_special[0], 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gFbo.getNormalTex());
        glUniform1i(shaders.get("ssao").uni_special[1], 1);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, gFbo.getPositionTex());
        glUniform1i(shaders.get("ssao").uni_special[2], 2);

        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, ssaoNoiseTex);
        glUniform1i(shaders.get("ssao").uni_special[3], 3);

        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, ssaoKernelTex);
        glUniform1i(shaders.get("ssao").uni_special[4], 4);

        glUniform1f(shaders.get("ssao").uni_special[5], ssaoRadius);
        glUniform1f(shaders.get("ssao").uni_special[6], ssaoBias);
        glUniform1f(shaders.get("ssao").uni_special[7], ssaoPower);

        // Reset
        glActiveTexture(GL_TEXTURE0);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        RGL.setBlendMode(RGL.BlendModes.OPAQUE);
        RGL.drawFullscreenQuad(shaders.get("ssao").attr_position,
                shaders.get("ssao").attr_uv);
    }

    private void drawBlur() {
        // 3. Simple blur

        glBindFramebuffer(GL_FRAMEBUFFER, blurFbo.getBufferHandle());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaders.get("blur").getShaderID());
        camera.uploadUniforms(shaders.get("blur"));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoFbo.getColorTex());
        glUniform1i(shaders.get("blur").uni_special[0], 0);

        glActiveTexture(GL_TEXTURE0);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        RGL.setBlendMode(RGL.BlendModes.OPAQUE);
        RGL.drawFullscreenQuad(shaders.get("blur").attr_position,
                shaders.get("blur").attr_uv);

    }

    private void drawLight() {
        // 4. Draw with light

        glBindFramebuffer(GL_FRAMEBUFFER, lightFbo.getBufferHandle());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        RGL.renderSkybox(skybox, camera.getPosition(), camera.getMatBuffVP());

        building.setShader(shaders.get("light"));
        glUseProgram(shaders.get("light").getShaderID());

        // Uniforms
        camera.uploadUniforms(building.getShader());
        glUniform3f(building.getShader().getUni_ambient(), 0.7f, 0.7f, 0.7f);
        glUniform3f(building.getShader().getUni_lightVector1(), 0.3f, -0.9f,
                0.7f);
        glUniform3f(building.getShader().getUni_lightColor1(), 0.2f, 0.2f, 0.2f);

        // Draw
        building.draw(building.getShader(), camera);

    }

    private void drawPostprocess() {
        // 5. Post-processing
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaders.get("postprocess").getShaderID());
        camera.uploadUniforms(shaders.get("postprocess"));

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, blurFbo.getColorTex());
        glUniform1i(shaders.get("postprocess").uni_special[0], 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, lightFbo.getColorTex());
        glUniform1i(shaders.get("postprocess").uni_special[1], 1);

        glUniform1i(shaders.get("postprocess").uni_special[2], ssaoMode);

        glActiveTexture(GL_TEXTURE0);

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        RGL.setBlendMode(RGL.BlendModes.OPAQUE);
        RGL.drawFullscreenQuad(shaders.get("postprocess").attr_position,
                shaders.get("postprocess").attr_uv);

    }

    private void initShaders() {
        // Load shaders
        shaders = new HashMap<>();
        shaders.put("prep", new GameShader("prep"));
        shaders.put("ssao", new GameShader("ssao"));
        shaders.put("blur", new GameShader("blur"));
        shaders.put("light", new GameShader("light"));
        shaders.put("postprocess", new GameShader("postprocess"));

        // Add uniforms to shaders
        shaders.get("ssao").findSpecialUniforms(
                1,
                Arrays.asList("gNormal", "gPosition", "noise", "kernel",
                        "ssaoRadius", "ssaoBias", "ssaoPower"));
        shaders.get("blur").findSpecialUniform(0, "ssaoColor");
        shaders.get("postprocess").findSpecialUniforms(0,
                Arrays.asList("ssao", "color", "ssaoMode"));
    }

    private void initCamera() {
        // Init camera
        camera = new GameCamera(GameCameraMode.GAMECAM_FIRST_PERSON);
        camera.setPosition(new Vec3(-1.0f, 9.0f, 0.0f));
        camera.setYaw(RGL.PIf);
        camera.update();
    }

    private void loadObjects() {
        // Load the skybox
        skybox = new int[6];
        RGL.loadSkybox("textures/moon", "jpg", skybox);
        int envMap = RGL.loadCubemap("textures/moon-E", "jpg");

        // Prepare the object
        building = new GameObject();
        building.setModel(RGL.loadModelOBJ("models/Building.obj"));
        building.setPosition(new Vec3(-30.0f, 0.0f, 0.0f));
        int texUV = RGL.loadTexture("textures/moon-E.png", true, true, false);
        building.setTexture(0, texUV, GL_TEXTURE_2D);
        building.setTexture(1, envMap, GL_TEXTURE_CUBE_MAP);
    }

    private void initFbos() {
        // 1. G buffer - prep
        int gColorTex = Utils.getTexture((FloatBuffer) null, RGL.getWidth(),
                RGL.getHeight(), GL_RGBA8, GL_REPEAT);
        gFbo = new FrameBufferObject(true, gColorTex);

        // 2. SSAO
        SSAO ssao = new SSAO(64);
        FloatBuffer kernelBuf = ssao.getKernelBuf();
        ssaoKernelTex = Utils.getTexture(kernelBuf, 8, 8, GL_RGB16F, GL_REPEAT);
        FloatBuffer noiseBuf = ssao.getNoiseBuf();
        ssaoNoiseTex = Utils.getTexture(noiseBuf, 4, 4, GL_RGB16F, GL_REPEAT);

        int ssaoColorTex = Utils.getTexture((FloatBuffer) null, RGL.getWidth(),
                RGL.getHeight(), GL_RED, GL_LINEAR);
        ssaoFbo = new FrameBufferObject(false, ssaoColorTex);

        // 3. Blur
        int blurColorTex = Utils.getTexture((FloatBuffer) null, RGL.getWidth(),
                RGL.getHeight(), GL_RED, GL_LINEAR);
        blurFbo = new FrameBufferObject(false, blurColorTex);

        // 4. Light
        int lightColorTex = Utils.getTexture((FloatBuffer) null,
                RGL.getWidth(), RGL.getHeight(), GL_RGBA8, GL_LINEAR);
        lightFbo = new FrameBufferObject(false, lightColorTex);

    }
}