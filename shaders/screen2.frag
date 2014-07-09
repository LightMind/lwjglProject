#version 330

uniform sampler2D lightBuffer;
uniform sampler2D diffuseBuffer;

out vec4 glc;

in Data {
    vec2 screenPosition;
    vec2 uv;
} In;

void main(){
    vec4 ambient = vec4(0.2,0.2,0.3,1.0)*0.2;
    vec4 diffuse = texture(diffuseBuffer,In.uv);
    vec4 light = texture(lightBuffer,In.uv);
    glc = light + ambient*diffuse;
}



