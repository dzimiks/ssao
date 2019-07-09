#version 330

in vec2 var_UV;  // texture coordinates

uniform mat4 uni_P;  // projection matrix

//uniform sampler2D gColor;  // gBuffer - color channel - UNUSED
uniform sampler2D gNormal;  // gBuffer - normal channel (view space)
uniform sampler2D gPosition;  // gBuffer - position channel (view space)
uniform sampler2D noise;  // random rotations for kernel 4x4
uniform sampler2D kernel;  // 8 x 8 random vectors in a hemisphere

uniform float ssaoRadius;
uniform float ssaoBias;
uniform float ssaoPower;

out float finalOcclusion;  // output

// tile noise texture over screen
const vec2 noiseScale = vec2(1280.0/4.0, 720.0/4.0);  // screen = 1280x720
const float kernelSize = 64;

void main()
{
	// get this pixel position, normal and random noise
    vec3 position = texture(gPosition, var_UV).xyz;
    vec3 normal = texture(gNormal, var_UV).rgb;
    vec3 randomVec = texture(noise, var_UV * noiseScale).xyz;

    // create tangent space -> view space transformation
    vec3 tangent   = normalize(randomVec - normal * dot(randomVec, normal));
    vec3 bitangent = cross(normal, tangent);
    mat3 TBN       = mat3(tangent, bitangent, normal);

    // SSAO parameters
    float occlusion = 0.0;  // total summed occlusion

    // use the whole kernel
	for(int i = 0; i < kernelSize; i++) {
		// extract the kernel sample
		vec2 kernelCoords = vec2(i/8, i%8);  // make a const
		vec3 tsSample = texture(kernel, kernelCoords).xyz;  // tangent space
		vec3 sampleT = TBN * tsSample;  // transform sample to view space
		sampleT = position + sampleT * ssaoRadius; // find the pixel to sample

		// project sample to clip space
		vec4 offset = vec4(sampleT, 1.0);
		offset      = uni_P * offset;  // from view to clip-space
		offset.xyz /= offset.w;  // perspective divide
		offset.xyz  = offset.xyz * 0.5 + 0.5;  // transform to range 0.0 - 1.0

		// finally, sample depth from position
		float sampleDepth = texture(gPosition, offset.xy).z;

		// if they are too far we should ignore this
		float ratio = ssaoRadius / abs(position.z - sampleDepth);
		float rangeCheck = smoothstep(0.0, 1.0, ratio);

		// we moved towards light in a hemisphere and we know
		// our view space z (sample.z), if there is something
		// 'in' from of us (sampleDepth), add occlusion
		if (sampleDepth >= sampleT.z + ssaoBias) {
			occlusion += 1.0 * rangeCheck;
		}
	}
    // calculate the ratio
    float occlusionFactor = 1.0 - (occlusion / kernelSize);
    // add power => make occlusion stronger
    // occlusionFactor = pow(occlusionFactor, ssaoPower);
    // return final color
    finalOcclusion = occlusionFactor;
}
