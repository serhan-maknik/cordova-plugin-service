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
 function start() {

   var httpHeaders = {
       "Accept": "application/json",
       "User-Agent": "Your-App-Name",
       "Cache-Control": "max-age=640000"
   }

   BackgroundService.start(
       {
           url: "http://192.168.1.62:3000/",
           header: httpHeaders,
           body: {
               SOS: {
                   module: "messenging",
                   action: "SOS",
                   args: { user_id: 1 }
               },
               location: {
                   module: "location",
                   action: "track",
                   args: { user_id: 1 }
               }
           },
           notification: {
               title: "Background Service is runnig",
               body: ""
           },
           toast: {
               start: "Basladi",
               stop: "Durduruldu"
           },
           // if the person denies the permissions these modals will appear
           permissions: { 
               // Battery optimization permission     
               batteryPermission: {
                   title: "Battery Permission ",
                   body: "Battery permission required for app to run in background",
                   button: "Settings"
               },
               // Enable location 
               enableLocation: {
                   title: "Enable GPS",
                   body: "GPS needs to be turned on",
                   button: "Allow"
               },
               // Location Forground permission
               // this permission is required!, before Background permission
               forgroundPermission: {
                   title: "Forground Location Permission",
                   body: "GPS permission is required.\t Go to settings and enable the permission.",
                   button: "Settings"
               },
               // Location Background permission
               backgroundPermission: {
                   title: "Background Location Permission",
                   body: "Background Application Permit Required!,\t Always allow on the next screen",
                   button: "Settings"
               },

           }
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
