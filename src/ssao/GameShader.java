package ssao;

import rafgl.RGL;

import java.util.List;

import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;

public class GameShader {
    // Shader program ID, koristimo ga za glUseProgram, itd.
    private int shaderID;

    // Potencijalne ID vrijednosti za uniforme koje ocekujemo da bismo
    // mogli koristiti u svojim shaderima u ovom projektu
    private int uni_MVP;
    private int uni_MV;
    private int uni_VP;
    private int uni_M;
    private int uni_V;
    private int uni_P;

    private int uni_camPosition;
    private int uni_camVector;

    private int uni_screenSize;

    private int uni_lightVector1;
    private int uni_lightColor1;
    private int uni_lightRange1;

    private int uni_lightVector2;
    private int uni_lightColor2;
    private int uni_lightRange2;

    private int uni_lightVector3;
    private int uni_lightColor3;
    private int uni_lightRange3;

    private int uni_lightCount;
    private int uni_ambient;
    private int uni_objectColor;

    // Za laksi rad cemo ostaviti mjesta i za neke specijalne uniform
    // vrijednosti specificne za pojedinacne shadere (animacije, recimo)
    public int[] uni_special = new int[16];

    // Za multi-texturing, ako koristimo vise tekstura (odnosno vise samplera),
    // tada je bitno postaviti i odgovarajuce texture jedinice
    public int[] uni_texture = new int[16];

    // Slicno kao i za uniform, lista atributa za koje ocekujemo da bi mogli
    // biti od koristi (naravno, ne moraju svi biti prisutni, kao i uniformi)
    public int attr_position;
    public int attr_color;
    public int attr_alpha;
    public int attr_uv;
    public int attr_normal;

    // Ponovo, mjesto za eventualne posebne atribute
    public int[] attr_special = new int[16];

    // Naziv shadera
    public String name;

    // Inicijalne vrijednosti
    public GameShader() {
        shaderID = 0;
        name = "";

        for (int i = 0; i < 16; i++) {
            uni_special[i] = -1;
            attr_special[i] = -1;
        }

        for (int i = 0; i < 8; i++)
            uni_texture[i] = -1;
    }

    public GameShader(String shaderName) {
        name = shaderName;

        for (int i = 0; i < 16; i++) {
            uni_special[i] = -1;
            attr_special[i] = -1;
        }

        for (int i = 0; i < 8; i++)
            uni_texture[i] = -1;

        loadShader(shaderName);
    }

    public void loadShader(String shaderName) {
        String vertPath;
        String fragPath;

        // Sablon po kom od naziva trazimo stvarne putanje fajlova
        vertPath = "shaders/" + shaderName + "-vert.glsl";
        fragPath = "shaders/" + shaderName + "-frag.glsl";

        // Stvarno ucitavanje GLSL shadera radimo postojecim pozivom
        shaderID = RGL.loadShader(vertPath, fragPath);

        if (shaderID > 0) {
            // Ako je shader uspjesno ucitan, idemo traziti uniforme i atribute
            queryAttributes();
            queryUniforms();
            RGL.log("[RGL] Game shader '" + shaderName + "' loaded");
        } else {
            RGL.log("[RGL] Failed to load game shader '" + shaderName + "' ('"
                    + vertPath + "' / '" + fragPath + "')");
        }
    }

    public void queryUniforms() {
        // Redom ispitujemo shader trazeci uniforme po nazivima koje planiramo
        // koristiti. Naravno, svi ovi nazivi su potpuno proizvoljni, ali
        // korisno
        // je odluciti se za neku konvenciju bas zbog ovakvih nacina
        // automatizacije
        // kroz objedinjene funkcije za ucitavanje, kako ne bismo svaki shader
        // na
        // drugaciji nacin ucitavali i koristili. Ako zelite koristiti drugacije
        // nazive u svojim GLSL fajlovima ili dodati ili oduzeti neke naziva,
        // ovo
        // je mjesto za to.

        uni_MVP = glGetUniformLocation(shaderID, "uni_MVP");
        uni_MV = glGetUniformLocation(shaderID, "uni_MV");
        uni_VP = glGetUniformLocation(shaderID, "uni_VP");
        uni_M = glGetUniformLocation(shaderID, "uni_M");
        uni_V = glGetUniformLocation(shaderID, "uni_V");
        uni_P = glGetUniformLocation(shaderID, "uni_P");

        uni_camPosition = glGetUniformLocation(shaderID, "uni_camPosition");
        uni_camVector = glGetUniformLocation(shaderID, "uni_camVector");

        uni_screenSize = glGetUniformLocation(shaderID, "uni_screenSize");

        uni_lightVector1 = glGetUniformLocation(shaderID, "uni_lightVector1");
        uni_lightColor1 = glGetUniformLocation(shaderID, "uni_lightColor1");
        uni_lightRange1 = glGetUniformLocation(shaderID, "uni_lightRange1");

        uni_lightVector2 = glGetUniformLocation(shaderID, "uni_lightVector2");
        uni_lightColor2 = glGetUniformLocation(shaderID, "uni_lightColor2");
        uni_lightRange2 = glGetUniformLocation(shaderID, "uni_lightRange2");

        uni_lightVector3 = glGetUniformLocation(shaderID, "uni_lightVector3");
        uni_lightColor3 = glGetUniformLocation(shaderID, "uni_lightColor3");
        uni_lightRange3 = glGetUniformLocation(shaderID, "uni_lightRange3");

        uni_lightCount = glGetUniformLocation(shaderID, "uni_lightCount");
        uni_ambient = glGetUniformLocation(shaderID, "uni_ambient");
        uni_objectColor = glGetUniformLocation(shaderID, "uni_objectColor");

        // Ocekujemo da ce sampleri tekstura biti nazvani uni_texture0,
        // uni_texture1, ...
        for (int i = 0; i < 8; ++i) {
            String uniname = "uni_texture" + i;
            uni_texture[i] = glGetUniformLocation(shaderID, uniname);
        }
    }

    public void queryAttributes() {
        // Slicno kao i za uniforme, trazimo atribute po unaprijed odlucenim
        // nazivima. Ovdje ih mozemo promijeniti po potrebi.

        attr_position = glGetAttribLocation(shaderID, "in_Position");
        attr_color = glGetAttribLocation(shaderID, "in_Color");
        attr_alpha = glGetAttribLocation(shaderID, "in_Alpha");
        attr_uv = glGetAttribLocation(shaderID, "in_UV");
        attr_normal = glGetAttribLocation(shaderID, "in_Normal");
    }

    // Trazenje posebnih uniform vrijednosti
    public void findSpecialUniform(int slot, String uniformName) {
        if (slot >= 0 && slot < 16) {
            uni_special[slot] = glGetUniformLocation(shaderID, uniformName);
            if (uni_special[slot] < 0)
                RGL.log("[RGL] Can't find uniform '" + uniformName
                        + "' for shader '" + name + "'");
        } else {
            RGL.log("[RGL] Special uniform '" + uniformName
                    + "' slot out of range (0 - 15, given: " + slot + ")");
        }
    }

    public void findSpecialUniforms(int startSlot, List<String> list) {
        int slot = startSlot;
        for (String name : list) {
            findSpecialUniform(slot, name);
            slot += 1;
        }
    }

    // Trazenje posebnih atributa
    public void findSpecialAttribute(int slot, String attributeName) {
        if (slot >= 0 && slot < 16) {
            attr_special[slot] = glGetAttribLocation(shaderID, attributeName);
            if (attr_special[slot] < 0)
                RGL.log("[RGL] Can't find attribute '" + attributeName
                        + "' for shader '" + name + "'");
        } else {
            RGL.log("[RGL] Special attribute '" + attributeName
                    + "' slot out of range (0 - 15, given: " + slot + ")");
        }
    }

    public int getShaderID() {
        return shaderID;
    }

    public void setShaderID(int shaderID) {
        this.shaderID = shaderID;
    }

    public int getUni_MVP() {
        return uni_MVP;
    }

    public void setUni_MVP(int uni_MVP) {
        this.uni_MVP = uni_MVP;
    }

    public int getUni_MV() {
        return uni_MV;
    }

    public void setUni_MV(int uni_MV) {
        this.uni_MV = uni_MV;
    }

    public int getUni_VP() {
        return uni_VP;
    }

    public void setUni_VP(int uni_VP) {
        this.uni_VP = uni_VP;
    }

    public int getUni_M() {
        return uni_M;
    }

    public void setUni_M(int uni_M) {
        this.uni_M = uni_M;
    }

    public int getUni_V() {
        return uni_V;
    }

    public void setUni_V(int uni_V) {
        this.uni_V = uni_V;
    }

    public int getUni_P() {
        return uni_P;
    }

    public void setUni_P(int uni_P) {
        this.uni_P = uni_P;
    }

    public int getUni_camPosition() {
        return uni_camPosition;
    }

    public void setUni_camPosition(int uni_camPosition) {
        this.uni_camPosition = uni_camPosition;
    }

    public int getUni_camVector() {
        return uni_camVector;
    }

    public void setUni_camVector(int uni_camVector) {
        this.uni_camVector = uni_camVector;
    }

    public int getUni_screenSize() {
        return uni_screenSize;
    }

    public void setUni_screenSize(int uni_screenSize) {
        this.uni_screenSize = uni_screenSize;
    }

    public int getUni_lightVector1() {
        return uni_lightVector1;
    }

    public void setUni_lightVector1(int uni_lightVector1) {
        this.uni_lightVector1 = uni_lightVector1;
    }

    public int getUni_lightColor1() {
        return uni_lightColor1;
    }

    public void setUni_lightColor1(int uni_lightColor1) {
        this.uni_lightColor1 = uni_lightColor1;
    }

    public int getUni_lightRange1() {
        return uni_lightRange1;
    }

    public void setUni_lightRange1(int uni_lightRange1) {
        this.uni_lightRange1 = uni_lightRange1;
    }

    public int getUni_lightVector2() {
        return uni_lightVector2;
    }

    public void setUni_lightVector2(int uni_lightVector2) {
        this.uni_lightVector2 = uni_lightVector2;
    }

    public int getUni_lightColor2() {
        return uni_lightColor2;
    }

    public void setUni_lightColor2(int uni_lightColor2) {
        this.uni_lightColor2 = uni_lightColor2;
    }

    public int getUni_lightRange2() {
        return uni_lightRange2;
    }

    public void setUni_lightRange2(int uni_lightRange2) {
        this.uni_lightRange2 = uni_lightRange2;
    }

    public int getUni_lightVector3() {
        return uni_lightVector3;
    }

    public void setUni_lightVector3(int uni_lightVector3) {
        this.uni_lightVector3 = uni_lightVector3;
    }

    public int getUni_lightColor3() {
        return uni_lightColor3;
    }

    public void setUni_lightColor3(int uni_lightColor3) {
        this.uni_lightColor3 = uni_lightColor3;
    }

    public int getUni_lightRange3() {
        return uni_lightRange3;
    }

    public void setUni_lightRange3(int uni_lightRange3) {
        this.uni_lightRange3 = uni_lightRange3;
    }

    public int getUni_lightCount() {
        return uni_lightCount;
    }

    public void setUni_lightCount(int uni_lightCount) {
        this.uni_lightCount = uni_lightCount;
    }

    public int getUni_ambient() {
        return uni_ambient;
    }

    public void setUni_ambient(int uni_ambient) {
        this.uni_ambient = uni_ambient;
    }

    public int getUni_objectColor() {
        return uni_objectColor;
    }

    public void setUni_objectColor(int uni_objectColor) {
        this.uni_objectColor = uni_objectColor;
    }

    public int[] getUni_special() {
        return uni_special;
    }

    public void setUni_special(int[] uni_special) {
        this.uni_special = uni_special;
    }

    public int[] getUni_texture() {
        return uni_texture;
    }

    public void setUni_texture(int[] uni_texture) {
        this.uni_texture = uni_texture;
    }

    public int getAttr_position() {
        return attr_position;
    }

    public void setAttr_position(int attr_position) {
        this.attr_position = attr_position;
    }

    public int getAttr_color() {
        return attr_color;
    }

    public void setAttr_color(int attr_color) {
        this.attr_color = attr_color;
    }

    public int getAttr_alpha() {
        return attr_alpha;
    }

    public void setAttr_alpha(int attr_alpha) {
        this.attr_alpha = attr_alpha;
    }

    public int getAttr_uv() {
        return attr_uv;
    }

    public void setAttr_uv(int attr_uv) {
        this.attr_uv = attr_uv;
    }

    public int getAttr_normal() {
        return attr_normal;
    }

    public void setAttr_normal(int attr_normal) {
        this.attr_normal = attr_normal;
    }

    public int[] getAttr_special() {
        return attr_special;
    }

    public void setAttr_special(int[] attr_special) {
        this.attr_special = attr_special;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}