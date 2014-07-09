#version 330

uniform float time;
uniform vec4 screen;  // width, height, origin x, origin y
uniform vec2 position;
uniform vec2 uvPosition;
uniform vec2 uvScalars;

uniform sampler2D norm;
uniform sampler2D tex;
uniform sampler2D specular;
uniform sampler2D heightMap;

layout(location = 0) out vec4 g1;
layout(location = 1) out vec4 g2;
layout(location = 2) out vec4 g3;

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
    vec3 norm = normalize((texture(norm,vec2(pos.x,pos.y)).rbg - 0.5)*2.0);
    norm.z = -norm.z;
    return norm;
}

void main(){
    float scalar = 256.0;

    vec2 uv = In.uv;
    vec2 uvTiled = floor(uv*scalar)/scalar;

    vec2 scp = In.screenPosition;

    vec3 screenPosition = vec3(scp.x, 0.0, scp.y);
    vec3 specVal = texture(specular,uv).rgb;

    float ax = rand0(position   + uvTiled)*2.0 - 1.0;
    float ay = rand0(position*2 + uvTiled)*2.0 - 1.0;
    float az = rand0(position*3 + uvTiled)*2.0 - 1.0;

    vec3 normal = texture(norm,uv).rgb;;

    bool randomizeNormals = false;

    if(randomizeNormals){
        normal = normalize(normal + vec3(ax,ay,az)*(specVal*specVal)*0.0);
    }

    vec4 color  = texture(tex,uv);
    float height = texture(heightMap,uv).x;

    g1 = vec4(color.rgb,height);
    g2 = vec4(specVal.rgb, uvTiled.x);
    g3 = vec4(normal.xyz, uvTiled.y);
}