package ssao;

//import org.lwjgl.BufferUtils;
//import rafgl.RGL;
//import rafgl.jglm.Mat4;
//import rafgl.jglm.Matrices;
//import rafgl.jglm.Vec3;
//import rafgl.jglm.Vec4;
//
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//
//import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
//import static org.lwjgl.opengl.GL11.glBindTexture;
//import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
//import static org.lwjgl.opengl.GL13.glActiveTexture;
//import static org.lwjgl.opengl.GL20.glUniform1i;
//import static org.lwjgl.opengl.GL20.glUniformMatrix4;

import org.lwjgl.BufferUtils;
import rafgl.RGL;
import rafgl.jglm.Mat4;
import rafgl.jglm.Matrices;
import rafgl.jglm.Vec3;
import rafgl.jglm.Vec4;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;

public class GameObject {
    // Pozicija, u prostoru scene ili roditelja (ako ga ima)
    private Vec3 position;

    // Skaliranje po X,Y,Z, pocinje na 1.0 po svim osama
    private Vec3 scale;

    // Tri ugla koji definisu orijentaciju, racunaju se u ovom redoslijedu
    private float yaw;
    private float pitch;
    private float roll;

    // Model matrica ovog objekta (ukljucuje i parent transformacije)
    private Mat4 matrix;

    // Lista djece (ne treba se rucno editovati)
    private List<GameObject> children = new ArrayList<>();

    // Pokazivac na roditelja (ne treba se rucno editovati)
    private GameObject parent = null;

    // Ime objekta
    // private String name;

    // Model (ako ga ima)
    private RGL.Model model;

    // Do 8 tekstura i njihovi OpenGL tipovi (npr. GL_TEXTURE_2D)
    private int[] textures = new int[8];
    private int[] textureType = new int[8];

    // Tri vektora orijentacije u world-space, automatski se azuriraju
    // private Vec3 up;
    // private Vec3 right;
    // private Vec3 forward;

    private GameShader shader;
    private RGL.BlendModes blendMode = RGL.BlendModes.OPAQUE;
    private boolean ghostly;
    private boolean autoDraw;

    private FloatBuffer matBuffModel = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer matBuffMV = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer matBuffMVP = BufferUtils.createFloatBuffer(4 * 4);

    // Inicijalne vrijednosti
    public GameObject() {
        position = new Vec3(0.0f, 0.0f, 0.0f);
        scale = new Vec3(1.0f, 1.0f, 1.0f);

        yaw = 0.0f;
        pitch = 0.0f;
        roll = 0.0f;

        matrix = new Mat4(1.0f);

        parent = null;

        // name = "";
        model = null;

        ghostly = false;
        autoDraw = true;

        for (int i = 0; i < 8; ++i) {
            textures[i] = 0;
            textureType[i] = GL_TEXTURE_2D;
        }

        // up = new Vec3(0.0f, 1.0f, 0.0f);
        // right = new Vec3(1.0f, 0.0f, 0.0f);
        // forward = new Vec3(0.0f, 0.0f, 1.0f);
    }

    public void attachChild(GameObject child) {
        // Ako je objekat vec bio prikacen na neki drugi, prvo tu vezu treba
        // ukloniti, kako ne bi bilo dupliranja
        if (child.parent != null)
            child.parent.detachChild(child);

        // Dodajemo objekat na listu i azuriramo mu matricu
        children.add(child);
        child.parent = this;
        child.updateMatrix(true);
    }

    public void detachChild(GameObject child) {
        // Brisemo objekat iz liste i postavljamo mu parent polje na NULL
        children.remove(child);
        child.parent = null;
    }

    public void updateMatrix() {
        updateMatrix(true);
    }

    public void updateMatrix(boolean deep) {
        // Ako objekat ima roditelja, preuzima mu matricu, ako nema, onda je na
        // vrhu hijerarhije, sto znaci da mu je "roditeljska matrica"
        // world-space,
        // odnosno, jedinicna/identity matrica
        Mat4 parentMatrix = (parent == null) ? new Mat4(1.0f) : parent.matrix;

        // Redoslijed operacija u ovom sistemu. Moze da bude i drugaciji,
        // zavisno
        // potreba aplikacije koju pravimo.

        Mat4 mat = new Mat4(parentMatrix);
        mat = mat.translate(position);

        mat = mat.scale(scale);

        mat = mat.multiply(Matrices.rotate(yaw, new Vec3(0.0f, 1.0f, 0.0f)));
        mat = mat.multiply(Matrices.rotate(pitch, new Vec3(1.0f, 0.0f, 0.0f)));
        mat = mat.multiply(Matrices.rotate(roll, new Vec3(0.0f, 0.0f, -1.0f)));

        matrix = mat;
        matrix.store(matBuffModel);
        matBuffModel.flip();

        // Vektore orijentacije ponovo dobijamo transformisanjem jedinicnih
        // vektora
        // Vec4 forward4 = new Vec4(0.0f, 0.0f, -1.0f, 0.0f).multiplyTP(matrix);
        // Vec4 up4 = new Vec4(0.0f, 1.0f, 0.0f, 0.0f).multiplyTP(matrix);
        // Vec4 right4 = new Vec4(1.0f, 0.0f, 0.0f, 0.0f).multiplyTP(matrix);

        // up = new Vec3(up4.x, up4.y, up4.z);
        // right = new Vec3(right4.x, right4.y, right4.z);
        // forward = new Vec3(forward4.x, forward4.y, forward4.z);

        // Deep znaci rekurzivno azuriranje i svih child objekata
        if (deep) {
            for (GameObject child : children)
                child.updateMatrix(true);
        }
    }

    public Vec3 transformDirection(Vec3 dir) {
        Vec4 t = new Vec4(dir.x, dir.y, dir.z, 0.0f).multiplyTP(matrix);
        return new Vec3(t.x, t.y, t.z);
    }

    public Vec3 transformPoint(Vec3 point) {
        Vec4 t = new Vec4(point.x, point.y, point.z, 1.0f).multiplyTP(matrix);
        return new Vec3(t.x, t.y, t.z);
    }

    public Vec3 getAbsolutePosition() {
        return transformPoint(new Vec3(0.0f, 0.0f, 0.0f));
    }

    public void draw(GameShader shader, GameCamera camera) {
        draw(shader, camera, true);
    }

    public void draw(GameShader shader, GameCamera camera, boolean deep) {
        if (model == null || shader == null || camera == null)
            return;

        // Ako shader ima model matricu kao uniform, upisujemo je
        if (shader.getUni_M() >= 0) {
            matrix.store(matBuffModel);
            matBuffModel.flip();
            glUniformMatrix4(shader.getUni_M(), false, matBuffModel);
        }

        // Ako ima ModelView matricu, mnozimo sa kamerom i upisujemo
        if (shader.getUni_MV() >= 0) {
            Mat4 mv = camera.getMatView().multiply(matrix);
            mv.store(matBuffMV);
            matBuffMV.flip();
            glUniformMatrix4(shader.getUni_MV(), false, matBuffMV);
        }

        // Slicno i za kompletnu MVP matricu
        if (shader.getUni_MVP() >= 0) {
            Mat4 mvp = camera.getMatVP().multiply(matrix);
            mvp.store(matBuffMVP);
            matBuffMVP.flip();
            glUniformMatrix4(shader.getUni_MVP(), false, matBuffMVP);
        }

        RGL.setBlendMode(blendMode);

        // Posto se cesto podrazumijeva samo jedna tekstura, pa tako i da je
        // samo
        // prva teksturna jedinica aktivna, pazimo da se vratimo u to stanje ako
        // smo ga mijenjali, kako ne bismo imali problema na drugim mjestima.
        boolean activeChanged = false;

        for (int i = 0; i < 8; i++) {
            if (textures[i] > 0) {
                // Ako imamo teksturu u trenutnom slotu, bindujemo je na
                // ekvivalentnu
                // teksturnu jedinicu, mijenjajuci glActiveTexture
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(textureType[i], textures[i]);

                // Za slucaj vise tekstura, trebamo i samplere postaviti na
                // prave jedinice
                if (shader.uni_texture[i] >= 0)
                    glUniform1i(shader.uni_texture[i], i);

                // Da li smo postavili activeTexture na nesto osim 0?
                activeChanged = activeChanged || (i > 0);
            }
        }

        RGL.BlendModes blendMode = RGL.getBlendMode();
        boolean depthWrite = RGL.getDepthWrite();
        boolean backfaceCull = RGL.getBackfaceCull();

        if (ghostly) {
            RGL.setBlendMode(RGL.BlendModes.ADDITIVE);
            RGL.setDepthWrite(false);
            RGL.setBackfaceCull(false);
        }

        // Samo iscrtavanje modela se vrsi postojecim pozivom, dajuci relevantne
        // podatke
        RGL.drawModel(model, shader.attr_position, shader.attr_uv,
                shader.attr_normal);

        // Ponovo, deep znaci dalji rekurzivan poziv i za sve child objekte
        if (deep) {
            for (GameObject child : children)
                if (child.autoDraw)
                    child.draw(shader, camera, true);
        }

        // Ako smo ovo promijenili, vracamo na nulu
        if (activeChanged)
            glActiveTexture(GL_TEXTURE0);

        if (ghostly) {
            RGL.setBlendMode(blendMode);
            RGL.setDepthWrite(depthWrite);
            RGL.setBackfaceCull(backfaceCull);
        }
    }

    public void setTexture(int idx, int tex, int type) {
        textures[idx] = tex;
        textureType[idx] = type;
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public RGL.Model getModel() {
        return model;
    }

    public void setModel(RGL.Model model) {
        this.model = model;
    }

    public void setShader(GameShader shader) {
        this.shader = shader;
    }

    public GameShader getShader() {
        return shader;
    }

}