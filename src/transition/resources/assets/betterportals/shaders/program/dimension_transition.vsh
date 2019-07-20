#version 120

attribute vec3 pos;

varying vec2 vpos;

void main() {
    vpos = pos.xy;
    gl_Position = vec4(vpos, 0.0, 1.0);
}
