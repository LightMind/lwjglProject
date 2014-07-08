#version 330

uniform float time;
uniform vec4 screen;  // width,height,origin x, origin y
uniform vec2[] lights;

in vec4 in_Position;
in vec2 in_uv;

out Data {
    vec2 screenPosition;
    vec2 uv;
} Out;

void main(){
    gl_Position = in_Position;
    Out.screenPosition = in_Position.xy;
    Out.uv = in_uv;
}