#version 330

uniform sampler2D g1;

out vec4 glc;

in Data {
    vec2 screenPosition;
    vec2 uv;
} In;

void main(){
    glc = texture(g1,In.uv);
}



