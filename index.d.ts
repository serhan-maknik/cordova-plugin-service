declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:object,
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
