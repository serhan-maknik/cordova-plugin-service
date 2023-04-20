# cordova-plugin-service

## Install
   ```
   cordova plugin add cordova-plugin-service
   ```
## Html
```html
<meta http-equiv="Content-Security-Policy"
        content=" style-src 'self' 'unsafe-inline'; media-src *; img-src 'self' data: content:;">

<button onclick="start()">Start</button>
<button onclick="stop()">Stop</button>

```

## javascript

```js
function start(){
    BackgroundService.start('start',function(a){
        console.log(a);
    },
    function(a){
        console.log(a);
    })
}

function stop(){
    BackgroundService.stop('stop',function(a){
        console.log(a);
    },
    function(a){
        console.log(a);
    })
}
```
