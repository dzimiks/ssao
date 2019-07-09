package ssao;

import org.lwjgl.BufferUtils;
import rafgl.RGL;
import rafgl.jglm.*;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;

public class GameCamera {
    // Kljucne komponente za lookUp
    private Vec3 position;

    private Vec3 target;

    // Samo za orijentaciju, nije trenutni "up" vektor
    private Vec3 orientationUp;

    // FoV i zoom se mnoze, zoom je tu samo za laksu upotrebu
    private float vfov;
    private float zoom;

    // Near i far clipping plane
    private float nearZ;
    private float farZ;

    // Trenutni mod kamere
    private GameCameraMode mode;

    // Tri vektora koji se stalno azuriraju i imaju tacne vrijednosti za
    // trenutni polozaj kamere, bez obzira na mod
    private Vec3 forward;
    private Vec3 up;
    private Vec3 right;

    // Uglovi koji se uzimaju u obzir u FIRST_PERSON i ORBIT modovima
    // (roll se trenutno ignorise)
    private float yaw;
    private float pitch;
    private float roll;

    // Udaljenost kamere od njene mete, bitno za ORBIT
    private float distance;

    // View i Projection matrice, automatski azurirane, zajedno sa proizvodom
    private Mat4 matProjection;
    private Mat4 matView;
    private Mat4 matVP;

    private FloatBuffer matBuffProjection = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer matBuffView = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer matBuffVP = BufferUtils.createFloatBuffer(4 * 4);

    // Pocetne vrednosti
    public GameCamera(GameCameraMode cmode) {
        mode = cmode;

        position = new Vec3(0.0f, 0.0f, -3.0f);
        target = new Vec3(0.0f, 0.0f, 0.0f);
        orientationUp = new Vec3(0.0f, 1.0f, 0.0f);
        vfov = 60.0f;
        zoom = 1.0f;

        forward = new Vec3(0.0f, 0.0f, 1.0f);
        up = new Vec3(0.0f, 1.0f, 0.0f);
        right = new Vec3(1.0f, 0.0f, 0.0f);

        yaw = 0.0f;
        pitch = 0.0f;
        roll = 0.0f;

        nearZ = 0.1f;
        farZ = 100.0f;

        distance = 3.0f;

        update();
    }

    public void update() {
        // Zavisno od trenutnog moda kamere, razlicite setove parametara uzimamo
        // kao ulaz i razlicite tretiramo kao izlaz, na kraju formirajuci
        // matrice.
        if (mode == GameCameraMode.GAMECAM_TARGETED) {
            distance = target.subtract(position).getLength();
            yaw = RGL.getAngleRad(position.x, position.z, target.x, target.z);
            pitch = RGL
                    .getAngleRad(0.0f, 0.0f, distance, target.y - position.y);
        } else if (mode == GameCameraMode.GAMECAM_FIRST_PERSON) {
            forward = new Vec3((float) (Math.cos(yaw) * Math.cos(pitch)),
                    (float) (Math.sin(pitch)),
                    (float) (Math.sin(yaw) * Math.cos(pitch)));

            target = position.add(forward);
        } else if (mode == GameCameraMode.GAMECAM_ORBIT) {
            position = target.add(new Vec3(distance * (float) Math.cos(yaw)
                    * (float) Math.cos(pitch), distance
                    * (float) Math.sin(pitch), distance * (float) Math.sin(yaw)
                    * (float) Math.cos(pitch)));
        }

        // Modovi kamere zapravo samo govore da li se target postavlja rucno ili
        // se
        // racuna po nekom pravilu, a na kraju uvijek koristimo position/target
        // za
        // glm::lookAt() poziv, kao i glm::perspective() za projection matricu.
        matProjection = Matrices.perspective(vfov * (1.0f / zoom),
                RGL.getAspectRatio(), nearZ, farZ);
        matView = Matrices.lookAt(position, target, orientationUp);
        matVP = matProjection.multiply(matView);

        matProjection.store(matBuffProjection);
        matBuffProjection.flip();

        matView.store(matBuffView);
        matBuffView.flip();

        matVP.store(matBuffVP);
        matBuffVP.flip();

        // Forward vektor je, logicno, normalizovana razlika mete i pozicije,
        // sto
        // nam daje vektor usmjerenja kamere
        forward = target.subtract(position).getUnitVector();

        // Vektore za gore i desno prosto dobijamo transformisuci jedinicne
        // vektore
        // po Y i X osama koristeci View matricu kamere, sto nam garantuje da ce
        // biti tacni, kakvu god transformaciju napravili
        Vec4 upVector4 = new Vec4(0.0f, 1.0f, 0.0f, 0.0f); // * matView;
        Vec4 rightVector4 = new Vec4(1.0f, 0.0f, 0.0f, 0.0f); // * matView;

        upVector4 = upVector4.multiply(matView); // matView.multiply(upVector4);
        rightVector4 = rightVector4.multiply(matView); // matView.multiply(rightVector4);

        up = new Vec3(upVector4.x, upVector4.y, upVector4.z);
        right = new Vec3(rightVector4.x, rightVector4.y, rightVector4.z);
    }

    public Vec3 projectToScreen(Vec3 worldPoint) {
        Vec4 tmp = new Vec4(worldPoint.x, worldPoint.y, worldPoint.z, 1.0f);
        tmp = tmp.multiplyTP(matVP);
        tmp = tmp.multiply((1.0f / tmp.w) * 0.5f);
        return new Vec3((tmp.x + 0.5f) * RGL.getWidth(),
                (1.0f - (tmp.y + 0.5f)) * RGL.getHeight(), (tmp.z + 0.5f));
    }

    public Vec3 getPixelViewVector(int x, int y) {
        // Tangensom racunamo world-space visinu pogleda na nearZ daljini,
        // uracunavajuci vertikalni field-of-view, zatim racunajuci i sirinu
        // jednostavnim mnozenjem sa proporcijom ekrana
        float Hnear = 2.0f
                * (float) Math
                .tan(((vfov * (1.0f / zoom)) * RGL.DEG_TO_RADf) * 0.5f)
                * nearZ;
        float Wnear = Hnear * RGL.getAspectRatio();

        // Normalizujemo 2D koordinate u pixelima u -0.5 do 0.5 opseg
        Vec2 screenSize = new Vec2((float) RGL.getWidth(),
                (float) RGL.getHeight());
        Vec2 scrPos = new Vec2((float) x - screenSize.x * 0.5f, (float) y
                - screenSize.y * 0.5f);
        scrPos = new Vec2(scrPos.x / screenSize.x, scrPos.y / screenSize.y);

        // Na poziciju kamere dodajemo forward vektor, pomnozen sa nearZ, sto
        // daje poziciju centra ekrana na nearZ daljini
        Vec3 nc = position.add(forward.multiply(nearZ));

        // Odave se pomijeramo desno i gore, po izracunatim proporcijama, kako
        // bismo dosli na polozaj koji odgovara trazenom pikeslu
        Vec3 res = nc.add(right.multiply(scrPos.x * Wnear))
                .add(up.multiply(-scrPos.y * Hnear)).subtract(position);

        // Na kraju vracamo normalizovanu razliku dobijene tacke i pozicije
        // kamere
        // kao trazeni vektor pravca
        return res.getUnitVector();
    }

    public void uploadUniforms(GameShader shader) {
        matView.store(matBuffView);
        matBuffView.flip();
        matProjection.store(matBuffProjection);
        matBuffProjection.flip();
        matVP.store(matBuffVP);
        matBuffVP.flip();

        // Ukoliko shader ima neke od poznatih uniform vrijednosti koje kamera
        // moze da ponudi, radimo upload glUniform pozivima

        if (shader.getUni_V() >= 0)
            glUniformMatrix4(shader.getUni_V(), false, matBuffView);

        if (shader.getUni_P() >= 0)
            glUniformMatrix4(shader.getUni_P(), false, matBuffProjection);

        if (shader.getUni_VP() >= 0)
            glUniformMatrix4(shader.getUni_VP(), false, matBuffVP);

        if (shader.getUni_camPosition() >= 0)
            glUniform3f(shader.getUni_camPosition(), position.x, position.y,
                    position.z);

        if (shader.getUni_camVector() >= 0)
            glUniform3f(shader.getUni_camVector(), forward.x, forward.y,
                    forward.z);
    }

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Vec3 getForward() {
        return forward;
    }

    public void setUp(Vec3 up) {
        this.up = up;
    }

    public Vec3 getRight() {
        return right;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public Mat4 getMatView() {
        return matView;
    }

    public Mat4 getMatVP() {
        return matVP;
    }

    public FloatBuffer getMatBuffVP() {
        return matBuffVP;
    }
}
