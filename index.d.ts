declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:{
                SOS?:BackgroundServiceBody,
                locationPost?:BackgroundServiceBody,
                gpsClosed?:BackgroundServiceBody,
                mockLocation?:BackgroundServiceBody,
            },
            /**
             * geolocation refresh interval in milliseconds
             */
            locationInterval:number,
            notification?:{
                title?:string,
                body?:string,
            },
            toast?:{
                start?:string,
                stop?:string
            },
            permissions?:{
                batteryPermission?:BackgroundServicePermission,
                enableLocation?:BackgroundServicePermission,
                forgroundPermission?:BackgroundServicePermission,
                backgroundPermission?:BackgroundServicePermission,
            }
        },
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,

    stop(
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,
}

type BackgroundServiceBody={
    module:string,
    action:string,
    args:object,
}
type BackgroundServicePermission={
    title?:string,
    body?:string,
    button?:string,
}
