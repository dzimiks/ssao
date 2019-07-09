#version 330
layout (location = 0) out vec4 gColor;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec3 gPosition;

uniform sampler2D uni_texture;

in vec2 var_UV;
in vec3 var_Normal;
in vec3 var_Position;

void main()
{
    vec4 finalColor = vec4(1.0);

    gPosition = var_Position;
    gNormal = normalize(var_Normal);
	gColor = finalColor;
}
