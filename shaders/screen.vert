#version 330

uniform float time;
uniform vec4 screen;  // width,height,origin x, origin y
uniform vec2[] lights;
uniform vec2 position;

in vec4 in_Position;
in vec4 in_Color;
in vec2 in_uv;

out Data {
    vec2 screenPosition;
    vec4 color;
    vec2 uv;
} Out;

void main(){
    gl_Position = vec4(((position+in_Position.xy)/screen.xy)*2.0 - 1.0,0.0,1.0);
     gl_Position.y = -gl_Position.y;
    Out.screenPosition = in_Position.xy + position;
    Out.color = in_Color;
    Out.uv = in_uv;
}