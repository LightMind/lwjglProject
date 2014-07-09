#version 330

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