#version 330
in vec4 in_Position;

uniform vec2 position;
uniform vec4 screen;
uniform float radius;

out Data {
    vec2 screenPosition;
    vec2 uv;
} Out;

void main(){
    gl_Position = vec4(((position+in_Position.xy*radius)/screen.xy)*2.0 - 1.0,0.0,1.0);
    gl_Position.y = -gl_Position.y;
    Out.screenPosition = (gl_Position.xy*0.5 +0.5) * screen.xy;
    Out.uv = gl_Position.xy*0.5 +0.5;
}