uniform float time;

in Data {
    vec2 texCoord;
} DataIn;

float a(float b){
    return (floor(mod(b, 8.0))/8.0 );
}

void main(){
    vec2 tc = DataIn.texCoord*0.5 + 0.5;
    float sintime = sin(time) + 1.0;

    float red = 0.4 + cos(tc.x*10 + time*0.5) * sintime*0.5;

    red = floor(red*8.0)/8.0;
    float green = floor(tc.y*12.0)/12.0 * floor(red*8.0)/8.0;
    gl_FragColor = vec4(red,green,0.0,1.0);
}

