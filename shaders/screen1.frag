#version 330
// this shader if for light accumulation
uniform sampler2D g1;
uniform sampler2D g2;
uniform sampler2D g3;

uniform vec2 position;
uniform vec4 screen;
uniform float radius;

out vec4 glc;

in Data {
    vec2 screenPosition;
    vec2 uv;
} In;

float rand0(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float getFalloff(float dist){
    return max(1.0/(dist*dist + 1.0),0.0);
}

vec3 getNormalAt(vec2 pos){
    vec3 norm = normalize((texture(g3,pos).rbg - 0.5)*2.0);
    norm.z = -norm.z;
    return norm;
}

void main(){
    vec2 uv = In.uv;
    float scalar = 256.0;
    vec2 uvTiled = floor(uv*scalar)/scalar;
    vec3 specVal = texture(g2,uv).rgb;
    vec3 normal = getNormalAt(uv);

    vec3 lightPosition = vec3(position.x, 40.0, position.y);
    vec3 location = vec3(In.screenPosition.x, 0.0, screen.y - In.screenPosition.y);
    vec3 toLight = lightPosition - location;
    float distance = length(toLight);

    float nDotL =  max(0.0,dot(normalize(toLight),normal));
    float falloff = getFalloff(distance*0.1) - getFalloff(radius*0.1);
    vec3 diffuseComponent = vec3(nDotL * falloff);

    vec3 t =  normalize(vec3(0.0,1.0,0.0) + toLight);
    float specular = pow(max(0.0,dot(normal,t)),3.5);
    vec3 specularComponent = specular*specVal;



    glc = vec4(specularComponent + diffuseComponent,1.0);
}



