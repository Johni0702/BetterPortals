#version 120

uniform sampler2D sampler;
uniform vec2 screenSize;
uniform float fogDensity;
uniform vec3 fogColor;

void main() {
    vec2 uv = gl_FragCoord.xy / screenSize;
    vec3 color = texture2D(sampler, uv).rgb;
    color = mix(color, gl_Fog.color.rgb, clamp((gl_FogFragCoord - gl_Fog.start) * gl_Fog.scale, 0.0, 1.0));
    color = mix(color, fogColor, fogDensity);
    gl_FragColor = vec4(color, 1.0);
    gl_FragDepth = gl_FragCoord.z;
}
