#version 330
// this shader if for light accumulation
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
    glc = vec4(texture(g2,uv).w,texture(g3,uv).w,0.0,1.0);
}



