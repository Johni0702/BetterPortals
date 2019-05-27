#version 120

uniform sampler2D sampler;
uniform sampler2D depthSampler;
uniform vec2 screenSize;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize;
    gl_FragColor = texture2D(sampler, uv);
    //gl_FragDepth = texture2D(depthSampler, uv).r;
    gl_FragDepth = gl_FragCoord.z;
}
