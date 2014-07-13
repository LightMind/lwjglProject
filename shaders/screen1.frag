#version 330
// this shader if for light accumulation
uniform sampler2D g1;
uniform sampler2D g2;
uniform sampler2D g3;

uniform vec2 position;
uniform vec4 screen;
uniform float radius;
uniform vec3 lightColor;
uniform float lightIntensity;

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

vec2 textureToScreen(vec2 p) {
    return p*screen.xy;
}

vec2 screenToTexture(vec2 p) {
    return vec2(p.x,-p.y)/screen.xy;
}

void main(){
    bool rasterize = true;

    float heightFactor = 3.0;

    vec2 uv = In.uv;
    float scalar = 256.0;
    vec2 uvTiled = floor(uv*scalar)/scalar;
    vec3 specVal = texture(g2,uv).rgb;
    vec3 normal = getNormalAt(uv);
    float height = texture(g1,uv).w;

    vec3 lightPosition = vec3(position.x, 50.0, position.y);
    vec3 location = vec3(In.screenPosition.x, height, In.screenPosition.y);
    vec3 toLight = lightPosition - location;

    vec3 toLightTexture = vec3(toLight);

    vec3 rLocation = floor(location/8.0)*8.0;
    vec3 rLight = floor(lightPosition/8.0)*8.0;

    if( rasterize ) {
        toLight = rLight - rLocation;
    }

    float distance = length(toLight);

    float nDotL =  max(0.0,dot(normalize(toLight),normal));
    float falloff = getFalloff(distance*0.01) - getFalloff(radius*0.01);

    vec3 t =  normalize(vec3(0.0,1.0,0.0) + toLight);
    float specular = pow(max(0.0,dot(normal,t)),10.5);

    if( rasterize ){
        nDotL = floor(nDotL*4.0)/4.0;
        specular = floor(specular*4.0)/4.0;
    }

    vec3 ntoLight = normalize(toLight);

    bool shadows = false;
    float shadow = 1.0;

    if(shadows){
        float dHeight = (lightPosition.y - location.y)/length(toLightTexture);
        vec2 tmov = normalize(vec2(toLightTexture.x,toLightTexture.z));
        for( int i = 1; i < 8; i++ ){
            float currentHeight = location.y + dHeight*i;
            vec2 currentPosition = location.xz + tmov*i*2;
            float heightSample = texture(g1,screenToTexture(currentPosition)).w;
            if(currentHeight < heightSample){
                float t = heightSample - currentHeight;
                shadow = min(shadow,1-t);
            }
        }
    }

    bool ao = false;
    float aof = 1.0;
    float counter = 0.0;
    if(ao){
        float h1 = texture(g1,screenToTexture(location.xz + vec2(1,1)*4)).w;
        float h2 = texture(g1,screenToTexture(location.xz + vec2(1,0)*4)).w;
        float h3 = texture(g1,screenToTexture(location.xz + vec2(1,-1)*4)).w;
        float h4 = texture(g1,screenToTexture(location.xz + vec2(-1,1)*4)).w;
        float h5 = texture(g1,screenToTexture(location.xz + vec2(-1,0)*4)).w;
        float h6 = texture(g1,screenToTexture(location.xz + vec2(-1,-1)*4)).w;
        float h7 = texture(g1,screenToTexture(location.xz + vec2(0,1 )*4)).w;
        float h8 = texture(g1,screenToTexture(location.xz + vec2(0,-1)*4)).w;

        if(h1 > height) counter += h1-height ;
        if(h2 > height) counter += h2-height ;
        if(h3 > height) counter += h3-height ;
        if(h4 > height) counter += h4-height ;
        if(h5 > height) counter += h5-height ;
        if(h6 > height) counter += h6-height ;
        if(h7 > height) counter += h7-height ;
        if(h8 > height) counter += h8-height ;

        aof = 1.0 - min(1.0,1.5*counter/8.0);
        aof = max(0.1 , aof);
    }

    vec3 diffuseComponent = aof*vec3(shadow *nDotL * falloff) * texture(g1,uv).rgb *lightColor*lightIntensity;
    vec3 specularComponent = specVal*specular*lightColor*lightIntensity*falloff*6;

    glc = vec4(specularComponent + diffuseComponent,1.0);
    //glc = vec4(aof,aof,aof,1.0);
}



