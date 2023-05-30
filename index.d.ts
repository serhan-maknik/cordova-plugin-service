declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:{
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
                    args: { user_id: 1 }
                },
                mockLocation: {
                    module: "location",
                    action: "moclocation",
                    args: { user_id: 1 }
                },
            },
            locationInterval:number, //millisecond ex: 5*60*1000 = 5 minutes
            notification?:{
                title?:string,
                body?:string,
            },
            toast?:{
                start?:string,
                stop?:string
            },
            
            permissions?:{
                batteryPermission?:{
                    title?:string,
                    body?:string,
                    button?:string,
                },
                enableLocation?:{
                    title?:string,
                    body?:string,
                    button?:string,
                },
                forgroundPermission?:{
                    title?:string,
                    body?:string,
                    button?:string,
                },
                backgroundPermission?:{
                    title?:string,
                    body?:string,
                    button?:string,
                }
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
