#version 330

uniform sampler2D uni_texture;

uniform vec3 uni_lightColor1;
uniform vec3 uni_lightVector1;
uniform vec3 uni_ambient;
uniform vec3 uni_camPosition;

in vec2 var_UV;
in vec3 var_Normal;
in vec3 var_Position;

out vec4 finalColor;

void main()
{
	vec3 uni_objectColor = vec3(1.0, 1.0, 1.0);
    vec3 transformedNormal = normalize(var_Normal);
    float lightFactor = clamp(dot(transformedNormal, uni_lightVector1), 0, 1);
    vec3 color = (uni_ambient * uni_objectColor) + (uni_objectColor * (uni_lightColor1 * lightFactor));

    vec3 viewVector = normalize(var_Position - uni_camPosition);
    float specFactor = dot(reflect(viewVector, transformedNormal), uni_lightVector1);
    specFactor = clamp(specFactor, 0, 1);
    specFactor = pow(specFactor, 5.0);
    color += uni_lightColor1 * specFactor;

	finalColor = texture(uni_texture, var_UV) * vec4(color, 1.0);
}
