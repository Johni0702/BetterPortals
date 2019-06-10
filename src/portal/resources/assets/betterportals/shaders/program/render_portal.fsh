#version 120

uniform sampler2D sampler;
uniform vec2 screenSize;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize;
    gl_FragColor = vec4(texture2D(sampler, uv).rgb, 1.0);
    gl_FragDepth = gl_FragCoord.z;
}
