#version 330

uniform float time;
uniform vec4 screen;  // width,height,origin x, origin y
uniform vec2 position;
uniform vec2 uvPosition;
uniform vec2 uvScalars;
uniform float depth;

in vec4 in_Position;
in vec2 in_uv;

out Data {
    vec2 screenPosition;
    vec2 uv;
} Out;

void main(){
    vec2 aPosition = floor(position/4.0)*4.0;
    gl_Position = vec4(((aPosition+in_Position.xy)/screen.xy)*2.0 - 1.0,depth,1.0);
    gl_Position.y = -gl_Position.y;
    Out.screenPosition = in_Position.xy + aPosition;
    Out.uv = uvPosition * uvScalars + in_uv * uvScalars;
}