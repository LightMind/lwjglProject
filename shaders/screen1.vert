uniform float time;

out Data {
    vec2 texCoord;
} DataOut;

const vec2 madd=vec2(0.5,0.5);

void main(){
    gl_Position = gl_Vertex;
    DataOut.texCoord = gl_Position.xy;
}