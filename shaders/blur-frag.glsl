#version 330

in vec2 var_UV;
uniform sampler2D ssaoColor;

out float finalColor;

const int width = 2;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(ssaoColor, 0));
    float result = 0;
    for (int x = -width; x < width; ++x)
    {
        for (int y = -width; y < width; ++y)
        {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            result += texture(ssaoColor, var_UV + offset).r;
        }
    }
    float f = 2*width*2*width;
    finalColor = result/f;
}
