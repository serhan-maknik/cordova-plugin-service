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
            autoStartVideo:{
                url?:string,
                closeButton?:string,
            }
            
            notification?:{
                title?:string,
                body?:string,
            },
            toast?:{
                start?:string,
                stop?:string
            },
            cancelShakeDialog: {
                title?: string,
                body?: string,
                button?: string,
                remainingTime?: string,
                duration?: number //second
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

    checkStatus(
        callback:(status:{
            isRunning:boolean
        })=>void,
        fallback:(error:Error)=>void,
    ):void,

    changeLocationInterval(
        options:{
            /**
             * in milliseconds
             */
            interruptionInterval:number,
            interruptionDuration:number,
        },
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
