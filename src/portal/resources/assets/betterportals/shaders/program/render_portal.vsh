#version 110

void main() {
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    gl_ClipVertex = viewPos;
    gl_Position = gl_ProjectionMatrix * viewPos;
}
