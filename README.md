# cordova-plugin-service

## cordova plugin add cordova-plugin-service

```html
<meta http-equiv="Content-Security-Policy"
        content=" style-src 'self' 'unsafe-inline'; media-src *; img-src 'self' data: content:;">

```

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
