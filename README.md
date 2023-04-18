# cordova-plugin-service

cordova plugin add C:\Users\USER\Downloads\cordova-plugin-service\cordova-plugin-service

## javascript

```js
function start(){
    ServicePlugin.start('start',function(a){
        console.log(a);
    },
    function(a){
        console.log(a);
    })
}

function stop(){
    ServicePlugin.stop('stop',function(a){
        console.log(a);
    },
    function(a){
        console.log(a);
    })
}
```
