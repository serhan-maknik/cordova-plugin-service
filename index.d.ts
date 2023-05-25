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
                    title:"",
                    body:"",
                    button:"",
                },
                enableLocation?:{
                    title:"",
                    body:"",
                    button:"",
                },
                forgroundPermission?:{
                    title:"",
                    body:"",
                    button:"",
                },
                backgroundPermission?:{
                    title:"",
                    body:"",
                    button:"",
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
