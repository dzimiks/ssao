#version 330

in vec2 var_UV;

uniform sampler2D ssao;
uniform sampler2D color;
uniform int ssaoMode;

out vec4 finalColor;

void main() {
    vec4 color = texture(color, var_UV);
	float occlusion = texture(ssao, var_UV).r;
    if (ssaoMode == 0) { // both
    	finalColor = color * occlusion;
    } else if (ssaoMode == 1) { // only color
    	finalColor = color;
    } else if (ssaoMode == 2) { // only occlusion mask
    	finalColor = vec4(occlusion, 0, 0, 1);
    } else { // fun
    	if (occlusion < 0.1) finalColor = vec4(1, 0, 0, 1); // occluded
    	else if (occlusion > 0.9) finalColor = vec4(0, 1, 0, 1); // free
    	else finalColor = vec4(0, 0, 1, 1);
    }
}
