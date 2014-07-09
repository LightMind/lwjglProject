#version 330
in vec4 in_Position;
in vec2 in_uv;

uniform vec4 screen;

out Data {
    vec2 screenPosition;
    vec2 uv;
} Out;

void main(){
    gl_Position = in_Position;
    Out.screenPosition = gl_Position.xy;
    Out.uv = in_uv;
}