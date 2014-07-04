#version 330

uniform float time;
uniform vec4 screen;  // width, height, origin x, origin y
uniform vec2[] lights;
uniform vec2 position;
uniform vec2 uvPosition;
uniform vec2 uvScalars;

uniform sampler2D norm;
uniform sampler2D tex;
uniform sampler2D specular;

out vec4 glc;

in Data {
    vec2 screenPosition;
    vec4 color;
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
    vec2 scp = In.screenPosition;

    vec3 screenPosition = vec3(scp.x, 0.0, scp.y);
    vec3 specVal = texture(specular,uv).rgb;

    float specInt = rand0(position*4 + floor(uv*scalar)/scalar)*2.0;

    float ax = rand0(position + floor(uv*scalar)/scalar)*2.0 - 1.0;
    float ay = rand0(position*2 + floor(uv*scalar)/scalar)*2.0 - 1.0;
    float az = rand0(position*3 + floor(uv*scalar)/scalar)*2.0 - 1.0;

    vec3 normal = normalize(getNormalAt(uv));//+vec3(ax,ay,az)*(specVal*specVal)*1.5);
    vec4 color  = texture(tex,uv);

    vec3 result = vec3(0.0);
    for(int i = 0 ; i < 1 ; i++){
        vec2 lightZero = lights[0];
        if(i==0){
            lightZero = lights[0];
        }
        if(i==1){
            lightZero = lights[1];
        }
        if(i==2){
            lightZero = lights[2];
        }
        if(i==3){
            lightZero = lights[3];
        }

        vec3 lightPosition = vec3(lightZero.x, 40.0, lightZero.y);
        vec3 toLight = floor(lightPosition/8.0)*8.0 - floor(screenPosition/8.0)*8.0;

        float lengthToLight = length(toLight)*0.01;
        float falloff = getFalloff(lengthToLight);
        float nDotL =  max(0.0,dot(normalize(toLight),normal));

        vec3 t =  normalize(vec3(0.0,1.0,0.0) + toLight);
        float specular = pow(max(0.0,dot(normal,t)),3.5);

        //nDotL = floor(nDotL*4.0)/4.0;
        specular = floor(specular*4.0)/4.0;

        vec3 l = vec3(nDotL);//specular*specVal*specInt + nDotL * falloff * color.rgb;
        result += l;
    }

    vec3 ambient = vec3(0.2,0.1,0.2)*0.2;
    result = result + ambient;
    glc = vec4(result, 1.0);
}



