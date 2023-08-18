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
<button onclick="check()">Check</button>
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
                    url: "http://192.168.1.2:3000/",
                    header: httpHeaders, 
                    locationInterval :5*60*1000,
                    body: {
                        SOS: {
                            module: "messenging",
                            action: "SOS",
                            args: { user_id: 1 }
                        },
                        locationPost: {
                            module: "location",
                            action: "track",
                            args: { user_id: 1 }
                        },
                        gpsClosed: {
                            module: "location",
                            action: "gps_close",
                            args: { user_id: 4 }
                        },
                        mockLocation: {
                            module: "location",
                            action: "moclocation",
                            args: { user_id: 1 }
                        },
                    },
                 
                    autoStartVideo:{
                        url : "https://www.goonex.app/attachments/autostart__1691058086843.mp4",
                        closeButton: "Kapat"
                    },

                    notification: {
                        title: "Background Service is runnig",
                        body: ""
                    },
                    toast: {
                        start: "Basladi",
                        stop: "Durduruldu"
                    },
                    cancelShakeDialog:{
                        title:"SOS",
                        body:"Emergency message activated",
                        button:"Cancel",
                        remainingTime:"Remaining time",
                        duration:10 //second
                    },
                    
                    // Permission modal text
                    permissions: { 
                        // Battery optimization permission     
                        batteryPermission: {
                            title: "Battery Permission",
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
                            title: "Allow background permissions",
                            body: "Change your location settings to 'Allow all the time' to try out background tracking for when a user may not have the app open",
                            positiveButton: "Allow background permission",
                            negativeButtton :"Not now"
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

 function check(){
      BackgroundService.checkService(
      function(data){
        console.log(data);
      },function(data){
          console.log(data);
      })
}


function changeInterval(){
        BackgroundService.changeLocationInterval({
            interruptionInterval:10000 // milliseconds
            interruptionDuration: 60*1000 // milliseconds
        },
        function(a){},
        function(b){})
}

```
