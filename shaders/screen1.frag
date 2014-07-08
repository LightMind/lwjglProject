#version 330

uniform float time;
uniform vec4 screen;
uniform vec2[] lights;
uniform vec2 position;

uniform sampler2D g1;
uniform sampler2D g2;
uniform sampler2D g3;

out vec4 glc;

in Data {
    vec2 screenPosition;
    vec2 uv;
} In;

void main(){
    vec2 uv = In.uv;
    glc = texture(g1,uv);
}



