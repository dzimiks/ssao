package ssao;

import rafgl.RGL;

public class MainApp {

    public static void main(String[] args) {
        // Init screen
        RGL.setParami(RGL.IParam.WIDTH, 1280);
        RGL.setParami(RGL.IParam.HEIGHT, 720);
        RGL.setParami(RGL.IParam.FULLSCREEN, 0);
        RGL.setParami(RGL.IParam.MSAA, 4);
        RGL.setParami(RGL.IParam.VSYNC, 1);
        RGL.init();
        RGL.setTitle("SSAO - Vanja Paunovic - RN 35/16");
        RGL.setRunning(true);

        // Init renderer
        Renderer renderer = new Renderer();

        // Print instructions
        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append("=============== CONTROLS ===============\n");
        sb.append("========================================\n");
        sb.append("= CAMERA:                              =\n");
        sb.append("= W/S - forward/back                   =\n");
        sb.append("= A/D - left/right                     =\n");
        sb.append("= Q/E - rotate left/right              =\n");
        sb.append("= SPACE/CTRL - up/down                 =\n");
        sb.append("========================================\n");
        sb.append("= SSAO:                                =\n");
        sb.append("= T/G: inc/dec radius (default 0.6)    =\n");
        sb.append("= Y/H: inc/dec bias (default 0.003)    =\n");
        sb.append("= U/J: inc/dec power (default 0.8)     =\n");
        sb.append("========================================\n");
        sb.append("= DEBUG:                               =\n");
        sb.append("= Left/right mouse click: next/prev    =\n");
        sb.append("= Mode 0: SSAO on                      =\n");
        sb.append("= Mode 1: SSAO off                     =\n");
        sb.append("= Mode 2: Occlusion mask               =\n");
        sb.append("= Mode 3: Smooth occlusion mask        =\n");
        sb.append("========================================\n");

        System.out.println(sb.toString());

        // MainApp loop
        while (RGL.isRunning()) {
            RGL.handleEvents();
            renderer.handleEvents();
            renderer.drawFrame();
        }

        RGL.deinit();
    }
}
