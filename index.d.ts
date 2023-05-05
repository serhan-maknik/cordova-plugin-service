declare const BackgroundService:BackgroundService;


interface BackgroundService {
    start(
        options:{
            url:string,
            header?:object,
            body?:object,
            notification:{
                title?:string,
                body?:string,
            },
            toast?:{
                start?:string,
                stop?:string
            },
        },
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,

    stop(
        callback:()=>void,
        fallback:(error:Error)=>void,
    ):void,
}
