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

      var httpHeaders = {
          "Accept": "application/json",
          "User-Agent": "Your-App-Name",
          "Cache-Control": "max-age=640000"
      }

      BackgroundService.start(
      {
           url: "http://185.230.138.93:3040",
           header: httpHeaders,
           body: {
               module:"messenging",
               action:"SOS",
               args:{user_id:1}
           },
      },
      function (data) {
          console.log(data);
      },
      function (error) {
           console.log(error);
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
