#version 330

// Colour grading for Tsunami's ColorSaturation module.
//
// One pass over the finished world frame. Every term is a pure function of the
// pixel it is given - nothing here samples a neighbour, reads depth or looks at
// the previous frame, so it cannot reveal anything the frame did not already
// contain.

uniform sampler2D InSampler;

layout(std140) uniform ColorGradingUniforms {
    float saturation;
    float vibrance;
    float contrast;
    float brightness;
    float gamma;
    float temperature;
    float tint;
    float padding0;
};

in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

// Rec. 709 luma. The green weight dominates because the eye does, which is why
// a naive (r+g+b)/3 desaturation turns foliage to mud.
const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;

    // Gamma first, while the signal is still untouched. Exponent is inverted so
    // that values above 1.0 read as "brighter", matching the slider's direction.
    c = pow(max(c, vec3(0.0)), vec3(1.0 / max(gamma, 0.01)));

    c *= brightness;

    // Contrast pivots on mid grey rather than black, so raising it does not also
    // raise exposure.
    c = (c - 0.5) * contrast + 0.5;

    // Temperature trades red against blue; tint moves green against both. Same
    // two axes a camera white balance uses.
    c.r *= 1.0 + temperature;
    c.b *= 1.0 - temperature;
    c.g *= 1.0 + tint;

    float luma = dot(c, LUMA);

    // Vibrance before saturation: it weights its boost by how grey the pixel
    // already is, so skin and sky move while an already-vivid block does not
    // clip. Plain saturation afterwards scales everything evenly.
    if (vibrance != 0.0) {
        float mx = max(c.r, max(c.g, c.b));
        float mn = min(c.r, min(c.g, c.b));
        c = mix(vec3(luma), c, 1.0 + vibrance * (1.0 - (mx - mn)));
        luma = dot(c, LUMA);
    }

    c = mix(vec3(luma), c, saturation);

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
